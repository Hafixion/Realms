package eu.hafixion.realms.towny.towns.events

import eu.hafixion.realms.towny.towns.Town
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class TownDeleteEvent(val town: Town) : Event(), Cancellable {

    companion object {
        private val handlerList = HandlerList()
        @JvmStatic
        @JvmName("getHandlerList")
        fun getHandlerList() = handlerList
    }
    private var cancel = false

    init {
        town.nation?.towns?.remove(town)
        if (town.nation?.capital == town) town.nation?.delete()
        town.nation?.recalculateClaims()
    }

    override fun getHandlers() = handlerList

    override fun isCancelled() = cancel

    override fun setCancelled(cancel: Boolean) {
        this.cancel = cancel
    }

}