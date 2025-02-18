package io.github.alelk.pws.domain.model.serialization

import io.github.alelk.pws.domain.model.TagId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DefaultTagIdSerializer : KSerializer<TagId> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TagId", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: TagId) = encoder.encodeString(value.toString())
  override fun deserialize(decoder: Decoder): TagId = TagId.parse(decoder.decodeString())
}