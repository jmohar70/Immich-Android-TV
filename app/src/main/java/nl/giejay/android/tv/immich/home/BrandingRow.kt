package nl.giejay.android.tv.immich.home

import androidx.leanback.widget.HeaderItem
import androidx.leanback.widget.Row

/**
 * A single, fixed, non-selectable row shown at the very top of the left
 * navigation panel (above "Albums"), displaying the Immich logo and the
 * currently connected server. Uses a blank-named HeaderItem defensively so
 * that any code path referencing row.headerItem.name (e.g. the edit-mode
 * click handler in HomeFragment) doesn't hit a null pointer, even though
 * this row's view is deliberately made non-focusable so it should never
 * actually be reachable via remote/D-pad navigation or clicks.
 */
class BrandingRow : Row(HeaderItem(-1L, ""))
