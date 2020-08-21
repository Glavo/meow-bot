@file:Suppress("RemoveRedundantQualifierName", "MemberVisibilityCanBePrivate", "unused")

package org.glavo.bot.data

import org.glavo.bot.Command

typealias Permissions = Set<Permission>

sealed class Permission(val name: String, val level: Int = 0) : Comparable<Permission> {
    class CommandPermission(val command: Command) : Permission(command.name, command.level) {
        companion object {
            val All by lazy { Command.All.map { CommandPermission(it) }.toSortedSet() }
        }
    }

    class UnknownPermission(name: String) : Permission(name)

    companion object {
        const val DefaultLevel = 0
        const val AdministratorLevel = 1000
        const val OwnerLevel = Int.MAX_VALUE
        const val EvalLevel = 4

        val All: Permissions by lazy { CommandPermission.All }

        private fun level(n: Int): Set<Permission> = Permission.All.filter { it.level <= n }.toSortedSet()

        /**
         * 普通成员
         */
        val Level0: Permissions by lazy { level(0) }

        /**
         *
         */
        val Level1: Permissions by lazy { level(1) }

        /**
         * 撤回群消息
         */
        val Level2: Permissions by lazy { level(2) }

        /**
         * 操作白名单
         */
        val Level3: Permissions by lazy { level(3) }

        /**
         * 执行任意命令
         */
        val Level4: Permissions by lazy { level(4) }

        val Default by lazy { Level0 }
        val Administrator: Permissions by lazy { level(AdministratorLevel) }
        val Owner: Permissions by lazy { Permission.All }

        fun ofQQ(qq: Long): Permissions = Player[qq]?.permissions ?: Default

        fun ofLevel(level: String?): Permissions {
            if (level == null) return Default

            level.toIntOrNull()?.let {
                return when (it) {
                    0 -> Level0
                    1 -> Level1
                    2 -> Level2
                    3 -> Level3
                    4 -> Level4
                    else -> level(it)
                }
            }

            when (level.toLowerCase()) {
                "administrator", "op" -> return Administrator
                "owner" -> return Owner
            }

            return Default
        }

        operator fun get(name: String): Permission? = All.firstOrNull { it.name.equals(name, true) }
    }

    override fun compareTo(other: Permission): Int = name.compareTo(other.name)
    override fun hashCode(): Int = name.hashCode()
    override fun toString(): String = "Permission[$name]"
    override fun equals(other: Any?): Boolean = other is Permission && name == other.name
}