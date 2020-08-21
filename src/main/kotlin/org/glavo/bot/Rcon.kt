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
    val IncorrectArgPattern: Pattern = Pattern.compile(
        "^Incorrect argument for command(.*)<--\\[HERE]$"
    )

    val UnknownCommandPattern: Pattern = Pattern.compile(
        "^Unknown or incomplete command, see below for error(.*)<--\\[HERE]$"
    )

    val listPattern: Pattern = Pattern.compile(
        "^There are ([0-9]+) of a max of ([0-9]+) players online: ([^, ]+(, [^, ]+)*)?"
    )

    val gamerulePattern: Pattern = Pattern.compile(
        "^Gamerule (\\p{Alpha}+) is currently set to: (.+)"
    )

    val gameruleSetPattern: Pattern = Pattern.compile(
        "^Gamerule (\\p{Alpha}+) is now set to: (.+)"
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
        var ma = Patterns.IncorrectArgPattern.matcher(ans)
        if (ma.matches()) {
            return "错误的命令参数：${ma.group(1)} <--此处"

        }
        ma = Patterns.UnknownCommandPattern.matcher(ans)
        if (ma.matches()) {
            return "未知或不完整的命令：${ma.group(1)}"

        }

        when {
            command == "list" -> {
                val m = Patterns.listPattern.matcher(ans)
                return if (m.matches()) {
                    val n = m.group(1)
                    if (n == "0") {
                        ("当前没有玩家在服务器中")

                    } else {
                        (m.group(3).split(',')
                            .joinToString("\n", "当前有 $n 个玩家在服务器中：\n") { "   " + it.trim() })
                    }
                } else {
                    ans
                }
            }
            command.startsWith("gamerule ") -> {
                var m = Patterns.gamerulePattern.matcher(ans)
                if (m.matches()) {
                    return ("游戏规则 ${m.group(1)} 目前为 ${m.group(2)}")
                }
                m = Patterns.gameruleSetPattern.matcher(ans)
                if (m.matches()) {
                    return ("游戏规则 ${m.group(1)} 被设置为 ${m.group(2)}")
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

    return commands.joinToString("\n ") { evalCommand(it) }
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
