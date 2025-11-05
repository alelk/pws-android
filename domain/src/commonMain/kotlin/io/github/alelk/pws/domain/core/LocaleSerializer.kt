package io.github.alelk.pws.domain.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object LocaleSerializer : KSerializer<Locale> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Locale", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Locale) = encoder.encodeString(value.toString())
  override fun deserialize(decoder: Decoder): Locale = Locale.of(decoder.decodeString().takeWhile { it != '-' })
}