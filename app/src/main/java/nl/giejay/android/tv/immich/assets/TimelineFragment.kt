package nl.giejay.android.tv.immich.assets

import android.os.Bundle
import arrow.core.Either
import arrow.core.flatMap
import nl.giejay.android.tv.immich.api.ApiClient
import nl.giejay.android.tv.immich.api.model.Asset
import nl.giejay.android.tv.immich.shared.prefs.ContentType
import nl.giejay.android.tv.immich.shared.prefs.PhotosOrder
import nl.giejay.android.tv.immich.shared.prefs.PreferenceManager
import nl.giejay.android.tv.immich.shared.prefs.SLIDER_SHOW_MEDIA_COUNT

/**
 * Photo grid for an entire Timeline year, reached from [TimelineBucketPickerFragment]
 * by clicking a specific month. Loads month-by-month (like AlbumDetailsFragment does
 * for albums), but starting from the *clicked* month and then wrapping forward through
 * the rest of the year (e.g. clicking March 2026 loads March, April, ... December, then
 * wraps to January, February), so both the overview and a slideshow started here begin
 * at the month the user actually picked, rather than always at January.
 */
class TimelineFragment : GenericAssetFragment() {

    private var clickedBucket: String? = null
    private var pageToBucket: Map<Int, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        clickedBucket = arguments?.getString("timeBucket")
        super.onCreate(savedInstanceState)
    }

    override fun clearState() {
        super.clearState()
        pageToBucket = null
    }

    // Preserve load order (clicked month first, then wrapping forward through the rest
    // of the year) instead of re-sorting by date - a global date sort would undo the
    // "start at the clicked month" ordering below.
    override fun sortItems(items: List<Asset>): List<Asset> {
        return items
    }

    override suspend fun loadData(): Either<String, List<Asset>> {
        if (pageToBucket == null) {
            val clicked = clickedBucket ?: return Either.Right(emptyList())
            val yearValue = clicked.take(4)
            return apiClient.listBuckets("", PhotosOrder.NEWEST_OLDEST).map { buckets ->
                val monthsThisYear = buckets
                    .filter { it.timeBucket.take(4) == yearValue }
                    .sortedBy { it.timeBucket }
                val startIndex = monthsThisYear.indexOfFirst { it.timeBucket == clicked }.coerceAtLeast(0)
                // Wrap the year so it starts at the clicked month: March, April, ... December,
                // January, February.
                val ordered = monthsThisYear.subList(startIndex, monthsThisYear.size) +
                    monthsThisYear.subList(0, startIndex)
                pageToBucket = ordered.mapIndexed { index, bucket -> (index + 1) to bucket.timeBucket }.toMap()
                return internalLoadData(emptyList())
            }
        } else {
            return internalLoadData(emptyList())
        }
    }

    private suspend fun internalLoadData(prevAssets: List<Asset>): Either<String, List<Asset>> {
        return loadItems(apiClient, currentPage, FETCH_PAGE_COUNT).flatMap {
            val filteredItems = it.filter { asset -> currentFilter == ContentType.ALL || asset.type.lowercase() == currentFilter.toString().lowercase() }
            val combined = prevAssets + filteredItems
            allPagesLoaded = allPagesLoaded(it)
            if (combined.size <= FETCH_COUNT && !allPagesLoaded) {
                // immediately load next month
                currentPage += 1
                internalLoadData(combined)
            } else {
                Either.Right(combined)
            }
        }
    }

    override suspend fun loadItems(
        apiClient: ApiClient,
        page: Int,
        pageCount: Int
    ): Either<String, List<Asset>> {
        val bucketForPage = pageToBucket?.get(page)
        return if (bucketForPage != null) {
            // Oldest -> newest within each month too, consistent with the forward
            // (clicked month -> onward) playback/browsing order.
            apiClient.getAssetsForBucket("", bucketForPage, PhotosOrder.OLDEST_NEWEST)
        } else {
            Either.Right(emptyList())
        }
    }

    override fun allPagesLoaded(items: List<Asset>): Boolean {
        return this.pageToBucket == null || !this.pageToBucket!!.contains(currentPage + 1)
    }

    override fun showMediaCount(): Boolean {
        return PreferenceManager.get(SLIDER_SHOW_MEDIA_COUNT)
    }

    // Starting a slideshow from Timeline should feel immediate - the whole point of
    // clicking a month is to watch its photos, not to browse a grid first.
    override fun autoStartSlideshow(): Boolean {
        return true
    }

    // sortItems() above preserves load order (clicked month first, wrapping forward
    // through the year) - so "forward" (incrementing index) already plays in that same
    // order. Ignore the global reverse-direction preference, which is calibrated for the
    // other views' default newest-first order.
    override fun reverseSlideshowDirection(): Boolean {
        return false
    }
}
