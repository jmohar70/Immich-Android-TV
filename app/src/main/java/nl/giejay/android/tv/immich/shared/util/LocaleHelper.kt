package nl.giejay.android.tv.immich.shared.util

import android.content.Context
import android.content.res.Configuration
import androidx.preference.PreferenceManager
import nl.giejay.android.tv.immich.shared.prefs.AppLanguage
import java.util.Locale

/**
 * Applies a user-chosen app language (independent of the Fire TV/Android device's
 * system language) by wrapping a Context with a Configuration that has the
 * requested Locale. Must be called from both Application.attachBaseContext and
 * Activity.attachBaseContext - the Application's wrapped resources are what
 * backs all the ImmichApplication.appContext!!.getString(...) calls used by the
 * preference definitions, while the Activity's wrapped resources back
 * Fragment/View resource lookups.
 *
 * Reads the raw SharedPreferences value directly (key must match
 * AppLanguage's Pref.key(), i.e. the lowercase class name "app_language")
 * since this can run before PreferenceManager.init() has been called.
 */
object LocaleHelper {
    private const val PREF_KEY = "app_language"

    fun wrap(context: Context): Context {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val stored = prefs.getString(PREF_KEY, AppLanguage.SYSTEM.toString()) ?: AppLanguage.SYSTEM.toString()
        val language = AppLanguage.valueOfSafe(stored, AppLanguage.SYSTEM)
        val localeTag = language.localeTag ?: return context

        val locale = Locale(localeTag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
