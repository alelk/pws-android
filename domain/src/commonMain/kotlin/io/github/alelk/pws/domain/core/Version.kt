package io.github.alelk.pws.domain.core

import kotlinx.serialization.Serializable

@Serializable(with = VersionSerializer::class)
data class Version(
  val major: Int,
  val minor: Int
): Comparable<Version> {
  init {
    require(major >= 0) { "Major version must be greater or equal to 0" }
    require(minor >= 0) { "Minor version must be greater or equal to 0" }
  }

  override fun toString() = "$major.$minor"

  override fun compareTo(other: Version): Int = compareValuesBy(this, other, Version::major, Version::minor)

  companion object {
    fun fromString(version: String): Version {
      val parts = version.split('.')
      require(parts.size == 2) { "Invalid version format: $version" }
      return Version(parts[0].toInt(), parts[1].toInt())
    }
  }

  fun nextMinor() = copy(minor = minor + 1)
}