package nl.giejay.android.tv.immich.home

import androidx.fragment.app.Fragment
import androidx.leanback.app.BrowseSupportFragment

/**
 * Leanback's BrowseSupportFragment always needs *some* fragment implementing
 * MainFragmentAdapterProvider for whatever row is currently at the selected
 * position - including briefly during the very first layout pass, before
 * HomeFragment gets a chance to move selectedPosition past the fixed
 * BrandingRow at index 0. This blank fragment satisfies that requirement
 * without showing anything, so that brief moment is harmless instead of
 * crashing with "Fragment must implement MainFragmentAdapterProvider".
 */
class BlankMainFragment : Fragment(), BrowseSupportFragment.MainFragmentAdapterProvider {
    private val mMainFragmentAdapter = BrowseSupportFragment.MainFragmentAdapter(this)

    override fun getMainFragmentAdapter(): BrowseSupportFragment.MainFragmentAdapter<*> {
        return mMainFragmentAdapter
    }
}
