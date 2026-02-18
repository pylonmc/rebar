package io.github.pylonmc.rebar.util.position

import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.util.Vector
import org.joml.Vector3i
import java.util.UUID

/**
 * Represents the position of a block (x, y, z, and world).
 *
 * Why not just use [Block]? Because [Block] contains lots of extra information such
 * as the type of the block, and so cannot practically be serialized. Holding
 * references to blocks for extended periods may also prevent chunks from unloading,
 * and increase memory usage.
 */
class BlockPosition(val worldId: UUID?, val x: Int, val y: Int, val z: Int) {
    val world: World?
        get() = worldId?.let { Bukkit.getWorld(it) }

    val chunk: ChunkPosition
        get() = ChunkPosition(worldId, x shr 4, z shr 4)

    @get:JvmSynthetic
    internal val asLong: Long
        get() = asLong(x, y, z)

    internal constructor(asLong: Long) : this(null, asLong)

    internal constructor(world: World?, asLong: Long) : this(world?.uid,
        (asLong shr 38).toInt(),
        ((asLong shl 52) shr 52).toInt(),
        ((asLong shl 26) shr 38).toInt()
    )

    constructor(x: Int, y: Int, z: Int) : this(null as UUID?, x, y, z)

    constructor(world: World?, x: Int, y: Int, z: Int) : this(world?.uid, x, y, z)

    constructor(location: Location) : this(location.world?.uid, location.blockX, location.blockY, location.blockZ)

    constructor(world: World?, position: Vector) : this(world?.uid, position.blockX, position.blockY, position.blockZ)

    constructor(block: Block) : this(block.world.uid, block.x, block.y, block.z)

    override fun hashCode(): Int {
        val prime = 31
        return prime * (world?.hashCode() ?: 0) + prime * asLong.hashCode()
    }

    override fun toString(): String {
        return if (world != null) {
            "$x, $y, $z in ${world!!.name}"
        } else {
            "$x, $y, $z"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (other is BlockPosition) {
            return other.world?.uid == world?.uid && other.asLong == asLong
        }
        return false
    }

    fun addScalar(x: Int, y: Int, z: Int): BlockPosition {
        return BlockPosition(worldId, this.x + x, this.y + y, this.z + z)
    }

    fun withScalar(x: Int, y: Int, z: Int): BlockPosition {
        return BlockPosition(worldId, x, y, z)
    }

    fun getRelative(face: BlockFace) = plus(face.direction.toVector3i())

    operator fun plus(other: BlockPosition): BlockPosition {
        check(worldId == other.worldId) { "Cannot add two BlockPositions in different worlds" }
        return BlockPosition(worldId, x + other.x, y + other.y, z + other.z)
    }

    operator fun plus(other: Vector3i): BlockPosition {
        return BlockPosition(worldId, x + other.x, y + other.y, z + other.z)
    }

    operator fun minus(other: BlockPosition): BlockPosition {
        check(worldId == other.worldId) { "Cannot subtract two BlockPositions in different worlds" }
        return BlockPosition(worldId, x - other.x, y - other.y, z - other.z)
    }

    operator fun minus(other: Vector3i): BlockPosition {
        return BlockPosition(worldId, x + other.x, y + other.y, z + other.z)
    }

    operator fun times(value: Int): BlockPosition {
        return BlockPosition(worldId, x * value, y * value, z * value)
    }

    operator fun div(value: Int): BlockPosition {
        return BlockPosition(worldId, x / value, y / value, z / value)
    }

    val vector3i: Vector3i
        get() = Vector3i(x, y, z)

    val location: Location
        get() = Location(world, x.toDouble(), y.toDouble(), z.toDouble())

    val block: Block
        get() = world?.getBlockAt(x, y, z) ?: error("World is null")

    companion object {
        fun asLong(x: Int, y: Int, z: Int): Long {
            return ((x and 0x3FFFFFF).toLong() shl 38)
                .or((z and 0x3FFFFFF).toLong() shl 12)
                .or((y and 0xFFF).toLong())
        }
    }
}

@get:JvmSynthetic
val Block.position: BlockPosition
    get() = BlockPosition(this)
