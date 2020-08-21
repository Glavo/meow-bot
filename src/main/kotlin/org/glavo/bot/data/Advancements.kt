package org.glavo.bot.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.content

@Suppress("EXPERIMENTAL_API_USAGE")
val Advancements: Map<String, String> =
    Json.parseJson(Config::class.java.getResource("Advancements.json").readText())
        .jsonObject
        .mapValues { (_, value) -> value.content }
