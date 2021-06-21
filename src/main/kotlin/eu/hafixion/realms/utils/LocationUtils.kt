package eu.hafixion.realms.utils

import eu.hafixion.realms.towny.block.ClaimChunk
import eu.hafixion.realms.towny.nations.NationDatabase
import eu.hafixion.realms.towny.towns.TownDatabase
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import java.io.Serializable
import java.util.*

val allClaims get() = hashSetOf<ClaimChunk>().apply {
    NationDatabase.nations.forEach { nation -> addAll(nation.claims) }
    TownDatabase.towns.forEach { town -> addAll(town.claims.keys) }
}

fun Location.getNearbyBlocks(radius: Int = 4): List<Location> {
    val locations = ArrayList<Location>()

    for (x in blockX - radius..blockX + radius)
        for (y in blockY - radius..blockY + radius)
            for (z in blockZ - radius..blockZ + radius) {
                val l = Location(world, x.toDouble(), y.toDouble(), z.toDouble())
                if (l.distance(this) <= radius) locations.add(l)
            }

    return locations
}

fun Block.isReinforced(): Boolean = allClaims.any { it.equalsPos(chunk) }

fun Block.getReinforcementChunk(): ClaimChunk? = allClaims.firstOrNull { it.equalsPos(chunk) }

data class BlockPos(var x: Int, var y: Int, var z: Int): Serializable {

    fun asBukkitLocation(world: World) = world.getBlockAt(x , y, z).location
    fun axis() = doubleArrayOf(x.toDouble(), y.toDouble(), z.toDouble())
    fun asBukkitBlock(world: World) = world.getBlockAt(x , y, z)
}

data class ChunkPos(var x: Int, var z: Int): Serializable

fun Block.asPos() = BlockPos(x, y, z)
fun Location.asPos() = BlockPos(blockX, blockY, blockZ)
fun Chunk.asPos() = ChunkPos(x, z)

enum class ClaimResult {
    POOR_UPGRADE,
    POOR_CLAIM,
    UPGRADED,
    TOP,
    CLAIMED,
    FAILED,
    NOT_CONTIGUOUS
}