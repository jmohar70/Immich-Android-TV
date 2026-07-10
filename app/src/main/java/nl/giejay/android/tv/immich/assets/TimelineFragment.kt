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
 * by clicking any month within that year. Loads month-by-month (like
 * AlbumDetailsFragment does for albums) starting from January through December, so a
 * slideshow started here plays through the whole year, not just the single clicked month.
 */
class TimelineFragment : GenericAssetFragment() {

    private var year: String? = null
    private var pageToBucket: Map<Int, String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        year = arguments?.getString("timeBucket")?.take(4)
        super.onCreate(savedInstanceState)
    }

    override fun clearState() {
        super.clearState()
        pageToBucket = null
    }

    // Always chronological ascending here (Jan -> Dec), regardless of the general
    // "Photos" sort preference - showing newest-first while progressively loading a whole
    // year made it look like early months were missing/skipped when they weren't; they
    // were just sorted further down/out of the initially visible range.
    override fun sortItems(items: List<Asset>): List<Asset> {
        return items.sortedWith(PhotosOrder.OLDEST_NEWEST.sort)
    }

    override suspend fun loadData(): Either<String, List<Asset>> {
        if (pageToBucket == null) {
            val yearValue = year ?: return Either.Right(emptyList())
            return apiClient.listBuckets("", PhotosOrder.NEWEST_OLDEST).map { buckets ->
                // Always January -> December for this year, regardless of the general
                // sort order preference, so a slideshow started here plays through the
                // whole year chronologically.
                val monthsThisYear = buckets
                    .filter { it.timeBucket.take(4) == yearValue }
                    .sortedBy { it.timeBucket }
                pageToBucket = monthsThisYear.mapIndexed { index, bucket -> (index + 1) to bucket.timeBucket }.toMap()
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
            apiClient.getAssetsForBucket("", bucketForPage, PhotosOrder.NEWEST_OLDEST)
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

    // Our sortItems() above always orders oldest -> newest, unlike the general default -
    // so "forward" (incrementing index) already means January -> December here.
    // Ignore the global reverse-direction preference, which is calibrated for the other
    // views' default newest-first order.
    override fun reverseSlideshowDirection(): Boolean {
        return false
    }
}
