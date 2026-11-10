package io.github.chiefleeb.sdamobile.steam

import io.github.chiefleeb.sdamobile.i18n.tr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class SteamApiException(
    override val message: String,
    val eresult: Int? = null
) : Exception(message)

class SteamNotLoggedInException(message: String = tr("Сессия Steam истекла")) : Exception(message)

object SteamErrors {
    fun message(code: Int, fallback: String? = null): String = when (code) {
        5 -> tr("Неверный логин или пароль")
        63 -> tr("Steam требует код Steam Guard")
        65 -> tr("Неверный код из письма")
        84, 85 -> tr("Слишком много попыток. Подождите несколько минут")
        88 -> tr("Неверный код Steam Guard")
        89 -> tr("Неверный код активации")
        else -> fallback?.takeIf { it.isNotBlank() }
            ?: tr("Steam вернул ошибку $code", "Steam returned error $code")
    }
}

object SteamWeb {

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val USER_AGENT = "okhttp/4.9.2"

    suspend fun get(url: HttpUrl, cookies: String? = null): String = withContext(Dispatchers.IO) {
        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
        if (!cookies.isNullOrBlank()) builder.header("Cookie", cookies)
        execute(builder.build())
    }

    suspend fun post(
        url: String,
        fields: Map<String, String>,
        cookies: String? = null
    ): String = withContext(Dispatchers.IO) {
        val bodyBuilder = FormBody.Builder()
        fields.forEach { (key, value) -> bodyBuilder.add(key, value) }
        val builder = Request.Builder()
            .url(url)
            .post(bodyBuilder.build())
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
        if (!cookies.isNullOrBlank()) builder.header("Cookie", cookies)
        execute(builder.build())
    }

    suspend fun postJson(url: String, fields: Map<String, String>, cookies: String? = null): JsonObject =
        parseObject(post(url, fields, cookies))

    suspend fun getJson(url: HttpUrl, cookies: String? = null): JsonObject =
        parseObject(get(url, cookies))

    fun parseObject(body: String): JsonObject {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return JsonObject(emptyMap())
        return try {
            SteamJson.parseToJsonElement(trimmed) as JsonObject
        } catch (e: Exception) {
            throw SteamApiException(tr("Steam вернул неожиданный ответ"))
        }
    }

    private fun execute(request: Request): String {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val eresult = response.header("x-eresult")?.toIntOrNull()

            if (!response.isSuccessful) {
                val detail = runCatching {
                    val obj = SteamJson.parseToJsonElement(body) as JsonObject
                    obj.optString("message")
                        ?: obj.optObject("response")?.optString("extended_error_message")
                }.getOrNull()
                if (response.code == 429) {
                    throw SteamApiException(tr("Слишком много запросов. Подождите немного"), 84)
                }
                throw SteamApiException(
                    "Steam: HTTP ${response.code}" + (detail?.let { ": $it" } ?: ""),
                    eresult
                )
            }

            if (eresult != null && eresult != 0 && eresult != 1) {
                val detail = runCatching {
                    val obj = SteamJson.parseToJsonElement(body) as JsonObject
                    obj.optObject("response")?.optString("extended_error_message")
                        ?: obj.optString("message")
                }.getOrNull()
                throw SteamApiException(SteamErrors.message(eresult, detail), eresult)
            }

            return body
        }
    }
}
