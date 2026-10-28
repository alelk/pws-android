package io.github.myorg.myapp.contract.error

import kotlinx.serialization.Serializable

@Serializable
data class ApiErrorDto(
    val code:          String,                       // machine-readable error code
    val message:       String,                       // human-readable
    val correlationId: String,                       // matches X-Request-Id
    val details:       Map<String, String> = emptyMap(),
)
