package org.glavo.bot.data

import kotlinx.serialization.json.*
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
            val pl = Player::class.java.getResource("PlayerList.json")
            if (pl == null) {
                System.err.println("PlayerList.json 缺失")
                exitProcess(-1)
            }
            Json.parseJson(pl.readText())
                .jsonArray
                .map { e ->
                    val obj = e.jsonObject

                    val qq = obj["qq"]!!.long
                    val names = obj["names"]?.jsonArray?.map { it.content } ?: emptyList()
                    val nicknames = obj["nicknames"]?.jsonArray?.map { it.content } ?: emptyList()
                    val permissions = run {
                        when (val p = obj["permission"]) {
                            null -> {
                                Permission.Default
                            }
                            is JsonPrimitive -> {
                                Permission.ofLevel(p.content)
                            }
                            is JsonObject -> {
                                @Suppress("CanBeVal") var permissions = Permission.ofLevel(p["level"]?.content) //TODO
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
    }

}
