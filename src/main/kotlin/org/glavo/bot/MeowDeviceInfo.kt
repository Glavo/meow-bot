package org.glavo.bot

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.content
import net.mamoe.mirai.utils.*
import org.glavo.bot.data.Config
import kotlin.system.exitProcess

class MeowDeviceInfo(context: Context) : SystemDeviceInfo(context) {
    private val info: JsonObject = run {
        val cj = Config::class.java.getResource("DeviceInfo.json")
        if (cj == null) {
            System.err.println("DeviceInfo.json 缺失")
            exitProcess(-1)
        }
        @Suppress("EXPERIMENTAL_API_USAGE")
        Json.parseJson(cj.readText()).jsonObject
    }

    override val display: ByteArray =
        (if (Config.isOnMCServer) info["display1"]!!.content else info["display2"]!!.content).toByteArray()

    override val product: ByteArray = info["m"]!!.content.toByteArray()
    override val device: ByteArray = info["m"]!!.content.toByteArray()
    override val board: ByteArray = info["m"]!!.content.toByteArray()
    override val brand: ByteArray = info["g"]!!.content.toByteArray()
    override val model: ByteArray = info["m"]!!.content.toByteArray()
    override val bootloader: ByteArray = "unknown".toByteArray()

    override val fingerprint: ByteArray =
        (if (Config.isOnMCServer) info["fingerprint1"]!!.content else info["fingerprint2"]!!.content).toByteArray()

    override val procVersion: ByteArray =
        info["procVersion"]!!.content.toByteArray()

    override val imei: String = if (Config.isOnMCServer) info["imei1"]!!.content else info["imei2"]!!.content
    override val imsiMd5: ByteArray =
        if (Config.isOnMCServer)
            info["imsiMd51"]!!.content.toByteArray()
        else
            info["imsiMd52"]!!.content.toByteArray()
}
