package eu.hafixion.realms.towny.towns.events

import eu.hafixion.realms.towny.towns.Town
import eu.hafixion.realms.utils.ChunkChangeEvent
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class TownEntryEvent(val town: Town, val player: Player, val event: ChunkChangeEvent) : Event(), Cancellable {

    companion object {
        private val handlerList = HandlerList()
        @JvmStatic
        @JvmName("getHandlerList")
        fun getHandlerList() = handlerList
    }
    private var cancel = false

    override fun getHandlers() = handlerList

    override fun isCancelled() = cancel

    override fun setCancelled(cancel: Boolean) {
        this.cancel = cancel
    }

}