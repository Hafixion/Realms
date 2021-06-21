package eu.hafixion.realms.towny.block

import eu.hafixion.realms.utils.ChunkPos
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.util.Vector
import java.awt.geom.Point2D

/**
 * I can't even begin to thank this guy for his work
 * https://github.com/trevorzucker/Reinforcement/blob/master/Reinforcement/src/main/java/codes/zucker/Reinforcement/Utils.java
 * This is all pretty much a one for one copy converted to kotlin with some modifications and code optimizations.
 *
 * @author trevorzucker, Hafixion (For minor edits)
 * @see ClaimChunk used for tasks involving breaking blocks.
 * @since 5/5/2021
 */

// Offsets of Each individual block face.
val faceOffsets = arrayOf(
    Vector(0.8, 0.0, 0.0),
    Vector(-0.8, 0.0, 0.0),
    Vector(0.0, 0.8, 0.0),
    Vector(0.0, -0.8, 0.0),
    Vector(0.0, 0.0, 0.8),
    Vector(0.0, 0.0, -0.8)
)
// Transparent Neighbors to show hologram through.
val neighborBlacklist = arrayOf(
    Material.WATER,
    Material.LAVA,
    Material.CAVE_AIR,
    Material.AIR,
    Material.FIRE,
    Material.GLASS
)

private fun Block.hasNeighbor(offset: Vector): Boolean {
    val l = this.location.clone()
    l.add(offset.x, offset.y, offset.z)
    val neighbor = this.world.getBlockAt(l.blockX, l.blockY, l.blockZ)
    for (m in neighborBlacklist) if (m == neighbor.type) return false
    return true
}

fun Block.getClosestFace(eye: Location): Vector? {
    val bCenter = this.location.clone()

    bCenter.add(0.5, 0.5, 0.5)
    var closestFace: Vector? = null

    for (originalOffset in faceOffsets) {
        val offset = originalOffset.clone()
        offset.multiply(1.25f)

        if (hasNeighbor(offset)) continue
        val face = bCenter.clone()
        face.add(originalOffset)
        val dist = eye.distance(bCenter.add(face))
        if (closestFace == null || dist < eye.toVector().distance(closestFace)) closestFace = originalOffset
    }
    return closestFace
}

fun ChunkPos.distance(chunkPos: ChunkPos) =
    Point2D.distance(x.toDouble(), z.toDouble(), chunkPos.x.toDouble(), chunkPos.z.toDouble())