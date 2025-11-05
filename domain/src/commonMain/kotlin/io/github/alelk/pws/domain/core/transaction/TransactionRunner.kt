package io.github.alelk.pws.domain.core.transaction

/**
 * Abstraction for executing a block inside a storage transaction. Platform implementations (Android Room, JVM SQL, etc.) provide atomicity.
 * In commonTest / JS you can use [NoopTransactionRunner].
 */
interface TransactionRunner {
  /** Execute [block] in a read-only transaction; return its result or propagate exception. */
  suspend fun <T> inRoTransaction(block: suspend RoTransactionScope.() -> T): T
  suspend fun <T> inRwTransaction(block: suspend RwTransactionScope.() -> T): T
}

/** Scope injected into the read-only transaction block */
interface RoTransactionScope

/** Scope injected into the read/write transaction block. */
interface RwTransactionScope

/** No-op implementation (no real transaction) suitable for tests or platforms without transactional storage. */
class NoopTransactionRunner : TransactionRunner {
  override suspend fun <T> inRoTransaction(block: suspend RoTransactionScope.() -> T): T =
    block(object : RoTransactionScope {})

  override suspend fun <T> inRwTransaction(block: suspend RwTransactionScope.() -> T): T =
    block(object : RwTransactionScope {})
}
