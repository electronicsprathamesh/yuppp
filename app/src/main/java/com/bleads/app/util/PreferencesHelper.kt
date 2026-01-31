package com.bleads.app.util

import android.content.Context
import android.content.SharedPreferences
import com.bleads.app.data.User

class PreferencesHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "BLE_SCAN_PREFS"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveUser(user: User) {
        prefs.edit().apply {
            putString(KEY_USER_NAME, user.name)
            putString(KEY_USER_PHONE, user.phone)
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getUser(): User? {
        val name = prefs.getString(KEY_USER_NAME, null)
        val phone = prefs.getString(KEY_USER_PHONE, null)
        return if (name != null && phone != null) {
            User(name, phone)
        } else {
            null
        }
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getUser() != null
    }

    fun clearUser() {
        prefs.edit().apply {
            remove(KEY_USER_NAME)
            remove(KEY_USER_PHONE)
            putBoolean(KEY_IS_LOGGED_IN, false)
            apply()
        }
    }
}
