package com.example.data.security

import android.util.Base64
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {
    private val salt = "AuraSalt123".toByteArray() // Stationary salt for deterministic key derivation
    private val iv = ByteArray(16) { 0 } // Simple stationary IV for simplicity on-device

    private fun deriveKey(passphrase: String): SecretKeySpec {
        val keySpec: KeySpec = PBEKeySpec(passphrase.toCharArray(), salt, 65536, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val tmp = factory.generateSecret(keySpec)
        return SecretKeySpec(tmp.encoded, "AES")
    }

    fun encrypt(data: String, passphrase: String): String {
        if (data.isEmpty() || passphrase.isEmpty()) return data
        return try {
            val secretKey = deriveKey(passphrase)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(iv))
            val encryptedBytes = cipher.doFinal(data.toByteArray(Charsets.UTF_8))
            Base64.encodeToString(encryptedBytes, Base64.DEFAULT).trim()
        } catch (e: Exception) {
            data
        }
    }

    fun decrypt(encryptedData: String, passphrase: String): String {
        if (encryptedData.isEmpty() || passphrase.isEmpty()) return encryptedData
        return try {
            val secretKey = deriveKey(passphrase)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(iv))
            val decodedBytes = Base64.decode(encryptedData, Base64.DEFAULT)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            encryptedData
        }
    }
}
