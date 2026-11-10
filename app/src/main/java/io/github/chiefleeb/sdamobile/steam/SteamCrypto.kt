package io.github.chiefleeb.sdamobile.steam

import android.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.math.BigInteger
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.RSAPublicKeySpec
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object SteamCrypto {

    private const val ALPHABET = "23456789BCDFGHJKMNPQRTVWXY"
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun codeForTime(sharedSecret: String, unixTime: Long): String {
        val key = Base64.decode(sharedSecret.trim(), Base64.DEFAULT)
        val counter = unixTime / 30L
        val data = ByteArray(8)
        var value = counter
        for (i in 7 downTo 0) {
            data[i] = (value and 0xFF).toByte()
            value = value shr 8
        }
        val mac = Mac.getInstance("HmacSHA1").apply {
            init(SecretKeySpec(key, "HmacSHA1"))
        }
        val hash = mac.doFinal(data)
        val offset = hash[19].toInt() and 0x0F
        var binary = ((hash[offset].toInt() and 0x7F) shl 24) or
                ((hash[offset + 1].toInt() and 0xFF) shl 16) or
                ((hash[offset + 2].toInt() and 0xFF) shl 8) or
                (hash[offset + 3].toInt() and 0xFF)
        val out = StringBuilder(5)
        repeat(5) {
            out.append(ALPHABET[binary % ALPHABET.length])
            binary /= ALPHABET.length
        }
        return out.toString()
    }

    fun confirmationKey(identitySecret: String, unixTime: Long, tag: String): String {
        val key = Base64.decode(identitySecret.trim(), Base64.DEFAULT)
        val tagBytes = tag.toByteArray(Charsets.UTF_8).let {
            if (it.size > 32) it.copyOf(32) else it
        }
        val data = ByteArray(8 + tagBytes.size)
        var value = unixTime
        for (i in 7 downTo 0) {
            data[i] = (value and 0xFF).toByte()
            value = value shr 8
        }
        System.arraycopy(tagBytes, 0, data, 8, tagBytes.size)
        val mac = Mac.getInstance("HmacSHA1").apply {
            init(SecretKeySpec(key, "HmacSHA1"))
        }
        return Base64.encodeToString(mac.doFinal(data), Base64.NO_WRAP)
    }

    fun encryptPassword(password: String, modulusHex: String, exponentHex: String): String {
        val modulus = BigInteger(modulusHex, 16)
        val exponent = BigInteger(exponentHex, 16)
        val publicKey = KeyFactory.getInstance("RSA")
            .generatePublic(RSAPublicKeySpec(modulus, exponent))
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return Base64.encodeToString(cipher.doFinal(password.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }

    fun deviceIdFor(steamId: String): String {
        val digest = MessageDigest.getInstance("SHA-1")
            .digest(steamId.toByteArray(Charsets.UTF_8))
        val hex = digest.joinToString("") { "%02x".format(it) }
        val uuid = "${hex.substring(0, 8)}-${hex.substring(8, 12)}-${hex.substring(12, 16)}-" +
                "${hex.substring(16, 20)}-${hex.substring(20, 32)}"
        return "android:$uuid"
    }

    fun newDeviceId(): String = "android:${UUID.randomUUID()}"

    fun newSessionId(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun secondsLeftInWindow(serverTime: Long): Int {
        val mod = (serverTime % 30L).toInt()
        return if (mod == 0) 30 else 30 - mod
    }

    fun jwtExpiry(token: String): Long? = try {
        val payload = token.split('.')[1]
        val decoded = Base64.decode(payload.replace('-', '+').replace('_', '/'), Base64.NO_WRAP)
        json.parseToJsonElement(decoded.toString(Charsets.UTF_8))
            .jsonObject["exp"]?.jsonPrimitive?.longOrNull
    } catch (_: Exception) {
        null
    }

    fun jwtSteamId(token: String): Long? = try {
        val payload = token.split('.')[1]
        val decoded = Base64.decode(payload.replace('-', '+').replace('_', '/'), Base64.NO_WRAP)
        json.parseToJsonElement(decoded.toString(Charsets.UTF_8))
            .jsonObject["sub"]?.jsonPrimitive?.content?.toLongOrNull()
    } catch (_: Exception) {
        null
    }
}
