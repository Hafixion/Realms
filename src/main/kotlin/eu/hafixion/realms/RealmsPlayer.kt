package eu.hafixion.realms

import br.com.devsrsouza.kotlinbukkitapi.extensions.server.offlinePlayer
import br.com.devsrsouza.kotlinbukkitapi.extensions.server.player
import br.com.devsrsouza.kotlinbukkitapi.utils.toBooleanOrNull
import eu.hafixion.realms.RealmsPlayerDatabase.realmsPlayers
import eu.hafixion.realms.confirmation.ConfirmationHandler
import eu.hafixion.realms.towny.nations.Nation
import eu.hafixion.realms.towny.towns.Town
import eu.hafixion.realms.towny.towns.TownDatabase
import org.bukkit.entity.Player
import java.util.*

object RealmsPlayerDatabase {
    val realmsPlayers = arrayListOf<RealmsPlayer>()

    operator fun get(uuid: UUID) = realmsPlayers.filter { it.uuid == uuid }.getOrNull(0)
    operator fun get(name: String) = realmsPlayers.filter { it.name.equals(name, true) }.getOrNull(0)

    operator fun set(uuid: UUID, player: RealmsPlayer) {
        for (pl in realmsPlayers.filter { it.uuid == uuid }) realmsPlayers.remove(pl)
        realmsPlayers.add(player)
    }

    fun serialize() = hashMapOf<String, String>().apply { realmsPlayers.forEach { put(it.uuid.toString(), it.serialize()) } }
}

class RealmsPlayer(val uuid: UUID) {
    val name get() = offlinePlayer(uuid).name ?: "NPC"
    val online get() = offlinePlayer(uuid).isOnline
    val offlinePlayer get() = offlinePlayer(uuid)
    val player get() = player(uuid)
    var repairMode = false
    var canTalk = true
    val town: Town? get() {
        for (town in TownDatabase.towns) if (town.residents.contains(this)) return town
        return null
    }
    val nation: Nation? get() = town?.nation
    val isMayor: Boolean get() = town?.mayor == this
    var hasMadeTownBefore = false
    var hasBrokeBlockBefore = false

    var lastTeleported = System.currentTimeMillis()
    var lastDamaged = System.currentTimeMillis() - 100000
    val combatTagged: Boolean get() = System.currentTimeMillis() - lastDamaged < 10000

    constructor(uuid: UUID, repair: Boolean, hasMadeTown: Boolean, hasBrokeBlock: Boolean): this(uuid) {
        this.repairMode = repair
        this.hasBrokeBlockBefore = hasBrokeBlock
        this.hasMadeTownBefore = hasMadeTown
    }

    companion object {
        fun deserialize(str: String) =
            RealmsPlayer(
                UUID.fromString(str.split(", ")[0]),
                str.split(", ")[1].toBooleanOrNull() ?: false,
                str.split(", ")[2].toBooleanOrNull() ?: false,
                str.split(", ")[3].toBooleanOrNull() ?: false
            )
    }

    init {
        if (RealmsPlayerDatabase[uuid] != null) RealmsPlayerDatabase[uuid] = this
        realmsPlayers.add(this)
    }

    fun serialize(): String = "$uuid, $repairMode, $hasMadeTownBefore, $hasBrokeBlockBefore"

    fun isEnemy(pl: RealmsPlayer) = if (pl.nation != null && nation != null)
        nation!!.enemies.contains(pl.nation!!) || pl.nation!!.enemies.contains(nation!!) else false

    fun hasTownPermission(perm: String): Boolean {
        if (offlinePlayer.isOp) return true
        if (town == null) return false
        if (town!!.mayor == this) return true

        return (town!!.permissions[this] ?: return false).contains(perm) || (town!!.permissions[this] ?: return false).contains("*")
    }

    fun hasNationPermission(perm: String): Boolean {
        if (offlinePlayer.isOp) return true
        if (nation == null) return false
        if (nation!!.leader == this) return true

        return (nation!!.permissions[this] ?: return false).contains(perm) || (nation!!.permissions[this] ?: return false).contains("*")
    }

    //todo War Implementation, idk, a lot of stuff, just a placeholder fn
    fun canTeleportHome(): Boolean = true
    fun canTeleportToTown(town: Town) = true

    fun isAtWarWith(town: Town) = true

    fun hasConfirmation() = ConfirmationHandler.hasConfirmation(player)

    override fun toString() = uuid.toString()
}

fun Player.toRealmsPlayer() = RealmsPlayerDatabase[uniqueId] ?: RealmsPlayer(uniqueId)

fun realmsPlayer(p: Player) = p.toRealmsPlayer()