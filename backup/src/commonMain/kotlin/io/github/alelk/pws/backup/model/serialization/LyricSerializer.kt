package io.github.alelk.pws.backup.model.serialization

import io.github.alelk.pws.backup.model.Lyric
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object LyricSerializer : KSerializer<Lyric> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Lyric", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Lyric) = encoder.encodeString(value.text)
  override fun deserialize(decoder: Decoder): Lyric = Lyric(decoder.decodeString())
}