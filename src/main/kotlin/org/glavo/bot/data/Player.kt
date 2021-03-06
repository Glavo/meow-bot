package org.glavo.bot.data

import kotlinx.serialization.json.*
import java.io.File
import kotlin.system.exitProcess

@Suppress("EXPERIMENTAL_API_USAGE")
data class Player(
    val qq: Long,
    val names: List<String>,
    val nicknames: List<String>,
    val permissions: Permissions = Permission.Default
) {
    companion object {
        val All: List<Player> = run {
            val pl = File("PlayerList.json")
            if (!pl.exists()) {
                System.err.println("PlayerList.json 缺失")
                exitProcess(-1)
            }
            Json.parseToJsonElement(pl.readText())
                .jsonArray
                .map { e ->
                    val obj = e.jsonObject

                    val qq = obj["qq"]!!.jsonPrimitive.long
                    val names = obj["names"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    val nicknames = obj["nicknames"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    val permissions = run {
                        when (val p = obj["permission"]) {
                            null -> {
                                Permission.Default
                            }
                            is JsonPrimitive -> {
                                Permission.ofLevel(p.content)
                            }
                            is JsonObject -> {
                                @Suppress("CanBeVal") var permissions =
                                    Permission.ofLevel(p["level"]?.jsonPrimitive?.content) //TODO
                                permissions
                            }
                            else -> {
                                System.err.println("错误的权限格式: $p")
                                exitProcess(-1)
                            }
                        }
                    }

                    Player(qq, names, nicknames, permissions)
                }.sortedBy { it.qq }
        }

        operator fun get(qq: Long): Player? {
            return All.binarySearchBy(qq) { it.qq }.let { if (it >= 0) All[it] else null }
        }

        fun search(name: String): Player? {
            Player.All.firstOrNull { it.names.contains(name) }?.let {
                return it
            }
            Player.All.firstOrNull { it.nicknames.contains(name) }?.let {
                return it
            }
            name.toLongOrNull()?.let { qq ->
                Player.All.firstOrNull { it.qq == qq }?.let {
                    return it
                }
            }
            return null
        }
    }

}
