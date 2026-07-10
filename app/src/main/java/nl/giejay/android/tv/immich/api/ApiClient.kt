package nl.giejay.android.tv.immich.api

import arrow.core.Either
import arrow.core.None
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.getOrElse
import nl.giejay.android.tv.immich.api.model.Album
import nl.giejay.android.tv.immich.api.model.AlbumDetails
import nl.giejay.android.tv.immich.api.model.Asset
import nl.giejay.android.tv.immich.api.model.AssetStatistics
import nl.giejay.android.tv.immich.api.model.Bucket
import nl.giejay.android.tv.immich.api.model.Folder
import nl.giejay.android.tv.immich.api.model.Person
import nl.giejay.android.tv.immich.api.model.SearchRequest
import nl.giejay.android.tv.immich.api.service.ApiService
import nl.giejay.android.tv.immich.api.util.ApiUtil.executeAPICall
import nl.giejay.android.tv.immich.shared.prefs.ContentType
import nl.giejay.android.tv.immich.shared.prefs.EXCLUDE_ASSETS_IN_ALBUM
import nl.giejay.android.tv.immich.shared.prefs.PhotosOrder
import nl.giejay.android.tv.immich.shared.prefs.PreferenceManager
import nl.giejay.android.tv.immich.shared.prefs.RECENT_ASSETS_MONTHS_BACK
import nl.giejay.android.tv.immich.shared.prefs.SIMILAR_ASSETS_PERIOD_DAYS
import nl.giejay.android.tv.immich.shared.prefs.SIMILAR_ASSETS_YEARS_BACK
import nl.giejay.android.tv.immich.shared.util.Utils.pmap
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

data class ApiClientConfig(
    val hostName: String,
    val apiKey: String,
    val disableSslVerification: Boolean,
    val debugMode: Boolean
)

class ApiClient(private val config: ApiClientConfig) {
    companion object ApiClient {
        private var apiClient: nl.giejay.android.tv.immich.api.ApiClient? = null
        fun getClient(config: ApiClientConfig): nl.giejay.android.tv.immich.api.ApiClient {
            if (config != apiClient?.config) {
                apiClient = ApiClient(config)
            }
            return apiClient!!
        }

        val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME;
    }

    private val retrofit = Retrofit.Builder()
        .client(ApiClientFactory.getClient(config.disableSslVerification, config.apiKey, config.debugMode))
        .addConverterFactory(GsonConverterFactory.create())
        .baseUrl("${config.hostName}/api/")
        .build()

    private val service: ApiService = retrofit.create(ApiService::class.java)

    suspend fun listAlbums(assetId: Option<String> = None): Either<String, List<Album>> {
        return executeAPICall(200) { service.listAlbums(false, assetId.getOrNull()) }.flatMap { albums ->
            return executeAPICall(200) { service.listAlbums(true, assetId.getOrNull()) }.map { sharedAlbums ->
                albums + sharedAlbums
            }
        }
    }

    suspend fun listPeople(): Either<String, List<Person>> {
        return executeAPICall(200) { service.listPeople() }.map { response -> response.people.filter { !it.name.isNullOrBlank() } }
    }

    suspend fun listPlaces(): Either<String, List<Asset>> {
        return executeAPICall(200) { service.searchCities() }.map { assets ->
            assets.filter { !it.exifInfo?.city.isNullOrBlank() }
        }
    }

    suspend fun getAssetStatistics(): Either<String, AssetStatistics> {
        // Empty filter body (no personIds/albumIds/type etc.) -> stats across all of the
        // current user's own accessible assets, matching what the web UI's account page shows.
        return executeAPICall(200) { service.searchAssetStatistics(SearchRequest()) }
    }

    suspend fun listAssetsFromAlbum(albumId: String): Either<String, AlbumDetails> {
        return executeAPICall(200) {
            val response = service.listAssetsFromAlbum(albumId)
            val album = response.body()!!
            // Immich v3+ no longer includes an embedded 'assets' list on the album response
            // (see https://immich.app/blog/v3-migration) - fetch them separately via
            // search/metadata with an albumIds filter instead. Using a large page size since
            // callers of this function (excluded-albums filtering, screensaver-by-album) want
            // the full set in one go, not paginated UI browsing (which already goes through
            // the timeline/bucket-based AlbumDetailsFragment instead, unaffected by this).
            val searchRequest = SearchRequest(page = 1, size = 1000, albumIds = listOf(albumId))
            val assetsResponse = service.listAssets(searchRequest)
            if (assetsResponse.isSuccessful) {
                val assets = assetsResponse.body()!!.assets.items.filter(excludeByTag()).map { it.copy(albumName = album.albumName) }
                Response.success(album.copy(assets = assets))
            } else {
                Response.error<AlbumDetails>(assetsResponse.code(), assetsResponse.errorBody()!!)
            }
        }
    }

    suspend fun recentAssets(page: Int, pageCount: Int, contentType: ContentType): Either<String, List<Asset>> {
        val now = LocalDateTime.now()
        return listAssets(page, pageCount, true, "desc",
            contentType = contentType, fromDate = now.minusMonths(PreferenceManager.get(RECENT_ASSETS_MONTHS_BACK).toLong()), endDate = now)
            .map { it.shuffled() }
    }

    suspend fun similarAssets(page: Int, pageCount: Int, contentType: ContentType): Either<String, List<Asset>> {
        val now = LocalDateTime.now()
        val map: List<Either<String, List<Asset>>> = (0 until PreferenceManager.get(SIMILAR_ASSETS_YEARS_BACK)).toList().map {
            listAssets(page,
                pageCount,
                true,
                "desc",
                fromDate = now.minusDays((PreferenceManager.get(SIMILAR_ASSETS_PERIOD_DAYS) / 2).toLong()).minusYears(it.toLong()),
                endDate = now.plusDays((PreferenceManager.get(SIMILAR_ASSETS_PERIOD_DAYS) / 2).toLong()).minusYears(it.toLong()),
                contentType = contentType)
        }
        if (map.all { it.isLeft() }) {
            return map.first()
        }
        return Either.Right(map.flatMap { it.getOrElse { emptyList() } }.shuffled())
    }

    suspend fun listAssets(page: Int,
                           pageCount: Int,
                           random: Boolean = false,
                           order: String = "desc",
                           personIds: List<UUID> = emptyList(),
                           fromDate: LocalDateTime? = null,
                           endDate: LocalDateTime? = null,
                           city: String? = null,
                           contentType: ContentType): Either<String, List<Asset>> {
        val searchRequest = SearchRequest(page,
            pageCount,
            order,
            // null for all content
            if (contentType == ContentType.ALL) null else contentType.toString(),
            personIds,
            endDate?.format(dateTimeFormatter),
            fromDate?.format(dateTimeFormatter),
            city,
            // albumIds intentionally omitted here (defaults to null) - not used by this
            // function, only by listAssetsFromAlbum()
            visibility = if (random) null else "timeline")
        return (if (random) {
            executeAPICall(200) { service.randomAssets(searchRequest) }
        } else {
            executeAPICall(200) { service.listAssets(searchRequest) }.map { res -> res.assets.items }
        }).map { it.filter(excludeByTag()) }.map {
            val excludedAlbums = PreferenceManager.get(EXCLUDE_ASSETS_IN_ALBUM)
            if (excludedAlbums.isNotEmpty()) {
                val excludedAssets =
                    excludedAlbums.toList().flatMap { albumId -> listAssetsFromAlbum(albumId).getOrNull()?.assets ?: emptyList() }.map { it.id }
                it.filterNot { asset -> excludedAssets.contains(asset.id) }
            } else {
                it
            }
        }
    }

    private fun excludeByTag() = { asset: Asset ->
        asset.tags?.none { t -> t.name == "exclude_immich_tv" } ?: true
    }

    suspend fun listBuckets(albumId: String, order: PhotosOrder): Either<String, List<Bucket>> {
        val safeAlbumId = if (albumId.isBlank()) null else albumId
        return executeAPICall(200) {
            service.listBuckets(albumId = safeAlbumId, order = if (order == PhotosOrder.OLDEST_NEWEST) "asc" else "desc")
        }
    }

    // Lightweight: only reads the columnar bucket response for its asset id list, without
    // resolving each id to a full Asset. Used to show a representative thumbnail per month
    // in the Timeline picker, where we only need one id per bucket, not every asset's details.
    suspend fun getBucketThumbnailAssetId(bucket: String): Either<String, String?> {
        return executeAPICall(200) {
            service.getBucketV2(albumId = null, timeBucket = bucket, order = "desc")
        }.map { it.id.firstOrNull() }
    }

    suspend fun getAssetsForBucket(albumId: String, bucket: String, order: PhotosOrder): Either<String, List<Asset>> {
        val safeAlbumId = if (albumId.isBlank()) null else albumId
        val response = executeAPICall(200) {
            service.getBucketV2(albumId = safeAlbumId, timeBucket = bucket, order = if (order == PhotosOrder.OLDEST_NEWEST) "asc" else "desc")
        }.map {
            it.id.pmap(concurrency = 8) { t -> service.getAsset(t).body() }.filterNotNull().toList()
        }
        if (response.isLeft()) {
            return executeAPICall(200) {
                service.getBucket(albumId = safeAlbumId, timeBucket = bucket, order = if (order == PhotosOrder.OLDEST_NEWEST) "asc" else "desc")
            }
        }
        return response
    }

    suspend fun listFolders(): Either<String, Folder> {
        return executeAPICall(200) {
            service.getUniquePaths()
        }.map { paths ->
            return Either.Right(createRootFolder(Folder("", mutableListOf(), null), paths))
        }
    }

    private fun createRootFolder(parent: Folder, paths: List<String>): Folder {
        paths.forEach { path ->
            val directories = path.split("/")
            createFolders(directories, parent)
        }
        return parent
    }

    private fun createFolders(paths: List<String>, currentParent: Folder): Folder {
        if (paths.isEmpty()) {
            return currentParent
        }
        val createdChild = Folder(paths.first(), mutableListOf(), currentParent)
        val alreadyOwnedChild = currentParent.hasPath(paths.first())
        if (alreadyOwnedChild != null) {
            return createFolders(paths.drop(1), alreadyOwnedChild)
        }
        currentParent.children.add(createdChild)
        return createFolders(paths.drop(1), createdChild)
    }

    suspend fun listAssetsForFolder(folder: String): Either<String, List<Asset>> {
        return executeAPICall(200) {
            service.getAssetsForPath(folder)
        }.map { it.filter(excludeByTag()) }
    }
}

