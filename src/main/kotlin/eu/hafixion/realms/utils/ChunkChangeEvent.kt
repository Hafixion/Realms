package eu.hafixion.realms.utils

import br.com.devsrsouza.kotlinbukkitapi.architecture.KotlinPlugin
import br.com.devsrsouza.kotlinbukkitapi.extensions.event.callEvent
import br.com.devsrsouza.kotlinbukkitapi.extensions.event.event
import br.com.devsrsouza.kotlinbukkitapi.extensions.event.events
import org.bukkit.Chunk
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerMoveEvent

class ChunkChangeEvent(val event: PlayerMoveEvent, val to: Chunk, val from: Chunk): Event() {
    val player get() = event.player

    companion object {
        @JvmStatic
        @JvmName("getHandlerList")
        fun getHandlerList() = handlers
        val handlers = HandlerList()

        fun setupListeners(plugin: KotlinPlugin) {
            plugin.events {
                event<PlayerMoveEvent> {
                    if (to != null && to?.chunk != from.chunk)
                        ChunkChangeEvent(this, to!!.chunk, from.chunk).callEvent()
                }
            }
        }
    }

    override fun getHandlers() = Companion.handlers
}