package io.github.alelk.pws.backup.model.serialization

import io.github.alelk.pws.backup.model.SongNumber
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder



object SongNumberSetSerializer: KSerializer<Set<SongNumber>> {
  override val descriptor: SerialDescriptor
    get() = TODO("Not yet implemented")

  override fun deserialize(decoder: Decoder): Set<SongNumber> {
    TODO("Not yet implemented")
  }

  override fun serialize(encoder: Encoder, value: Set<SongNumber>) {
    TODO("Not yet implemented")
  }
}