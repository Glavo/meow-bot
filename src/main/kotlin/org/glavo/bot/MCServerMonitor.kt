@file:Suppress("BlockingMethodInNonBlockingContext", "EXPERIMENTAL_API_USAGE")

package org.glavo.bot

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.message.data.EmptyMessageChain
import net.mamoe.mirai.message.data.Message
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.at
import org.glavo.bot.data.Advancements
import org.glavo.bot.data.Player
import org.intellij.lang.annotations.Language
import java.io.BufferedReader
import java.io.File
import java.lang.Exception
import java.util.regex.Matcher
import java.util.regex.Pattern

@Language("RegExp")
const val NamePattern = "[a-zA-Z0-9_]+"

private val AtPattern: Pattern = Pattern.compile(
    "@(?<name>\\p{javaJavaIdentifierPart}+)"
)

private class LogMatcherBuilder {
    private val list = arrayListOf<Pair<Pattern, suspend (Matcher) -> Unit>>()

    fun match(@Language("RegExp") pattern: String, action: suspend (Matcher) -> Unit) {
        list += Pattern.compile(
            "^\\[(?<time>\\p{Digit}{2}:\\p{Digit}{2}:\\p{Digit}{2})] \\[Server thread/INFO]: $pattern$"
        ) to action
    }

    fun build(): List<Pair<Pattern, suspend (Matcher) -> Unit>> = list
}

private inline fun buildLogMatcher(action: LogMatcherBuilder.() -> Unit) = LogMatcherBuilder().apply(action).build()

private val LogMatcher = buildLogMatcher {
    match("(?<name>$NamePattern) joined the game") {
        val name = it.group("name")
        val m = PlainText("$name 进入服务器")
        CommandGroup.sendMessage(m)
        if (name != "Glavo") {
            MainGroup.sendMessage(m)
        }
    }

    match("(?<name>$NamePattern) left the game") {
        val name = it.group("name")
        val m = PlainText("$name 离开服务器")
        CommandGroup.sendMessage(m)
        if (name != "Glavo") {
            MainGroup.sendMessage(m)
        }
    }

    match("<(?<name>$NamePattern)> (?<info>.*)") { m1 ->
        val name = m1.group("name")
        val info = m1.group("info")

        if (!info.startsWith(";;") && !info.startsWith("；；")) {
            val group = MainGroup
            var res: Message = EmptyMessageChain
            var offset = 0

            val m = AtPattern.matcher(info)
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

            group.sendMessage(PlainText("$name: ") + res)
        }
    }

    match("(?<name>$NamePattern) has made the advancement \\[(?<adv>[^]]+)]") { m ->
        val adv = m.group("adv").let { Advancements[it] ?: it }
        MainGroup.sendMessage("${m.group("name")} 取得了进度「$adv」")
    }

    match("(?<name>$NamePattern) has completed the challenge \\[(?<adv>[^]]+)]") { m ->
        val adv = m.group("adv").let { Advancements[it] ?: it }
        MainGroup.sendMessage("${m.group("name")} 完成了挑战「$adv」")
    }

    match("(?<name>$NamePattern) has reached the goal \\[(?<adv>[^]]+)]") { m ->
        val adv = m.group("adv").let { Advancements[it] ?: it }
        MainGroup.sendMessage("${m.group("name")} 达成了目标「$adv」")
    }
}

private class SteamClosed : Throwable() {
    override fun fillInStackTrace(): Throwable = this
}

suspend fun monitorServer() {
    GlobalScope.launch {
        var reader: BufferedReader? = File("/tmp/mc").bufferedReader()

        Runtime.getRuntime().addShutdownHook(Thread {
            reader?.close()
        })

        while (true) {
            try {
                val str = (reader ?: throw SteamClosed()).readLine()
                    ?: throw SteamClosed()
                bot.logger.info("[Minecraft] $str")

                for ((p, a) in LogMatcher) {
                    val m = p.matcher(str)
                    if(m.matches()) {
                        a(m)
                        break
                    }
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