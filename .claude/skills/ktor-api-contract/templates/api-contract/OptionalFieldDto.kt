package io.github.myorg.myapp.contract.common

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Wire counterpart of the domain `OptionalField`: a PATCH field that is absent
 * ([Unchanged]), present-and-null ([Clear]), or present-with-a-value ([Set]).
 * `api:contract` cannot depend on `domain` (module-structure.md), so this is a
 * separate type; the mapping to `OptionalField` lives in `api:mapping`.
 *
 * The [OptionalFieldDtoSerializer] realises the three states in JSON:
 * - field omitted → the property default ([Unchanged]) — the serializer is not invoked;
 * - JSON `null` → [Clear];
 * - JSON value → [Set].
 *
 * A property of this type must default to [Unchanged] so an absent field decodes
 * correctly.
 */
@Serializable(with = OptionalFieldDtoSerializer::class)
sealed interface OptionalFieldDto<out T> {
    data object Unchanged : OptionalFieldDto<Nothing>

    data class Set<out T>(
        val value: T,
    ) : OptionalFieldDto<T>

    data object Clear : OptionalFieldDto<Nothing>
}

/**
 * Distinguishes present-null from present-value by reading the null mark directly.
 * Absence is handled by the property default, so this serializer only ever runs on
 * a present field. Its descriptor is the (non-null) element descriptor; a null
 * literal is decoded manually into [OptionalFieldDto.Clear].
 */
class OptionalFieldDtoSerializer<T>(
    private val element: KSerializer<T>,
) : KSerializer<OptionalFieldDto<T>> {
    override val descriptor: SerialDescriptor = element.descriptor

    override fun deserialize(decoder: Decoder): OptionalFieldDto<T> =
        if (decoder.decodeNotNullMark()) {
            OptionalFieldDto.Set(decoder.decodeSerializableValue(element))
        } else {
            decoder.decodeNull()
            OptionalFieldDto.Clear
        }

    override fun serialize(
        encoder: Encoder,
        value: OptionalFieldDto<T>,
    ) {
        when (value) {
            is OptionalFieldDto.Unchanged -> error("OptionalFieldDto.Unchanged must be omitted, not serialized")
            is OptionalFieldDto.Clear -> encoder.encodeNull()
            is OptionalFieldDto.Set -> encoder.encodeSerializableValue(element, value.value)
        }
    }
}
