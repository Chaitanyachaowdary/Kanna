package com.example.data.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Supplies the passphrase used to open the SQLCipher-encrypted Room database.
 *
 * A random 256-bit passphrase is generated once on first launch and then
 * *wrapped* (encrypted) with a non-exportable AES key held in the Android
 * Keystore. Only the wrapped blob is persisted (in private SharedPreferences),
 * so the raw passphrase never touches disk in the clear and cannot be recovered
 * without the hardware-backed Keystore key.
 */
object DatabaseKeyProvider {

    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val WRAP_KEY_ALIAS = "aura_db_passphrase_wrapper"
    private const val PREFS_NAME = "aura_secure_db_prefs"
    private const val PREF_WRAPPED = "wrapped_passphrase"
    private const val PREF_IV = "wrap_iv"
    private const val GCM_TAG_BITS = 128
    private const val PASSPHRASE_BYTES = 32
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    /**
     * True when [getOrCreatePassphrase] generated a brand-new passphrase during
     * the most recent call — i.e. encryption was being set up for the first
     * time. Callers use this to discard any pre-existing plaintext database that
     * SQLCipher would be unable to open.
     */
    @Volatile
    var passphraseWasJustCreated: Boolean = false
        private set

    /** Returns the (decrypted) database passphrase, creating and persisting it if needed. */
    fun getOrCreatePassphrase(context: Context): ByteArray {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val wrappedB64 = prefs.getString(PREF_WRAPPED, null)
        val ivB64 = prefs.getString(PREF_IV, null)
        if (wrappedB64 != null && ivB64 != null) {
            passphraseWasJustCreated = false
            return unwrap(
                Base64.decode(wrappedB64, Base64.NO_WRAP),
                Base64.decode(ivB64, Base64.NO_WRAP),
            )
        }

        val passphrase = ByteArray(PASSPHRASE_BYTES).also { SecureRandom().nextBytes(it) }
        val (wrapped, iv) = wrap(passphrase)
        prefs.edit()
            .putString(PREF_WRAPPED, Base64.encodeToString(wrapped, Base64.NO_WRAP))
            .putString(PREF_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
            .apply()
        passphraseWasJustCreated = true
        return passphrase
    }

    private fun getOrCreateWrapKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(WRAP_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)
            ?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE,
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                WRAP_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun wrap(data: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateWrapKey())
        return cipher.doFinal(data) to cipher.iv
    }

    private fun unwrap(wrapped: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateWrapKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(wrapped)
    }
}
