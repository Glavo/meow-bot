package org.glavo.bot.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val Advancements: Map<String, String> =
    Json.parseToJsonElement(Config::class.java.getResource("Advancements.json").readText())
        .jsonObject
        .mapValues { (_, value) -> value.jsonPrimitive.content }
