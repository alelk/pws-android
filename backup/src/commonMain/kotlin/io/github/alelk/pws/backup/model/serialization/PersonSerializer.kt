package io.github.alelk.pws.backup.model.serialization

import io.github.alelk.pws.domain.model.Person
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object PersonSerializer : KSerializer<Person> {
  override val descriptor = PrimitiveSerialDescriptor("Person", PrimitiveKind.STRING)
  override fun serialize(encoder: Encoder, value: Person) = encoder.encodeString(value.name)
  override fun deserialize(decoder: Decoder): Person = Person(decoder.decodeString())
}