@file:Suppress("ObjectPropertyName", "NonAsciiCharacters")

package org.glavo.bot.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.content
import kotlinx.serialization.json.long
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.system.exitProcess

object Config {
    private val config: JsonObject = run {
        val cj = Config::class.java.getResource("Config.json")
        if (cj == null) {
            System.err.println("Config.json 缺失")
            exitProcess(-1)
        }
        @Suppress("EXPERIMENTAL_API_USAGE")
        Json.parseJson(cj.readText()).jsonObject
    }

    val qq = config["qq"]!!.long

    val password: String
        get() = String(Base64.getDecoder().decode(config["password"]!!.content), StandardCharsets.UTF_8)

    val rconPassword = config["rconPassword"]!!.content

    val MainGroupID = config["MainGroupID"]!!.long
    val CommandGroupID = config["CommandGroupID"]!!.long

    val ServerIP: String = run {
        val iip = config["IntranetIP"]!!.content
        for (networkInterface in NetworkInterface.getNetworkInterfaces()) {
            for (interfaceAddress in networkInterface.interfaceAddresses) {
                if (interfaceAddress.address.hostAddress == iip) {
                    return@run "127.0.0.1"
                }
            }
        }
        iip
    }

    val ServerDomain: String = config["ServerDomain"]!!.content

    val Post: String = config["Post"]!!.content

    val isOnMCServer = ServerIP == "127.0.0.1"
}