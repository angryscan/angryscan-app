package org.angryscan.app.scan.common.files.types

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.angryscan.common.engine.IScanEngine
import org.bouncycastle.asn1.pkcs.ContentInfo
import org.bouncycastle.asn1.pkcs.SignedData
import org.angryscan.app.scan.common.Document
import org.angryscan.app.scan.functions.CertDetectFun
import java.io.File
import java.io.FileInputStream
import java.security.cert.CertificateFactory
import kotlin.coroutines.CoroutineContext

private val logger = KotlinLogging.logger { }

@Serializable
enum class CertFileType: IFileType {
    @Serializable
    ASCII {
        override val extensions = listOf("ovpn", "pem", "crt", "cer", "key", "ca-bundle")
    },
    @Serializable
    PKCS {
        override val extensions = listOf("p7b", "p7s", "der")
    },
    @Serializable
    KEYSTORE {
        override val extensions = listOf("pfx", "p12", "keystore", "jks")
    }
    ;

    override suspend fun scanFile(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean,
        selectedExtensions: List<IFileType>
    ): Document = when (this) {
        ASCII -> scanASCII(file, context, engines, fastScan, selectedExtensions)
        PKCS -> scanPKCS(file, context, engines, fastScan, selectedExtensions)
        KEYSTORE -> scanKeyStore(file)
    }

    private suspend fun scanASCII(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean,
        selectedExtensions: List<IFileType>
    ): Document {
        return TextType.scanFile(
            file,
            context,
            engines,
            fastScan,
            selectedExtensions
        )
    }

    private fun scanKeyStore(file: File): Document {
        return Document(file.length(), file.absolutePath).also { it + (CertDetectFun to 1) }
    }

    private suspend fun scanPKCS(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean,
        selectedExtensions: List<IFileType>
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
                        fastScan,
                        selectedExtensions
                    ).getDocumentFields()
                }
            }
        } catch (e: Exception) {
            logger.error { "Error while scanning pkcs ${file.absolutePath}: ${e.message}" }
            res + scanASCII(
                file,
                context,
                engines,
                fastScan,
                selectedExtensions
            ).getDocumentFields()
            return res
        }
        return  res
    }
}