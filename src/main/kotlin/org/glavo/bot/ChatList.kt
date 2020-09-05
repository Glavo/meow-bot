package org.glavo.bot

import java.io.File
import java.util.concurrent.CopyOnWriteArraySet

object ChatList {
    private val chatListFile = File("ChatList.txt")

    val list: CopyOnWriteArraySet<Long> =
        if (chatListFile.exists()) {
            CopyOnWriteArraySet(chatListFile.readLines().mapNotNull { it.toLongOrNull() })
        } else {
            CopyOnWriteArraySet()
        }

    private fun save() {
        synchronized(chatListFile) {
            chatListFile.printWriter().use { w ->
                for (qq in list) {
                    w.println(qq)
                }
            }
        }
    }

    fun add(qq: Long): Boolean {
        return list.add(qq).also { if (it) save() }
    }

    fun remove(qq: Long): Boolean {
        return list.remove(qq).also { if (it) save() }
    }

    operator fun contains(qq: Long): Boolean = list.contains(qq)
}


