package nl.giejay.android.tv.immich.places

import androidx.navigation.fragment.findNavController
import arrow.core.Either
import nl.giejay.android.tv.immich.api.ApiClient
import nl.giejay.android.tv.immich.api.model.Asset
import nl.giejay.android.tv.immich.api.util.ApiUtil
import nl.giejay.android.tv.immich.card.Card
import nl.giejay.android.tv.immich.home.HomeFragmentDirections
import nl.giejay.android.tv.immich.shared.fragment.VerticalCardGridFragment

/**
 * Shows one card per city that has at least one geotagged photo, similarly to
 * the web/mobile app's "Places" view. Immich's GET /search/cities returns one
 * representative asset per city, which we use directly for both the thumbnail
 * and the city name (from its exifInfo).
 */
class PlacesFragment : VerticalCardGridFragment<Asset>() {

    override fun sortItems(items: List<Asset>): List<Asset> {
        return items.sortedBy { it.exifInfo?.city }
    }

    override suspend fun loadItems(
        apiClient: ApiClient,
        page: Int,
        pageCount: Int
    ): Either<String, List<Asset>> {
        if (page > startPage) {
            // The cities endpoint returns everything in one call, no paging.
            return Either.Right(emptyList())
        }
        return apiClient.listPlaces()
    }

    override fun allPagesLoaded(items: List<Asset>): Boolean = true

    override fun onItemSelected(card: Card, indexOf: Int) {
        // no use case yet
    }

    override fun onItemClicked(card: Card) {
        findNavController().navigate(
            HomeFragmentDirections.actionHomeFragmentToPlaceAssetsFragment(card.title)
        )
    }

    override fun getBackgroundPicture(it: Asset): String? {
        return ApiUtil.getFileUrl(it.id, "IMAGE")
    }

    override fun createCard(a: Asset): Card {
        val city = a.exifInfo?.city ?: ""
        return Card(
            city,
            a.exifInfo?.country ?: "",
            a.id,
            ApiUtil.getThumbnailUrl(a.id, "thumbnail"),
            ApiUtil.getThumbnailUrl(a.id, "preview")
        )
    }
}
