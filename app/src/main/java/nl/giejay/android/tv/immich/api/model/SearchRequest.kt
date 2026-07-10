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
                         val albumIds: List<String> = emptyList(),
                         val withExif: Boolean = true,
                         // Immich v3+ defaults to "any visibility" (incl. archived) when this
                         // is omitted, instead of the pre-v3 default of "timeline" visibility -
                         // set it explicitly to keep the same behavior as before on both old
                         // and new server versions (https://immich.app/blog/v3-migration).
                         val visibility: String = "timeline"
)
