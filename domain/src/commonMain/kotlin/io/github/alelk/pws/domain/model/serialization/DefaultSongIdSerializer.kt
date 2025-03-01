package io.github.alelk.pws.domain.model.serialization

import io.github.alelk.pws.domain.model.SongId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DefaultSongIdSerializer : KSerializer<SongId> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("SongId", PrimitiveKind.LONG)
  override fun serialize(encoder: Encoder, value: SongId) = encoder.encodeLong(value.value)
  override fun deserialize(decoder: Decoder): SongId = SongId(decoder.decodeLong())
}