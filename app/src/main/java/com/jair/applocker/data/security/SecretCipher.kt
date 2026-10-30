package com.jair.applocker.data.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Cifra secretos chicos para guardarlos en disco. */
interface SecretCipher {
    fun encrypt(plain: ByteArray): String

    /** @throws GeneralSecurityException si no se puede descifrar, por ejemplo porque se perdió la clave. */
    fun decrypt(stored: String): ByteArray
}

/**
 * Cifra con AES-256-GCM usando una clave del Android Keystore, que no se puede exportar del
 * teléfono. El secreto TOTP queda cifrado en disco y solo se descifra en memoria al verificar un
 * código.
 *
 * Se usa AES en vez de importar el secreto al Keystore como clave HMAC porque las apps de
 * autenticación esperan HMAC-SHA1, y no todos los chips seguros lo soportan.
 */
class KeystoreSecretCipher(private val alias: String = "applocker_holiday_secret") : SecretCipher {

    override fun encrypt(plain: ByteArray): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, existingKey() ?: generateKey()) }
        val encoder = Base64.getEncoder()
        return encoder.encodeToString(cipher.iv) + SEPARATOR + encoder.encodeToString(cipher.doFinal(plain))
    }

    override fun decrypt(stored: String): ByteArray {
        val parts = stored.split(SEPARATOR)
        if (parts.size != 2) throw GeneralSecurityException("Formato de secreto inválido")
        val (iv, data) = try {
            parts.map { Base64.getDecoder().decode(it) }
        } catch (e: IllegalArgumentException) {
            throw GeneralSecurityException("Formato de secreto inválido", e)
        }
        val key = existingKey() ?: throw GeneralSecurityException("No existe la clave $alias")

        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        }
        return cipher.doFinal(data)
    }

    private fun existingKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.secretKey
    }

    private fun generateKey(): SecretKey {
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            .apply { init(spec) }
            .generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_TAG_BITS = 128
        const val SEPARATOR = ":"
    }
}
