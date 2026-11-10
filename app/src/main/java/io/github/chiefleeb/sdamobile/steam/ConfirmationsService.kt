package io.github.chiefleeb.sdamobile.steam

import io.github.chiefleeb.sdamobile.i18n.tr

import io.github.chiefleeb.sdamobile.data.AccountRepository
import io.github.chiefleeb.sdamobile.data.SdaAccount
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

data class SteamConfirmation(
    val id: String,
    val nonce: String,
    val creatorId: String,
    val type: Int,
    val typeName: String,
    val headline: String,
    val summary: List<String>,
    val creationTime: Long,
    val icon: String
) {
    val isMarket: Boolean
        get() = typeName.contains("market", ignoreCase = true) ||
                headline.contains("маркет", ignoreCase = true) ||
                headline.contains("market", ignoreCase = true)
}

object ConfirmationsService {

    private const val BASE = "https://steamcommunity.com"

    suspend fun fetch(
        account: SdaAccount,
        timeOffset: Long,
        repository: AccountRepository
    ): List<SteamConfirmation> {
        if (account.identitySecret.isBlank()) {
            throw SteamApiException(tr("В maFile нет identity_secret — подтверждения недоступны"))
        }
        val token = SessionManager.freshAccessToken(account, repository)
        val cookie = cookie(account, token)
        val time = System.currentTimeMillis() / 1000 + timeOffset
        val key = SteamCrypto.confirmationKey(account.identitySecret, time, "conf")
        val url = "$BASE/mobileconf/getlist".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("p", deviceId(account))
            .addQueryParameter("a", account.steamId.toString())
            .addQueryParameter("k", key)
            .addQueryParameter("t", time.toString())
            .addQueryParameter("m", "react")
            .addQueryParameter("tag", "conf")
            .build()

        val body = SteamWeb.get(url, cookie)
        val root = SteamWeb.parseObject(body)
        if (root.optBool("success") != true) {
            if (root.optBool("needauth") == true) throw SteamNotLoggedInException()
            throw SteamApiException(
                root.optString("message") ?: root.optString("detail") ?: tr("Не удалось загрузить подтверждения")
            )
        }

        return (root["conf"] as? JsonArray).orEmpty().mapNotNull { element ->
            val obj = element.asObjectOrNull() ?: return@mapNotNull null
            parseConfirmation(obj)
        }
    }

    suspend fun respond(
        account: SdaAccount,
        confirmation: SteamConfirmation,
        accept: Boolean,
        timeOffset: Long,
        repository: AccountRepository
    ): Boolean {
        if (account.identitySecret.isBlank()) {
            throw SteamApiException(tr("В maFile нет identity_secret"))
        }
        val token = SessionManager.freshAccessToken(account, repository)
        val cookie = cookie(account, token)
        val time = System.currentTimeMillis() / 1000 + timeOffset
        val tag = if (accept) "accept" else "reject"
        val key = SteamCrypto.confirmationKey(account.identitySecret, time, tag)
        val url = "$BASE/mobileconf/ajaxop".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("op", if (accept) "allow" else "cancel")
            .addQueryParameter("p", deviceId(account))
            .addQueryParameter("a", account.steamId.toString())
            .addQueryParameter("k", key)
            .addQueryParameter("t", time.toString())
            .addQueryParameter("m", "react")
            .addQueryParameter("tag", tag)
            .addQueryParameter("cid", confirmation.id)
            .addQueryParameter("ck", confirmation.nonce)
            .build()

        val root = SteamWeb.parseObject(SteamWeb.get(url, cookie))
        if (root.optBool("needauth") == true) throw SteamNotLoggedInException()
        return root.optBool("success") == true
    }

    private fun cookie(account: SdaAccount, token: String): String {
        val sessionId = account.session?.sessionId?.takeIf { it.isNotBlank() }
            ?: SteamCrypto.newSessionId()
        val loginSecure = "${account.steamId}%7C%7C$token"
        return "steamLoginSecure=$loginSecure; sessionid=$sessionId; " +
                "mobileClient=android; mobileClientVersion=777777 3.6.1"
    }

    private fun deviceId(account: SdaAccount): String =
        account.deviceId.ifBlank { SteamCrypto.deviceIdFor(account.steamId.toString()) }

    private fun parseConfirmation(obj: JsonObject): SteamConfirmation? {
        val id = (obj["id"] as? JsonPrimitive)?.contentOrNull ?: return null
        val nonce = (obj["nonce"] as? JsonPrimitive)?.contentOrNull ?: return null
        val summary = (obj["summary"] as? JsonArray).orEmpty().mapNotNull { item ->
            (item as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotBlank() }
        }
        return SteamConfirmation(
            id = id,
            nonce = nonce,
            creatorId = (obj["creator_id"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            type = obj.optInt("type") ?: 0,
            typeName = obj.optString("type_name").orEmpty(),
            headline = obj.optString("headline").orEmpty(),
            summary = summary,
            creationTime = obj.optLong("creation_time") ?: 0L,
            icon = obj.optString("icon").orEmpty()
        )
    }
}
