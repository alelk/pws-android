package io.github.alelk.pws.android.compose.di

import org.koin.core.qualifier.named
import org.koin.dsl.module

internal val deviceLanguageModule = module {
  single(named("deviceLanguage")) { java.util.Locale.getDefault().language }
}
