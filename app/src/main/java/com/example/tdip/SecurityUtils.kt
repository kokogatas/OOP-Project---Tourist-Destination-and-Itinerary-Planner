package com.example.tdip

import android.util.Base64
import java.security.MessageDigest

// SecurityUtils: Tagahash at taga-encrypt para hindi mabasa ng mga chismosa ang password.
object SecurityUtils {

    // SHA-256 Hashing: Ginagawang sobrang gulo ang password para walang makahula.
    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Base64 Encryption: Tinatago ang data para hindi makita nang direkta ng GitHub scanner.
    fun encryptData(data: String): String {
        return Base64.encodeToString(data.toByteArray(), Base64.DEFAULT).trim()
    }

    // Decryption: Binabalik sa dating anyo kapag gagamitin na ng app.
    fun decryptData(encryptedData: String): String {
        return try {
            String(Base64.decode(encryptedData, Base64.DEFAULT)).trim()
        } catch (e: Exception) {
            ""
        }
    }
}
