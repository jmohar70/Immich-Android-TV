package nl.giejay.android.tv.immich.shared.prefs

import nl.giejay.android.tv.immich.ImmichApplication
import nl.giejay.android.tv.immich.R

enum class AppLanguage(val localeTag: String?) : EnumWithTitle {
    SYSTEM(null) {
        override fun getTitle(): String {
            return ImmichApplication.appContext!!.getString(R.string.language_system)
        }
    },
    ENGLISH("en") {
        override fun getTitle(): String {
            return ImmichApplication.appContext!!.getString(R.string.language_english)
        }
    },
    GERMAN("de") {
        override fun getTitle(): String {
            return ImmichApplication.appContext!!.getString(R.string.language_german)
        }
    };

    companion object {
        fun valueOfSafe(name: String, default: AppLanguage): AppLanguage {
            return entries.find { it.toString() == name } ?: default
        }
    }
}
