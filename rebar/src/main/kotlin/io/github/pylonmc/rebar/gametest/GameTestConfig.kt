package io.github.pylonmc.rebar.gametest

import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.position.BlockPosition
import org.bukkit.Keyed
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.util.BoundingBox
import java.util.concurrent.CompletableFuture
import java.util.function.Consumer
import kotlin.properties.Delegates

/**
 * Represents the configuration for a [GameTest]. Prefer using the [Builder] to create instances of this class.
 *
 * @see GameTest
 */
class GameTestConfig(
    private val key: NamespacedKey,
    val size: Int,
    val setUp: Consumer<GameTest>,
    val delayTicks: Long,
    val timeoutTicks: Long,
    val positionOverride: BlockPosition?
) : Keyed {
    override fun getKey(): NamespacedKey = key

    /**
     * Builds a [io.github.pylonmc.rebar.gametest.GameTestConfig].
     */
    class Builder(val key: NamespacedKey) {
        private var size by Delegates.notNull<Int>()
        private var setUp: Consumer<GameTest> = Consumer {}
        private var delayTicks = 0L
        private var timeoutTicks = 5L * 60 * 20
        private var positionOverride: BlockPosition? = null

        /**
         * Size is the buffer around the central block. A size of 0 means the test will only run
         * on the central block, a size of 1 means the test will run on a 3x3x3 area centered on
         * the central block, etc.
         */
        fun size(size: Int): Builder = apply { this.size = size }

        /**
         * Run before the test starts
         */
        fun setUp(setUp: Consumer<GameTest>): Builder = apply { this.setUp = setUp }

        /**
         * Delay in ticks before the test starts. Defaults to 0.
         */
        fun delayTicks(delayTicks: Long): Builder = apply { this.delayTicks = delayTicks }

        /**
         * Timeout in ticks before the test fails. Defaults to 5 minutes.
         */
        fun timeoutTicks(timeoutTicks: Long): Builder = apply { this.timeoutTicks = timeoutTicks }

        /**
         * Override the position where the test will be launched.
         */
        fun positionOverride(position: BlockPosition): Builder = apply { this.positionOverride = position }

        fun build() = GameTestConfig(key, size, setUp, delayTicks, timeoutTicks, positionOverride)
    }

    /**
     * Launches the game test at the given position.
     * If [positionOverride] is set, it will be used instead of the given position.
     * Returns a future that completes with a [GameTestFailException] if the test fails, or null if it succeeds.
     */
    fun launch(position: BlockPosition): CompletableFuture<GameTestFailException?> {
        val realPosition = positionOverride ?: position
        val boundingBox = BoundingBox(
            realPosition.x - size - 1.0,
            realPosition.y - 1.0,
            realPosition.z - size - 1.0,
            realPosition.x + size + 1.0,
            realPosition.y + size + 1.0,
            realPosition.z + size + 1.0
        )
        val gameTest = GameTest(
            this,
            realPosition.world ?: throw IllegalArgumentException("Position must have world"),
            realPosition,
            boundingBox
        )

        // x
        for (z in -size..size) {
            for (y in 0..size) {
                gameTest.position(size + 1, y, z).block.type = Material.BARRIER
                gameTest.position(-size - 1, y, z).block.type = Material.BARRIER
            }
        }

        // z
        for (x in -size..size) {
            for (y in 0..size) {
                gameTest.position(x, y, size + 1).block.type = Material.BARRIER
                gameTest.position(x, y, -size - 1).block.type = Material.BARRIER
            }
        }

        // y
        for (x in -size..size) {
            for (z in -size..size) {
                gameTest.position(x, size + 1, z).block.type = Material.BARRIER
                gameTest.position(x, -1, z).block.type = Material.BEDROCK
            }
        }

        try {
            setUp.accept(gameTest)
        } catch (e: Throwable) {
            return CompletableFuture.completedFuture(GameTestFailException(gameTest, "Error on setup", e))
        }

        return GameTest.submit(gameTest, delayTicks)
    }

    fun register() = RebarRegistry.GAMETESTS.register(this)
}