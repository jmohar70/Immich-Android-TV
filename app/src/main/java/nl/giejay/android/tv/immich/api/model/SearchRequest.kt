package nl.giejay.android.tv.immich.api.model

import java.util.UUID

data class SearchRequest(val page: Int = 0,
                         val size: Int = 100,
                         val order: String = "desc",
                         val type: String? = null,
                         val personIds: List<UUID> = emptyList(),
                         val takenBefore: String? = null,
                         val takenAfter: String? = null,
                         val city: String? = null,
                         // Nullable and defaulting to null (not emptyList()/a fixed string) so
                         // Gson omits these from the JSON body entirely unless a call site sets
                         // them explicitly - search/random appears to validate its request body
                         // more strictly than search/metadata (Immich v3 switched to Zod
                         // validation) and returned 400 once these were always being sent.
                         val albumIds: List<String>? = null,
                         val withExif: Boolean = true,
                         // Immich v3+ defaults to "any visibility" (incl. archived) when this
                         // is omitted, instead of the pre-v3 default of "timeline" visibility -
                         // set it explicitly (only on search/metadata calls) to keep the same
                         // behavior as before on both old and new server versions
                         // (https://immich.app/blog/v3-migration).
                         val visibility: String? = null
)
