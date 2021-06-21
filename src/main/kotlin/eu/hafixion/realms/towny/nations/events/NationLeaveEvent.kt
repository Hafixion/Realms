package eu.hafixion.realms.towny.nations.events

import eu.hafixion.realms.towny.nations.Nation
import eu.hafixion.realms.utils.ChunkChangeEvent
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class NationLeaveEvent(val nation: Nation, val event: ChunkChangeEvent) : Event(), Cancellable {

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