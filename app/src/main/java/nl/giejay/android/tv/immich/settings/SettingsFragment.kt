package nl.giejay.android.tv.immich.settings

import android.app.Activity
import android.app.AlertDialog
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
import kotlinx.coroutines.launch
import nl.giejay.android.tv.immich.ImmichApplication
import nl.giejay.android.tv.immich.R
import nl.giejay.android.tv.immich.api.ApiClient
import nl.giejay.android.tv.immich.api.ApiClientConfig
import nl.giejay.android.tv.immich.home.HomeFragmentDirections
import nl.giejay.android.tv.immich.shared.donate.DonateService
import nl.giejay.android.tv.immich.shared.prefs.API_KEY
import nl.giejay.android.tv.immich.shared.prefs.DEBUG_MODE
import nl.giejay.android.tv.immich.shared.prefs.DISABLE_SSL_VERIFICATION
import nl.giejay.android.tv.immich.shared.prefs.DebugPrefScreen
import nl.giejay.android.tv.immich.shared.prefs.HOST_NAME
import nl.giejay.android.tv.immich.shared.prefs.PreferenceManager
import nl.giejay.android.tv.immich.shared.prefs.ScreensaverPrefScreen
import nl.giejay.android.tv.immich.shared.prefs.ViewPrefScreen


class SettingsFragment : RowsSupportFragment() {
    private val mRowsAdapter: ArrayObjectAdapter
    private lateinit var donateService: DonateService
    private val ioScope = CoroutineScope(Job() + Dispatchers.IO)

    init {
        val selector = ListRowPresenter()
        selector.setNumRows(1)
        mRowsAdapter = ArrayObjectAdapter(selector)
        onItemViewClickedListener = OnItemViewClickedListener { _, item, _, _ ->
            val card = item as SettingsCard
            card.onClick()
        }
        adapter = mRowsAdapter
    }

    override fun onAttach(activity: Activity) {
        super.onAttach(activity)
        donateService = DonateService(activity)
        loadData()
    }

    private fun showStatistics() {
        val apiClient = ApiClient.getClient(
            ApiClientConfig(
                PreferenceManager.get(HOST_NAME),
                PreferenceManager.get(API_KEY),
                PreferenceManager.get(DISABLE_SSL_VERIFICATION),
                PreferenceManager.get(DEBUG_MODE)
            )
        )
        ioScope.launch {
            apiClient.getAssetStatistics().fold(
                { error ->
                    activity?.runOnUiThread {
                        showDialog(getString(R.string.statistics_dialog_error, error))
                    }
                },
                { stats ->
                    activity?.runOnUiThread {
                        showDialog(getString(R.string.statistics_dialog_message, stats.images, stats.videos, stats.total))
                    }
                }
            )
        }
    }

    private fun showDialog(message: String) {
        val context = context ?: return
        AlertDialog.Builder(context)
            .setTitle(R.string.statistics_dialog_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun loadData() {
        if (isAdded) {
            mRowsAdapter.add(
                createCardRow(
                    listOf(
                        SettingsCard(
                            ImmichApplication.appContext!!.getString(R.string.server),
                            null,
                            "server",
                            "ic_settings_settings",
                            "ic_settings_settings"
                        ) {
                            findNavController().navigate(
                                HomeFragmentDirections.actionGlobalSignInFragment()
                            )
                        },
                        SettingsCard(
                            ImmichApplication.appContext!!.getString(R.string.view_settings),
                            null,
                            "view_settings",
                            "icon_view",
                            "icon_view"
                        ) {
                            findNavController().navigate(
                                HomeFragmentDirections.actionGlobalToSettingsDialog(ViewPrefScreen.key)
                            )
                        },
                        SettingsCard(
                            ImmichApplication.appContext!!.getString(R.string.screensaver),
                            null,
                            "screensaver",
                            "screensaver",
                            "ic_settings_settings"
                        ) {
                            findNavController().navigate(
                                HomeFragmentDirections.actionGlobalToSettingsDialog(ScreensaverPrefScreen.key)
                            )
                        },
                        SettingsCard(
                            ImmichApplication.appContext!!.getString(R.string.statistics),
                            null,
                            "statistics",
                            "ic_settings_settings",
                            "ic_settings_settings"
                        ) {
                            showStatistics()
                        },
                        SettingsCard(
                            ImmichApplication.appContext!!.getString(R.string.debug),
                            null,
                            "debug",
                            "bug",
                            "bug"
                        ) {
                            findNavController().navigate(
                                HomeFragmentDirections.actionGlobalToSettingsDialog(DebugPrefScreen.key)
                            )
                        },
                        SettingsCard(
                            ImmichApplication.appContext!!.getString(R.string.donate),
                            null,
                            "donate",
                            "donate",
                            "donate",
//                            donateService.isInitialized()
                        ) {
                            findNavController().navigate(
                                HomeFragmentDirections.actionHomeToDonate()
                            )
                        }
                    )
                )
            )
            mainFragmentAdapter.fragmentHost?.notifyDataReady(
                mainFragmentAdapter
            )
        }
    }

    private fun createCardRow(cards: List<SettingsCard>): ListRow {
        val iconCardPresenter = SettingsIconPresenter(requireContext())
        val adapter = ArrayObjectAdapter(iconCardPresenter)
        adapter.addAll(0, cards.filter { it.visible })
        val headerItem = HeaderItem(ImmichApplication.appContext!!.getString(R.string.settings))
        return ListRow(headerItem, adapter)
    }
}
