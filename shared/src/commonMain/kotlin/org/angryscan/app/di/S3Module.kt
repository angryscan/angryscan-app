package org.angryscan.app.di

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.angryscan.app.scan.common.connectors.S3ViewModel

val s3Module = module {
    viewModelOf(::S3ViewModel)
}