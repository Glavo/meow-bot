@file:Suppress("NOTHING_TO_INLINE")

package org.glavo.bot

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
    bot = Bot(Config.qq, Config.password) { fileBasedDeviceInfo() }.alsoLogin()

    MainGroup = bot.getGroup(Config.MainGroupID)
    CommandGroup = bot.getGroup(Config.CommandGroupID)

    if (Config.isOnMCServer) {
        monitorServer()
    }

    bot.subscribeAlways<GroupMessageEvent> { event ->
        when (group.id) {
            Config.MainGroupID, Config.CommandGroupID -> {
                val qr = message[QuoteReply]
                val m = if (qr == null) {
                    message
                } else {
                    val tem = arrayListOf<Message>()
                    var rmAt = true
                    var rmSpace = false

                    for (m in message) {
                        when (m) {
                            is At -> {
                                if (rmAt && m.target == qr.source.fromId) {
                                    rmAt = false
                                    rmSpace = true
                                } else {
                                    tem += m
                                }
                            }
                            is PlainText -> {
                                if (m.content.isNotEmpty()) {
                                    if (rmSpace) {
                                        if (m.content.startsWith(' ')) {
                                            if (m.content.length > 1) {
                                                tem += PlainText(m.content.substring(1))
                                            }
                                        } else {
                                            tem += m
                                        }
                                        rmSpace = false
                                    } else {
                                        tem += m
                                    }
                                }
                            }
                            else -> tem += m
                        }
                    }
                    tem.asMessageChain()
                }

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
        }
    }
    bot.join()
}
