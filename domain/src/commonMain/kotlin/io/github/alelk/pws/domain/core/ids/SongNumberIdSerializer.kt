package io.github.alelk.pws.domain.core.ids

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Serializer for SongNumberId as a compact string "bookId/songId". */
object SongNumberIdSerializer : KSerializer<SongNumberId> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SongNumberId", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: SongNumberId) = encoder.encodeString(value.toString())
  override fun deserialize(decoder: Decoder): SongNumberId = SongNumberId.parse(decoder.decodeString())
}

