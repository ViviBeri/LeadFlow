package com.leadflow.app.data

import android.content.Context

/** Small wrapper around SharedPreferences for the two settings. */
class Prefs(context: Context) {
    private val sp = context.applicationContext
        .getSharedPreferences("leadflow_prefs", Context.MODE_PRIVATE)

    var template: String
        get() = sp.getString(KEY_TEMPLATE, DEFAULT_TEMPLATE) ?: DEFAULT_TEMPLATE
        set(value) { sp.edit().putString(KEY_TEMPLATE, value).apply() }

    var delayReminder: Boolean
        get() = sp.getBoolean(KEY_DELAY, true)
        set(value) { sp.edit().putBoolean(KEY_DELAY, value).apply() }

    companion object {
        const val DEFAULT_TEMPLATE = "Hello {Business Name}, we'd like to share something with you."
        private const val KEY_TEMPLATE = "template"
        private const val KEY_DELAY = "delay_reminder"
    }
}
