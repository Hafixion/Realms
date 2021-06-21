@file:Suppress("unused")

package eu.hafixion.realms.towny.towns

import br.com.devsrsouza.kotlinbukkitapi.extensions.bukkit.broadcast
import br.com.devsrsouza.kotlinbukkitapi.extensions.bukkit.server
import br.com.devsrsouza.kotlinbukkitapi.extensions.event.callEvent
import br.com.devsrsouza.kotlinbukkitapi.extensions.server.onlinePlayers
import br.com.devsrsouza.kotlinbukkitapi.extensions.text.msg
import eu.hafixion.realms.RealmsCorePlugin
import eu.hafixion.realms.RealmsPlayer
import eu.hafixion.realms.RealmsPlayerDatabase
import eu.hafixion.realms.toRealmsPlayer
import eu.hafixion.realms.towny.block.ClaimChunk
import eu.hafixion.realms.towny.block.distance
import eu.hafixion.realms.towny.nations.Nation
import eu.hafixion.realms.towny.nations.NationDatabase
import eu.hafixion.realms.towny.towns.TownDatabase.getTown
import eu.hafixion.realms.towny.towns.commands.TownPermissions
import eu.hafixion.realms.towny.towns.events.NewTownEvent
import eu.hafixion.realms.towny.towns.events.TownClaimEvent
import eu.hafixion.realms.towny.towns.events.TownDeleteEvent
import eu.hafixion.realms.towny.towns.listeners.BlockListener
import eu.hafixion.realms.towny.towns.listeners.EntityListener
import eu.hafixion.realms.towny.towns.listeners.PlayerListener
import eu.hafixion.realms.utils.*
import net.md_5.bungee.api.ChatColor
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.Chest
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.time.seconds

object TownDatabase {
    val towns = hashSetOf<Town>()

    fun serialize() = hashMapOf<String, String>().apply { towns.forEach { put(it.uuid.toString(), it.serialize()) } }

    operator fun get(uuid: UUID) = towns.filter { it.uuid == uuid }.getOrNull(0)
    operator fun set(uuid: UUID, town: Town) {
        for (town1 in towns.filter { it.uuid == uuid }) towns.remove(town1)
        towns.add(town)
    }

    fun getTown(loc: Location): Town? {
        for (town in towns) if (town.claims.keys.any { it.equalsPos(loc.chunk) }) return town
        return null
    }

    fun getTown(chunk: Chunk): Town? {
        for (town in towns) if (town.claims.keys.any { it.equalsPos(chunk) }) return town
        return null
    }

    fun getTown(chunk: ChunkPos): Town? {
        for (town in towns) if (town.claims.keys.any { it.chunkPos == chunk }) return town
        return null
    }

    fun getTown(name: String): Town? {
        for (town in towns) if (town.name.equals(name, true)) return town
        return null
    }

    fun getTown(uuid: UUID): Town? {
        for (town in towns) if (town.uuid == uuid) return town
        return null
    }
}

//todo overclaim
class Town(
    var name: String,
    val uuid: UUID = UUID.randomUUID(),
    var mayor: RealmsPlayer,
    val citizens: HashSet<RealmsPlayer> = hashSetOf(),
    var homeBlock: Block,
    val claims: HashMap<ClaimChunk, Int> = hashMapOf(),
    val permissions: HashMap<RealmsPlayer, HashSet<String>> = hashMapOf(),
    //todo add a hologram to show where it is
    var stockpileChest: Location? = null
) : Comparable<Town> {
    val nation get() = NationDatabase[this]
    val isCapital get() = nation?.capital == this
    @Suppress("UNCHECKED_CAST")
    val residents
        get() = (citizens.clone() as HashSet<RealmsPlayer>).apply { add(mayor) }
    var registered = System.currentTimeMillis()
    //todo war implementation lol
    val occupied get() = false

    // Permissions: Build, Break, Interact, Switch
    var townPerms = arrayOf(true, true, true, true)
    var nationPerms = arrayOf(false, false, false, false)
    var outsiderPerms = arrayOf(false, false, false, false)

    var lastPvpToggle = System.currentTimeMillis() - 30.seconds.toLongMilliseconds()

    // Toggle-able settings
    var fire = false
    var mobs = false
    var explosions = false
    var pvp = false
    var public = false
    var newbie = true

    val claimLimit get() = 30 + 10 * residents.size

    companion object {
        val reinforceLevels = arrayOf(0, 500, 2000, 10000, 20000)

        val requirementsForATown = arrayOf(
            ItemStack(Material.OAK_PLANKS, 64),
            ItemStack(Material.COBBLESTONE, 64)
        )

        val claimRequirements = arrayOf(
            arrayOf(),
            arrayOf(
                ItemStack(Material.GOLD_INGOT, 1)
            ),
            arrayOf(
                ItemStack(Material.GOLD_INGOT, 4)
            ),
            arrayOf(
                ItemStack(Material.GOLD_INGOT, 6)
            ),
            arrayOf(
                ItemStack(Material.GOLD_INGOT, 8)
            )
        )

        val townUpkeep = arrayOf(
            ItemStack(Material.GOLD_INGOT, 2)
        )
        val claimUpkeep = arrayOf(
            arrayOf(),
            arrayOf(
                ItemStack(Material.GOLD_INGOT, 0),
            ),
            arrayOf(
                ItemStack(Material.GOLD_INGOT, 1)
            ),
            arrayOf(
                ItemStack(Material.GOLD_INGOT, 2)
            ),
            arrayOf(
                ItemStack(Material.GOLD_INGOT, 4)
            )
        )

        suspend fun newTown(p: Player, name: String) {
            if (p.toRealmsPlayer().town != null) {
                p.msg(errorMessage("You're already in ${p.toRealmsPlayer().town!!.name}."))
                return
            }

            if (name.length > 15) {
                p.msg(errorMessage("Your town's name has too many characters, maximum is 15!"))
                return
            }

            if (name.contains('`')) {
                p.msg(errorMessage("You aren't allowed to have backticks in town names."))
                return
            }

            val town = Town(name, mayor = p.toRealmsPlayer(), homeBlock = p.location.block)

            if (!p.confirmation("Are you sure you want to make a town here?")) return

            var hasResources = true
            for (item in requirementsForATown) if (!p.inventory.contains(item.type, item.amount)) hasResources = false
            if (!hasResources) {
                p.msg(errorMessage("You don't have enough resources to make a town!"))

                // another idea, command that sets resource goal on hotbar

                val remainingItems = requirementsForATown.toList().toHashMap()
                    .apply { removeMap(p.inventory.contents.toList().toHashMap()) }

                p.msg("§6You still need§e:")
                for (itemString in remainingItems.materialToItemList().toStringList()) p.msg("§e- $itemString")

                return
            } else for (item in requirementsForATown) p.inventory.removeItem(item)

            if (getTown(p.location) != null) {
                p.msg(errorMessage("A town already claims the land you're in!"))
                return
            }

            if (getTown(name) != null) {
                p.msg(errorMessage("There's already a town named $name!"))
                return
            }

            val event = NewTownEvent(town, p)
            event.callEvent()
            if (event.isCancelled) return

            broadcast(
                """
                 ${blackLine()}
                §6§lNEW TOWN §e${p.name} has just founded the town of §6$name§e!
                 ${blackLine()}
            """.trimIndent()
            )

            TownDatabase[town.uuid] = town
        }

        fun deserialize(str: String): Town {
            val lines = str.split("\n")

            val uuid = UUID.fromString(lines[0])
            val name = lines[1]
            val mayor = RealmsPlayerDatabase[UUID.fromString(lines[2])]!!

            val citizens = hashSetOf<RealmsPlayer>().apply {
                val uuids = lines[3].split(", ")
                if (uuids[0] != "") uuids.forEach { add(RealmsPlayerDatabase[UUID.fromString(it)]!!) }
            }

            val homeBlockArray = lines[4].split(", ")
            val homeBlock = BlockPos(
                homeBlockArray[0].toDouble().toInt(),
                homeBlockArray[1].toDouble().toInt(),
                homeBlockArray[2].toDouble().toInt()
            )

            val claimString = lines[5].split(", ")
            val claims = hashMapOf<ClaimChunk, Int>()
            if (claimString[0] != "") claimString.forEach {
                val serializedEntry = it.split("-")
                val serializedClaim = serializedEntry[0]
                val int = serializedEntry[1].toInt()

                claims[ClaimChunk.deserialize(serializedClaim)] = int
            }

            val permString = lines[6].split(", ")
            val permissions = hashMapOf<RealmsPlayer, HashSet<String>>()
            if (permString[0] != "") permString.forEach {
                val permUUID = UUID.fromString(it.split("/")[0])
                val perms = it.split("/")[1].split("+")
                permissions[RealmsPlayerDatabase[permUUID]!!] = perms.toHashSet()
            }

            val registered = lines[7].toLong()

            val townPermString = lines[8].split(", ")
            val townPerms = arrayOf(false, false, false, false)
            townPermString.withIndex().forEach { (index, bool) -> townPerms[index] = bool.toBoolean() }

            val nationPermString = lines[9].split(", ")
            val nationPerms = arrayOf(false, false, false, false)
            nationPermString.withIndex().forEach { (index, bool) -> nationPerms[index] = bool.toBoolean() }

            val outsiderPermString = lines[10].split(", ")
            val outsiderPerms = arrayOf(false, false, false, false)
            outsiderPermString.withIndex().forEach { (index, bool) -> outsiderPerms[index] = bool.toBoolean() }

            val fire = lines[11].toBoolean()
            val mobs = lines[12].toBoolean()
            val public = lines[13].toBoolean()
            val explosions = lines[14].toBoolean()
            val pvp = lines[15].toBoolean()
            val newbie = lines[16].toBoolean()

            val stockpileArray = lines[17].split(", ")
            val stockpileChest = try { BlockPos(
                stockpileArray[0].toDouble().toInt(),
                stockpileArray[1].toDouble().toInt(),
                stockpileArray[2].toDouble().toInt()
            ) } catch (e: NumberFormatException) { null }

            val town = Town(
                name,
                uuid,
                mayor,
                citizens,
                homeBlock.asBukkitBlock(server.worlds[0]),
                claims,
                permissions,
                stockpileChest?.asBukkitLocation(server.worlds[0])
            )
            town.registered = registered
            town.townPerms = townPerms
            town.nationPerms = nationPerms
            town.outsiderPerms = outsiderPerms
            town.fire = fire
            town.mobs = mobs
            town.public = public
            town.explosions = explosions
            town.pvp = pvp
            town.newbie = newbie

            return town
        }
    }

    init {
        setupListeners(RealmsCorePlugin.instance)
        var hasHomeblock = false
        for (chunk in claims.keys) if (chunk.chunkPos == homeBlock.chunk.asPos()) hasHomeblock = true
        if (!hasHomeblock) claims[ClaimChunk(homeBlock.chunk, reinforceLevels[4])] = 4
    }

    fun canPlayerBuild(p: RealmsPlayer, block: Block): Boolean {
        return if (p.player?.isOp == true) true else if (p.town == this) townPerms[0] || p.hasTownPermission("${block.chunk.x}-${block.chunk.z}.build")
        else if (p.nation != null && p.nation == nation) nationPerms[0] else outsiderPerms[0]
    }

    fun canPlayerDestroy(p: RealmsPlayer, block: Block): Boolean {
        return if (p.player?.isOp == true) true else if (p.town == this) townPerms[1] || p.hasTownPermission("${block.chunk.x}-${block.chunk.z}.break")
        else if (p.nation != null && p.nation == nation) nationPerms[1] else outsiderPerms[1]
    }

    fun canPlayerSwitch(p: RealmsPlayer, block: Block): Boolean {
        return if (p.player?.isOp == true) true else if (p.town == this) townPerms[2] || p.hasTownPermission("${block.chunk.x}-${block.chunk.z}.switch")
        else if (p.nation != null && p.nation == nation) nationPerms[2] else outsiderPerms[2]
    }

    fun canPlayerInteract(p: RealmsPlayer, loc: Location): Boolean {
        return if (p.player?.isOp == true) true else if (p.town == this) townPerms[3] || p.hasTownPermission("${loc.chunk.x}-${loc.chunk.z}.interact")
        else if (p.nation != null && p.nation == nation) nationPerms[3] else outsiderPerms[3]
    }

    //todo self explanatory
    fun atWar() = true

    // Used for Newbie town protection.
    fun isNew() =
        System.currentTimeMillis() - registered < 259200000L && RealmsCorePlugin.town_newbie_enabled && newbie

    fun takeFromStockpileChest(list: List<ItemStack>): Pair<Boolean, ArrayList<ItemStack>> {
        if (stockpileChest == null) return false to arrayListOf()

        val chest = stockpileChest!!.block.state as? Chest ?: return false to arrayListOf()

        var remainingItems = list.toHashMap()
            .apply { removeMap(chest.inventory.contents.toList().toHashMap()) }.materialToItemList()

        return if (remainingItems.size != 0) {

            val contents = remainingItems.toHashMap()
            val contentIterator = contents.iterator()

            while (contentIterator.hasNext()) {
                val item = contentIterator.next()
                if (item.key == Material.GOLD_BLOCK) {
                    contents.remove(item.key)
                    contents.addMap(hashMapOf(Material.GOLD_INGOT to item.value * 9))
                }
            }

            remainingItems = contents.apply { removeMap(chest.inventory.contents.toList().toHashMap()) }.materialToItemList()

            return if (remainingItems.size != 0) false to remainingItems else {
                val listContents = list.toHashMap()
                val listIterator = listContents.iterator()

                while (listIterator.hasNext()) {
                    val item = listIterator.next()
                    if (item.key == Material.GOLD_BLOCK) {
                        listContents.remove(item.key)
                        listContents.addMap(hashMapOf(Material.GOLD_INGOT to item.value * 9))
                    }
                }

                for (item in listContents.materialToItemList()) chest.inventory.removeItem(item)
                true to arrayListOf()
            }
        } else {
            for (item in list) chest.inventory.removeItem(item)
            true to arrayListOf()
        }
    }

    fun hasItemsInStockpileChest(list: List<ItemStack>): Pair<Boolean, ArrayList<ItemStack>> {
        if (stockpileChest == null) return false to arrayListOf()

        val chest = stockpileChest!!.block.state as? Chest ?: return false to arrayListOf()

        val contents = chest.inventory.contents.toList().toHashMap()
        val contentIterator = contents.iterator()

        while (contentIterator.hasNext()) {
            val item = contentIterator.next()
            if (item.key == Material.GOLD_BLOCK) {
                contents.remove(item.key)
                contents.addMap(hashMapOf(Material.GOLD_INGOT to item.value * 9))
            }
        }
        val remainingItems = list.toHashMap()
            .apply { removeMap(chest.inventory.contents.toList().toHashMap()) }.materialToItemList()

        return if (remainingItems.size != 0) false to remainingItems else true to arrayListOf()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun calculateUpkeep(): List<ItemStack> {
        val upkeepItems = arrayListOf<ItemStack>()

        upkeepItems.addAll(townUpkeep)

        //todo calculate overclaim
        var hasCountedOriginalClaim = false
        for (value in claims.values) if (!hasCountedOriginalClaim && value != 4) upkeepItems.addAll(claimUpkeep[value])
        else hasCountedOriginalClaim = true

        // Nation Calculation
        if (nation != null && isCapital) {
            upkeepItems.addAll(Nation.nationUpkeep)

            repeat(nation!!.claims.size) { upkeepItems.addAll(Nation.claimUpkeep) }
        }

        val hashMap = upkeepItems.toHashMap()
        val overclaim = claims.keys.size - claimLimit
        if (overclaim > 0)
            hashMap[Material.GOLD_INGOT] = (hashMap[Material.GOLD_INGOT]!!.toDouble() + (1.4.pow(overclaim.toDouble()))).roundToInt()

        return upkeepItems.toHashMap().materialToItemList()
    }

    /**
     * @return Whether or not the town will survive.
     */
    fun takeUpkeep(): Pair<Boolean, ArrayList<ItemStack>> {
        return takeFromStockpileChest(calculateUpkeep())
    }

    fun getResidentsAsStringList(): HashSet<String> {
        val stringList = hashSetOf<String>()

        for (resident in residents) stringList.add(resident.name)

        return stringList
    }

    fun claim(chunk: Chunk): Pair<ClaimResult, ArrayList<ItemStack>> {

        // Check it if it's contiguous
        var smallestDistance = 5.0
        for (claim in claims.keys) if (claim.chunkPos.distance(chunk.asPos()) <= smallestDistance) smallestDistance =
            claim.chunkPos.distance(chunk.asPos())

        if (smallestDistance > 1) return ClaimResult.NOT_CONTIGUOUS to arrayListOf()

        val townClaimed = getTown(chunk) == this
        if (getTown(chunk) != null && !townClaimed) return ClaimResult.FAILED to arrayListOf()

        //todo There's a claim roof proportional to resident count, claim price increases as it passes the roof

        if (townClaimed) {
            // Over the top complicated line to just get the damn fu 
            val claimLevel = claims.filterKeys { it.equalsPos(chunk) }.values.toIntArray()[0]
            if (claimLevel == 4) return ClaimResult.TOP to arrayListOf()

            val taken = takeFromStockpileChest(claimRequirements[claimLevel + 1].toList())
            if (!taken.first)
                return ClaimResult.POOR_UPGRADE to taken.second

            claims.entries.removeIf { it.key.equalsPos(chunk) }
            claims[ClaimChunk(chunk, reinforceLevels[claimLevel + 1])] = claimLevel + 1
            return ClaimResult.UPGRADED to taken.second
        } else {
            val taken = takeFromStockpileChest(claimRequirements[1].toList())
            if (!taken.first)
                return ClaimResult.POOR_CLAIM to taken.second

            claims.entries.removeIf { it.key.equalsPos(chunk) }
            claims[ClaimChunk(chunk, reinforceLevels[1])] = 1

            TownClaimEvent(this, chunk.asPos()).callEvent()
            return ClaimResult.CLAIMED to taken.second
        }
    }

    fun unclaim(chunk: Chunk): ClaimResult {
        val claim = claims.filter { it.key.equalsPos(chunk) }.toList().first()

        if (claim.second != 1) {
            claims[ClaimChunk(chunk, reinforceLevels[claim.second - 1])] = claim.second - 1
            return ClaimResult.UPGRADED
        } else claims.remove(claim.first)

        recalculateClaims()

        return ClaimResult.CLAIMED
    }
    
    fun recalculateClaims() {
        for (claim in claims.keys) {
            var smallestDistance = 5.0
            for (otherClaim in claims.keys) if (claim.chunkPos.distance(otherClaim.chunkPos) <= smallestDistance) smallestDistance =
                claim.chunkPos.distance(otherClaim.chunkPos)

            if (smallestDistance > 1) claims.remove(claim)
        }
    }

    //todo Idea: Play Sad/Happy music on broadcasts.
    fun broadcast(msg: String) =
        onlinePlayers.filter { it.toRealmsPlayer().town == this }.forEach { it.msg("§6§l${name.toUpperCase()} §e$msg") }

    fun delete(bcast: Boolean = true) {
        TownDatabase.towns.remove(this)

        if (bcast) Bukkit.broadcastMessage(
            """
                 §8§m                                                                         §r
                §8§lTOWN DEATH §7$name has fallen into the pages of history.
                 §8§m                                                                         §r
            """.trimIndent()
        )

        TownDeleteEvent(this).callEvent()
    }

    fun setupListeners(plugin: RealmsCorePlugin) {
        BlockListener(plugin, this)
        EntityListener(plugin, this)
        PlayerListener(plugin, this)
    }

    fun sendStatusMessage(entity: CommandSender) {
        val town = this
        //todo upkeep, perms, newbie, nation, and toggle

        val nationString = if (nation != null) """
            §7This town is part of ${ChatColor.of(nation!!.color)}${nation!!.name}
             
            """.trimIndent() else ""
        val newbieString = if (town.isNew()) """
            §eThe town is currently under newbie protection.
             
            """.trimIndent() else ""

        val buildString = when {
            townPerms[0] && nationPerms[0] && outsiderPerms[0] -> "§eResidents, Outsiders §7and §eNation Members §7can §ebuild §7here."
            townPerms[0] && nationPerms[0] && !outsiderPerms[0] -> "§eResidents §7and §eNation Members §7can §ebuild §7here."
            townPerms[0] && nationPerms[0] && outsiderPerms[0] -> "§eResidents §7and §eOutsiders §7can §ebuild §7here."
            townPerms[0] && !nationPerms[0] && !outsiderPerms[0] -> "§eResidents §7can §ebuild §7here."
            !townPerms[0] && nationPerms[0] && !outsiderPerms[0] -> "§eNation Members §7can §ebuild §7here."
            !townPerms[0] && nationPerms[0] && outsiderPerms[0] -> "§eOutsiders §7and §eNation Members §7can §ebuild §7here."
            !townPerms[0] && !nationPerms[0] && outsiderPerms[0] -> "§eOutsiders §7can §ebuild §7here."
            else -> "§eNo-one §7can §ebuild §7here."
        }

        val destroyString = when {
            townPerms[1] && nationPerms[1] && outsiderPerms[1] -> "§eResidents, Outsiders §7and §eNation Members §7can §edestroy §7here."
            townPerms[1] && nationPerms[1] && !outsiderPerms[1] -> "§eResidents §7and §eNation Members §7can §edestroy §7here."
            townPerms[1] && !nationPerms[1] && !outsiderPerms[1] -> "§eResidents §7can §edestroy §7here."
            townPerms[1] && nationPerms[1] && outsiderPerms[1] -> "§eResidents §7and §eOutsiders §7can §edestroy §7here."
            !townPerms[1] && nationPerms[1] && !outsiderPerms[1] -> "§eNation Members §7can §edestroy §7here."
            !townPerms[1] && nationPerms[1] && outsiderPerms[1] -> "§eOutsiders §7and §eNation Members §7can §edestroy §7here."
            !townPerms[1] && !nationPerms[1] && outsiderPerms[1] -> "§eOutsiders §7can §edestroy §7here."
            else -> "§eNo-one §7can §edestroy §7here."
        }

        val interactString = when {
            townPerms[2] && nationPerms[2] && outsiderPerms[2] -> "§eResidents, Outsiders §7and §eNation Members §7can §einteract §7here."
            townPerms[2] && nationPerms[2] && !outsiderPerms[2] -> "§eResidents §7and §eNation Members §7can §einteract §7here."
            townPerms[2] && !nationPerms[2] && !outsiderPerms[2] -> "§eResidents §7can §einteract §7here."
            townPerms[2] && nationPerms[2] && outsiderPerms[2] -> "§eResidents §7and §eOutsiders §7can §einteract §7here."
            !townPerms[2] && nationPerms[2] && !outsiderPerms[2] -> "§eNation Members §7can §einteract §7here."
            !townPerms[2] && nationPerms[2] && outsiderPerms[2] -> "§eOutsiders §7and §eNation Members §7can §einteract §7here."
            !townPerms[2] && !nationPerms[2] && outsiderPerms[2] -> "§eOutsiders §7can §einteract §7here."
            else -> "§eNo-one §7can §einteract §7here."
        }

        val switchString = when {
            townPerms[3] && nationPerms[3] && outsiderPerms[3] -> "§eResidents, Outsiders §7and §eNation Members §7can §eswitch §7here."
            townPerms[3] && nationPerms[3] && !outsiderPerms[3] -> "§eResidents §7and §eNation Members §7can §eswitch §7here."
            townPerms[3] && !nationPerms[3] && !outsiderPerms[3] -> "§eResidents §7can §eswitch §7here."
            townPerms[3] && nationPerms[3] && outsiderPerms[3] -> "§eResidents §7and §eOutsiders §7can §eswitch §7here."
            !townPerms[3] && nationPerms[3] && !outsiderPerms[3] -> "§eNation Members §7can §eswitch §7here."
            !townPerms[3] && nationPerms[3] && outsiderPerms[3] -> "§eOutsiders §7and §eNation Members §7can §eswitch §7here."
            !townPerms[3] && !nationPerms[3] && outsiderPerms[3] -> "§eOutsiders §7can §eswitch §7here."
            else -> "§eNo-one §7can §eswitch §7here."
        }

        val pvpString = if (pvp) "§ePVP is enabled here" else "§ePVP is disabled here"
        val publicString = if (public) "§eThis town is public" else "§eThis town is private"

        entity.msg("""
                     §8§m                   §r §6§l${town.name.toUpperCase()} §8§m                   §r
                    §e${town.mayor.name} §7is the mayor.
                     
                    $newbieString
                    $nationString
                     
                    §6Residents (${town.residents.size})§e: §6[ §e${town.getResidentsAsStringList().joinToString()}§6 ]
                    §6Claims: §e ${town.claims.keys.size} / $claimLimit §7(${town.claims.filterValues { it == 1 }.size}, ${town.claims.filterValues { it == 2 }.size}, ${town.claims.filterValues { it == 3 }.size}, ${town.claims.filterValues { it == 4 }.size})
                     
                    $buildString
                    $destroyString
                    $interactString
                    $switchString
                    
                    $pvpString
                    $publicString
                     
                    §eFounded on §6${SimpleDateFormat("MM/dd/yyyy").format(town.registered)}
                """.trimIndent()
        )
    }

    fun serialize(): String {

        val claimString = hashSetOf<String>().apply {
            claims.forEach { (chunk, int) -> add("$chunk-$int") }
        }
        val permString = hashSetOf<String>().apply {
            permissions.forEach { (player, perms) -> add("$player/${perms.joinToString("+")}")}
        }
        val stockpileString = if (stockpileChest != null) stockpileChest!!.asPos().axis().joinToString() else "null"

        return """
            $uuid
            $name
            $mayor
            ${citizens.joinToString()}
            ${homeBlock.asPos().axis().joinToString()}
            ${claimString.joinToString()}
            ${permString.joinToString()}
            $registered
            ${townPerms.joinToString()}
            ${nationPerms.joinToString()}
            ${outsiderPerms.joinToString()}
            $fire
            $mobs
            $public
            $explosions
            $pvp
            $newbie
            $stockpileString
        """.trimIndent()
    }

    fun getDynmapMenu(): String {
        val nationString = if (nation != null) {
            if (isCapital) "<h3>Capital of <strong>${nation!!.name}</strong></h3>"
            else "<h3>Member of <strong>${nation!!.name}</strong></h3>"
        } else ""

        val pvpString = if (pvp) "PVP is enabled here" else "PVP is disabled here"
        val publicString = if (public) "This town is public" else "This town is private"

        return """
            <h2 style="text-align: center;"><strong>$name</strong></h2>
            <p style="text-align: center;">$nationString</p>
            <hr />
            <p style="text-align: center;"><img src="https://minotar.net/avatar/${mayor.name}/64" alt="" width="64" height="64" /></p>
            <p style="text-align: center;"><strong>Mayor </strong>${mayor.name}</p>
            <hr />
            <p style="text-align: center;">Residents (${residents.size}) <strong>${getResidentsAsStringList().joinToString()}</strong></p>
            <p style="text-align: center;">$pvpString</p>
            <p style="text-align: center;">$publicString</p>
            <hr />
            <p style="text-align: center;">Founded on <strong>${SimpleDateFormat("MM/dd/yyyy").format(registered)}</strong></p>
        """.trimIndent()
    }


    // Nation-Specific Util values
    var lastNationInvite = System.currentTimeMillis() - 600000
    override fun compareTo(other: Town): Int {
        return citizens.size.compareTo(other.citizens.size)
    }
}

fun String.isValidTownPerm() = TownPermissions.all.contains(this)