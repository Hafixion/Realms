package eu.hafixion.realms.towny.nations

import br.com.devsrsouza.kotlinbukkitapi.extensions.event.callEvent
import br.com.devsrsouza.kotlinbukkitapi.extensions.server.onlinePlayers
import br.com.devsrsouza.kotlinbukkitapi.extensions.text.msg
import eu.hafixion.realms.RealmsCorePlugin
import eu.hafixion.realms.RealmsPlayer
import eu.hafixion.realms.RealmsPlayerDatabase
import eu.hafixion.realms.toRealmsPlayer
import eu.hafixion.realms.towny.block.ClaimChunk
import eu.hafixion.realms.towny.block.distance
import eu.hafixion.realms.towny.nations.commands.NationPermission
import eu.hafixion.realms.towny.nations.events.NationDeleteEvent
import eu.hafixion.realms.towny.nations.listeners.PermissionListener
import eu.hafixion.realms.towny.towns.Town
import eu.hafixion.realms.towny.towns.TownDatabase
import eu.hafixion.realms.utils.ChunkPos
import eu.hafixion.realms.utils.ClaimResult
import eu.hafixion.realms.utils.asPos
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.inventory.ItemStack
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*

object NationDatabase {
    val nations = hashSetOf<Nation>()

    operator fun get(name: String) = nations.firstOrNull { it.name.equals(name, true) }
    operator fun get(uuid: UUID) = nations.firstOrNull { it.uuid == uuid }
    operator fun get(loc: Location) = nations.firstOrNull { nation ->
        nation.claims.any { it.equalsPos(loc.chunk) } || TownDatabase.getTown(loc)?.nation == nation
    }
    operator fun get(town: Town) = nations.firstOrNull { it.allTowns.contains(town) }

    operator fun get(chunk: ChunkPos) = nations.firstOrNull { nation -> nation.claims.any { it.chunkPos == chunk } }

    fun serialize() = hashMapOf<String, String>().apply { nations.forEach { put(it.uuid.toString(), it.serialize()) } }
}

class Nation(
    val uuid: UUID = UUID.randomUUID(),
    var name: String,
    var capital: Town,
    val towns: HashSet<Town> = hashSetOf(),
    val claims: HashSet<ClaimChunk> = hashSetOf(),
    val permissions: HashMap<RealmsPlayer, HashSet<String>> = hashMapOf(),
    var color: String = "#" + Integer.toHexString(randColor().rgb).substring(2),
    val enemyUUIDs: HashSet<UUID> = hashSetOf()
//todo war
    ) : Comparable<Nation> {
    val leader get() = capital.mayor
    val enemies get() = hashSetOf<Nation>().apply { for (uuid in enemyUUIDs) add(NationDatabase[uuid]!!) }
    var registered = System.currentTimeMillis()
    val allTowns get() = (towns.clone() as HashSet<Town>).apply { add(capital) }
    val allClaims get() = hashSetOf<ClaimChunk>().apply { allTowns.forEach { this.addAll(it.claims.keys) }
        addAll(claims)
    }
    val townClaims get() = hashSetOf<ClaimChunk>().apply { allTowns.forEach { this.addAll(it.claims.keys) } }
    val residents get() = hashSetOf<RealmsPlayer>().apply { allTowns.forEach { this.addAll(it.residents) } }

    // Build, Break, Interact
    var nationPerms = arrayOf(true, true, true)
    var outsiderPerms = arrayOf(false, false, false)

    fun serialize(): String {
        val permString = hashSetOf<String>().apply {
            permissions.forEach { (player, perms) -> add("$player/${perms.joinToString("+")}")}
        }

        return """
            $uuid
            $name
            ${capital.uuid}
            ${towns.joinToString { it.uuid.toString() }}
            ${claims.joinToString { it.toString() }}
            ${permString.joinToString()}
            $color
            $registered
            ${nationPerms.joinToString()}
            ${outsiderPerms.joinToString()}
            ${enemyUUIDs.joinToString { it.toString() }}
        """.trimIndent()
    }

    companion object {
        fun deserialize(str: String): Nation {

            val lines = str.split("\n")

            val uuid = UUID.fromString(lines[0])
            val name = lines[1]
            val capital = TownDatabase.getTown(UUID.fromString(lines[2])) ?: TownDatabase.getTown("Washington_DC")!!
            val towns = hashSetOf<Town>().apply {
                try { lines[3].split(", ").forEach { add(TownDatabase.getTown(UUID.fromString(it))!!) } } catch (e: Exception) {}
            }

            val claims = hashSetOf<ClaimChunk>().apply {
                lines[4].split(", ").forEach { try { add(ClaimChunk.deserialize(it)) } catch (e: Exception) {} }
            }

            val permString = lines[5].split(", ")
            val permissions = hashMapOf<RealmsPlayer, HashSet<String>>()
            if (permString[0] != "") permString.forEach {
                val permUUID = UUID.fromString(it.split("/")[0])
                val perms = it.split("/")[1].split("+")
                permissions[RealmsPlayerDatabase[permUUID]!!] = perms.toHashSet()
            }

            val color = lines[6]
            val registered = lines[7].toLong()

            val nationPermString = lines[8].split(", ")
            val nationPerms = arrayOf(false, false, false, false)
            nationPermString.withIndex().forEach { (index, bool) -> nationPerms[index] = bool.toBoolean() }

            val outsiderPermString = lines[9].split(", ")
            val outsiderPerms = arrayOf(false, false, false, false)
            outsiderPermString.withIndex().forEach { (index, bool) -> outsiderPerms[index] = bool.toBoolean() }

            val enemyUIDs = if (lines.size > 10 && '-' in lines[10]) lines[10].split(", ") else "".split(", ")
            val enemyUUIDs = hashSetOf<UUID>().apply { enemyUIDs.forEach { uid -> try { add(UUID.fromString(uid)) } catch (e: Exception) {} } }

            val nation = Nation(uuid, name, capital, towns, claims, permissions, color, enemyUUIDs)
            nation.registered = registered
            nation.nationPerms = nationPerms
            nation.outsiderPerms = outsiderPerms

            return nation
        }

        const val claimProtection = 50

        val nationPrice = arrayOf(
            ItemStack(Material.GOLD_INGOT, 128),
        )

        val claimRequirements = arrayOf(
            ItemStack(Material.GOLD_INGOT, 2)
        )

        val nationUpkeep = arrayOf(
            ItemStack(Material.GOLD_INGOT, 10),
        )

        val claimUpkeep = arrayOf(
            ItemStack(Material.GOLD_INGOT, 0),
        )

        const val highestDistanceFromTown = 5
    }

    fun canPlayerBuild(p: RealmsPlayer): Boolean {
        return if (p.nation == this) nationPerms[0] || p.hasNationPermission(NationPermission.build)
        else outsiderPerms[0] || p.offlinePlayer.isOp
    }

    fun canPlayerBreak(p: RealmsPlayer): Boolean {
        return if (p.nation == this) nationPerms[1] || p.hasNationPermission(NationPermission.destroy)
        else outsiderPerms[1] || p.offlinePlayer.isOp
    }

    fun canPlayerInteract(p: RealmsPlayer): Boolean {
        return if (p.nation == this) nationPerms[2] || p.hasNationPermission(NationPermission.interact)
        else outsiderPerms[2] || p.offlinePlayer.isOp
    }

    fun delete(bcast: Boolean = true) {

        NationDatabase.nations.remove(this)

        if (bcast) Bukkit.broadcastMessage(
            """
                 §8§m                                                                         §r
                §8§lNATION DEATH §7$name has been lost to time.
                 §8§m                                                                         §r
            """.trimIndent()
        )

        NationDeleteEvent(this).callEvent()
    }

    fun broadcast(msg: String) =
        onlinePlayers.filter { it.toRealmsPlayer().nation == this }.forEach { it.msg("§5§l${name.toUpperCase()} §d$msg") }

    fun claim(chunk: Chunk): Pair<ClaimResult, List<ItemStack>> {
        if (NationDatabase[chunk.getBlock(0, 0, 0).location] != null || TownDatabase.getTown(chunk) != null)
            return ClaimResult.FAILED to listOf()

        // Check it if it's contiguous
        var smallestDistance = 5.0
        for (claim in allClaims) if (claim.chunkPos.distance(chunk.asPos()) <= smallestDistance) smallestDistance =
            claim.chunkPos.distance(chunk.asPos())

        if (smallestDistance > 1) return ClaimResult.NOT_CONTIGUOUS to listOf()

        smallestDistance = 5.0
        // Reusing the same variable, checking if it's within 2 chunks of towns.
        for (claim in townClaims) if (claim.chunkPos.distance(chunk.asPos()) <= smallestDistance) smallestDistance =
            claim.chunkPos.distance(chunk.asPos())
        if (smallestDistance > highestDistanceFromTown) return ClaimResult.TOP to listOf()

        //todo fill-in mechanic
        val remainingItems = capital.takeFromStockpileChest(claimRequirements.toList())

        return if (remainingItems.first) {
            val claimChunk = ClaimChunk(chunk, claimProtection)
            claims.add(claimChunk)
            ClaimResult.CLAIMED to listOf()
        } else ClaimResult.POOR_CLAIM to remainingItems.second
    }

    fun unclaim(chunk: Chunk) {
        val claim = claims.firstOrNull { it.equalsPos(chunk) }
        if (claim != null) claims.remove(claim)
    }

    fun recalculateClaims() {
        val claimList = hashSetOf<ClaimChunk>()
        for (claim in claims) for (townClaim in townClaims) {
            var smallestDistance = 5.0
            if (townClaim.chunkPos.distance(claim.chunkPos) < smallestDistance)
                smallestDistance = townClaim.chunkPos.distance(claim.chunkPos)

            if (smallestDistance > highestDistanceFromTown.toDouble()) claimList.add(claim)
        }

        claims.removeIf { claimList.contains(it) }
    }

    override fun equals(other: Any?): Boolean {
        if (other is Nation) return other.uuid == uuid
        return false
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }

    fun setupListener() {
        PermissionListener(RealmsCorePlugin.instance, this)
    }

    fun getResidentsAsStringList(): List<String> {
        val stringList = arrayListOf<String>()

        for (resident in residents) stringList.add(resident.name)

        return stringList
    }

    fun sendStatusMessage(entity: CommandSender) {
        val buildString = when {
            nationPerms[0] && !outsiderPerms[0] -> "§dNation Members §7can §dbuild §7in these claims."
            nationPerms[0] && outsiderPerms[0] -> "§dOutsiders §7and §dNation Members §7can §dbuild §7in these claims."
            !nationPerms[0] && outsiderPerms[0] -> "§dOutsiders §7can §dbuild §7in these claims."
            else -> "§dNo-one §7can §dbuild §7in these claims."
        }

        val destroyString = when {
            nationPerms[1] && !outsiderPerms[1] -> "§dNation Members §7can §ddestroy §7in these claims."
            nationPerms[1] && outsiderPerms[1] -> "§dOutsiders §7and §dNation Members §7can §ddestroy §7in these claims."
            !nationPerms[1] && outsiderPerms[1] -> "§dOutsiders §7can §ddestroy §7in these claims."
            else -> "§dNo-one §7can §ddestroy §7in these claims."
        }

        val interactString = when {
            nationPerms[2] && !outsiderPerms[2] -> "§dNation Members §7can §dinteract §7in these claims."
            nationPerms[2] && outsiderPerms[2] -> "§dOutsiders §7and §dNation Members §7can §dinteract §7in these claims."
            nationPerms[2] && outsiderPerms[2] -> "§dOutsiders §7can §dinteract §7in these claims."
            else -> "§dNo-one §7can §dinteract §7in these claims."
        }

        entity.msg("""
                     §8§m                   §r §5§l${name.toUpperCase()} §8§m                   §r
                    §d${leader.name} §7is the leader.
                    
                    §dResidents (§5${residents.size}§d): §5[ §d${getResidentsAsStringList().joinToString()}§5 ]
                    §dTowns (§5${allTowns.size}§d): §5[ §d ${allTowns.joinToString {it.name}} §5 ]
                    §dClaims: §5${claims.size} 
                     
                    $buildString
                    $destroyString
                    $interactString
                     
                    §dFounded on §5${SimpleDateFormat("MM/dd/yyyy").format(registered)}
                """.trimIndent()
        )
    }

    fun getDynmapLabel(): String = """
        <h2><strong>$name</strong></h2>
        <p>Under ${capital.name}'s authority.</p>
    """.trimIndent()

    override fun compareTo(other: Nation) = this.residents.size.compareTo(other.residents.size)
}

fun randColor(): Color {
    val rand = Random()
    val r = rand.nextFloat()
    val g = rand.nextFloat()
    val b = rand.nextFloat()
    return Color(r, g, b)
}