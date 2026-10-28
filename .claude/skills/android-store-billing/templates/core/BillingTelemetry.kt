package com.example.app.domain.telemetry

/*
 * TEMPLATE — the two telemetry calls the billing stack needs. If the project already has a Telemetry
 * port (Crashlytics, AppMetrica, Sentry adapter), use it and delete this file.
 *
 * Why it matters for billing: every store-SDK failure degrades silently for the user (a message on
 * the paywall, the offline entitlement untouched). Without a non-fatal it is invisible to you too.
 * Never put purchase tokens, invoice ids, raw dates or account ids into attributes.
 */
interface BillingTelemetry {
    fun recordError(throwable: Throwable, message: String? = null, attributes: Map<String, String> = emptyMap())

    fun event(name: String, params: Map<String, Any?> = emptyMap())
}

object NoOpBillingTelemetry : BillingTelemetry {
    override fun recordError(throwable: Throwable, message: String?, attributes: Map<String, String>) = Unit

    override fun event(name: String, params: Map<String, Any?>) = Unit
}
