package org.glavo.bot

import org.glavo.bot.data.Config
import org.glavo.rcon.Rcon
import org.intellij.lang.annotations.Language
import java.io.IOException
import java.util.regex.Matcher
import java.util.regex.Pattern

private var rcon: Rcon? = null
private val o = Object()

object Patterns {
    const val IncorrectArgStart = "Incorrect argument for command"
    const val UnknownCommandStart = "Unknown or incomplete command, see below for error"
    const val HereEnd = "<--[HERE]"
    const val HereEndLength = HereEnd.length

    const val IncorrectArgOffset = IncorrectArgStart.length
    const val UnknownCommandOffset = UnknownCommandStart.length

    const val IncorrectArgMinLength = IncorrectArgOffset + HereEndLength
    const val UnknownCommandMinLength = UnknownCommandOffset + HereEndLength

    val listPattern: Pattern = Pattern.compile(
        "^There are ([0-9]+) of a max of ([0-9]+) players online: ($NamePattern(, $NamePattern)*)?"
    )

    val gamerulePattern: Pattern = Pattern.compile(
        "^Gamerule (\\p{Alpha}+) is currently set to: (.+)"
    )

    val gameruleSetPattern: Pattern = Pattern.compile(
        "^Gamerule (\\p{Alpha}+) is now set to: (.+)"
    )

    val whitelistAddPattern: Pattern = Pattern.compile(
        "^Added (?<name>$NamePattern) to the whitelist"
    )

    val whitelistRemovePattern: Pattern = Pattern.compile(
        "^Removed (?<name>$NamePattern) from the whitelist$"
    )

    val givePattern: Pattern = Pattern.compile(
        "Gave (?<count>[0-9]+) \\[(?<item>[^]]+)] to (?<target>.*)"
    )
}

fun rcon(command: String): String {
    if (rcon == null) {
        synchronized(o) {
            if (rcon == null) {
                rcon = Rcon(Config.ServerIP, Config.rconPassword)
            }
        }
    }
    val r = rcon ?: throw IOException("连接服务器失败")
    try {
        return r.command(command)
    } catch (ex: IOException) {
        try {
            rcon?.disconnect()
        } catch (ex: Throwable) {
        }
        rcon = null
        throw ex
    }
}

fun evalCommand(command: String): String {
    val ans = try {
        rcon(command)
    } catch (e: Throwable) {
        return "连接服务器失败"
    }

    if (ans.isEmpty()) {
        return "（无结果）"
    } else {
        val length = ans.length

        if (length >= Patterns.IncorrectArgMinLength
            && ans.startsWith(Patterns.IncorrectArgStart)
            && ans.endsWith(Patterns.HereEnd)
        ) {
            return "错误的命令参数：${ans.substring(Patterns.IncorrectArgOffset, length - Patterns.HereEndLength)} <--此处"
        }

        if (length >= Patterns.UnknownCommandMinLength
            && ans.startsWith(Patterns.UnknownCommandStart)
            && ans.endsWith(Patterns.HereEnd)
        ) {
            return "未知或不完整的命令：${ans.substring(Patterns.UnknownCommandOffset, length - Patterns.HereEndLength)}"
        }

        when {
            command == "list" -> {
                Patterns.listPattern.tryMatch(ans) { m ->
                    val n = m.group(1)
                    return if (n == "0") {
                        "当前没有玩家在服务器中"
                    } else {
                        m.group(3).split(',')
                            .joinToString("\n", "当前有 $n 个玩家在服务器中：\n") { "    " + it.trim() }
                    }
                }
                return ans

            }
            command.startsWith("gamerule ") -> {
                Patterns.gamerulePattern.tryMatch(ans) {
                    return "游戏规则 ${it.group(1)} 目前为 ${it.group(2)}"
                }
                Patterns.gameruleSetPattern.tryMatch(ans) {
                    return "游戏规则 ${it.group(1)} 被设置为 ${it.group(2)}"
                }
                return ans
            }
            command.startsWith("whitelist add ") -> {
                if (ans == "That player does not exist") {
                    return "该玩家不存在"
                }
                if (ans == "Player is already whitelisted") {
                    return "玩家已在白名单中"
                }
                Patterns.whitelistAddPattern.tryMatch(ans) {
                    "已将 ${it.group("name")} 加入白名单"
                }
                return ans
            }
            command.startsWith("whitelist remove ") -> {
                if (ans == "That player does not exist") {
                    return "该玩家不存在"
                }
                if (ans == "Player is not whitelisted") {
                    return "玩家不在白名单内"
                }

                Patterns.whitelistRemovePattern.tryMatch(ans) {
                    return "已将 ${it.group("name")} 移出白名单"
                }
                return ans
            }
            command.startsWith("give ") -> {
                if (ans == "No player was found") {
                    return "未找到玩家"
                }
                Patterns.givePattern.tryMatch(ans) {
                    val target = it.group("target").toIntOrNull().let { t ->
                        if (t == null) {
                            it.group("target")
                        } else {
                            "$t 个目标"
                        }

                    }
                    return "已将 ${it.group("count")} 个「${it.group("item")}」给予 $target"
                }
                return ans
            }
            else -> return ans
        }
    }
}

fun evalCommands(vararg commands: String): String {
    when (commands.size) {
        0 -> return ""
        1 -> return evalCommand(commands[0])
    }

    return commands.joinToString("\n") { evalCommand(it) }
}


/*
private val translators: List<Pair<Pattern, (Matcher) -> String>> = listOf(
    Pattern.compile("^Incorrect argument for command(?<command>.*)<--\\[HERE]$") to {
        "错误的命令参数：${it.group("command")} <--此处"
    },
    Pattern.compile("^Unknown or incomplete command, see below for error(?<command>.*)<--\\[HERE]$") to {
        "未知或不完整的命令：${it.group("command")}"
    },
    Pattern.compile("^There are (?<num>[0-9]+) of a max of ([0-9]+) players online: (?<list>[^, ]+(, [^, ]+)*)?") to {
        when (val n = it.group("num")) {
            "0" -> "当前没有玩家在服务器中"
            else ->
                it.group("list")
                    .split(',')
                    .joinToString("\n", "当前有 $n 个玩家在服务器中：\n") { p -> "   " + p.trim() }

        }
    },
    Pattern.compile("^Gamerule (?<name>\\p{Alpha}+) is currently set to: (?<value>.+)$") to {
        "游戏规则 ${it.group("name")} 目前为 ${it.group("value")}"
    },
    Pattern.compile("^Gamerule (?<name>\\p{Alpha}+) is now set to: (?<value>.+)$") to {
        "游戏规则 ${it.group("name")} 被设置为 ${it.group("value")}"
    }
)
*/
