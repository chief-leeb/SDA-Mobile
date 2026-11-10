package io.github.chiefleeb.sdamobile.i18n

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import java.util.Locale

enum class AppLanguage(val tag: String, val nativeName: String) {
    RU("ru", "Русский"),
    EN("en", "English")
}

/**
 * App language is stored in SharedPreferences. The activity is recreated after a change,
 * so all strings resolve against the new language.
 */
object LanguageState {
    private const val PREFS = "sda_settings"
    private const val KEY = "app_language"

    @Volatile
    var current: AppLanguage = AppLanguage.RU
        private set

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val saved = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        current = when (saved) {
            AppLanguage.EN.tag -> AppLanguage.EN
            AppLanguage.RU.tag -> AppLanguage.RU
            else -> if (Locale.getDefault().language.equals("ru", ignoreCase = true)) {
                AppLanguage.RU
            } else {
                AppLanguage.EN
            }
        }
        Locale.setDefault(Locale.forLanguageTag(current.tag))
        initialized = true
    }

    fun set(context: Context, language: AppLanguage) {
        current = language
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, language.tag)
            .apply()
        Locale.setDefault(Locale.forLanguageTag(language.tag))
        initialized = true
    }
}

/** Translate a static string. The Russian text is the lookup key. */
fun tr(ru: String): String =
    if (LanguageState.current == AppLanguage.EN) EN_STRINGS[ru] ?: ru else ru

/** Translate a string with placeholders or a different word order. */
fun tr(ru: String, en: String): String =
    if (LanguageState.current == AppLanguage.EN) en else ru

/** Persist the chosen language and recreate the activity so every screen re-reads it. */
fun switchLanguage(context: Context, language: AppLanguage) {
    LanguageState.set(context, language)
    context.findActivity()?.recreate()
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
