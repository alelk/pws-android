package io.github.alelk.pws.domain.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object NonEmptyStringSerializer : KSerializer<NonEmptyString> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("NonEmptyString", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: NonEmptyString) = encoder.encodeString(value.value)
  override fun deserialize(decoder: Decoder): NonEmptyString = NonEmptyString(decoder.decodeString())
}