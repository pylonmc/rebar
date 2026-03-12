package io.github.pylonmc.rebar.async

import com.google.common.collect.HashMultimap
import io.github.pylonmc.rebar.util.position.ChunkPosition
import io.github.pylonmc.rebar.util.position.position
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkUnloadEvent
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineScope] that is canceled when the chunk is unloaded.
 */
class ChunkScope(override val coroutineContext: CoroutineContext, chunk: ChunkPosition) : CoroutineScope {

    init {
        check(chunk.isLoaded) { "Cannot create a ChunkScope for an unloaded chunk" }
        chunkScopes.put(chunk, this)
    }

    companion object : Listener {
        private val chunkScopes = HashMultimap.create<ChunkPosition, ChunkScope>()

        @EventHandler(priority = EventPriority.MONITOR)
        private fun onChunkUnload(event: ChunkUnloadEvent) {
            chunkScopes.removeAll(event.chunk.position).forEach { it.cancel() }
        }
    }
}