package io.github.myorg.myapp.mapping.common

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.github.myorg.myapp.contract.common.OptionalFieldDto
import io.github.myorg.myapp.contract.common.PageDto
import io.github.myorg.myapp.domain.common.DomainError
import io.github.myorg.myapp.domain.common.OptionalField
import kotlin.time.Instant

/** A domain page → the wire envelope. */
fun <T, D> List<T>.toPageDto(nextCursor: String?, transform: (T) -> D): PageDto<D> =
    PageDto(items = map(transform), nextCursor = nextCursor)

/** Wire enum name → domain enum, as a value. Never `valueOf()` — it throws on unknown input. */
inline fun <reified E : Enum<E>> enumOrLeft(raw: String, field: String): Either<DomainError, E> =
    enumValues<E>().firstOrNull { it.name == raw }?.right()
        ?: DomainError.ValidationError("unknown $field '$raw'").left()

/** Client side: an unknown value from a newer server degrades to null, never a crash. */
inline fun <reified E : Enum<E>> enumOrNull(raw: String?): E? = raw?.let { r -> enumValues<E>().firstOrNull { it.name == r } }

/** Tri-state PATCH field: wire → domain, with a conversion that may fail. */
inline fun <W, D> OptionalFieldDto<W>.toDomain(convert: (W) -> Either<DomainError, D>): Either<DomainError, OptionalField<D?>> =
    when (this) {
        OptionalFieldDto.Unchanged -> OptionalField.Unchanged.right()
        OptionalFieldDto.Clear -> OptionalField.Clear.right()
        is OptionalFieldDto.Set -> convert(value).map { OptionalField.Set(it) }
    }

fun parseInstant(raw: String, field: String = "instant"): Either<DomainError, Instant> =
    try {
        Instant.parse(raw).right()
    } catch (_: IllegalArgumentException) {
        DomainError.ValidationError("invalid $field '$raw'").left()
    }
