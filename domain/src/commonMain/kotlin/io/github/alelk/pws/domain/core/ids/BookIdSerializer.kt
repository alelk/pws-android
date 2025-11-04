package io.github.alelk.pws.domain.core.ids

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BookIdSerializer : KSerializer<BookId> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BookId", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: BookId) = encoder.encodeString(value.identifier)
  override fun deserialize(decoder: Decoder): BookId = BookId.parse(decoder.decodeString())
}