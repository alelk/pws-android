package io.github.alelk.pws.backup.model.serialization

import io.github.alelk.pws.backup.model.Numbers
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

internal object NumbersSerializer : KSerializer<Numbers> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Numbers", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Numbers) = encoder.encodeString(value.toString())
  override fun deserialize(decoder: Decoder): Numbers = Numbers.parse(decoder.decodeString())
}