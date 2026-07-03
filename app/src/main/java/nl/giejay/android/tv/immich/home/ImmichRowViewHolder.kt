package nl.giejay.android.tv.immich.home

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.RowHeaderPresenter
import nl.giejay.android.tv.immich.R

class ImmichRowViewHolder(view: View) : RowHeaderPresenter.ViewHolder(view) {
    val tvTitle: TextView
    val icon: ImageView
    val focusIndicator: View
    init {
        tvTitle = view.findViewById(R.id.tvTitle)
        icon = view.findViewById(R.id.row_visible)
        focusIndicator = view.findViewById(R.id.focus_indicator)
    }
}