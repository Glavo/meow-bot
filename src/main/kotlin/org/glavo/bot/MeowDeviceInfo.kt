package org.glavo.bot

import kotlinx.serialization.json.*
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
        Json.parseToJsonElement(cj.readText()).jsonObject
    }

    override val display: ByteArray =
        (if (Config.isOnMCServer) info["display1"]!!.jsonPrimitive.content else info["display2"]!!.jsonPrimitive.content).toByteArray()

    override val product: ByteArray = info["m"]!!.jsonPrimitive.content.toByteArray()
    override val device: ByteArray = info["m"]!!.jsonPrimitive.content.toByteArray()
    override val board: ByteArray = info["m"]!!.jsonPrimitive.content.toByteArray()
    override val brand: ByteArray = info["g"]!!.jsonPrimitive.content.toByteArray()
    override val model: ByteArray = info["m"]!!.jsonPrimitive.content.toByteArray()
    override val bootloader: ByteArray = "unknown".toByteArray()

    override val fingerprint: ByteArray =
        (if (Config.isOnMCServer) info["fingerprint1"]!!.jsonPrimitive.content else info["fingerprint2"]!!.jsonPrimitive.content).toByteArray()

    override val procVersion: ByteArray =
        info["procVersion"]!!.jsonPrimitive.content.toByteArray()

    override val imei: String =
        if (Config.isOnMCServer) info["imei1"]!!.jsonPrimitive.content else info["imei2"]!!.jsonPrimitive.content
    override val imsiMd5: ByteArray =
        if (Config.isOnMCServer)
            info["imsiMd51"]!!.jsonPrimitive.content.toByteArray()
        else
            info["imsiMd52"]!!.jsonPrimitive.content.toByteArray()
}
