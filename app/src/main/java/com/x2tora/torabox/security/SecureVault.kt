package com.x2tora.torabox.security

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
 * Android Keystore (AES-GCM) backed local storage for sensitive credentials
 * such as Personal Customs Clearance Code (PCCC) and SweetTracker API keys.
 */
object SecureVault {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "torabox_vault_aes_key"
    private const val PREFS_NAME = "torabox_secure_vault"
    private const val KEY_PCCC = "enc_pccc"
    private const val KEY_SWEET_TRACKER = "enc_sweet_tracker"
    private const val GCM_TAG_BITS = 128
    private const val IV_SIZE_BYTES = 12

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String? = runCatching {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        if (combined.size <= IV_SIZE_BYTES) return null
        val iv = combined.copyOfRange(0, IV_SIZE_BYTES)
        val ciphertext = combined.copyOfRange(IV_SIZE_BYTES, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }.getOrNull()

    fun putSecret(context: Context, key: String, value: String) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (value.isBlank()) {
            prefs.edit().remove(key).apply()
            return
        }
        val encrypted = runCatching { encrypt(value.trim()) }.getOrNull() ?: return
        prefs.edit().putString(key, encrypted).apply()
    }

    fun getSecret(context: Context, key: String): String {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encoded = prefs.getString(key, null) ?: return ""
        return decrypt(encoded).orEmpty()
    }

    fun savePersonalCustomsCode(context: Context, pccc: String) = putSecret(context, KEY_PCCC, pccc)
    fun loadPersonalCustomsCode(context: Context): String = getSecret(context, KEY_PCCC)

    fun saveSweetTrackerApiKey(context: Context, apiKey: String) = putSecret(context, KEY_SWEET_TRACKER, apiKey)
    fun loadSweetTrackerApiKey(context: Context): String = getSecret(context, KEY_SWEET_TRACKER)
}
