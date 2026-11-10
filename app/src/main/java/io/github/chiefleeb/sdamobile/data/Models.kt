package io.github.chiefleeb.sdamobile.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class SteamSession(
    @SerialName("SteamID") val steamId: Long = 0,
    @SerialName("AccessToken") val accessToken: String = "",
    @SerialName("RefreshToken") val refreshToken: String = "",
    @SerialName("SessionID") val sessionId: String = ""
)

/**
 * Mirrors the SDA maFile format. Extra fields are ignored by SDA and are only
 * used to keep the mobile experience self-contained.
 */
@Serializable
data class SdaAccount(
    @SerialName("local_id") val localId: String = UUID.randomUUID().toString(),
    @SerialName("shared_secret") val sharedSecret: String = "",
    @SerialName("serial_number") val serialNumber: String = "",
    @SerialName("revocation_code") val revocationCode: String = "",
    @SerialName("uri") val uri: String = "",
    @SerialName("server_time") val serverTime: Long = 0,
    @SerialName("account_name") val accountName: String = "",
    @SerialName("token_gid") val tokenGid: String = "",
    @SerialName("identity_secret") val identitySecret: String = "",
    @SerialName("secret_1") val secret1: String = "",
    @SerialName("status") val status: Int = 1,
    @SerialName("device_id") val deviceId: String = "",
    @SerialName("fully_enrolled") val fullyEnrolled: Boolean = true,
    @SerialName("Session") val session: SteamSession? = null,
    @SerialName("access_token_at") val accessTokenAt: Long = 0,
    @SerialName("added_at") val addedAt: Long = 0
) {
    val steamId: Long get() = session?.steamId ?: 0L
    val refreshToken: String get() = session?.refreshToken.orEmpty()
    val accessToken: String get() = session?.accessToken.orEmpty()
    val hasSession: Boolean get() = refreshToken.isNotBlank() && steamId != 0L
}

@Serializable
data class VaultData(
    val accounts: List<SdaAccount> = emptyList(),
    val timeOffset: Long = 0,
    val timeSyncedAt: Long = 0
)
