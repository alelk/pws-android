package io.github.alelk.pws.android.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PwsApplication : Application() {
  override fun onCreate() {
    super.onCreate()
  }
}