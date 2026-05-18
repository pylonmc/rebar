package io.github.pylonmc.rebar.util

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.util.PlayerInput.requestInput
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.future.await
import kotlinx.coroutines.launch
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object PlayerInput : Listener {

    private val handlerLock = ReentrantLock()
    private val handlers = ConcurrentHashMap<UUID, MutableList<Handler>>()

    @EventHandler(ignoreCancelled = true)
    private fun onPlayerInput(event: AsyncChatEvent) {
        val id = event.player.uniqueId
        val handlers = handlerLock.withLock { handlers.remove(id) } ?: return
        if (handlers.isEmpty()) return
        event.isCancelled = true
        val message = PlainTextComponentSerializer.plainText().serialize(event.message())
        for (handler in handlers) {
            if (!event.isAsynchronous || handler.allowAsync) {
                handler.future.complete(message)
            } else {
                Rebar.scope.launch {
                    handler.future.complete(message)
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun onPlayerQuit(event: PlayerQuitEvent) {
        val id = event.player.uniqueId
        val handlers = handlerLock.withLock { handlers.remove(id) } ?: return
        for (handler in handlers) {
            handler.future.complete(null)
        }
    }

    /**
     * Returns a future that will be completed with the player's next chat message, or null if they disconnect before sending a message.
     * The message sending will be canceled to prevent it from appearing in chat.
     *
     * If [allowAsync] is false and the player sends a message asynchronously, the future will be completed on the main thread on the next tick.
     * Conversely, if [allowAsync] is true, the future will be completed immediately on message send.
     */
    @JvmStatic
    fun requestInput(player: Player, allowAsync: Boolean): CompletableFuture<String?> {
        val future = CompletableFuture<String?>()
        handlerLock.withLock {
            handlers.getOrPut(player.uniqueId, ::mutableListOf).add(Handler(future, allowAsync))
        }
        return future
    }

    /**
     * Returns a future that will be completed with the player's next chat message, or null if they disconnect before sending a message.
     * The message sending will be canceled to prevent it from appearing in chat.
     * 
     * This is a convenience overload of [requestInput] that defaults to not allowing asynchronous messages, so the future will always be completed on the main thread.
     */
    @JvmStatic
    fun requestInput(player: Player): CompletableFuture<String?> = requestInput(player, allowAsync = false)

    private data class Handler(val future: CompletableFuture<String?>, val allowAsync: Boolean)
}

/**
 * Suspends until the player sends a chat message, then returns the message, or null if they disconnect before sending a message.
 * The message sending will be canceled to prevent it from appearing in chat.
 */
@JvmSynthetic
suspend fun Player.requestInput(): String? = PlayerInput.requestInput(this, allowAsync = true).await()