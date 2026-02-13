package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.event.RebarBlockDeserializeEvent
import io.github.pylonmc.rebar.event.RebarBlockSerializeEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.jetbrains.annotations.ApiStatus
import java.util.IdentityHashMap
import kotlin.math.max

/**
 * A common pattern is a 'fluid tank' which can only store one fluid at a
 * time, but can store many types of fluids. `RebarFluidTank` implements this
 * pattern.
 *
 * You must call [setCapacity] for this
 * block to work.
 *
 * As with [RebarFluidBufferBlock], you do not need to handle saving buffers
 * or implement any of the [RebarFluidBlock] methods for this; this is all
 * done automatically.
 *
 */
interface RebarFluidTank : RebarFluidBlock {

    private val fluidData: FluidTankData
        get() = fluidTankBlocks.getOrPut(this) { FluidTankData(
            null,
            0.0,
            0.0,
            input = false,
            output = false
        )}

    /**
     * The type of fluid stored in the tank
     */
    val fluidType: RebarFluid?
        get() = fluidData.fluid

    /**
     * The capacity of the tank
     */
    val fluidCapacity: Double
            get() = fluidData.capacity

    /**
     * The amount of fluid stored in the tank
     */
    val fluidAmount: Double
            get() = fluidData.amount

    /**
     * The amount of space remaining in the tank
     */
    val fluidSpaceRemaining: Double
            get() = fluidData.capacity - fluidData.amount

    /**
     * Sets the type of fluid in the fluid tank
     */
    fun setFluidType(fluid: RebarFluid?) {
        fluidData.fluid = fluid
    }

    /**
     * Sets the capacity of the fluid tank
     */
    fun setCapacity(capacity: Double) {
        check(capacity > -1.0e-6)
        fluidData.capacity = max(0.0, capacity)
    }

    /**
     * Checks if a new amount of fluid is greater than zero and fits inside
     * the tank.
     */
    fun canSetFluid(amount: Double): Boolean
            = amount > -1.0e-6 && amount < fluidData.capacity + 1.0e-6

    /**
     * Checks if adding a certain amount of fluid would result in a valid
     * fluid amount.
     */
    fun canAddFluid(amount: Double)
            = canSetFluid(fluidData.amount + amount)

    /**
     * Checks if adding a certain amount of fluid of a certain type would
     * result in a valid fluid amount.
     */
    fun canAddFluid(fluid: RebarFluid?, amount: Double)
            = (fluid == null || fluidData.fluid == null || fluid == fluidData.fluid) && (fluid == null || isAllowedFluid(fluid)) && canSetFluid(fluidData.amount + amount)

    /**
     * Sets the fluid amount only if the new amount of fluid is greater
     * than zero and fits in the tank.
     *
     * @return true only if the fluid amount was set successfully
     */
    fun setFluid(amount: Double): Boolean {
        if (canSetFluid(amount)) {
            val original = fluidData.amount
            fluidData.amount = max(0.0, amount)

            if (original > amount && amount < 1.0e-6) {
                setFluidType(null)
                setFluid(0.0)
            }
            return true
        }
        return false
    }

    /**
     * Adds to the tank only if the new amount of fluid is greater
     * than zero and fits in the tank.
     *
     * @return true only if the tank was added to successfully
     */
    fun addFluid(amount: Double): Boolean
            = setFluid(fluidAmount + amount)

    /**
     * Removes from the tank only if the new amount of fluid is greater
     * than zero and fits in the tank.
     *
     * @return true only if the tank was added to successfully
     */
    fun removeFluid(amount: Double): Boolean
            = setFluid(fluidAmount - amount)

    fun isAllowedFluid(fluid: RebarFluid): Boolean

    override fun fluidAmountRequested(fluid: RebarFluid): Double{
        if (!isAllowedFluid(fluid)) {
            return 0.0
        }

        val fluidData = this.fluidData // local variable to save calling fluidData getter multiple times
        return if (fluidData.fluid == null) {
            fluidData.capacity
        } else if (fluid == fluidData.fluid && fluidData.amount <= fluidData.capacity - 1.0e-6) {
            fluidData.capacity - fluidData.amount
        } else {
            0.0
        }
    }

    override fun getSuppliedFluids(): Map<RebarFluid, Double> {
        val fluidData = this.fluidData // local variable to save calling fluidData getter multiple times
        return if (fluidData.fluid == null) {
            emptyMap()
        } else {
            mapOf(fluidData.fluid!! to fluidData.amount)
        }
    }

    override fun onFluidAdded(fluid: RebarFluid, amount: Double) {
        if (fluid != fluidType) {
            setFluidType(fluid)
        }
        addFluid(amount)
    }

    override fun onFluidRemoved(fluid: RebarFluid, amount: Double) {
        check(fluid == fluidType)
        removeFluid(amount)
    }

    @ApiStatus.Internal
    companion object : Listener {

        internal data class FluidTankData(
            var fluid: RebarFluid?,
            var amount: Double,
            var capacity: Double,
            var input: Boolean,
            var output: Boolean,
        )

        private val fluidTankKey = rebarKey("fluid_tank_data")

        private val fluidTankBlocks = IdentityHashMap<RebarFluidTank, FluidTankData>()

        @EventHandler
        private fun onDeserialize(event: RebarBlockDeserializeEvent) {
            val block = event.rebarBlock
            if (block is RebarFluidTank) {
                fluidTankBlocks[block] = event.pdc.get(fluidTankKey, RebarSerializers.FLUID_TANK_DATA)
                    ?: error("Fluid tank data not found for ${block.key}")
            }
        }

        @EventHandler
        private fun onSerialize(event: RebarBlockSerializeEvent) {
            val block = event.rebarBlock
            if (block is RebarFluidTank) {
                event.pdc.set(fluidTankKey, RebarSerializers.FLUID_TANK_DATA, fluidTankBlocks[block]!!)
            }
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            val block = event.rebarBlock
            if (block is RebarFluidTank) {
                fluidTankBlocks.remove(block)
            }
        }
    }
}