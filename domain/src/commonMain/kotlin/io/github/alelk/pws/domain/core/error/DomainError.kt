package io.github.alelk.pws.domain.core.error

/**
 * Базовые доменные ошибки (без платформенных зависимостей).
 * Реализации инфраструктурных слоёв должны маппить свои исключения в эти типы.
 */
sealed interface DomainError

/** Сущность не найдена */
data class NotFound(val id: String?, val entity: String) : RuntimeException("$entity not found: ${id ?: "<no-id>"}"), DomainError

/** Нарушение валидации инвариантов */
data class Validation(val details: String) : RuntimeException(details), DomainError

/** Конфликт версий / уникальности */
data class Conflict(val details: String) : RuntimeException(details), DomainError

/** Сетевая / транспортная ошибка (для remote реализаций) */
data class Network(val status: Int?, val body: String?) : RuntimeException("Network error status=${status ?: "?"}"), DomainError
