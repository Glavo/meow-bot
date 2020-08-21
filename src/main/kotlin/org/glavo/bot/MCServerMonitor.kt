@file:Suppress("BlockingMethodInNonBlockingContext", "EXPERIMENTAL_API_USAGE")

package org.glavo.bot

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.content
import net.mamoe.mirai.message.data.EmptyMessageChain
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.at
import org.glavo.bot.data.Advancements
import org.glavo.bot.data.Config
import org.glavo.bot.data.Player
import org.intellij.lang.annotations.Language
import java.io.BufferedReader
import java.io.File
import java.lang.Exception
import java.util.regex.Matcher
import java.util.regex.Pattern

object MCServerLogPatterns {

    @Language("RegExp")
    const val timePattern = "\\[(\\p{Digit}{2}:\\p{Digit}{2}:\\p{Digit}{2})]"

    @Language("RegExp")
    const val stiPattern = "\\[Server thread/INFO]"

    @Language("RegExp")
    const val namePattern = "[a-zA-Z0-9_]+"

    val joinTheGamePattern: Pattern = Pattern.compile(
        "^$timePattern $stiPattern: (?<name>$namePattern) joined the game$"
    )

    val leftTheGamePattern: Pattern = Pattern.compile(
        "^$timePattern $stiPattern: (?<name>$namePattern) left the game$"
    )

    val sayPattern: Pattern = Pattern.compile(
        "^$timePattern $stiPattern: <(?<name>$namePattern)> (?<info>.*)"
    )

    val advPattern: Pattern = Pattern.compile(
        "^$timePattern $stiPattern: (?<name>$namePattern) has made the advancement \\[(?<adv>[^]]+)]"
    )

    val challengePattern: Pattern = Pattern.compile(
        "^$timePattern $stiPattern: (?<name>$namePattern) has completed the challenge \\[(?<adv>[^]]+)]"
    )

    val goalPattern: Pattern = Pattern.compile(
        "^$timePattern $stiPattern: (?<name>$namePattern) has reached the goal \\[(?<adv>[^]]+)]"
    )

    val AtPattern: Pattern = Pattern.compile(
        "@(?<name>\\p{javaJavaIdentifierPart}+)"
    )
}

private class SteamClosed : Throwable() {
    override fun fillInStackTrace(): Throwable = this
}

suspend fun monitorServer() {
    GlobalScope.launch {
        var reader: BufferedReader? = File("/tmp/mc").bufferedReader()
        val group = bot.getGroup(Config.MainGroupID)
        val groupCommand = bot.getGroup(Config.CommandGroupID)

        Runtime.getRuntime().addShutdownHook(Thread {
            reader?.close()
        })

        while (true) {
            try {
                val str = (reader ?: throw SteamClosed()).readLine()
                    ?: throw SteamClosed()
                bot.logger.info("[Minecraft] $str")

                @Suppress("CanBeVal")
                var m: Matcher = MCServerLogPatterns.sayPattern.matcher(str)
                if (m.matches()) {
                    val name = m.group("name")
                    val info = m.group("info")

                    if (info.startsWith(";;") || info.startsWith("；；")) {
                        continue
                    }

                    var res: Message = EmptyMessageChain
                    var offset = 0

                    m = MCServerLogPatterns.AtPattern.matcher(info)
                    while (m.find()) {
                        val start = m.start()
                        val end = m.end()

                        if (start > offset) {
                            res += PlainText(info.substring(offset, start))
                        }
                        offset = end

                        val n = m.group("name")

                        val qn = n.toLongOrNull()
                        if (qn != null) {
                            val mem = group.getOrNull(qn)
                            if (mem != null) {
                                res += mem.at()
                                continue
                            }
                        }

                        var p = Player.All.firstOrNull { pl -> pl.names.any { it.equals(n, true) } }
                        if (p != null) {
                            val mem = group.getOrNull(p.qq)
                            if (mem != null) {
                                res += mem.at()
                                continue
                            }
                        }

                        p = Player.All.firstOrNull { pl -> pl.nicknames.any { it.equals(n, true) } }
                        if (p != null) {
                            val mem = group.getOrNull(p.qq)
                            if (mem != null) {
                                res += mem.at()
                                continue
                            }
                        }

                        res += PlainText(info.substring(start, end))
                    }

                    if (offset < info.length) {
                        res += info.substring(offset, info.length)
                    }

                    group.sendMessage(PlainText(name) + ": " + res)
                    continue
                }

                m = MCServerLogPatterns.joinTheGamePattern.matcher(str)
                if (m.matches()) {
                    val name = m.group("name")
                    (if (name == "Glavo") groupCommand else group).sendMessage("$name 进入服务器")
                    continue
                }

                m = MCServerLogPatterns.leftTheGamePattern.matcher(str)
                if (m.matches()) {
                    val name = m.group("name")
                    (if (name == "Glavo") groupCommand else group).sendMessage("$name 离开服务器")
                    continue
                }

                m = MCServerLogPatterns.advPattern.matcher(str)
                if (m.matches()) {
                    val adv = m.group("adv").let { Advancements[it] ?: it }
                    group.sendMessage("${m.group("name")} 取得了进度「$adv」")
                    continue
                }

                m = MCServerLogPatterns.challengePattern.matcher(str)
                if (m.matches()) {
                    val adv = m.group("adv").let { Advancements[it] ?: it }
                    group.sendMessage("${m.group("name")} 完成了挑战「$adv」")
                    continue
                }

                m = MCServerLogPatterns.goalPattern.matcher(str)
                if (m.matches()) {
                    val adv = m.group("adv").let { Advancements[it] ?: it }
                    group.sendMessage("${m.group("name")} 达成了目标「$adv」")
                    continue
                }

            } catch (ex: SteamClosed) {
                bot.logger.warning("FIFO File Closed")
                delay(1000)
                try {
                    reader?.close()
                    reader = File("/tmp/mc").bufferedReader()
                } catch (e: Throwable) {
                    reader = null
                    bot.logger.warning("Reopen FIFO File failure")
                    bot.logger.warning(e)
                }
            } catch (ex: Exception) {
                bot.logger.warning(ex)
            }
        }
    }
}