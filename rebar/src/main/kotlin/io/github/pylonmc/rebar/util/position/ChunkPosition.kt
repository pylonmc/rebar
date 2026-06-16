package io.github.pylonmc.rebar.util.position

import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.block.Block
import java.util.UUID

/**
 * Represents the position of a chunk (x, z, and world).
 *
 * Why not just use [Chunk]? Because [Chunk] contains lots of extra information,
 * and so cannot practically be serialized. Holding references to blocks for
 * extended periods may also prevent chunks from unloading, and increase memory
 * usage.
 */
class ChunkPosition(val worldId: UUID?, val x: Int, val z: Int) {
    val world: World?
        get() = worldId?.let { Bukkit.getWorld(it) }

    internal val asLong: Long
        @JvmSynthetic
        get() = (x.toLong() shl 32) or (z.toLong() and 0xFFFFFFFFL)

    val chunk: Chunk?
        /**
         * WARNING: Getting the chunk will load the chunk if it is not already loaded.
         */
        get() = world?.getChunkAt(x, z)

    /**
     * Obtaining an instance of a Chunk (eg through block.getChunk()) will
     * often LOAD THE CHUNK ITSELF. Yes, actually. This method does not
     * load the chunk, so it's a safe way to check if a chunk is loaded.
     */
    val isLoaded: Boolean
        get() = world?.isChunkLoaded(x, z) == true

    constructor(asLong: Long) : this(null, asLong)

    constructor(world: World?, asLong: Long) : this(world?.uid, (asLong shr 32).toInt(), asLong.toInt())

    constructor(x: Int, z: Int) : this(null as UUID?, x, z)

    constructor(world: World?, x: Int, z: Int) : this(world?.uid, x, z)

    constructor(chunk: Chunk) : this(chunk.world.uid, chunk.x, chunk.z)

    constructor(location: Location) : this(location.world?.uid, location.blockX shr 4, location.blockZ shr 4)

    constructor(block: Block) : this(block.location)

    override fun hashCode(): Int {
        val prime = 31
        return prime * (world?.hashCode() ?: 0) + prime * asLong.hashCode()
    }

    override fun toString(): String {
        return if (world != null) {
            "$x, $z in ${world!!.name}"
        } else {
            "$x, $z"
        }
    }

    override fun equals(other: Any?): Boolean {
        return (other is ChunkPosition)
                && other.worldId == worldId
                && other.x == x
                && other.z == z
    }
}

@get:JvmSynthetic
val Chunk.position: ChunkPosition
    get() = ChunkPosition(this)

@get:JvmSynthetic
val Block.chunkPosition: ChunkPosition
    get() = ChunkPosition(this)