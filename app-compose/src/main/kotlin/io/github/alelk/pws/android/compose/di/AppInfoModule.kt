package io.github.alelk.pws.android.compose.di

import io.github.alelk.pws.features.app.PwsAppInfo
import org.koin.dsl.module

internal fun appInfoModule(appVersion: String) = module {
  single { PwsAppInfo(appVersion) }
}
