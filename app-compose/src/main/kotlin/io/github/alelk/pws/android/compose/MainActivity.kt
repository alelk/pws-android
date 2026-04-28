package io.github.alelk.pws.android.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.alelk.pws.features.app.AppRoot

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    setContent {
      AppRoot()
    }
  }
}
