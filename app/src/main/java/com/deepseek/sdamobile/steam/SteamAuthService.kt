package com.deepseek.sdamobile.steam

import kotlinx.serialization.json.JsonArray
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class AllowedGuard(val type: Int, val message: String?)

data class AuthSession(
    val clientId: Long,
    val requestId: String,
    val steamId: Long,
    val accountName: String,
    val pollInterval: Int,
    val guards: List<AllowedGuard>,
    val extendedError: String?
)

data class PollResult(
    val refreshToken: String?,
    val accessToken: String?,
    val accountName: String?,
    val newClientId: Long?
)

object SteamAuthService {

    private const val BASE = "https://api.steampowered.com"
    private const val PLATFORM_MOBILE = "3"
    private const val PERSISTENCE_PERSISTENT = "1"

    const val GUARD_NONE = 1
    const val GUARD_EMAIL_CODE = 2
    const val GUARD_DEVICE_CODE = 3
    const val GUARD_DEVICE_CONFIRM = 4
    const val GUARD_EMAIL_CONFIRM = 5
    const val GUARD_MACHINE_TOKEN = 6

    suspend fun beginSession(accountName: String, password: String): AuthSession {
        val rsaUrl = "$BASE/IAuthenticationService/GetPasswordRSAPublicKey/v1/"
            .toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("account_name", accountName)
            .build()
        val rsaResponse = SteamWeb.getJson(rsaUrl).optObject("response")
            ?: throw SteamApiException("Steam не вернул RSA-ключ")
        val modulus = rsaResponse.optString("publickey_mod")
            ?: throw SteamApiException("Steam не вернул RSA-ключ")
        val exponent = rsaResponse.optString("publickey_exp")
            ?: throw SteamApiException("Steam не вернул RSA-ключ")
        val timestamp = rsaResponse.optString("timestamp")
            ?: throw SteamApiException("Steam не вернул метку времени RSA")

        val encrypted = SteamCrypto.encryptPassword(password, modulus, exponent)
        val response = SteamWeb.postJson(
            "$BASE/IAuthenticationService/BeginAuthSessionViaCredentials/v1/",
            mapOf(
                "account_name" to accountName,
                "encrypted_password" to encrypted,
                "encryption_timestamp" to timestamp,
                "remember_login" to "true",
                "platform_type" to PLATFORM_MOBILE,
                "persistence" to PERSISTENCE_PERSISTENT,
                "website_id" to "Mobile"
            )
        ).optObject("response") ?: throw SteamApiException("Steam не начал сессию входа")

        val guards = (response["allowed_confirmations"] as? JsonArray)?.mapNotNull { item ->
            val obj = item.asObjectOrNull() ?: return@mapNotNull null
            AllowedGuard(
                type = obj.optInt("confirmation_type") ?: 0,
                message = obj.optString("associated_message")
            )
        }.orEmpty()

        val clientId = response.optLong("client_id")
            ?: throw SteamApiException("Steam не вернул идентификатор сессии")
        val requestId = response.optString("request_id")
            ?: throw SteamApiException("Steam не вернул запрос входа")
        val steamId = response.optString("steamid")?.toLongOrNull() ?: 0L

        return AuthSession(
            clientId = clientId,
            requestId = requestId,
            steamId = steamId,
            accountName = accountName,
            pollInterval = response.optInt("interval")?.coerceAtLeast(2) ?: 5,
            guards = guards,
            extendedError = response.optString("extended_error_message")
        )
    }

    suspend fun submitGuardCode(session: AuthSession, code: String, codeType: Int) {
        SteamWeb.post(
            "$BASE/IAuthenticationService/UpdateAuthSessionWithSteamGuardCode/v1/",
            mapOf(
                "client_id" to session.clientId.toString(),
                "steamid" to session.steamId.toString(),
                "code" to code,
                "code_type" to codeType.toString()
            )
        )
    }

    suspend fun poll(session: AuthSession): PollResult {
        val response = SteamWeb.postJson(
            "$BASE/IAuthenticationService/PollAuthSessionStatus/v1/",
            mapOf(
                "client_id" to session.clientId.toString(),
                "request_id" to session.requestId
            )
        ).optObject("response") ?: throw SteamApiException("Steam не ответил на опрос сессии")

        return PollResult(
            refreshToken = response.optString("refresh_token"),
            accessToken = response.optString("access_token"),
            accountName = response.optString("account_name"),
            newClientId = response.optString("new_client_id")?.toLongOrNull()
        )
    }

    suspend fun generateAccessToken(refreshToken: String, steamId: Long): String {
        val response = SteamWeb.postJson(
            "$BASE/IAuthenticationService/GenerateAccessTokenForApp/v1/",
            mapOf(
                "refresh_token" to refreshToken,
                "steamid" to steamId.toString(),
                "renewal_type" to "0"
            )
        ).optObject("response") ?: throw SteamApiException("Steam не выдал access token")
        return response.optString("access_token")
            ?: throw SteamApiException("Steam не выдал access token")
    }

    suspend fun queryServerTime(): Long {
        val response = SteamWeb.postJson(
            "$BASE/ITwoFactorService/QueryTime/v1/",
            mapOf("steamid" to "0")
        ).optObject("response") ?: throw SteamApiException("Не удалось получить время Steam")
        return response.optLong("server_time")
            ?: throw SteamApiException("Steam не вернул время")
    }
}
