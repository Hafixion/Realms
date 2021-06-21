package eu.hafixion.realms.towny.towns.listeners

import br.com.devsrsouza.kotlinbukkitapi.extensions.event.KListener
import br.com.devsrsouza.kotlinbukkitapi.extensions.event.event
import br.com.devsrsouza.kotlinbukkitapi.extensions.text.msg
import eu.hafixion.realms.RealmsCorePlugin
import eu.hafixion.realms.toRealmsPlayer
import eu.hafixion.realms.towny.towns.Town
import eu.hafixion.realms.towny.towns.TownDatabase
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.block_fully_protected
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.build_ask_mayor_for_perm
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.build_fail
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.build_fail_frostwalk
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.destroy_ask_mayor_for_perm
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.fire_disabled
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.town_newbie
import eu.hafixion.realms.utils.asPos
import eu.hafixion.realms.utils.errorMessage
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.Container
import org.bukkit.block.data.Directional
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.*
import org.bukkit.event.entity.EntityExplodeEvent
import org.bukkit.event.player.PlayerMoveEvent

class BlockListener(override val plugin: RealmsCorePlugin, val town: Town) : KListener<RealmsCorePlugin> {

    init {
        blockBreakEvent()
        blockPlaceEvent()
        blockFireEvents()
        blockExplodeEvents()
        pistonEvents()
        blockTransformEvents()
        blockDispenseEvent()
        blockGlitchFix()
    }

    private fun blockGlitchFix() {
        event<PlayerMoveEvent> {
            if (TownDatabase.getTown(player.location) != town) return@event
            if (to == null || from.y >= to!!.y) return@event
            if (town.canPlayerBuild(player.toRealmsPlayer(), player.location.block)) return@event

            if (from.block.getRelative(BlockFace.DOWN).type === Material.AIR && player.fallDistance == 0f && player.velocity.y <= -0.6 && player.location.y > 0) {
                // plugin.sendErrorMsg(player, "Cheat Detected!");
                val blockLocation = from

                // find the first non air block below us
                while (blockLocation.block
                        .type === Material.AIR && blockLocation.y > 0
                ) blockLocation.y = blockLocation.y - 1

                // set to 1 block up so we are not sunk in the
                // ground
                blockLocation.y = blockLocation.y + 1

                player.teleport(blockLocation)
                return@event
            }
        }
    }

    fun blockBreakEvent() {
        event<BlockBreakEvent> {
            if (TownDatabase.getTown(block.location) != town) return@event
            val chunk = town.claims.keys.first { it.chunkPos == block.chunk.asPos() }

            if (town.canPlayerDestroy(player.toRealmsPlayer(), block)) {
                if (chunk.originalMap[block.asPos()]?.let { chunk.map[block.asPos()]?.minus(it) } ?: 0 > 10 && block.state !is Container) {
                    this.expToDrop = 0
                    this.isDropItems = false
                    player.msg("§cThe block was too damaged to recover.")
                }
                chunk.map.remove(block.asPos())
                return@event
            }

            if (chunk.defaultReinforcement < 0) {
                player.msg(errorMessage(block_fully_protected))
                isCancelled = true
                return@event
            }

            if (player.toRealmsPlayer().town == town) {
                player.msg(errorMessage(destroy_ask_mayor_for_perm))
                isCancelled = true
                return@event
            }

            if (!town.isNew()) chunk.onBlockBreakEvent(this)
            else {
                player.msg(errorMessage(town_newbie))
                isCancelled = true
            }
        }
    }

    fun blockPlaceEvent() {
        event<BlockPlaceEvent> {
            if (TownDatabase.getTown(block.location) != town) return@event

            if (!town.canPlayerBuild(player.toRealmsPlayer(), block)) {
                isCancelled = true
                if (player.toRealmsPlayer().town == town)
                    player.msg(errorMessage(build_ask_mayor_for_perm))
                else player.msg(errorMessage(build_fail))
            }
        }
    }

    fun blockFireEvents() {
        event<BlockBurnEvent> {
            if (TownDatabase.getTown(block.location) != town) return@event
            isCancelled = !town.fire
        }

        event<BlockIgniteEvent> {
            if (TownDatabase.getTown(block.location) != town) return@event

            if (!town.fire) {
                isCancelled = true
                if (player != null) player!!.msg(errorMessage(fire_disabled))
            }
        }
    }

    fun blockExplodeEvents() {
        event<BlockExplodeEvent>(EventPriority.HIGHEST) {
            val combinedHashMap = hashMapOf<Block, Boolean>()

            if (blockList().any { TownDatabase.getTown(it.location) == town } && !town.explosions)
                town.claims.keys.forEach { combinedHashMap.putAll(it.onExplodeEvent(blockList())) }
            blockList().removeAll(combinedHashMap.filter { it.value }.keys)
        }

        event<EntityExplodeEvent>(EventPriority.HIGHEST) {
            val combinedHashMap = hashMapOf<Block, Boolean>()

            if (blockList().any { TownDatabase.getTown(it.location) == town } && !town.explosions)
                town.claims.keys.forEach { combinedHashMap.putAll(it.onExplodeEvent(blockList())) }
            blockList().removeAll(combinedHashMap.filter { it.value }.keys)
        }
    }

    fun pistonEvents() {
        event<BlockPistonRetractEvent> {
            if (blocks.all { TownDatabase.getTown(it.location) == town }) return@event
            for (block in blocks) if (TownDatabase.getTown(block.location) == town) isCancelled = true
        }

        event<BlockPistonExtendEvent> {
            if (blocks.all { TownDatabase.getTown(it.location) == town }) return@event
            for (block in blocks) if (TownDatabase.getTown(block.location) == town) isCancelled = true
        }
    }

    fun blockTransformEvents() {
        event<EntityBlockFormEvent> {
            if (TownDatabase.getTown(block.location) != town) return@event

            if (entity is Player) {
                val player = entity as Player

                if (!town.canPlayerBuild(player.toRealmsPlayer(), block)) {
                    isCancelled = true
                    player.msg(errorMessage(build_fail_frostwalk))
                }
            } else isCancelled = town.mobs
        }

        event<BlockFromToEvent> {
            if (!arrayOf(block, toBlock).any { TownDatabase.getTown(it.location) != town }) return@event
            if (block.type == Material.DRAGON_EGG) return@event
            isCancelled = TownDatabase.getTown(toBlock.location) == town
                    && TownDatabase.getTown(block.location) != town
        }
    }

    // Might not be working?
    fun blockDispenseEvent() {
        event<BlockDispenseEvent> {
            if ("BUCKET" !in item.type.toString()) return@event
            if (TownDatabase.getTown(block.location) != town
                && TownDatabase.getTown(block.getRelative((block.blockData as Directional).facing).location) == town
            ) {
                isCancelled = true
                return@event
            }
        }
    }
}