package io.github.alelk.pws.android.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.alelk.pws.database.BuildConfig
import timber.log.Timber
import timber.log.Timber.DebugTree

@HiltAndroidApp
class PwsApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    if (BuildConfig.DEBUG) Timber.plant(DebugTree())
  }
}