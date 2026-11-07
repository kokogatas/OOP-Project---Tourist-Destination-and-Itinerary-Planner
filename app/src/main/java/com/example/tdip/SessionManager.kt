package com.example.tdip

import android.content.Context
import android.content.SharedPreferences

// SessionManager: Taga-alala kung sino ang huling nag-login para hindi paulit-ulit ang login screen.
object SessionManager {

    private const val PREF_NAME = "TDIP_USER_SESSION"
    private const val KEY_IS_LOGGED_IN = "isLoggedIn"
    private const val KEY_ENCRYPTED_EMAIL = "encryptedEmail"
    private const val KEY_ENCRYPTED_NAME = "encryptedName"
    private const val KEY_USER_ROLE = "userRole"
    private const val KEY_ENCRYPTED_RESORT = "encryptedResort"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    // Save session: Tinatago nang naka-encrypt sa cellphone.
    fun saveSession(context: Context, user: Person) {
        val editor = getPreferences(context).edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_ENCRYPTED_EMAIL, SecurityUtils.encryptData(user.email))
        editor.putString(KEY_ENCRYPTED_NAME, SecurityUtils.encryptData(user.name))

        if (user is ResortAdmin) {
            editor.putString(KEY_USER_ROLE, "Admin")
            editor.putString(KEY_ENCRYPTED_RESORT, SecurityUtils.encryptData(user.resortName))
        } else {
            editor.putString(KEY_USER_ROLE, "Customer")
        }

        editor.apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getSessionData(context: Context): Map<String, String> {
        val prefs = getPreferences(context)
        val encryptedEmail = prefs.getString(KEY_ENCRYPTED_EMAIL, "") ?: ""
        val encryptedName = prefs.getString(KEY_ENCRYPTED_NAME, "") ?: ""
        val encryptedResort = prefs.getString(KEY_ENCRYPTED_RESORT, "") ?: ""
        val role = prefs.getString(KEY_USER_ROLE, "Customer") ?: "Customer"

        return mapOf(
            "email" to SecurityUtils.decryptData(encryptedEmail),
            "name" to SecurityUtils.decryptData(encryptedName),
            "resort" to SecurityUtils.decryptData(encryptedResort),
            "role" to role,
            "rawEncryptedEmail" to encryptedEmail,
            "rawEncryptedName" to encryptedName
        )
    }

    // Clear session kapag nag-logout para walang makigamit ng account mo.
    fun clearSession(context: Context) {
        val editor = getPreferences(context).edit()
        editor.clear()
        editor.apply()
    }
}
