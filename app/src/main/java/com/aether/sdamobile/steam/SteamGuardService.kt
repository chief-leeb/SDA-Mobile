package com.aether.sdamobile.steam

import kotlinx.coroutines.delay

data class LinkedAuthenticator(
    val sharedSecret: String,
    val identitySecret: String,
    val secret1: String,
    val serialNumber: String,
    val revocationCode: String,
    val uri: String,
    val tokenGid: String,
    val deviceId: String,
    val serverTime: Long
)

sealed class AddAuthenticatorResult {
    data class Success(val data: LinkedAuthenticator) : AddAuthenticatorResult()
    data object NoPhone : AddAuthenticatorResult()
    data object AlreadyPresent : AddAuthenticatorResult()
    data class Failure(val message: String) : AddAuthenticatorResult()
}

data class PhoneSetupResult(
    val confirmationEmail: String,
    val formattedPhone: String
)

object SteamGuardService {

    private const val BASE = "https://api.steampowered.com"

    suspend fun addAuthenticator(
        accessToken: String,
        steamId: Long,
        deviceId: String,
        serverTime: Long
    ): AddAuthenticatorResult {
        val response = SteamWeb.postJson(
            "$BASE/ITwoFactorService/AddAuthenticator/v1/?access_token=$accessToken",
            mapOf(
                "steamid" to steamId.toString(),
                "authenticator_time" to serverTime.toString(),
                "authenticator_type" to "1",
                "device_identifier" to deviceId,
                "sms_phone_id" to "1",
                "version" to "2"
            )
        ).optObject("response") ?: return AddAuthenticatorResult.Failure("Steam не ответил")

        return when (val status = response.optInt("status") ?: -1) {
            1 -> {
                val shared = response.optString("shared_secret")
                val identity = response.optString("identity_secret")
                if (shared.isNullOrBlank() || identity.isNullOrBlank()) {
                    AddAuthenticatorResult.Failure("Steam вернул неполные данные аутентификатора")
                } else {
                    AddAuthenticatorResult.Success(
                        LinkedAuthenticator(
                            sharedSecret = shared,
                            identitySecret = identity,
                            secret1 = response.optString("secret_1").orEmpty(),
                            serialNumber = response.optString("serial_number").orEmpty(),
                            revocationCode = response.optString("revocation_code").orEmpty(),
                            uri = response.optString("uri").orEmpty(),
                            tokenGid = response.optString("token_gid").orEmpty(),
                            deviceId = deviceId,
                            serverTime = response.optLong("server_time") ?: serverTime
                        )
                    )
                }
            }
            2 -> AddAuthenticatorResult.NoPhone
            29 -> AddAuthenticatorResult.AlreadyPresent
            else -> AddAuthenticatorResult.Failure("Не удалось добавить аутентификатор (код $status)")
        }
    }

    /**
     * @return server time reported during finalization
     */
    suspend fun finalizeAuthenticator(
        accessToken: String,
        steamId: Long,
        sharedSecret: String,
        activationCode: String,
        timeOffset: Long
    ): Long {
        var shift = 0L
        var lastServerTime = System.currentTimeMillis() / 1000 + timeOffset
        repeat(15) { attempt ->
            val localNow = System.currentTimeMillis() / 1000
            val serverNow = localNow + timeOffset + shift
            val code = SteamCrypto.codeForTime(sharedSecret, serverNow)
            val response = SteamWeb.postJson(
                "$BASE/ITwoFactorService/FinalizeAddAuthenticator/v1/?access_token=$accessToken",
                mapOf(
                    "steamid" to steamId.toString(),
                    "authenticator_code" to code,
                    "authenticator_time" to serverNow.toString(),
                    "activation_code" to activationCode,
                    "validate_sms_code" to "1"
                )
            ).optObject("response") ?: throw SteamApiException("Steam не ответил при активации")

            response.optLong("server_time")?.let { lastServerTime = it }
            val status = response.optInt("status") ?: -1
            if (status == 89) {
                throw SteamApiException("Неверный код из SMS", 89)
            }
            if (response.optBool("success") == true && response.optBool("want_more") != true) {
                return lastServerTime
            }
            if (response.optBool("want_more") == true || status == 88) {
                shift += 30L
                delay(1000)
            } else {
                throw SteamApiException("Не удалось активировать аутентификатор (код $status)")
            }
        }
        throw SteamApiException("Steam не смог подтвердить коды. Проверьте время на телефоне")
    }

    suspend fun removeAuthenticator(
        accessToken: String,
        steamId: Long,
        revocationCode: String
    ) {
        val response = SteamWeb.postJson(
            "$BASE/ITwoFactorService/RemoveAuthenticator/v1/?access_token=$accessToken",
            mapOf(
                "steamid" to steamId.toString(),
                "revocation_code" to revocationCode,
                "revocation_reason" to "1",
                "steamguard_scheme" to "1"
            )
        ).optObject("response") ?: throw SteamApiException("Steam не ответил")
        if (response.optBool("success") != true) {
            val attempts = response.optInt("revocation_attempts_remaining")
            throw SteamApiException(
                if (attempts != null) "Неверный код отзыва. Осталось попыток: $attempts"
                else "Steam отклонил отзыв аутентификатора"
            )
        }
    }

    suspend fun setPhoneNumber(
        accessToken: String,
        phone: String,
        countryCode: String
    ): PhoneSetupResult {
        val response = SteamWeb.postJson(
            "$BASE/IPhoneService/SetAccountPhoneNumber/v1/?access_token=$accessToken",
            mapOf(
                "phone_number" to phone,
                "phone_country_code" to countryCode
            )
        ).optObject("response") ?: throw SteamApiException("Steam не ответил")
        val email = response.optString("confirmation_email_address").orEmpty()
        val formatted = response.optString("phone_number_formatted").orEmpty()
        return PhoneSetupResult(email, formatted.ifBlank { phone })
    }

    suspend fun isWaitingForEmailConfirmation(accessToken: String): Boolean {
        val response = SteamWeb.postJson(
            "$BASE/IPhoneService/IsAccountWaitingForEmailConfirmation/v1/?access_token=$accessToken",
            emptyMap()
        ).optObject("response") ?: throw SteamApiException("Steam не ответил")
        return response.optBool("awaiting_email_confirmation") == true
    }

    suspend fun sendPhoneVerificationCode(accessToken: String) {
        SteamWeb.post(
            "$BASE/IPhoneService/SendPhoneVerificationCode/v1/?access_token=$accessToken",
            emptyMap()
        )
    }

    suspend fun getUserCountry(accessToken: String, steamId: Long): String? {
        val response = SteamWeb.postJson(
            "$BASE/IUserAccountService/GetUserCountry/v1?access_token=$accessToken",
            mapOf("steamid" to steamId.toString())
        ).optObject("response") ?: return null
        return response.optString("country")
    }
}
