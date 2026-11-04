package com.irvingwu.huiniaotable

import java.util.Locale

/**
 * Lightweight localization helper for the app's existing inline copy.
 *
 * The app follows the system / per-app language. Chinese is the default language; English is
 * selected whenever the active locale starts with "en".
 */
object I18n {
    val isEnglish: Boolean
        get() = Locale.getDefault().language.startsWith("en", ignoreCase = true)
}

fun t(zh: String, en: String): String = if (I18n.isEnglish) en else zh

