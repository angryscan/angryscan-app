package org.angryscan.app.scan.functions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.angryscan.common.engine.IScanEngine
import org.bouncycastle.asn1.pkcs.ContentInfo
import org.bouncycastle.asn1.pkcs.SignedData
import org.angryscan.app.scan.common.Document
import org.angryscan.app.scan.common.files.FileType
import java.io.File
import java.io.FileInputStream
import java.security.cert.CertificateFactory
import kotlin.coroutines.CoroutineContext

enum class CertFileType(val extensions: List<String>) {
    ASCII(listOf("ovpn", "pem", "crt", "cer", "key", "ca-bundle")),
    PKCS(listOf("p7b", "p7s", "der")),
    KEYSTORE(listOf("pfx", "p12", "keystore", "jks"))
    ;

    suspend fun scanFile(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean
    ): Document = when (this) {
        ASCII -> scanASCII(file, context, engines, fastScan)
        PKCS -> scanPKCS(file, context, engines, fastScan)
        KEYSTORE -> scanKeyStore(file)
    }

    private suspend fun scanASCII(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean
    ): Document {
        return FileType.Text.scanFile(
            file,
            context,
            engines,
            fastScan
        )
    }

    private fun scanKeyStore(file: File): Document {
        return Document(file.length(), file.absolutePath).also { it + (CertDetectFun to 1) }
    }

    private suspend fun scanPKCS(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean
    ): Document {
        val factory = CertificateFactory.getInstance("X.509")
        val res = Document(file.length(), file.absolutePath)
        try {
            withContext(Dispatchers.IO) {
                val count: Int = FileInputStream(file).use { fis ->
                    when (file.extension) {
                        "p7b", "der" -> {
                            factory.generateCertificates(fis).size
                        }
                        "p7s" -> {
                            val signedData = SignedData.getInstance(ContentInfo.getInstance(file.readBytes()).content)
                            signedData.certificates.size()
                        }
                        else -> {
                            1
                        }
                    }


                }
                if(count > 0)
                    res + (CertDetectFun to count)
                else {
                    res + scanASCII(
                        file,
                        context,
                        engines,
                        fastScan
                    ).getDocumentFields()
                }
            }
        } catch (_: Exception) {
            res + scanASCII(
                file,
                context,
                engines,
                fastScan
            ).getDocumentFields()
            return res
        }
        return  res
    }
}