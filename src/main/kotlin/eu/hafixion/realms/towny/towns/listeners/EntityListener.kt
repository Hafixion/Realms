package eu.hafixion.realms.towny.towns.listeners

import br.com.devsrsouza.kotlinbukkitapi.extensions.event.KListener
import br.com.devsrsouza.kotlinbukkitapi.extensions.event.event
import br.com.devsrsouza.kotlinbukkitapi.extensions.text.msg
import eu.hafixion.realms.RealmsCorePlugin
import eu.hafixion.realms.toRealmsPlayer
import eu.hafixion.realms.towny.towns.Town
import eu.hafixion.realms.towny.towns.TownDatabase
import eu.hafixion.realms.utils.errorMessage
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.Monster
import org.bukkit.entity.Player
import org.bukkit.entity.ThrownPotion
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.hanging.HangingBreakByEntityEvent

class EntityListener(override val plugin: RealmsCorePlugin, val town: Town) : KListener<RealmsCorePlugin> {

    init {
        changeBlockEvent()
        hangingBreak()
        spawnEvent()
    }

    fun changeBlockEvent() {
        event<EntityChangeBlockEvent> {
            if (TownDatabase.getTown(entity.location) != town || TownDatabase.getTown(block.location) != town) return@event

            if (block.type == Material.FARMLAND) {
                isCancelled = true
                return@event
            }

            when (entity.type) {
                EntityType.RAVAGER,
                EntityType.ENDERMAN -> {
                    isCancelled = true
                    return@event
                }

                EntityType.SPLASH_POTION -> {
                    val player = ((entity as ThrownPotion).shooter as Player?)?.toRealmsPlayer() ?: return@event
                    isCancelled = !town.canPlayerDestroy(player, block)
                    return@event
                }

                EntityType.WITHER -> {
                    isCancelled = !town.explosions
                    return@event
                }

                else -> {
                }
            }
        }
    }


    fun hangingBreak() {
        event<HangingBreakByEntityEvent> {
            if (remover == null) return@event
            if (TownDatabase.getTown(entity.location) != town) return@event

            if (remover is Player
                && !town.canPlayerDestroy((remover!! as Player).toRealmsPlayer(), entity.location.block)
            ) {
                isCancelled = true
                remover!!.msg(errorMessage("You aren't allowed to break here."))
            }
        }
    }

    fun spawnEvent() {
        event<CreatureSpawnEvent> {
            if (TownDatabase.getTown(entity.location) != town) return@event

            if (entity is Monster) isCancelled = !town.mobs
        }
    }

}