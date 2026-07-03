package nl.giejay.android.tv.immich.places

import android.os.Bundle
import arrow.core.Either
import nl.giejay.android.tv.immich.api.ApiClient
import nl.giejay.android.tv.immich.api.model.Asset
import nl.giejay.android.tv.immich.assets.GenericAssetFragment

class PlaceAssetsFragment : GenericAssetFragment() {
    private lateinit var cityName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        cityName = PlaceAssetsFragmentArgs.fromBundle(requireArguments()).cityName
        super.onCreate(savedInstanceState)
    }

    override suspend fun loadItems(
        apiClient: ApiClient,
        page: Int,
        pageCount: Int
    ): Either<String, List<Asset>> {
        return apiClient.listAssets(page,
            pageCount,
            random = false,
            order = "desc",
            contentType = currentFilter,
            city = cityName)
    }

    override fun showMediaCount(): Boolean {
        return false
    }

    override fun setTitle(response: List<Asset>) {
        title = cityName
    }
}
