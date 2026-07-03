package nl.giejay.android.tv.immich.assets

import android.os.Bundle
import arrow.core.Either
import nl.giejay.android.tv.immich.api.ApiClient
import nl.giejay.android.tv.immich.api.model.Asset
import nl.giejay.android.tv.immich.shared.prefs.PhotosOrder
import nl.giejay.android.tv.immich.shared.prefs.PreferenceManager
import nl.giejay.android.tv.immich.shared.prefs.SLIDER_SHOW_MEDIA_COUNT

/**
 * Photo grid for a single Timeline bucket (one calendar month), reached
 * from [TimelineBucketPickerFragment]. Reuses all of GenericAssetFragment's
 * existing grid/slideshow machinery; the only difference is that it loads
 * exactly one bucket's assets in a single call instead of paging through
 * the full library.
 */
class TimelineFragment : GenericAssetFragment() {

    private var timeBucket: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        timeBucket = arguments?.getString("timeBucket")
        super.onCreate(savedInstanceState)
    }

    override suspend fun loadItems(
        apiClient: ApiClient,
        page: Int,
        pageCount: Int
    ): Either<String, List<Asset>> {
        val bucket = timeBucket ?: return Either.Right(emptyList())
        // The bucket endpoint always returns the full month in one call, there is no
        // paging by page number here - so only fetch on the very first page.
        if (page > startPage) {
            return Either.Right(emptyList())
        }
        return apiClient.getAssetsForBucket("", bucket, PhotosOrder.NEWEST_OLDEST)
    }

    // We always get the complete bucket back in a single call, so there is never a "next page".
    override fun allPagesLoaded(items: List<Asset>): Boolean = true

    override fun showMediaCount(): Boolean {
        return PreferenceManager.get(SLIDER_SHOW_MEDIA_COUNT)
    }
}
