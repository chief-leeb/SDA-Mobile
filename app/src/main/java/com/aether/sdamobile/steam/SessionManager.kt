package com.aether.sdamobile.steam

import com.aether.sdamobile.data.AccountRepository
import com.aether.sdamobile.data.SdaAccount

object SessionManager {

    suspend fun freshAccessToken(
        account: SdaAccount,
        repository: AccountRepository,
        force: Boolean = false
    ): String {
        if (account.steamId == 0L || account.refreshToken.isBlank()) {
            throw SteamNotLoggedInException("Нужно войти в Steam, чтобы включить подтверждения")
        }

        val existing = account.accessToken
        if (!force && existing.isNotBlank()) {
            val expiry = SteamCrypto.jwtExpiry(existing)
            val now = System.currentTimeMillis() / 1000
            val freshByTime = account.accessTokenAt > 0 &&
                    now - account.accessTokenAt < 10 * 60
            val validByExpiry = expiry != null && expiry - now > 5 * 60
            if (validByExpiry || (expiry == null && freshByTime)) {
                return existing
            }
        }

        val token = try {
            SteamAuthService.generateAccessToken(account.refreshToken, account.steamId)
        } catch (e: SteamApiException) {
            throw SteamNotLoggedInException("Сессия устарела. Войдите в Steam заново")
        }

        repository.updateAccount(account.localId) { current ->
            current.copy(
                session = (current.session ?: com.aether.sdamobile.data.SteamSession(
                    steamId = current.steamId
                )).copy(accessToken = token),
                accessTokenAt = System.currentTimeMillis() / 1000
            )
        }
        repository.flush()
        return token
    }
}
