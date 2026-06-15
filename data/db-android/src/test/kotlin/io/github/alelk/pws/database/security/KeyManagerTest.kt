package io.github.alelk.pws.database.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import br.com.colman.kotest.FeatureSpec
import br.com.colman.kotest.android.extensions.robolectric.RobolectricTest
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch

@RobolectricTest(sdk = 34)
class KeyManagerTest : FeatureSpec({

  fun freshPrefs(name: String = "test_key_manager_${System.nanoTime()}") =
    ApplicationProvider.getApplicationContext<Context>()
      .getSharedPreferences(name, Context.MODE_PRIVATE)

  feature("getOrCreatePassphrase") {

    scenario("first call generates a 64-char lowercase hex passphrase") {
      val passphrase = KeyManager.getOrCreatePassphrase(freshPrefs())
      passphrase.toString(Charsets.UTF_8) shouldMatch Regex("[0-9a-f]{64}")
    }

    scenario("passphrase is exactly 64 bytes (hex-encoded 32-byte key)") {
      val passphrase = KeyManager.getOrCreatePassphrase(freshPrefs())
      passphrase.size shouldBe 64
    }

    scenario("second call on the same prefs returns the identical passphrase") {
      val prefs = freshPrefs()
      val first = KeyManager.getOrCreatePassphrase(prefs).toString(Charsets.UTF_8)
      val second = KeyManager.getOrCreatePassphrase(prefs).toString(Charsets.UTF_8)
      first shouldBe second
    }

    scenario("independent prefs produce different passphrases") {
      val a = KeyManager.getOrCreatePassphrase(freshPrefs()).toString(Charsets.UTF_8)
      val b = KeyManager.getOrCreatePassphrase(freshPrefs()).toString(Charsets.UTF_8)
      a shouldNotBe b
    }

    scenario("passphrase is not all zeroes") {
      val passphrase = KeyManager.getOrCreatePassphrase(freshPrefs()).toString(Charsets.UTF_8)
      passphrase shouldNotBe "0".repeat(64)
    }
  }
})
