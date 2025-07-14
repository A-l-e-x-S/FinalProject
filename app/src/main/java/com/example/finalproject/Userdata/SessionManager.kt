package com.example.finalproject.Userdata

import android.content.Context

object SessionManager {

    private const val PREF_NAME = "user_session"
    private const val KEY_USER_UID = "user_uid"

    fun saveUserSession(context: Context, uid: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER_UID, uid).apply()
    }

    fun getUserSession(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USER_UID, null)
    }

    fun clearUserSession(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}
