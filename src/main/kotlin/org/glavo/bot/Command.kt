@file:Suppress("BlockingMethodInNonBlockingContext")

package org.glavo.bot

import kotlinx.serialization.json.*
import net.mamoe.mirai.contact.PermissionDeniedException
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.message.MessageEvent
import net.mamoe.mirai.message.data.*
import org.glavo.bot.data.Config
import org.glavo.bot.data.Permission
import org.glavo.bot.data.Player
import java.net.InetAddress

enum class Command(val level: Int = Permission.DefaultLevel) {
    Help("#help", action = { it.quoteReply("请查看 https://gitee.com/Glavo/meow-bot") }),
    IPAddress("#ip", action = {
        try {
            it.quoteReply(InetAddress.getByName(Config.ServerDomain).hostAddress)
        } catch (e: Throwable) {
            it.quoteReply("获取服务器IP失败")
        }
    }),
    PlayerList("#list", action = {
        it.quoteReply(evalCommand("list"))
    }),
    WhiteListAdd(level = 3) {
        private val command = "#whitelist "

        override suspend fun invoke(event: MessageEvent, message: MessageChain, content: String): Boolean {
            if (!content.startsWith(command)) return false
            val n = content.substring(command.length).trim()
            withCheck(event) {
                if (n.isEmpty()) {
                    event.quoteReply("需要指定角色名")
                } else {
                    event.quoteReply(evalCommand("whitelist add $n"))
                }
            }
            return true
        }

    },
    Ban(level = 3) {
        private val command = "#ban "

        override suspend fun invoke(event: MessageEvent, message: MessageChain, content: String): Boolean {
            if (!content.startsWith(command)) return false
            val n = content.substring(command.length).trim()
            withCheck(event) {
                if (n.isEmpty()) {
                    event.quoteReply("需要指定角色名")
                } else {
                    event.quoteReply(evalCommand("ban $n"))
                }
            }
            return true
        }
    },
    Eval(level = 4) {
        private val command = "##"

        override suspend fun invoke(event: MessageEvent, message: MessageChain, content: String): Boolean {
            if (!content.startsWith(command)) return false
            withCheck(event) {
                val c = content.substring(command.length)
                if (c.isBlank()) {
                    event.quoteReply("需要指定命令")
                } else {
                    event.quoteReply(evalCommand(c))
                }
            }
            return true
        }
    },
    Recall(level = 2) {
        private val command = "#recall"
        override suspend fun invoke(event: MessageEvent, message: MessageChain, content: String): Boolean {
            val qr = event.message[QuoteReply]
                ?: return if (content == command) {
                    event.quoteReply("请回复您要撤回的消息")
                    true
                } else {
                    false
                }

            val m = message.filterNot { it is At && it.target == qr.source.fromId }.asMessageChain().content.trim()
            if (m != command) return false

            withCheck(event) {
                try {
                    qr.recallSource()
                } catch (e: PermissionDeniedException) {
                    event.quoteReply("机器人无权撤回对应消息")
                } catch (e: IllegalStateException) {
                }
                try {
                    event.message.recall()
                } catch (e: IllegalStateException) {
                }
            }
            return true
        }

    },
    Donate("#donate", action = {
        MainGroup.sendMessage("捐赠支持服务器请查看：https://donate.glavo.org/")
    }),
    Live("#live", action = {
        MainGroup.sendMessage("服务器开始直播了：https://live.bilibili.com/331537")
        evalCommand("tellraw @a " + kotlinx.serialization.json.jsonArray {
            +json {
                "text" to ""
                "color" to "gray"
            }

            +"服务器开始直播了，"
            +json {
                "text" to "点击这里进入直播间"
                "underlined" to true
                "clickEvent" to kotlinx.serialization.json.json {
                    "action" to "open_url"
                    "value" to "https://live.bilibili.com/331537"
                }
            }
            +"。"
        })
    }),
    Spectate(level = 1) {
        private val command = "#sp"
        override suspend fun invoke(event: MessageEvent, message: MessageChain, content: String): Boolean {
            if (!content.startsWith(command)) return false
            withCheck(event) {
                val c = content.substring(command.length).trim()
                if (c.isBlank()) {
                    event.quoteReply("请指定被附身者的名称")
                    return true
                }

                suspend fun spectate(i: String) {
                    event.quoteReply(
                        evalCommands(
                            "tp Glavo $i",
                            "spectate $i Glavo"
                        )
                    )
                }

                Player.All.firstOrNull { it.names.contains(c) }?.let {
                    spectate(c)
                    return true
                }

                Player.All.firstOrNull { it.nicknames.contains(c) }?.let {
                    val name = it.names.firstOrNull()
                    if (name == null) {
                        event.quoteReply("指定的玩家未登入过服务器")
                    } else {
                        spectate(name)
                    }
                    return true
                }

                event.message.firstOrNull { it is At && it.target != event.bot.id }?.let { m ->
                    val a = m as At
                    val p = Player.All.firstOrNull { it.qq == a.target }
                    if (p == null) {
                        event.quoteReply("指定的玩家未登入过服务器")
                    } else {
                        val name = p.names.firstOrNull()
                        if (name == null) {
                            event.quoteReply("指定的玩家未登入过服务器")
                        } else {
                            spectate(name)
                        }
                    }
                    return true
                }

                if (Permission.ofQQ(event.sender.id).any { it is Permission.CommandPermission && it.command == Eval }) {
                    spectate(c)
                } else {
                    event.quoteReply("无法找到命令指定的对象")
                }
            }
            return true
        }

    },

    Forward {
        override suspend fun invoke(event: MessageEvent, message: MessageChain, content: String): Boolean {
            val qr = message[QuoteReply]
            val mc = if (qr == null) {
                if (!content.startsWith('>') && !content.startsWith('＞')) return false
                message
            } else {
                val tem = arrayListOf<Message>()
                var rmAt = true
                var rmSpace = false

                for (m in message) {
                    when (m) {
                        !is MessageContent -> {
                        }
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
                                            tem += m.content.substring(1).toMessage()
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
                val mc = tem.asMessageChain()
                if (mc.firstOrNull().let {
                        it == null || it !is PlainText || !(it.content.startsWith('>') || it.content.startsWith('＞'))
                    }) {
                    return false
                }
                mc
            }

            var rmPre = true
            val tem = mutableListOf<JsonElement>()

            val player = Player[event.sender.id]
            val playerName = player?.names?.firstOrNull().let {
                if (it == null) event.sender.nameCardOrNick else "$it"
            }
            val atCommand = "@" + (player?.names?.firstOrNull() ?: event.sender.id) + " "
            val card = player?.names?.firstOrNull().let {
                if (it == null) {
                    "@${event.sender.nameCardOrNick}(${event.sender.id})"
                } else {
                    "@$it"
                }
            }
            tem += json {
                "text" to ""
                "color" to "gray"
            }
            tem += json {
                "text" to "<$playerName>"
                "hoverEvent" to json {
                    "action" to "show_text"
                    "contents" to card
                }
                "insertion" to atCommand
                "clickEvent" to json {
                    "action" to "suggest_command"
                    "value" to atCommand
                }
            }
            tem += JsonPrimitive(" ")

            loop@
            for (m in mc) {
                val c = m.content
                when {
                    c.isEmpty() -> continue@loop
                    m is PlainText -> {
                        if (rmPre) {
                            if (c.startsWith('>') || c.startsWith('＞')) {
                                if (c.length > 1) {
                                    tem += json {
                                        "text" to c.substring(1)
                                    }
                                }
                            }
                            rmPre = false
                        } else {
                            tem += json {
                                "text" to c
                            }
                        }
                    }
                    m is At -> tem += JsonPrimitive(
                        (Player[m.target]?.names?.firstOrNull()?.let { "@$it" } ?: m.display)
                    )
                    m is Image -> {
                        val imageUrl = m.queryUrl()
                        tem += json {
                            "text" to c
                            "underlined" to true
                            "clickEvent" to json {
                                "action" to "open_url"
                                "value" to imageUrl
                            }
                            "hoverEvent" to json {
                                "action" to "show_text"
                                "contents" to "点击查看图片"
                            }
                        }
                    }
                    else -> tem += JsonPrimitive(c)
                }

            }

            try {
                rcon("tellraw @a " + JsonArray(tem))
            } catch (e: Throwable) {
                event.quoteReply("连接服务器失败")
            }

            return true
        }

    }
    ;

    private var commandName: String? = null
    private var action: (suspend (MessageEvent) -> Unit)? = null

    constructor(commandName: String, level: Int = 0, action: suspend (MessageEvent) -> Unit) : this(level) {
        this.commandName = commandName
        this.action = action
    }

    open suspend operator fun invoke(event: MessageEvent, message: MessageChain, content: String): Boolean {
        if (commandName == null) {
            event.bot.logger.warning("$this 命令未实现")
            return false
        }
        if (content != commandName) return false
        withCheck(event) {
            action!!.invoke(event)
        }
        return true
    }

    protected suspend inline fun withCheck(event: MessageEvent, action: () -> Unit) {
        val p = Permission.ofQQ(event.sender.id)
        if (p.any { it is Permission.CommandPermission && it.command == this }) {
            action()
        } else {
            event.quoteReply("您无权执行该命令")
        }
    }

    companion object {
        val All: List<Command> = Command.values().asList()
    }
}