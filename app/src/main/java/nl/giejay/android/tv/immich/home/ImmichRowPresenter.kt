package nl.giejay.android.tv.immich.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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

        // Registering the global focus listener directly here (in onCreateViewHolder) is
        // unreliable: at this point the view has been inflated but is NOT YET attached to
        // the window, so root.viewTreeObserver returns a temporary, detached observer.
        // Listeners added to it can silently fail to carry over once the view actually
        // attaches - which matches exactly what was reported: rows freshly scrolled into
        // view for the first time (created right as they're about to receive focus) missed
        // the indicator, while rows that already existed and were re-focused later worked
        // fine. Registering on actual window-attachment guarantees we always use the real,
        // live ViewTreeObserver.
        var focusListener: ViewTreeObserver.OnGlobalFocusChangeListener? = null
        root.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {
                focusListener = ViewTreeObserver.OnGlobalFocusChangeListener { _, newFocus ->
                    viewHolder.focusIndicator.visibility = if (newFocus == viewHolder.tvTitle) View.VISIBLE else View.INVISIBLE
                }
                v.viewTreeObserver.addOnGlobalFocusChangeListener(focusListener)
            }

            override fun onViewDetachedFromWindow(v: View) {
                focusListener?.let { v.viewTreeObserver.removeOnGlobalFocusChangeListener(it) }
                focusListener = null
            }
        })
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
