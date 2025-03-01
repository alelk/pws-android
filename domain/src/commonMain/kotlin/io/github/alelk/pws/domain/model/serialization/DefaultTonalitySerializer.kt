package io.github.alelk.pws.domain.model.serialization

import io.github.alelk.pws.domain.model.Tonality
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DefaultTonalitySerializer : KSerializer<Tonality> {
  override val descriptor = PrimitiveSerialDescriptor("Tonality", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Tonality) = encoder.encodeString(value.identifier)
  override fun deserialize(decoder: Decoder): Tonality = Tonality.fromIdentifier(decoder.decodeString())
}