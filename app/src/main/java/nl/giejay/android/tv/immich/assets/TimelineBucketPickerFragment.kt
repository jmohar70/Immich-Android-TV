package nl.giejay.android.tv.immich.assets

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.leanback.app.BrowseSupportFragment
import androidx.leanback.app.RowsSupportFragment
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.ListRow
import androidx.leanback.widget.ListRowPresenter
import androidx.leanback.widget.OnItemViewClickedListener
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import nl.giejay.android.tv.immich.R
import nl.giejay.android.tv.immich.api.ApiClient
import nl.giejay.android.tv.immich.api.ApiClientConfig
import nl.giejay.android.tv.immich.api.model.Bucket
import nl.giejay.android.tv.immich.api.util.ApiUtil
import nl.giejay.android.tv.immich.card.Card
import nl.giejay.android.tv.immich.card.MonthPresenter
import nl.giejay.android.tv.immich.shared.prefs.API_KEY
import nl.giejay.android.tv.immich.shared.prefs.DEBUG_MODE
import nl.giejay.android.tv.immich.shared.prefs.DISABLE_SSL_VERIFICATION
import nl.giejay.android.tv.immich.shared.prefs.HOST_NAME
import nl.giejay.android.tv.immich.shared.prefs.PhotosOrder
import nl.giejay.android.tv.immich.shared.prefs.PreferenceManager
import nl.giejay.android.tv.immich.shared.util.Utils.pmap
import timber.log.Timber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Timeline entry point: shows one horizontal row of months per year,
 * newest year first. Clicking a month navigates to [TimelineFragment]
 * which shows the regular photo grid filtered to that month's bucket.
 */
class TimelineBucketPickerFragment : RowsSupportFragment(), BrowseSupportFragment.MainFragmentAdapterProvider {

    private val ioScope = CoroutineScope(Job() + Dispatchers.IO)
    private lateinit var apiClient: ApiClient
    private lateinit var rowsAdapter: ArrayObjectAdapter

    // Required so this fragment can be embedded as a page inside HomeFragment's BrowseSupportFragment
    private val mMainFragmentAdapter = BrowseSupportFragment.MainFragmentAdapter(this)

    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> {
        return mMainFragmentAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rowsAdapter = ArrayObjectAdapter(ListRowPresenter())
        adapter = rowsAdapter

        setupClient()
        loadBuckets()

        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            val card = item as? Card ?: return@OnItemViewClickedListener
            val bucketId = card.id

            findNavController().navigate(
                R.id.action_global_to_timeline_grid,
                bundleOf("timeBucket" to bucketId)
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ioScope.coroutineContext.cancelChildren()
    }

    private fun setupClient() {
        apiClient = ApiClient.getClient(
            ApiClientConfig(
                PreferenceManager.get(HOST_NAME),
                PreferenceManager.get(API_KEY),
                PreferenceManager.get(DISABLE_SSL_VERIFICATION),
                PreferenceManager.get(DEBUG_MODE)
            )
        )
    }

    private fun loadBuckets() {
        ioScope.launch {
            apiClient.listBuckets("", PhotosOrder.NEWEST_OLDEST).fold(
                { error -> Timber.e("Error loading timeline buckets: $error") },
                { buckets -> processBucketsByYear(buckets) }
            )
        }
    }

    private fun processBucketsByYear(buckets: List<Bucket>) {
        val bucketsByYear = buckets.groupBy { bucket ->
            bucket.timeBucket.take(4)
        }

        val listRowAdaptersByBucket = mutableListOf<Pair<Bucket, ArrayObjectAdapter>>()

        activity?.runOnUiThread {
            // The fragment can be detached by the time this posted runnable actually runs
            // (e.g. the user navigated away again in the meantime) - activity being non-null
            // at schedule time doesn't guarantee that's still true at execution time.
            if (!isAdded) return@runOnUiThread

            rowsAdapter.clear()
            val cardPresenter = MonthPresenter(requireContext())

            // groupBy keeps insertion order and the API already returns buckets newest-first,
            // so years naturally come out newest-first too.
            bucketsByYear.forEach { (year, yearBuckets) ->
                val header = HeaderItem(year)
                val listRowAdapter = ArrayObjectAdapter(cardPresenter)

                val cards = yearBuckets.sortedBy { it.timeBucket }.map { bucket ->
                    Card(
                        id = bucket.timeBucket,
                        title = formatMonthOnly(bucket.timeBucket),
                        description = "(${bucket.count})",
                        thumbnailUrl = null,
                        backgroundUrl = null
                    )
                }
                listRowAdapter.addAll(0, cards)
                rowsAdapter.add(ListRow(header, listRowAdapter))
                yearBuckets.forEach { bucket -> listRowAdaptersByBucket.add(bucket to listRowAdapter) }
            }

            loadThumbnails(listRowAdaptersByBucket)
        }
    }

    /**
     * Fetches one representative asset id per bucket (bounded concurrency, so we don't fire
     * off a request per month all at once for someone with many years of photos) and swaps
     * each card's colored placeholder for a real photo thumbnail once it's ready.
     */
    private fun loadThumbnails(listRowAdaptersByBucket: List<Pair<Bucket, ArrayObjectAdapter>>) {
        ioScope.launch {
            val results = listRowAdaptersByBucket.pmap(concurrency = 8) { (bucket, rowAdapter) ->
                Triple(bucket, rowAdapter, apiClient.getBucketThumbnailAssetId(bucket.timeBucket).getOrNull())
            }
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread

                results.forEach { (bucket, rowAdapter, assetId) ->
                    if (assetId == null) return@forEach
                    val index = (0 until rowAdapter.size()).firstOrNull { (rowAdapter.get(it) as? Card)?.id == bucket.timeBucket }
                    if (index != null) {
                        val card = rowAdapter.get(index) as Card
                        rowAdapter.replace(index, card.copy(thumbnailUrl = ApiUtil.getThumbnailUrl(assetId, "thumbnail")))
                    }
                }
            }
        }
    }

    private fun formatMonthOnly(rawDate: String): String {
        return try {
            val date = LocalDate.parse(rawDate.take(10))
            date.format(DateTimeFormatter.ofPattern("MMMM", Locale.getDefault()))
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        } catch (e: Exception) {
            rawDate
        }
    }
}
