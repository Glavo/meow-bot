@file:Suppress(
    "BlockingMethodInNonBlockingContext", "EXPERIMENTAL_API_USAGE", "NOTHING_TO_INLINE",
    "RemoveRedundantQualifierName"
)

package org.glavo.bot

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.message.data.*
import org.glavo.bot.data.Advancements
import org.glavo.bot.data.Player
import org.intellij.lang.annotations.Language
import java.io.BufferedReader
import java.io.File
import java.lang.Exception
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

private val logMatcher = LogMatcher.build {
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
        val info = m1.group("info")!!

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

    val names: MutableSet<String> = Collections.synchronizedSet(hashSetOf())

    match(
        "com.mojang.authlib.GameProfile@[0-9a-f]+\\[id=<null>,name=(?<name>$NamePattern),properties=\\{},legacy=false] \\([^)]*\\) lost connection: Disconnected"
    ) { m ->
        val name = m.group("name")!!
        if (names.add(name)) {
            val group = MainGroup

            val a = Player.All.firstOrNull { it.names.contains(name) }
                ?.let { group.getOrNull(it.qq)?.at() }
                ?: PlainText("@$name ")

            group.sendMessage(a + "登录服务器时如果提示“登录失败：无效会话”，请退出游戏，在启动器内删除账号并重新登录")
        }
    }
}


private class SteamClosed : Throwable() {
    override fun fillInStackTrace(): Throwable = this
}

suspend fun monitorServer() {
    GlobalScope.launch {
        var reader: BufferedReader? = File("/tmp/mc").bufferedReader()

        Runtime.getRuntime().addShutdownHook(Thread { reader?.close() })

        while (true) {
            try {
                val str = (reader ?: throw SteamClosed()).readLine()
                    ?: throw SteamClosed()
                bot.logger.info("[Minecraft] $str")

                logMatcher.match(str)
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

@Language("RegExp")
const val NamePattern = "[a-zA-Z0-9_]+"

private val AtPattern: Pattern = Pattern.compile(
    "@(?<name>\\p{javaJavaIdentifierPart}+)"
)

class LogMatcher(private val ms: List<Pair<Pattern, suspend (LogMatcher.Result) -> Unit>>) {
    class Builder {
        private val list = arrayListOf<Pair<Pattern, suspend (LogMatcher.Result) -> Unit>>()

        fun match(@Language("RegExp") pattern: String, action: suspend (LogMatcher.Result) -> Unit) {
            list += Pattern.compile(pattern) to action
        }

        fun build(): LogMatcher = LogMatcher(list)
    }

    class Result(val time: String, val matcher: Matcher) {
        inline fun group(group: Int): String? = matcher.group(group)
        inline fun group(group: String): String? = matcher.group(group)
    }

    suspend fun match(log: String) {
        if (log.length <= 33) return
        if (log[0] != '[') return
        if (log[9] != ']') return
        val time = log.substring(1, 9)
        if (!log.startsWith(InfoStr, 10)) {
            return
        }
        val str = log.substring(33)

        for (m in ms) {
            val matcher = m.first.matcher(str)
            if (matcher.matches()) {
                m.second.invoke(Result(time, matcher))
                return
            }
        }
    }

    companion object {
        private const val InfoStr = " [Server thread/INFO]: "

        inline fun build(action: LogMatcher.Builder.() -> Unit) = LogMatcher.Builder().apply(action).build()
    }
}
