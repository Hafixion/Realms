package eu.hafixion.realms.towny.towns.listeners

import br.com.devsrsouza.kotlinbukkitapi.extensions.event.KListener
import br.com.devsrsouza.kotlinbukkitapi.extensions.event.event
import br.com.devsrsouza.kotlinbukkitapi.extensions.text.msg
import eu.hafixion.realms.RealmsCorePlugin
import eu.hafixion.realms.toRealmsPlayer
import eu.hafixion.realms.towny.towns.Town
import eu.hafixion.realms.towny.towns.TownDatabase.getTown
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.build_ask_mayor_for_perm
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.build_fail
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.destroy_ask_mayor_for_perm
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.destroy_fail
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.dyes
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.interact_ask_mayor_for_perm
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.interact_fail
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.itemUseMaterials
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.pottedPlants
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.redstoneInteractibles
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.switchMaterials
import eu.hafixion.realms.towny.towns.listeners.ListenerUtils.switch_fail
import eu.hafixion.realms.utils.blackLine
import eu.hafixion.realms.utils.errorMessage
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.Projectile
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.*

class PlayerListener(override val plugin: RealmsCorePlugin, val town: Town) : KListener<RealmsCorePlugin> {

    init {
        playerBucketEvents()
        playerInteractEntityEvent()
        playerInteractEvent()
        playerTeleportEvent()
        playerFishAndArmorStandEvents()
        playerLecternEvent()
        playerJoinEvent()

        //todo town home checks
        playerPvpEvent()
    }

    private fun playerJoinEvent() {
        event<PlayerJoinEvent> {
            if (player.toRealmsPlayer().town != town) return@event

            if (town.stockpileChest == null || town.stockpileChest?.block?.type != Material.CHEST) player.msg("""
                ${blackLine()}
                §4§lALERT §cYour town doesn't have a set stockpile, it can't pay upkeep!
                ${blackLine()}
            """.trimIndent())
            else if (!town.hasItemsInStockpileChest(town.calculateUpkeep()).first) player.msg("""
                ${blackLine()}
                §4§lALERT §cYour town can't afford upkeep! It'll die the next time upkeep is collected!
                ${blackLine()}
            """.trimIndent())
        }
    }

    fun playerPvpEvent() {
        event<EntityDamageByEntityEvent>(ignoreCancelled = true) {
            if (getTown(entity.location) != town) return@event

            if (damager is Projectile && entity is Player) {
                val projectile = damager as Projectile

                if (town.pvp) return@event
                if (town.isNew()) {
                    isCancelled = true
                    return@event
                }

                if (projectile.shooter is Player) {
                    val player = (projectile.shooter as Player).toRealmsPlayer()
                    val damaged = (entity as Player).toRealmsPlayer()

                    if (player.nation == null && damaged.nation == null) isCancelled = false
                }

                return@event
            }

            if (damager !is Player) return@event

            val player = (damager as Player).toRealmsPlayer()

            if (entity !is Player) {
                if (town.mayor.isEnemy(player)) return@event

                if (!town.canPlayerDestroy(
                        player,
                        entity.location.block
                    )
                ) {
                    isCancelled = true
                    player.player?.msg(errorMessage("You don't have permission to attack mobs here. (Destroy)"))
                }
                return@event
            }

            if (town.pvp) return@event

            isCancelled = !player.isEnemy((entity as Player).toRealmsPlayer())

            if (player.nation == null && (entity as Player).toRealmsPlayer().nation == null) isCancelled = false
            if (town.isNew()) isCancelled = true
        }

    }

    fun playerBucketEvents() {
        event<PlayerBucketEmptyEvent> {
            if (getTown(block.location) != town) return@event

            if (!town.canPlayerBuild(player.toRealmsPlayer(), block)) {
                isCancelled = true
                if (player.toRealmsPlayer().town == town) {
                    player.msg(errorMessage(build_ask_mayor_for_perm))
                    return@event
                }
                player.msg(errorMessage(build_fail))
            }
        }

        event<PlayerBucketFillEvent> {
            if (getTown(block.location) != town) return@event

            if (!town.canPlayerInteract(player.toRealmsPlayer(), block.location)) {
                isCancelled = true
                if (player.toRealmsPlayer().town == town) {
                    player.msg(errorMessage(interact_ask_mayor_for_perm))
                    return@event
                }
                player.msg(errorMessage(interact_fail))
            }
        }
    }

    fun playerInteractEvent() {
        event<PlayerInteractEvent> {
            if (clickedBlock != null) {
                if (getTown(clickedBlock!!.location) != town) return@event

                if (switchMaterials.contains(clickedBlock!!.type)) {
                    if ("RIGHT" in action.toString()) {
                        if (!town.canPlayerSwitch(player.toRealmsPlayer(), clickedBlock!!)) {
                            isCancelled = true
                            player.msg(errorMessage(switch_fail))
                        }
                    }
                    return@event
                }

                if (
                    pottedPlants.contains(clickedBlock!!.type.name)
                    || redstoneInteractibles.contains(clickedBlock!!.type.name)
                    || clickedBlock!!.type == Material.BEACON
                    || clickedBlock!!.type == Material.DRAGON_EGG
                    || clickedBlock!!.type == Material.COMMAND_BLOCK
                    || clickedBlock!!.type == Material.SWEET_BERRY_BUSH
                ) {
                    if (!town.canPlayerDestroy(player.toRealmsPlayer(), clickedBlock!!)) {
                        isCancelled = true
                        player.msg(errorMessage(destroy_fail))
                    }
                    return@event
                }
            }

            if (getTown(player.location) != town) return@event

            if (hasItem() && (itemUseMaterials.contains(item?.type)
                        && !town.canPlayerInteract(player.toRealmsPlayer(), clickedBlock?.location ?: player.location))
            ) {
                isCancelled = true
                player.msg(errorMessage(interact_fail))
                return@event
            }
        }
    }

    fun playerInteractEntityEvent() {
        event<PlayerInteractEntityEvent> {
            if (getTown(player.location) != town) return@event

            if (getTown(rightClicked.location) != town) return@event

            val item = player.inventory.getItem(hand)
            when (rightClicked.type) {
                EntityType.ITEM_FRAME,
                EntityType.PAINTING,
                EntityType.LEASH_HITCH,
                EntityType.MINECART_COMMAND,
                EntityType.MINECART_MOB_SPAWNER -> {
                    if (!town.canPlayerDestroy(player.toRealmsPlayer(), rightClicked.location.block)) {
                        player.msg(errorMessage(destroy_fail))
                        isCancelled = true
                    }
                    return@event
                }

                EntityType.WOLF, EntityType.SHEEP -> {
                    if (dyes.contains(item.type.name) || item.type == Material.SHEARS) {
                        if (!town.canPlayerDestroy(player.toRealmsPlayer(), rightClicked.location.block)) {
                            player.msg(errorMessage(destroy_fail))
                            isCancelled = true
                        }
                        return@event
                    }
                }

                EntityType.MINECART_CHEST, EntityType.MINECART_FURNACE, EntityType.MINECART_HOPPER -> {
                    if (!town.canPlayerSwitch(player.toRealmsPlayer(), rightClicked.location.block)) {
                        player.msg(errorMessage(switch_fail))
                        isCancelled = true
                    }
                    return@event
                }
                else -> {
                }
            }

            if (itemUseMaterials.contains(item.type)
                && !town.canPlayerInteract(player.toRealmsPlayer(), rightClicked.location)
            ) {
                isCancelled = true
                player.msg(errorMessage(switch_fail))
                return@event
            }
        }
    }

    fun playerTeleportEvent() {
        event<PlayerTeleportEvent> {
            if (getTown(from) == town || getTown(to!!) == town) {
                when (cause) {
                    PlayerTeleportEvent.TeleportCause.ENDER_PEARL,
                    PlayerTeleportEvent.TeleportCause.CHORUS_FRUIT ->
                        if (!town.canPlayerInteract(player.toRealmsPlayer(), to!!)
                            && town.canPlayerInteract(player.toRealmsPlayer(), from)
                        ) {
                            player.msg(errorMessage(interact_fail))
                            isCancelled = true
                        }
                    else -> {}
                }
            }
        }
    }

    fun playerFishAndArmorStandEvents() {
        event<PlayerArmorStandManipulateEvent> {
            if (getTown(player.location) != town) return@event

            if (!town.canPlayerInteract(player.toRealmsPlayer(), player.location)) {
                isCancelled = true
                if (player.toRealmsPlayer().town == town)
                    player.msg(errorMessage(interact_ask_mayor_for_perm))
                else player.msg(errorMessage(interact_fail))
            }
        }

        event<PlayerFishEvent> {
            if (state == PlayerFishEvent.State.CAUGHT_ENTITY && getTown(player.location) == town
                && !town.canPlayerInteract(player.toRealmsPlayer(), player.location)
            ) {
                isCancelled = true
                if (player.toRealmsPlayer().town == town)
                    player.msg(errorMessage(interact_ask_mayor_for_perm))
                else player.msg(errorMessage(interact_fail))
            }
        }
    }

    fun playerLecternEvent() {
        event<PlayerTakeLecternBookEvent> {
            if (getTown(lectern.location) != town) return@event

            if (!town.canPlayerDestroy(player.toRealmsPlayer(), lectern.block)) {
                isCancelled = true
                if (player.toRealmsPlayer().town == town)
                    player.msg(errorMessage(destroy_ask_mayor_for_perm))
                else player.msg(errorMessage(destroy_fail))
            }
        }
    }
}