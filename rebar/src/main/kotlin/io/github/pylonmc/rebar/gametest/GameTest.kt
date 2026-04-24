package io.github.pylonmc.rebar.gametest

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.util.delayTicks
import io.github.pylonmc.rebar.util.position.BlockPosition
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import org.bukkit.*
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.util.BoundingBox
import java.util.concurrent.CompletableFuture

/**
 * A game test is a special blocked-off area in the world that allows you to test blocks, items,
 * entities, and other game mechanics in a controlled environment. It contains a success condition
 * that looks at the world and determines when the test has succeeded.
 *
 * This represents a running test in the world.
 *
 * Default behaviour is to simply succeed.
 *
 * @see GameTestConfig
 */
class GameTest(
    val config: GameTestConfig,
    val world: World,
    val center: BlockPosition,
    val boundingBox: BoundingBox
) {

    private var successCondition: () -> Boolean = { true }

    fun succeed() {
        successCondition = { true }
    }

    /**
     * Set the condition under which the test will succeed. Will be checked periodically.
     */
    fun succeedWhen(condition: () -> Boolean) {
        successCondition = condition
    }

    @JvmOverloads
    fun fail(message: String, cause: Throwable? = null) {
        throw GameTestFailException(this, message, cause)
    }

    /**
     * Checks whether any entity in the gametest matches [predicate]
     */
    inline fun entityInBounds(predicate: (Entity) -> Boolean): Boolean {
        return world.getNearbyEntities(boundingBox).any(predicate)
    }

    /**
     * Runs [runnable] on the main thread after waiting for the specified number of [ticks]
     */
    fun withDelay(ticks: Long, runnable: Runnable) {
        Rebar.scope.launch {
            delayTicks(ticks)
            runnable.run()
        }
    }

    /**
     * Returns the center position of the game test
     */
    fun position(): BlockPosition = center

    /**
     * Returns a position relative to the center of the game test
     */
    fun position(offset: BlockPosition): BlockPosition = center + offset

    /**
     * Returns a position relative to the center of the game test
     */
    fun position(x: Int, y: Int, z: Int): BlockPosition = center + BlockPosition(world, x, y, z)

    /**
     * Returns the center location of the game test
     */
    fun location(): Location = center.toLocation()

    /**
     * Returns a location relative to the center of the game test
     */
    fun location(location: Location): Location = location.clone().add(center.toLocation())

    /**
     * Returns a location relative to the center of the game test
     */
    fun location(x: Double, y: Double, z: Double): Location = center.toLocation().clone().add(x, y, z)

    companion object {
        @JvmSynthetic
        internal fun submit(gameTest: GameTest, delay: Long): CompletableFuture<GameTestFailException?> {
            return Rebar.scope.future {
                val chunks = mutableSetOf<Chunk>()
                for (x in gameTest.boundingBox.minX.toInt()..gameTest.boundingBox.maxX.toInt()) {
                    for (z in gameTest.boundingBox.minZ.toInt()..gameTest.boundingBox.maxZ.toInt()) {
                        val chunk = gameTest.world.getBlockAt(x, 0, z).chunk
                        chunk.isForceLoaded = true
                        chunks.add(chunk)
                    }
                }
                delayTicks(delay)
                var result: GameTestFailException? = null
                val ticksAtStart = Bukkit.getCurrentTick()
                try {
                    while (true) {
                        val currentTick = Bukkit.getCurrentTick()
                        if (currentTick - ticksAtStart >= gameTest.config.timeoutTicks) {
                            result = GameTestFailException(gameTest, "Test timed out")
                            break
                        }
                        if (gameTest.successCondition()) {
                            result = null
                            break
                        }
                        delayTicks(1)
                    }
                } catch (e: GameTestFailException) {
                    result = e
                } catch (e: Throwable) {
                    result = GameTestFailException(gameTest, "An exception occurred", e)
                }
                for (entity in gameTest.world.getNearbyEntities(gameTest.boundingBox)) {
                    if (entity !is Player) {
                        entity.remove()
                    }
                }
                for (x in gameTest.boundingBox.minX.toInt()..gameTest.boundingBox.maxX.toInt()) {
                    for (y in gameTest.boundingBox.minY.toInt()..gameTest.boundingBox.maxY.toInt()) {
                        for (z in gameTest.boundingBox.minZ.toInt()..gameTest.boundingBox.maxZ.toInt()) {
                            gameTest.world.getBlockAt(x, y, z).setType(Material.AIR, false)
                        }
                    }
                }
                for (chunk in chunks) {
                    chunk.isForceLoaded = false
                }
                return@future result
            }
        }

    }
}