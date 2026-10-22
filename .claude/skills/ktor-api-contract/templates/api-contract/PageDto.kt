package io.github.myorg.myapp.contract.common

import kotlinx.serialization.Serializable

/**
 * Cursor-pagination envelope for every list response: the page [items] plus the opaque
 * [nextCursor] for the next page, or `null` on the last one. No `total` — a keyset scan does not
 * count the tail. Clients never parse the cursor.
 */
@Serializable
data class PageDto<T>(
    val items: List<T>,
    val nextCursor: String? = null,
)
