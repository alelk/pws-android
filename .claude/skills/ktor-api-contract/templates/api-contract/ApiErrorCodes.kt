package io.github.myorg.myapp.contract.error

/**
 * Every `ApiErrorDto.code` the server answers with. Both ends read this declaration, as with
 * `ApiHeaders`: renaming a code is a compile error on both, not a silent mismatch. The server
 * transport writes no literal (`ErrorCodeLiteralTest`).
 */
object ApiErrorCodes {
    const val BAD_REQUEST: String = "BAD_REQUEST"
    const val UNAUTHORIZED: String = "UNAUTHORIZED"
    const val FORBIDDEN: String = "FORBIDDEN"
    const val NO_ACTIVE_TENANT: String = "NO_ACTIVE_TENANT"
    const val NOT_FOUND: String = "NOT_FOUND"
    const val ALREADY_EXISTS: String = "ALREADY_EXISTS"
    const val CONFLICT: String = "CONFLICT"
    const val GONE: String = "GONE"
    const val VALIDATION_ERROR: String = "VALIDATION_ERROR"
    const val LOCKED: String = "LOCKED"
    const val TOO_MANY_REQUESTS: String = "TOO_MANY_REQUESTS"
    const val PAYLOAD_TOO_LARGE: String = "PAYLOAD_TOO_LARGE"
    const val UNSUPPORTED_MEDIA_TYPE: String = "UNSUPPORTED_MEDIA_TYPE"
    const val INSUFFICIENT_STORAGE: String = "INSUFFICIENT_STORAGE"
    const val BAD_GATEWAY: String = "BAD_GATEWAY"
    const val INTERNAL_ERROR: String = "INTERNAL_ERROR"
}

/**
 * `details.reason`: the machine name of a refusal a screen branches on. `message` stays for the logs —
 * a screen never reads it (`ErrorReasonTest`). A reason is added the day a screen needs one, with a
 * KDoc saying which code carries it when that is not `VALIDATION_ERROR`.
 */
object ValidationReasons {
    /** The key in `ApiErrorDto.details`. */
    const val DETAILS_KEY: String = "reason"

    /** A text above its ceiling (the text policy). */
    const val TEXT_TOO_LONG: String = "text_too_long"

    /** A text with a control or bidi character, or a line break in a one-line field. */
    const val TEXT_FORBIDDEN_CHARS: String = "text_forbidden_chars"

    /** The tenant is at one of its ceilings (members, open items, …). */
    const val TENANT_LIMIT: String = "tenant_limit"

    /** The account already owns as many tenants as allowed (tenants being deleted do not count). */
    const val TOO_MANY_TENANTS: String = "too_many_tenants"

    /** The account still owns a tenant; `details` lists them. Refuses deleting the account. */
    const val OWNS_TENANT: String = "owns_tenant"

    /** The tenant has no AI access today (entitlement lapsed or switched off): the path without a model. */
    const val AI_OFF: String = "ai_off"

    /** The reason of a `FORBIDDEN` (403): the member's consent to an external processor is missing. */
    const val AI_CONSENT: String = "ai_consent"

    /** An imported archive's format is not one this version reads; `details.format` names it. */
    const val ARCHIVE_FORMAT_UNSUPPORTED: String = "archive_format_unsupported"

    /** The reason of a `FORBIDDEN` (403): the account has not proved its address yet. */
    const val EMAIL_UNVERIFIED: String = "email_unverified"

    /** The reason of a `FORBIDDEN` (403): the tenant is being deleted; the screen says so instead of "no access". */
    const val TENANT_DELETING: String = "tenant_deleting"

    /** The reason of an `INSUFFICIENT_STORAGE` (507): one reason for every storage ceiling — the remedy is the same. */
    const val STORAGE_FULL: String = "storage_full"

    /**
     * The reason of an `UNAUTHORIZED` (401) of a RE-AUTHENTICATION only (the current password of an
     * irreversible action). A sign-in, a PIN and a refresh carry none: there the person is not known yet.
     */
    const val INVALID_PASSWORD: String = "invalid_password"

    /** The reason of a `CONFLICT` (409): a one-at-a-time operation (an archive link) is already waiting. */
    const val ARCHIVE_IN_PROGRESS: String = "archive_in_progress"
}
