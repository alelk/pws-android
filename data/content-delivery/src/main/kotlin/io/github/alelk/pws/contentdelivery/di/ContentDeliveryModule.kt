package io.github.alelk.pws.contentdelivery.di

import io.github.alelk.pws.contentdelivery.ContentKeyProvider
import io.github.alelk.pws.contentdelivery.catalog.BookCatalogRepositoryImpl
import io.github.alelk.pws.contentdelivery.install.BookImporterImpl
import io.github.alelk.pws.contentdelivery.install.BookUninstallerImpl
import io.github.alelk.pws.contentdelivery.install.InstallBookUseCaseImpl
import io.github.alelk.pws.contentdelivery.install.UninstallBookUseCaseImpl
import io.github.alelk.pws.database.PwsDatabase
import io.github.alelk.pws.domain.booklibrary.repository.BookCatalogRepository
import io.github.alelk.pws.domain.booklibrary.usecase.InstallBookUseCase
import io.github.alelk.pws.domain.booklibrary.usecase.UninstallBookUseCase
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.bind
import org.koin.dsl.module

fun contentDeliveryModule(
    catalogUrl: String,
    bundleVariant: String,
    keyProvider: ContentKeyProvider,
) = module {

    single {
        HttpClient(CIO) {
            install(HttpRedirect)
            install(HttpTimeout) {
                connectTimeoutMillis = 15_000
                requestTimeoutMillis = 120_000
            }
        }
    }

    single {
        BookCatalogRepositoryImpl(catalogUrl, bundleVariant, get<HttpClient>())
    } bind BookCatalogRepository::class

    single { BookImporterImpl(get<PwsDatabase>()) }

    single { BookUninstallerImpl(get<PwsDatabase>()) }

    single {
        InstallBookUseCaseImpl(
            context = androidContext(),
            importer = get<BookImporterImpl>(),
            keyProvider = keyProvider,
            httpClient = get<HttpClient>(),
        )
    } bind InstallBookUseCase::class

    single {
        UninstallBookUseCaseImpl(get<BookUninstallerImpl>())
    } bind UninstallBookUseCase::class
}
