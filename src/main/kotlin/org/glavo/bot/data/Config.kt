@file:Suppress("ObjectPropertyName", "NonAsciiCharacters")

package org.glavo.bot.data

import kotlinx.serialization.json.*
import java.io.File
import java.net.NetworkInterface
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.system.exitProcess

object Config {
    private val config: JsonObject = run {
        val cj = File("Config.json")
        if (!cj.exists()) {
            System.err.println("Config.json 缺失")
            exitProcess(-1)
        }
        Json.parseToJsonElement(cj.readText()).jsonObject
    }

    val qq = config["qq"]!!.jsonPrimitive.long

    val password: String
        get() = String(Base64.getDecoder().decode(config["password"]!!.jsonPrimitive.content), StandardCharsets.UTF_8)

    val rconPassword = config["rconPassword"]!!.jsonPrimitive.content

    val MainGroupID = config["MainGroupID"]!!.jsonPrimitive.long
    val CommandGroupID = config["CommandGroupID"]!!.jsonPrimitive.long

    val ServerIP: String = run {
        val iip = config["IntranetIP"]!!.jsonPrimitive.content
        for (networkInterface in NetworkInterface.getNetworkInterfaces()) {
            for (interfaceAddress in networkInterface.interfaceAddresses) {
                if (interfaceAddress.address.hostAddress == iip) {
                    return@run "127.0.0.1"
                }
            }
        }
        iip
    }

    val ServerDomain: String = config["ServerDomain"]!!.jsonPrimitive.content

    val Post: String = config["Post"]!!.jsonPrimitive.content

    val isOnMCServer = ServerIP == "127.0.0.1"
}