package nl.giejay.android.tv.immich.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import androidx.leanback.widget.Row
import nl.giejay.android.tv.immich.R
import nl.giejay.android.tv.immich.shared.prefs.HIDDEN_HOME_ITEMS
import nl.giejay.android.tv.immich.shared.prefs.PreferenceManager

class ImmichRowPresenter : Presenter() {
    var editMode: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup): ImmichRowViewHolder {
        val root: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.presenter_row, parent, false)

        val viewHolder = ImmichRowViewHolder(root)
        // A plain OnFocusChangeListener on tvTitle didn't reliably fire for every row once
        // Leanback's RecyclerView started recycling/reusing view holders. A global focus
        // observer comparing against this row's own tvTitle instance is more robust, since
        // it doesn't depend on the listener surviving whatever Leanback does internally with
        // focus during recycling.
        root.viewTreeObserver.addOnGlobalFocusChangeListener { _, newFocus ->
            viewHolder.focusIndicator.visibility = if (newFocus == viewHolder.tvTitle) View.VISIBLE else View.INVISIBLE
        }
        return viewHolder
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any?) {
        val headerItem = if (item == null) null else (item as Row).headerItem
        val vh = viewHolder as ImmichRowViewHolder
        vh.tvTitle.text = headerItem?.name
        vh.focusIndicator.visibility = if (vh.tvTitle.isFocused) View.VISIBLE else View.INVISIBLE

        if (editMode) {
            if (headerItem?.name == viewHolder.view.context.getString(R.string.edit)) {
                vh.tvTitle.text = viewHolder.view.context.getString(R.string.done)
            } else {
                vh.icon.visibility = View.VISIBLE
                if (PreferenceManager.itemInStringSet(headerItem?.name, HIDDEN_HOME_ITEMS)) {
                    vh.icon.setImageResource(R.drawable.closed_eye)
                } else {
                    vh.icon.setImageResource(R.drawable.visible_eye)
                }
            }
        } else {
            vh.icon.visibility = View.GONE
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val vh = viewHolder as ImmichRowViewHolder
        vh.tvTitle.text = null
        vh.focusIndicator.visibility = View.INVISIBLE
    }

}
