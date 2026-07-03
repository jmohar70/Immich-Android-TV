package nl.giejay.android.tv.immich.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.leanback.widget.Presenter
import nl.giejay.android.tv.immich.R
import nl.giejay.android.tv.immich.shared.prefs.HOST_NAME
import nl.giejay.android.tv.immich.shared.prefs.PreferenceManager

class BrandingPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val root: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_branding_header, parent, false)
        // Deliberately non-focusable: this is a fixed, decorative item, not a
        // selectable tab, so remote D-pad navigation should skip straight
        // over it (the same way Leanback's built-in DividerRow works).
        root.isFocusable = false
        root.isFocusableInTouchMode = false
        return ViewHolder(root)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val serverText = viewHolder.view.findViewById<TextView>(R.id.branding_server)
        serverText.text = formatServerLabel(PreferenceManager.get(HOST_NAME))
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val serverText = viewHolder.view.findViewById<TextView>(R.id.branding_server)
        serverText.text = null
    }

    private fun formatServerLabel(hostName: String): String {
        return hostName
            .removePrefix("https://")
            .removePrefix("http://")
            .removeSuffix("/")
    }
}
