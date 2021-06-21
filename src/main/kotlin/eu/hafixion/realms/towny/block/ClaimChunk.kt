package eu.hafixion.realms.towny.block

import br.com.devsrsouza.kotlinbukkitapi.extensions.server.scheduler
import com.okkero.skedule.schedule
import eu.hafixion.realms.RealmsCorePlugin
import eu.hafixion.realms.RealmsCorePlugin.Companion.coreProtect
import eu.hafixion.realms.towny.block.ClaimChunk.Companion.naturalBlockRate
import eu.hafixion.realms.towny.block.ClaimChunk.Companion.stoneBlockRate
import eu.hafixion.realms.towny.block.ClaimChunk.Companion.stoneBlocks
import eu.hafixion.realms.utils.BlockPos
import eu.hafixion.realms.utils.ChunkPos
import eu.hafixion.realms.utils.asPos
import org.bukkit.*
import org.bukkit.block.Block
import org.bukkit.entity.ArmorStand
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.util.Vector
import java.io.*
import java.util.*

/**
 * The main class used for assigning reinforced blocks.
 *
 * @since 5/5/2021
 * @author Hafixion
 * @param chunkPos The position of the chunk
 * @param defaultReinforcement The default reinforcement of a chunk
 * @param map The map of block positions and their reinforcements.
 *
 * @property stoneBlocks All blocks that receive a bonus reinforcement due to them being stone
 * @property stoneBlockRate The multiplier added to stone blocks.
 * @property naturalBlockRate The multiplier added to natural blocks.
 */

//todo Add option to see nearby damaged blocks, and repair them
class ClaimChunk(val chunkPos: ChunkPos, val defaultReinforcement: Int,
                 val map: HashMap<BlockPos, Int> = hashMapOf()): Serializable {
    val originalMap = hashMapOf<BlockPos, Int>()

    companion object {

        val stoneBlocks = Material.values().filter {
            it.isBlock && ("STONE" in it.name && "REDSTONE" !in it.name
                    || "GRANITE" in it.name || "ANDESITE" in it.name || "DIORITE" in it.name || "DEEPSLATE" in it.name)
        }

        const val naturalBlockRate = 3
        const val stoneBlockRate = 20

        fun deserialize(string: String): ClaimChunk {
            val byteArray = Base64.getDecoder().decode(string)
            val objectStream = ObjectInputStream(ByteArrayInputStream(byteArray))

            val obj = objectStream.readObject()
            return obj as? ClaimChunk ?: error("ClaimChunk deserialization failed.")
        }
    }

    val activeIndicators = hashMapOf<BlockPos, ArmorStand>()

    constructor(chunk: Chunk, reinforcement: Int) : this(ChunkPos(chunk.asPos().x, chunk.asPos().z), reinforcement)

    private fun Block.addToMap() {
        if (map[asPos()] == null) {
            val blockPos = this.asPos()
            val isStone = stoneBlocks.contains(type)
            var isNatural = true
            coreProtect.blockLookup(this, Int.MAX_VALUE).forEach {
                val parseResult = coreProtect.parseResult(it)
                if (parseResult.actionId <= 1) {
                    isNatural = false
                    return@forEach
                }
            }

            map[blockPos] = when {
                isStone && isNatural -> stoneBlockRate * naturalBlockRate * defaultReinforcement
                isStone -> stoneBlockRate * defaultReinforcement
                isNatural -> naturalBlockRate * defaultReinforcement
                else -> defaultReinforcement
            }

            if ("OBSIDIAN" in type.toString() || type == Material.RESPAWN_ANCHOR
                || type == Material.ENDER_CHEST || type == Material.ENCHANTING_TABLE) map[blockPos] = 1

            originalMap[blockPos] = map[blockPos]!!
        }
    }

    fun onBlockBreakEvent(event: BlockBreakEvent) {
        // Just some utils to prevent unnecessary computation
        when (defaultReinforcement) {
            -1 -> {
                event.isCancelled = true
                return
            }
            0 -> return
        }

        val blockRecedeTicks = 14
        val blockPos = event.block.asPos()

        if (!map.contains(blockPos)) event.block.addToMap()
        map[blockPos] = map[blockPos]!! - 1

        val face = event.block.getClosestFace(event.player.eyeLocation)
        if (map[blockPos]!! >= 0 && !event.player.hasPermission("realms.reinforcement.bypass")) {

            // Clear previous indicators
            clearIndicators(blockPos)

            if (face != null) createIndicator(event.block, face, blockRecedeTicks)
            event.isCancelled = true
        } else {
            map.remove(blockPos)
            originalMap.remove(blockPos)
            clearIndicators(blockPos)
        }
    }

    fun onExplodeEvent(affectedBlocks: List<Block>) : HashMap<Block, Boolean> {
        val affectedBlocksInChunk = affectedBlocks.filter { it.location.chunk.asPos().x == chunkPos.x &&
                it.location.chunk.asPos().z == chunkPos.z }
        
        val hashMap = hashMapOf<Block, Boolean>()
        for (block in affectedBlocksInChunk) {
            val blockPos = BlockPos(block.x, block.y, block.z)

            if (!map.contains(blockPos)) block.addToMap()
            // maybe add proximity later?
            map[blockPos] = map[blockPos]!! - 50
            
            hashMap[block] = map[blockPos]!! > 0
        }

        return hashMap
    }

    fun clearIndicators(pos: BlockPos) {
        if (activeIndicators[pos] != null)
            activeIndicators[pos]!!.remove()
        activeIndicators.remove(pos)
    }

    // This function describes if they're equal to each other in location, not in reinforcements.
    fun equalsPos(other: Chunk): Boolean {
        return other.asPos() == chunkPos
    }

    fun createIndicator(block: Block, face: Vector, blockRecedeTicks: Int = 14) {
        block.addToMap()
        val blockPos = block.asPos()

        val armorStandLocation = blockPos.asBukkitLocation(Bukkit.getServer().worlds[0]).clone().add(0.5, 0.1, 0.5)
        armorStandLocation.add(face)

        val stand = Bukkit.getServer().worlds[0].spawn(armorStandLocation, ArmorStand::class.java) { t ->
            t.setGravity(false)
            t.canPickupItems = false
            t.isVisible = false
            t.isSilent = true
            t.isInvulnerable = true
            t.isMarker = true
            t.isMarker = true
            t.isSilent = true
            t.canPickupItems = false
            t.setMetadata("isMarker", FixedMetadataValue(RealmsCorePlugin.instance, true))
        }
        if (activeIndicators[blockPos] != null) activeIndicators[blockPos]!!.remove()
        activeIndicators.remove(blockPos)
        activeIndicators[blockPos] = stand

        val pct = map[blockPos]!!.toDouble() / originalMap[blockPos]!!.toDouble() * 100.0
        val c = when {
            pct < 65 && pct >= 35 -> ChatColor.YELLOW
            pct < 35 -> ChatColor.RED
            else -> ChatColor.GREEN
        }

        stand.customName = "$c${map[blockPos]!!}"
        stand.isCustomNameVisible = true

        val finalClosest = face.clone()
        finalClosest.multiply(0.5f)

        scheduler.schedule(RealmsCorePlugin.instance) {
            waitFor(blockRecedeTicks.toLong())
            activeIndicators.remove(blockPos)
            stand.remove()
        }
    }

    override fun toString(): String {
        val stream = ByteArrayOutputStream()
        val outputStream = ObjectOutputStream(stream)
        outputStream.writeObject(this)

        return Base64.getEncoder().encodeToString(stream.toByteArray())
    }
}