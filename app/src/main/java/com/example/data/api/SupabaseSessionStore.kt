package com.example.data.api

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores Supabase session tokens encrypted with an Android Keystore AES-GCM key.
 * The encryption key is non-exportable and bound to this Android installation.
 */
class SupabaseSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("supabase_secure_session", Context.MODE_PRIVATE)
    private val alias = "loy_pos_supabase_session_key"

    @Synchronized
    fun save(accessToken: String, refreshToken: String) {
        val access = encrypt(accessToken)
        val refresh = encrypt(refreshToken)
        prefs.edit()
            .putString("access_iv", access.first).putString("access_value", access.second)
            .putString("refresh_iv", refresh.first).putString("refresh_value", refresh.second)
            .apply()
    }

    @Synchronized
    fun accessToken(): String? = decrypt("access")

    @Synchronized
    fun refreshToken(): String? = decrypt("refresh")

    @Synchronized
    fun clear() { prefs.edit().clear().apply() }

    private fun decrypt(prefix: String): String? {
        val iv = prefs.getString(prefix + "_iv", null) ?: return null
        val value = prefs.getString(prefix + "_value", null) ?: return null
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)))
            String(cipher.doFinal(Base64.decode(value, Base64.NO_WRAP)), Charsets.UTF_8)
        } catch (_: Exception) {
            clear()
            null
        }
    }

    private fun encrypt(value: String): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv, Base64.NO_WRAP) to Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
