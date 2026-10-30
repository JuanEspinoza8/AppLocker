package com.jair.applocker.domain

import java.nio.ByteBuffer
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Códigos de un solo uso basados en el tiempo (TOTP, RFC 6238) con HMAC-SHA1, 6 dígitos y
 * pasos de 30 segundos: los parámetros que usan Google Authenticator y las demás apps de
 * autenticación.
 */
object Totp {
    const val DIGITS = 6
    const val PERIOD_SECONDS = 30L

    /** Tamaño recomendado del secreto por RFC 4226: 160 bits. */
    const val SECRET_BYTES = 20

    /** Pasos aceptados antes y después del actual, por si los relojes no están sincronizados. */
    private const val ALLOWED_DRIFT_STEPS = 1

    fun timeStep(epochMillis: Long): Long = Math.floorDiv(epochMillis / 1000, PERIOD_SECONDS)

    fun code(secret: ByteArray, step: Long, digits: Int = DIGITS): String {
        val mac = Mac.getInstance("HmacSHA1").apply { init(SecretKeySpec(secret, "HmacSHA1")) }
        val hash = mac.doFinal(ByteBuffer.allocate(8).putLong(step).array())

        // Truncamiento dinámico (RFC 4226, sección 5.3)
        val offset = hash.last().toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)

        var modulo = 1
        repeat(digits) { modulo *= 10 }
        return (binary % modulo).toString().padStart(digits, '0')
    }

    /**
     * Devuelve el paso de tiempo al que corresponde [code], o `null` si no es válido. Solo acepta
     * pasos posteriores a [lastUsedStep], así un código no se puede usar dos veces.
     */
    fun matchStep(secret: ByteArray, code: String, epochMillis: Long, lastUsedStep: Long): Long? {
        if (code.length != DIGITS || !code.all { it.isDigit() }) return null

        val current = timeStep(epochMillis)
        return (current - ALLOWED_DRIFT_STEPS..current + ALLOWED_DRIFT_STEPS)
            .filter { it > lastUsedStep }
            .firstOrNull { step ->
                MessageDigest.isEqual(code(secret, step).toByteArray(), code.toByteArray())
            }
    }

    /** URI `otpauth://` que las apps de autenticación leen desde un código QR. */
    fun otpauthUri(secret: ByteArray, issuer: String, account: String): String =
        "otpauth://totp/$issuer:$account?secret=${Base32.encode(secret)}&issuer=$issuer" +
            "&algorithm=SHA1&digits=$DIGITS&period=$PERIOD_SECONDS"
}

/** Codificación Base32 (RFC 4648) sin relleno, el formato de los secretos TOTP. */
object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun encode(data: ByteArray): String {
        val out = StringBuilder()
        var buffer = 0
        var bits = 0
        for (byte in data) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bits += 8
            while (bits >= 5) {
                out.append(ALPHABET[(buffer shr (bits - 5)) and 0x1F])
                bits -= 5
            }
        }
        if (bits > 0) out.append(ALPHABET[(buffer shl (5 - bits)) and 0x1F])
        return out.toString()
    }
}
