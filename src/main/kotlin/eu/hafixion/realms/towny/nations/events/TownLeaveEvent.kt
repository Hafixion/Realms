package eu.hafixion.realms.towny.nations.events

import eu.hafixion.realms.towny.nations.Nation
import eu.hafixion.realms.towny.towns.Town
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class TownLeaveEvent(val town: Town, val nation: Nation) : Event(), Cancellable {

    companion object {
        private val handlerList = HandlerList()
        @JvmStatic
        @JvmName("getHandlerList")
        fun getHandlerList() = handlerList
    }
    private var cancel = false

    init {
        nation.recalculateClaims()
    }

    override fun getHandlers() = handlerList

    override fun isCancelled() = cancel

    override fun setCancelled(cancel: Boolean) {
        this.cancel = cancel
    }

}