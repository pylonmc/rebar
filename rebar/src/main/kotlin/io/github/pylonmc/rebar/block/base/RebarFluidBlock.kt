package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.context.BlockBreakContext
import io.github.pylonmc.rebar.block.context.BlockCreateContext
import io.github.pylonmc.rebar.content.fluid.FluidEndpointDisplay
import io.github.pylonmc.rebar.fluid.FluidPointType
import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.util.rotateFaceToReference
import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.MustBeInvokedByOverriders

/**
 * A block that interacts with fluids in some way.
 *
 * This is a very flexible class which requires you to define exactly how fluid should
 * be input and output. You are responsible for keeping track of any state, like how
 * much fluid is stored.
 *
 * Most fluid blocks can use [RebarFluidBufferBlock] or [RebarFluidTank] instead.
 *
 * This interface allowed you to request fluids from input points, supply fluids to
 * output points, and specify how to add/remove fluids from your block.
 *
 * Multiple inputs/outputs are not supported. You can have at most 1 input and 1 output.
 *
 *
 * @see RebarFluidBufferBlock
 * @see RebarFluidTank
 */
interface RebarFluidBlock : RebarEntityHolderBlock, RebarBreakHandler {

    fun getFluidPointDisplay(type: FluidPointType) =
        getHeldRebarEntity(FluidEndpointDisplay::class.java, getFluidPointName(type))

    fun getFluidPointDisplayOrThrow(type: FluidPointType) =
        getHeldRebarEntityOrThrow(FluidEndpointDisplay::class.java, getFluidPointName(type))

    /**
     * Creates a fluid input point. Call in your place constructor. Should be called at most once per block.
     */
    fun createFluidPoint(type: FluidPointType, face: BlockFace, radius: Float) {
        check(getFluidPointDisplay(type) == null) { "A fluid point of type $type already exists on this block" }
        addEntity(getFluidPointName(type), FluidEndpointDisplay(block, type, face, radius))
    }

    /**
     * Creates a fluid input point. Call in your place constructor. Should be called at most once per block.
     */
    fun createFluidPoint(type: FluidPointType, face: BlockFace) = createFluidPoint(type, face, 0.5F)

    /**
     * Creates a fluid input point. Call in your place constructor. Should be called at most once per block.
     *
     * @param context If a player placed the block, the point will be rotated to the player's frame of reference,
     * with NORTH considered 'forward'
     * @param allowVerticalFaces Whether up/down should be considered when rotating to the player's frame
     * of reference
     *
     * @see rotateFaceToReference
     */
    fun createFluidPoint(type: FluidPointType, face: BlockFace, context: BlockCreateContext, allowVerticalFaces: Boolean, radius: Float)
        = createFluidPoint(
            type,
            rotateFaceToReference(if (allowVerticalFaces) context.facingVertical else context.facing, face),
            radius
        )

    /**
     * Creates a fluid input point. Call in your place constructor. Should be called at most once per block.
     *
     * @param context If a player placed the block, the point will be rotated to the player's frame of reference,
     * with NORTH considered 'forward'
     * @param allowVerticalFaces Whether up/down should be considered when rotating to the player's frame
     * of reference
     *
     * @see rotateFaceToReference
     */
    fun createFluidPoint(type: FluidPointType, face: BlockFace, context: BlockCreateContext, allowVerticalFaces: Boolean)
        = createFluidPoint(type, face, context, allowVerticalFaces, 0.5F)

    @MustBeInvokedByOverriders
    override fun onBreak(drops: MutableList<ItemStack>, context: BlockBreakContext) {
        val player = (context as? BlockBreakContext.PlayerBreak)?.event?.player
        getFluidPointDisplay(FluidPointType.INPUT)?.pipeDisplay?.delete(player, drops)
    }

    /**
     * Returns a map of fluid types - and their corresponding amounts - that can be supplied by
     * the block for this fluid tick.
     *
     * If you have a machine that can supply up to 100 fluid per second, it should supply
     * 5 * RebarConfig.fluidTickInterval of that fluid
     *
     * Any implementation of this method must NEVER call the same method for any other connection
     * point, otherwise you risk creating infinite loops.
     *
     * Called exactly one per fluid tick.
     */
    fun getSuppliedFluids(): Map<RebarFluid, Double> = mapOf()

    /**
     * Indicates that the block requires *the exact* amount of fluid as returned by [fluidAmountRequested].
     * Any less will fail, and any more will be truncated to the requested amount.
     */
    @Suppress("INAPPLICABLE_JVM_NAME")
    @get:JvmName("requiresExactFluidAmount")
    val requiresExactFluidAmount: Boolean
        get() = false

    /**
     * Returns the amount of the given fluid that the machine wants to receive next tick.
     *
     * If you have a machine that consumes 5 water per tick, it should request
     * 5*RebarConfig.fluidTickInterval of water, and return 0 for every other fluid.
     *
     * Any implementation of this method must NEVER call the same method for any other connection
     * point, otherwise you risk creating infinite loops.
     *
     * Called at most once for any given fluid type per tick.
     */
    fun fluidAmountRequested(fluid: RebarFluid): Double = 0.0

    /**
     * `amount` is always at most `fluidAmountRequested(fluid)` and will never
     * be zero or less.
     *
     * Called at most once per fluid tick.
     */
    fun onFluidAdded(fluid: RebarFluid, amount: Double) {
        throw UnsupportedOperationException("Block requested fluids, but does not implement onFluidAdded")
    }

    /**
     * `amount` is always at least `getSuppliedFluids().get(fluid)` and will never
     * be zero or less.
     *
     * Called at most once per fluid tick.
     */
    fun onFluidRemoved(fluid: RebarFluid, amount: Double) {
        throw UnsupportedOperationException("Block supplied fluids, but does not implement removeFluid")
    }

    companion object {

        @JvmSynthetic
        internal fun getFluidPointName(type: FluidPointType) = when (type) {
            FluidPointType.INPUT -> "fluid_point_input"
            FluidPointType.OUTPUT -> "fluid_point_output"
            FluidPointType.INTERSECTION -> throw IllegalStateException("You cannot create an intersection point from a fluid block")
        }
    }
}