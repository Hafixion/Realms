package eu.hafixion.realms.towny.towns.events

import eu.hafixion.realms.towny.nations.NationDatabase
import eu.hafixion.realms.towny.towns.Town
import eu.hafixion.realms.utils.ChunkPos
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.bukkit.event.HandlerList

class TownClaimEvent(val town: Town, val chunk: ChunkPos) : Event(), Cancellable {

    companion object {
        private val handlerList = HandlerList()
        @JvmStatic
        @JvmName("getHandlerList")
        fun getHandlerList() = handlerList
    }
    private var cancel = false

    init {
        if (NationDatabase[chunk] != null) {
            val nation = NationDatabase[chunk]!!
            nation.claims.removeIf { it.chunkPos == chunk }
        }
    }

    override fun getHandlers() = handlerList

    override fun isCancelled() = cancel

    override fun setCancelled(cancel: Boolean) {
        this.cancel = cancel
    }

}