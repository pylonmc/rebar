package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.event.RebarBlockDeserializeEvent
import io.github.pylonmc.rebar.event.RebarBlockLoadEvent
import io.github.pylonmc.rebar.event.RebarBlockSerializeEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import io.github.pylonmc.rebar.util.gui.ProgressItem
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.jetbrains.annotations.ApiStatus
import java.util.IdentityHashMap
import kotlin.math.roundToInt

/**
 * An interface that tracks progress of some kind of process, such as processing a
 * recipe, burning a piece of fuel, enchanting an item, etc
 *
 * You can set a progress item with `setRecipeProgressItem`. This item
 * will be automatically synchronized to the process progress, and will
 * be persisted.
 *
 * @see RecipeProcessorRebarBlock
 */
interface ProcessorRebarBlock {

    @ApiStatus.Internal
    data class ProcessorData(
        var processTimeTicks: Int?,
        var processTicksRemaining: Int?,
        var progressItem: ProgressItem?,
    )
    private val processorData: ProcessorData
        get() = processorBlocks.getOrPut(this) { ProcessorData(null, null, null)}

    val processTimeTicks: Int?
        @ApiStatus.NonExtendable
        get() = processorData.processTimeTicks

    val processTimeSeconds: Int?
        @ApiStatus.NonExtendable
        get() = processTimeTicks?.toDouble()?.div(20)?.roundToInt()

    val processTicksRemaining: Int?
        @ApiStatus.NonExtendable
        get() = processorData.processTicksRemaining

    val processSecondsRemaining: Int?
        @ApiStatus.NonExtendable
        get() = processTicksRemaining?.toDouble()?.div(20)?.roundToInt()

    val processProgress: Double?
        @ApiStatus.NonExtendable
        get() = processTicksRemaining?.toDouble()?.div(processTimeTicks!!)

    val isProcessing: Boolean
        @ApiStatus.NonExtendable
        get() = processTimeTicks != null

    var processProgressItem: ProgressItem
        get() = processorData.progressItem ?: error("No recipe progress item was set")
        set(progressItem) {
            processorData.progressItem = progressItem
        }

    /**
     * Starts a new process with duration [ticks]
     */
    fun startProcess(ticks: Int) {
        processorData.processTimeTicks = ticks
        processorData.processTicksRemaining = ticks
        processorData.progressItem?.setTotalTimeTicks(ticks)
        processorData.progressItem?.setRemainingTimeTicks(ticks)
    }

    fun stopProcess() {
        val data = processorData
        data.processTimeTicks = null
        data.processTicksRemaining = null
        data.progressItem?.totalTime = null
    }

    fun finishProcess() {
        check(isProcessing) {
            "Cannot finish process because there is no process ongoing"
        }
        stopProcess()
        onProcessFinished()
    }

    fun onProcessFinished() {}

    /**
     * Progresses the progress by [ticks] ticks
     */
    @ApiStatus.Internal
    fun progressProcess(ticks: Int) {
        val data = processorData
        if (data.processTimeTicks == null) {
            return
        }

        data.processTicksRemaining = data.processTicksRemaining!! - ticks
        data.progressItem?.setRemainingTimeTicks(data.processTicksRemaining!!)
        if (data.processTicksRemaining!! <= 0) {
            finishProcess()
        }
    }

    @ApiStatus.Internal
    companion object : Listener {

        private val processorKey = rebarKey("processor_data")

        private val processorBlocks = IdentityHashMap<ProcessorRebarBlock, ProcessorData>()

        @EventHandler
        private fun onDeserialize(event: RebarBlockDeserializeEvent) {
            val block = event.rebarBlock
            if (block !is ProcessorRebarBlock) {
                return
            }

            event.pdc.get(processorKey, RebarSerializers.PROCESSOR_DATA)?.let { processorBlocks[block] = it }

        }

        @EventHandler
        private fun onLoad(event: RebarBlockLoadEvent) {
            // This separate listener is needed because when [RebarBlockDeserializeEvent] fires, then the
            // block may not have been fully initialised yet (e.g. postInitialise may not have been called)
            // which means progressItem may not have been set yet
            val block = event.rebarBlock
            if (block is ProcessorRebarBlock) {
                val data = processorBlocks[block]!!
                data.progressItem?.setTotalTimeTicks(data.processTimeTicks)
                data.processTicksRemaining?.let { data.progressItem?.setRemainingTimeTicks(it) }
            }
        }

        @EventHandler
        private fun onSerialize(event: RebarBlockSerializeEvent) {
            val block = event.rebarBlock
            if (block is ProcessorRebarBlock) {
                event.pdc.set(processorKey, RebarSerializers.PROCESSOR_DATA, block.processorData)
            }
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            val block = event.rebarBlock
            if (block is ProcessorRebarBlock) {
                processorBlocks.remove(block)
            }
        }
    }
}