package io.github.alelk.pws.backup.model.serialization

import io.github.alelk.pws.domain.model.BookExternalId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object BookExternalIdSerializer : KSerializer<BookExternalId> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BookId", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: BookExternalId) = encoder.encodeString(value.identifier)
  override fun deserialize(decoder: Decoder): BookExternalId = BookExternalId.parse(decoder.decodeString())
}