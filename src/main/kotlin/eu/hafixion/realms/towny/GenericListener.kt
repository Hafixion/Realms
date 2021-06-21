package eu.hafixion.realms.towny

import br.com.devsrsouza.kotlinbukkitapi.architecture.lifecycle.extensions.pluginCoroutineScope
import br.com.devsrsouza.kotlinbukkitapi.extensions.event.KListener
import br.com.devsrsouza.kotlinbukkitapi.extensions.event.callEvent
import br.com.devsrsouza.kotlinbukkitapi.extensions.event.event
import br.com.devsrsouza.kotlinbukkitapi.extensions.skedule.BukkitDispatchers
import br.com.devsrsouza.kotlinbukkitapi.extensions.text.msg
import eu.hafixion.realms.RealmsCorePlugin
import eu.hafixion.realms.confirmation.ConfirmationHandler
import eu.hafixion.realms.toRealmsPlayer
import eu.hafixion.realms.towny.block.ClaimChunk
import eu.hafixion.realms.towny.block.getClosestFace
import eu.hafixion.realms.towny.nations.NationDatabase
import eu.hafixion.realms.towny.towns.TownDatabase.getTown
import eu.hafixion.realms.utils.*
import kotlinx.coroutines.launch
import org.bukkit.Chunk
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.ItemStack

class GenericListener(override val plugin: RealmsCorePlugin) : KListener<RealmsCorePlugin> {

    init {
        chunkMoveEvent()
        playerMoveEvent()
        playerInteractEvent()
        playerPvpEvent()
        playerChatEvent()
        playerConfirmationEvents()
        playerJoinEvents()
        blockBreakEvent()

    }

    private fun blockBreakEvent() {

    }

    private fun playerJoinEvents() {
        event<PlayerJoinEvent> {
            player.toRealmsPlayer()
        }
    }

    private fun playerConfirmationEvents() {
        event<PlayerCommandPreprocessEvent> {
            if (player.toRealmsPlayer().hasConfirmation()) {

                if (message == "/accept") {
                    ConfirmationHandler.acceptConfirmation(player)
                    isCancelled = true
                }
                if (message == "/deny") {
                    ConfirmationHandler.revokeConfirmation(player)
                    isCancelled = true
                }
            }
        }
    }

    private fun playerChatEvent() {
        event<AsyncPlayerChatEvent> {
            isCancelled = !player.toRealmsPlayer().canTalk
        }
    }

    private fun playerPvpEvent() {
        event<EntityDamageByEntityEvent> {
            val pl = (entity as? Player ?: return@event).toRealmsPlayer()
            if (!pl.combatTagged) {
                damager as? Player ?: return@event
                (entity as Player).sendActionBar("§cCombat Tagged for 10 seconds.")
            }
            pl.lastDamaged = System.currentTimeMillis()
        }

        event<PlayerQuitEvent> {
            if (player.toRealmsPlayer().combatTagged) player.health = 0.0
        }
    }

    private fun chunkMoveEvent() {
        event<ChunkChangeEvent> {
            when {
                // It. Just. Works.
                getTown(from) != null && getTown(to) != null &&
                        getTown(to) != getTown(from) ->
                    player.sendTownChange(getTown(from)!!, getTown(to)!!, this)
                getTown(from) == null && getTown(to) != null -> player.sendEnteringTown(getTown(to)!!, this)
                getTown(from) != null && getTown(to) == null -> player.sendLeavingTown(getTown(from)!!, this)
                getTown(to) == null && getTown(from) == null
                        && NationDatabase[from.asPos()] == null && NationDatabase[to.asPos()] != null ->
                    player.sendEnteringNation(NationDatabase[to.asPos()]!!)
                getTown(to) == null && getTown(from) == null
                        && NationDatabase[from.asPos()] != null && NationDatabase[to.asPos()] == null ->
                    player.sendLeavingNation(NationDatabase[from.asPos()]!!, this)
            }
        }
    }

    private fun playerMoveEvent() {
        event<PlayerMoveEvent> {
            if (player.inventory.itemInMainHand.type == Material.CLOCK)
                plugin.pluginCoroutineScope.launch(BukkitDispatchers.SYNC) {
                    try {
                        val nearby = player.location.getNearbyBlocks(5)
                        val hashMap = hashMapOf<Chunk, ClaimChunk>()

                        for (loc in nearby) if (loc.block.type != Material.AIR) {
                            if (loc.block.isReinforced()) {
                                val block = loc.block
                                val claimChunk = if (hashMap[block.chunk] != null) hashMap[block.chunk]!! else {
                                    hashMap[block.chunk] = block.getReinforcementChunk()!!
                                    block.getReinforcementChunk()!!
                                }

                                if (claimChunk.activeIndicators.keys.contains(block.asPos())) break

                                val face = block.getClosestFace(player.eyeLocation)
                                val damaged = claimChunk.map[block.asPos()] != claimChunk.originalMap[block.asPos()]
                                if (face != null && claimChunk.defaultReinforcement > 0 && damaged)
                                    claimChunk.createIndicator(block, face, 140)
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

//            if (getTown(player.location) != null && !player.hasPermission("fly")
//                && getTown(player.location)?.residents?.contains(player.toRealmsPlayer()) == true) {
//                val blockLocation = from
//
//                while (blockLocation.block.type === Material.AIR && blockLocation.y > 0
//                ) blockLocation.y = blockLocation.y - 1
//
//                blockLocation.y = blockLocation.y + 1
//
//                player.teleport(blockLocation)
//            }
        }

        event<PlayerItemHeldEvent> {
            if (player.inventory.itemInMainHand.type == Material.CLOCK)
                PlayerMoveEvent(player, player.location, player.location).callEvent()
        }
    }

    private fun playerInteractEvent() {
        event<PlayerInteractEvent> {
            if (player.toRealmsPlayer().repairMode) {
                if (clickedBlock == null || item == null || !isBlockInHand) return@event

                if (clickedBlock!!.type != item!!.type) return@event

                val claimChunk = clickedBlock!!.getReinforcementChunk() ?: return@event
                if (claimChunk.map[clickedBlock!!.asPos()] == claimChunk.originalMap[clickedBlock!!.asPos()]) return@event

                player.inventory.removeItem(ItemStack(item!!.type, 1))


                claimChunk.map.remove(clickedBlock!!.asPos())
                claimChunk.originalMap.remove(clickedBlock!!.asPos())

                player.msg(successMessage("Successfully repaired block."))
                claimChunk.clearIndicators(clickedBlock!!.asPos())
                isCancelled = true
            }
        }
    }
}

