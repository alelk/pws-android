package io.github.alelk.pws.domain.model

import io.github.alelk.pws.domain.model.serialization.DefaultVersionSerializer
import kotlinx.serialization.Serializable

@Serializable(with = DefaultVersionSerializer::class)
data class Version(
  val major: Int,
  val minor: Int
) {
  init {
    require(major >= 0) { "Major version must be greater or equal to 0" }
    require(minor >= 0) { "Minor version must be greater or equal to 0" }
  }

  override fun toString() = "$major.$minor"

  companion object {
    fun fromString(version: String): Version {
      val parts = version.split('.')
      require(parts.size == 2) { "Invalid version format: $version" }
      return Version(parts[0].toInt(), parts[1].toInt())
    }
  }

  fun nextMinor() = copy(minor = minor + 1)
}