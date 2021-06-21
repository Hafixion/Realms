package eu.hafixion.realms.towny.nations.listeners

import br.com.devsrsouza.kotlinbukkitapi.extensions.event.KListener
import br.com.devsrsouza.kotlinbukkitapi.extensions.event.event
import br.com.devsrsouza.kotlinbukkitapi.extensions.text.msg
import eu.hafixion.realms.RealmsCorePlugin
import eu.hafixion.realms.toRealmsPlayer
import eu.hafixion.realms.towny.nations.Nation
import eu.hafixion.realms.towny.nations.NationDatabase
import eu.hafixion.realms.utils.asPos
import eu.hafixion.realms.utils.errorMessage
import org.bukkit.block.Block
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockExplodeEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerTeleportEvent

class PermissionListener(override val plugin: RealmsCorePlugin, val nation: Nation) : KListener<RealmsCorePlugin> {

    init {
        blockBreakEvent()
        blockPlaceEvent()
        playerTeleportEvent()
        blockExplodeEvents()
    }

    fun blockBreakEvent() {
        event<BlockBreakEvent> {
            if (NationDatabase[block.chunk.asPos()] != nation) return@event

            val claimChunk = nation.claims.first { it.chunkPos == block.chunk.asPos() }
            if (!nation.canPlayerBreak(player.toRealmsPlayer())) claimChunk.onBlockBreakEvent(this)
            else if (claimChunk.originalMap[block.asPos()]?.let { claimChunk.map[block.asPos()]?.minus(it) } ?: 0 > 10) {
                this.expToDrop = 0
                this.isDropItems = false
                player.msg("§cThe block was too damaged to recover.")
            }
        }
    }

    fun blockPlaceEvent() {
        event<BlockPlaceEvent> {
            if (NationDatabase[block.chunk.asPos()] != nation) return@event

            isCancelled = !nation.canPlayerBuild(player.toRealmsPlayer())
            if (isCancelled) player.msg(errorMessage("You can't build in claim wilderness."))
        }
    }

    fun playerTeleportEvent() {
        event<PlayerTeleportEvent> {
            if (to != null
                && NationDatabase[from.chunk.asPos()] == nation || NationDatabase[to!!.chunk.asPos()] == nation)
                when (cause) {
                    PlayerTeleportEvent.TeleportCause.ENDER_PEARL,
                    PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT -> {
                        isCancelled = !nation.canPlayerInteract(player.toRealmsPlayer())
                        if (isCancelled) player.msg(errorMessage("You can't use ender pearls in claimed wilderness."))
                    }
                    else -> {}
                }
        }
    }


    fun blockExplodeEvents() {
        event<BlockExplodeEvent> {
            val combinedHashMap = hashMapOf<Block, Boolean>()

            if (blockList().any { block -> nation.claims.any { it.chunkPos == block.chunk.asPos() } })
                nation.claims.forEach { combinedHashMap.putAll(it.onExplodeEvent(blockList())) }
            blockList().removeAll(combinedHashMap.filter { it.value }.keys)
        }

        event<EntityExplodeEvent> {
            val combinedHashMap = hashMapOf<Block, Boolean>()

            if (blockList().any { block -> nation.claims.any { it.chunkPos == block.chunk.asPos() } })
                nation.claims.forEach { combinedHashMap.putAll(it.onExplodeEvent(blockList())) }
            blockList().removeAll(combinedHashMap.filter { it.value }.keys)
        }
    }
}