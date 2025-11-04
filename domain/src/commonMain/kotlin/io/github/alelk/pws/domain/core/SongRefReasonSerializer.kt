package io.github.alelk.pws.domain.core

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object SongRefReasonSerializer : KSerializer<SongRefReason> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SongRefReason", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: SongRefReason) = encoder.encodeString(value.identifier)
  override fun deserialize(decoder: Decoder): SongRefReason = SongRefReason.fromIdentifier(decoder.decodeString())
}