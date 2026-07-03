package nl.giejay.android.tv.immich.card

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ContextThemeWrapper
import android.widget.ImageView
import androidx.leanback.widget.ImageCardView
import nl.giejay.android.tv.immich.R
import nl.giejay.android.tv.immich.shared.presenter.AbstractPresenter

/**
 * Small "chip" style presenter used by the Timeline's year/month picker.
 * Instead of a photo thumbnail it shows a stable, generated background
 * color per month/year label, since fetching a representative thumbnail
 * for every bucket up front would be expensive.
 */
class MonthPresenter(context: Context, style: Int = R.style.DefaultCardTheme) :
    AbstractPresenter<ImageCardView, ICard>(ContextThemeWrapper(context, style)) {

    override fun onCreateView(): ImageCardView {
        val cardView = ImageCardView(context)
        cardView.setMainImageDimensions(300, 100)
        return cardView
    }

    override fun onBindViewHolder(card: ICard, cardView: ImageCardView) {
        cardView.tag = card
        cardView.titleText = card.title
        cardView.contentText = card.description

        val color = generateColor(card.title)
        cardView.mainImageView!!.setImageDrawable(ColorDrawable(color))
        cardView.mainImageView!!.scaleType = ImageView.ScaleType.CENTER

        setSelected(cardView, card.selected)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        super.onUnbindViewHolder(viewHolder)
        (viewHolder.view as ImageCardView).mainImageView!!.setImageDrawable(null)
    }

    private fun generateColor(str: String): Int {
        val hash = str.hashCode()
        val r = (hash and 0xFF0000 shr 16)
        val g = (hash and 0x00FF00 shr 8)
        val b = (hash and 0x0000FF)
        // Muted/darker tones so white text stays readable
        return Color.rgb((r + 64) / 2, (g + 64) / 2, (b + 64) / 2)
    }

    private fun setSelected(imageCardView: ImageCardView, selected: Boolean) {
        imageCardView.mainImageView!!.background = if (selected) context.getDrawable(R.drawable.border) else null
    }
}
