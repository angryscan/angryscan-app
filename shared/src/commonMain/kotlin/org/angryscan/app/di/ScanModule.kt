package org.angryscan.app.di

import org.angryscan.app.common.SavedSqlConnectionsRepository
import org.angryscan.app.db.DatabaseConnector
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.TaskFilesViewModel
import org.angryscan.app.scan.TasksViewModel
import org.angryscan.app.scan.functions.rkn.DomainRepository
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val scanModule = module {
    singleOf(::DatabaseConnector)
    singleOf(::TasksViewModel)
    singleOf(::ScanService)
    singleOf(::DomainRepository)
    singleOf(::SavedSqlConnectionsRepository)
    factoryOf(::TaskFilesViewModel)
}