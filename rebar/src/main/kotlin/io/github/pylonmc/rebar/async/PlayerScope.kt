package io.github.pylonmc.rebar.async

import com.google.common.collect.HashMultimap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import kotlin.coroutines.CoroutineContext

/**
 * A [CoroutineScope] that is canceled when the player logs out.
 */
class PlayerScope(override val coroutineContext: CoroutineContext, playerId: UUID) : CoroutineScope {

    constructor(coroutineContext: CoroutineContext, player: Player) : this(coroutineContext, player.uniqueId)

    init {
        check(Bukkit.getPlayer(playerId)?.isOnline == true) { "Cannot create a PlayerScope for an offline player" }
        playerScopes.put(playerId, this)
    }

    companion object : Listener {
        private val playerScopes = HashMultimap.create<UUID, PlayerScope>()

        @EventHandler(priority = EventPriority.MONITOR)
        private fun onPlayerQuit(event: PlayerQuitEvent) {
            playerScopes.removeAll(event.player.uniqueId).forEach { it.cancel() }
        }
    }
}