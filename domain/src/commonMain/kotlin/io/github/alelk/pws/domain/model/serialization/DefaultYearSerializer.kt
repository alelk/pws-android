package io.github.alelk.pws.domain.model.serialization

import io.github.alelk.pws.domain.model.Year
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DefaultYearSerializer : KSerializer<Year> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Year", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: Year) {
    encoder.encodeString(value.toString())
  }

  override fun deserialize(decoder: Decoder): Year {
    return Year.parse(decoder.decodeString())
  }
}