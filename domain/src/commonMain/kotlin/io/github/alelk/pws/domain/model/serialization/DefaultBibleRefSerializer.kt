package io.github.alelk.pws.domain.model.serialization

import io.github.alelk.pws.domain.model.BibleRef
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object DefaultBibleRefSerializer : KSerializer<BibleRef> {
  override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BibleRef", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: BibleRef) = encoder.encodeString(value.text)
  override fun deserialize(decoder: Decoder): BibleRef = BibleRef(decoder.decodeString())
}