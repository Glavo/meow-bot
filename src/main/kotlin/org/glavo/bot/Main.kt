@file:Suppress("NOTHING_TO_INLINE")

package org.glavo.bot

import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.*
import net.mamoe.mirai.event.events.MemberJoinEvent
import net.mamoe.mirai.join
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import org.glavo.bot.data.Config

lateinit var bot: Bot
lateinit var MainGroup: Group
lateinit var CommandGroup: Group

suspend fun main() {
    bot = Bot(Config.qq, Config.password) { deviceInfo = { c -> MeowDeviceInfo(c) } }.alsoLogin()

    MainGroup = bot.getGroup(Config.MainGroupID)
    CommandGroup = bot.getGroup(Config.CommandGroupID)

    if (Config.isOnMCServer) {
        monitorServer()
    }

    bot.subscribeAlways<GroupMessageEvent> { event ->
        when (group.id) {
            Config.MainGroupID, Config.CommandGroupID -> {
                val m = message
                val c = m.content
                if (Command.All.firstOrNull { it.invoke(event, m, c) } == null
                    && c.startsWith('#')
                    && m[QuoteReply] == null
                ) {
                    quoteReply("未知命令")
                }
            }
        }
    }

    bot.subscribeAlways<MemberJoinEvent> { event ->
        if (event.group == MainGroup) {
            group.sendMessage(PlainText("欢迎") + event.member.at() + "加入！关于服务器的更多信息，请查看服务器公告： ${Config.Post}")
            evalCommand("}${event.member.nameCardOrNick}加入了群聊")
        }
    }
    bot.join()
}
