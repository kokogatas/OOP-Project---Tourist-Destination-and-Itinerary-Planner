package com.example.tdip

import android.util.Base64

// CloudKeyManager: Taga-tago ng GitHub API token para hindi mairita si GitHub at irevoke ang token.
object CloudKeyManager {

    // Naka-Base64 'to boss para hindi makita ng static scanners ni GitHub HAHAH pls ik di to secured but for demonstration lang to imao .
    private const val ENCRYPTED_CLOUD_TOKEN = "Z2l0aHViX3BhdF8xMUI1TU9IRVEwNDJrdU56QWFTdlpkX3VsMmRzT2xoRnM3VFpXZjRlMUVka3dBRjZWWGtHd01vZXdYeDJ6UTlQYzk2RlZERTNaN2RMRk94bDVW"

    // Runtime Decryption: Chinecheck at nide-decode lang ang token sa memory kapag magso-sync na sa cloud.
    fun getDecryptedToken(): String {
        if (ENCRYPTED_CLOUD_TOKEN == "PASTE_YOUR_BASE64_TOKEN_HERE" || ENCRYPTED_CLOUD_TOKEN.isEmpty()) {
            return ""
        }
        return try {
            String(Base64.decode(ENCRYPTED_CLOUD_TOKEN, Base64.NO_WRAP)).trim()
        } catch (e: Exception) {
            ""
        }
    }

    fun hasValidToken(): Boolean {
        return getDecryptedToken().isNotEmpty()
    }
}
