package io.github.alelk.pws.backup.model.serialization

import io.github.alelk.pws.domain.model.Color
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ColorSerializer : KSerializer<Color> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Color", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Color) = encoder.encodeString(value.toString())
  override fun deserialize(decoder: Decoder): Color = Color.parse(decoder.decodeString())
}