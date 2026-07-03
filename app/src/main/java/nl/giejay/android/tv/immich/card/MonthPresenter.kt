package nl.giejay.android.tv.immich.card

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.ContextThemeWrapper
import android.widget.ImageView
import androidx.leanback.widget.ImageCardView
import com.bumptech.glide.Glide
import nl.giejay.android.tv.immich.R
import nl.giejay.android.tv.immich.shared.presenter.AbstractPresenter

/**
 * Small "chip" style presenter used by the Timeline's year/month picker.
 * Shows a representative photo thumbnail from that month when available;
 * falls back to a stable, generated background color per month/year label
 * (e.g. while the thumbnail is still loading, or if the month has no
 * assets for some reason).
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
        cardView.mainImageView!!.scaleType = ImageView.ScaleType.CENTER
        cardView.mainImageView!!.setImageDrawable(ColorDrawable(color))

        if (card.thumbnailUrl != null) {
            Glide.with(context)
                .asBitmap()
                .centerCrop()
                .load(card.thumbnailUrl)
                .into(cardView.mainImageView!!)
            cardView.mainImageView!!.scaleType = ImageView.ScaleType.CENTER_CROP
        }

        setSelected(cardView, card.selected)
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        super.onUnbindViewHolder(viewHolder)
        try {
            Glide.with(context).clear((viewHolder.view as ImageCardView).mainImageView!!)
        } catch (e: IllegalArgumentException) {
            // view already detached, nothing to clean up
        }
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
