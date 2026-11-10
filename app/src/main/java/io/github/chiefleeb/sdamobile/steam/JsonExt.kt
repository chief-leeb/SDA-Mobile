package io.github.chiefleeb.sdamobile.steam

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

internal val SteamJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
    explicitNulls = false
}

fun JsonObject.optObject(key: String): JsonObject? = this[key] as? JsonObject

fun JsonObject.optArray(key: String): JsonArray? = this[key] as? JsonArray

fun JsonObject.optString(key: String): String? =
    (this[key] as? JsonPrimitive)?.contentOrNull?.takeIf { it.isNotEmpty() }

fun JsonObject.optLong(key: String): Long? =
    (this[key] as? JsonPrimitive)?.longOrNull

fun JsonObject.optInt(key: String): Int? =
    (this[key] as? JsonPrimitive)?.longOrNull?.toInt()

fun JsonObject.optBool(key: String): Boolean? =
    (this[key] as? JsonPrimitive)?.booleanOrNull

fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject
