package com.deepseek.sdamobile.data

import android.util.Base64
import com.deepseek.sdamobile.steam.SteamCrypto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.net.URLEncoder
import java.util.UUID

object MaFile {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun parse(text: String): SdaAccount {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) throw MaFileException("Файл пуст")
        if (!trimmed.startsWith("{")) {
            throw MaFileException(
                "Это не maFile в формате JSON. Зашифрованные maFiles нужно сначала расшифровать в SDA."
            )
        }
        val root = try {
            json.parseToJsonElement(trimmed).jsonObject
        } catch (e: Exception) {
            throw MaFileException("Некорректный JSON: ${e.message}")
        }

        if (root["encrypted"]?.jsonPrimitive?.content == "true" && root["entries"] != null) {
            throw MaFileException("Это manifest.json, а не maFile. Выберите файл *.maFile.")
        }

        val shared = root.string("shared_secret")
            ?: throw MaFileException("В maFile нет shared_secret")
        if (!isBase64Secret(shared)) {
            throw MaFileException("shared_secret повреждён")
        }

        val identity = root.string("identity_secret").orEmpty()
        if (identity.isNotEmpty() && !isBase64Secret(identity)) {
            throw MaFileException("identity_secret повреждён")
        }

        val sessionObj = root["Session"] as? JsonObject
        val steamId = sessionObj?.string("SteamID")?.toLongOrNull()
            ?: root.string("steamid")?.toLongOrNull()
            ?: root.string("steam_id")?.toLongOrNull()
            ?: 0L

        val accountName = root.string("account_name").orEmpty()
        val deviceId = root.string("device_id").orEmpty().ifBlank {
            if (steamId != 0L) SteamCrypto.deviceIdFor(steamId.toString())
            else "android:${UUID.randomUUID()}"
        }

        val now = System.currentTimeMillis() / 1000
        val account = SdaAccount(
            localId = UUID.randomUUID().toString(),
            sharedSecret = shared.trim(),
            serialNumber = root.string("serial_number").orEmpty(),
            revocationCode = root.string("revocation_code").orEmpty(),
            uri = root.string("uri").orEmpty().ifBlank { buildUri(accountName, shared) },
            serverTime = root.string("server_time")?.toLongOrNull() ?: 0L,
            accountName = accountName,
            tokenGid = root.string("token_gid").orEmpty(),
            identitySecret = identity,
            secret1 = root.string("secret_1").orEmpty(),
            status = root.string("status")?.toIntOrNull() ?: 1,
            deviceId = deviceId,
            fullyEnrolled = root.string("fully_enrolled")?.toBooleanStrictOrNull() ?: true,
            session = steamId.takeIf { it != 0L }?.let { id ->
                SteamSession(
                    steamId = id,
                    accessToken = sessionObj?.string("AccessToken").orEmpty(),
                    refreshToken = sessionObj?.string("RefreshToken").orEmpty(),
                    sessionId = sessionObj?.string("SessionID").orEmpty()
                )
            },
            accessTokenAt = 0,
            addedAt = now
        )
        if (account.accountName.isBlank()) {
            throw MaFileException("В maFile нет account_name")
        }
        return account
    }

    fun encode(account: SdaAccount): String {
        val obj = buildJsonObject {
            put("shared_secret", JsonPrimitive(account.sharedSecret))
            put("serial_number", JsonPrimitive(account.serialNumber))
            put("revocation_code", JsonPrimitive(account.revocationCode))
            put(
                "uri",
                JsonPrimitive(
                    account.uri.ifBlank { buildUri(account.accountName, account.sharedSecret) }
                )
            )
            put("server_time", JsonPrimitive(account.serverTime.toString()))
            put("account_name", JsonPrimitive(account.accountName))
            put("token_gid", JsonPrimitive(account.tokenGid))
            put("identity_secret", JsonPrimitive(account.identitySecret))
            put("secret_1", JsonPrimitive(account.secret1))
            put("status", JsonPrimitive(account.status))
            put("device_id", JsonPrimitive(account.deviceId))
            put("fully_enrolled", JsonPrimitive(account.fullyEnrolled))
            account.session?.let { session ->
                put(
                    "Session",
                    buildJsonObject {
                        put("SteamID", JsonPrimitive(session.steamId.toString()))
                        put("AccessToken", JsonPrimitive(session.accessToken))
                        put("RefreshToken", JsonPrimitive(session.refreshToken))
                        put("SessionID", JsonPrimitive(session.sessionId))
                    }
                )
            }
        }
        return obj.toString()
    }

    fun fileName(account: SdaAccount): String {
        val base = account.accountName.ifBlank { "steam" }
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return "$base.maFile"
    }

    private fun buildUri(accountName: String, sharedSecret: String): String {
        val label = URLEncoder.encode("Steam:$accountName", "UTF-8")
        return "otpauth://totp/$label?secret=$sharedSecret&issuer=Steam"
    }

    private fun isBase64Secret(value: String): Boolean = try {
        Base64.decode(value.trim(), Base64.DEFAULT).size == 20
    } catch (_: Exception) {
        false
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
}

class MaFileException(message: String) : Exception(message)
