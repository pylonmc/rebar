package io.github.pylonmc.rebar.block.base

import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.minecraftDispatcher
import com.github.shynixn.mccoroutine.bukkit.ticks
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.event.*
import io.github.pylonmc.rebar.util.rebarKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.jetbrains.annotations.ApiStatus
import java.util.IdentityHashMap

/**
 * Represents a block that 'ticks' (does something at a fixed time interval).
 */
interface RebarTickingBlock {

    private val tickingData: TickingBlockData
        get() = tickingBlocks.getOrPut(this) { TickingBlockData(
            RebarConfig.DEFAULT_TICK_INTERVAL,
            false,
            null
        )}

    /**
     * The interval at which the [tick] function is called. You should generally use [setTickInterval]
     * in your place constructor instead of overriding this.
     */
    val tickInterval
        get() = tickingData.tickInterval

    /**
     * Whether the [tick] function should be called asynchronously. You should generally use
     * [setAsync] in your place constructor instead of overriding this.
     */
    val isAsync
        get() = tickingData.isAsync

    /**
     * Sets how often the [tick] function should be called (in Minecraft ticks)
     */
    fun setTickInterval(tickInterval: Int) {
        tickingData.tickInterval = tickInterval
    }

    /**
     * Sets whether the [tick] function should be called asynchronously.
     *
     * WARNING: Settings a block to tick asynchronously could have unintended consequences.
     *
     * Only set this option if you understand what 'asynchronous' means, and note that you
     * cannot interact with the world asynchronously.
     */
    fun setAsync(isAsync: Boolean) {
        tickingData.isAsync = isAsync
    }

    /**
     * The function that should be called periodically.
     */
    fun tick()

    @ApiStatus.Internal
    companion object : Listener {

        internal data class TickingBlockData(
            var tickInterval: Int,
            var isAsync: Boolean,
            var job: Job?,
        )

        private val tickingBlockKey = rebarKey("ticking_block_data")

        private val tickingBlocks = IdentityHashMap<RebarTickingBlock, TickingBlockData>()

        @EventHandler
        private fun onDeserialize(event: RebarBlockDeserializeEvent) {
            val block = event.rebarBlock
            if (block is RebarTickingBlock) {
                tickingBlocks[block] = event.pdc.get(tickingBlockKey, RebarSerializers.TICKING_BLOCK_DATA)
                    ?: error("Ticking block data not found for ${block.key}")
            }
        }

        @EventHandler
        private fun onSerialize(event: RebarBlockSerializeEvent) {
            val block = event.rebarBlock
            if (block is RebarTickingBlock) {
                event.pdc.set(tickingBlockKey, RebarSerializers.TICKING_BLOCK_DATA, tickingBlocks[block]!!)
            }
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            val block = event.rebarBlock
            if (block is RebarTickingBlock) {
                tickingBlocks.remove(block)?.job?.cancel()
            }
        }

        @EventHandler
        private fun onBreak(event: RebarBlockBreakEvent) {
            val block = event.rebarBlock
            if (block is RebarTickingBlock) {
                tickingBlocks.remove(block)?.job?.cancel()
            }
        }

        @EventHandler
        private fun onRebarBlockPlace(event: RebarBlockPlaceEvent) {
            val block = event.rebarBlock
            if (block is RebarTickingBlock) {
                startTicker(block)
            }
        }

        @EventHandler
        private fun onRebarBlockLoad(event: RebarBlockLoadEvent) {
            val block = event.rebarBlock
            if (block is RebarTickingBlock) {
                startTicker(block)
            }
        }

        /**
         * Returns true if the block is still ticking, or false if the block does
         * not exist, is not a ticking block, or has errored and been unloaded.
         */
        @JvmStatic
        @ApiStatus.Internal
        fun isTicking(block: RebarBlock?): Boolean {
            return block is RebarTickingBlock && tickingBlocks[block]?.job?.isActive == true
        }

        @JvmSynthetic
        internal fun stopTicking(block: RebarTickingBlock) {
            tickingBlocks[block]?.job?.cancel()
        }

        private fun startTicker(tickingBlock: RebarTickingBlock) {
            val dispatcher = if (tickingBlock.isAsync) Rebar.asyncDispatcher else Rebar.minecraftDispatcher
            tickingBlocks[tickingBlock]?.job = Rebar.launch(dispatcher) {
                while (true) {
                    delay(tickingBlock.tickInterval.ticks)
                    try {
                        tickingBlock.tick()
                    } catch (e: Exception) {
                        Rebar.launch(Rebar.minecraftDispatcher) {
                            BlockListener.logEventHandleErr(null, e, tickingBlock as RebarBlock)
                        }
                    }
                }
            }
        }
    }
}