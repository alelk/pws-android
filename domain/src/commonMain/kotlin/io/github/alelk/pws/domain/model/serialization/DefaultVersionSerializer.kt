package io.github.alelk.pws.domain.model.serialization

import io.github.alelk.pws.domain.model.Version
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DefaultVersionSerializer : KSerializer<Version> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Version", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Version) {
    encoder.encodeString(value.toString())
  }

  override fun deserialize(decoder: Decoder): Version {
    return Version.fromString(decoder.decodeString())
  }
}