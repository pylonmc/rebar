package io.github.pylonmc.rebar.fluid.placement

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.RebarBlock.Companion.isVanillaBlock
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.content.fluid.*
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.entity.display.ItemDisplayBuilder
import io.github.pylonmc.rebar.entity.display.transform.LineBuilder
import io.github.pylonmc.rebar.entity.display.transform.TransformBuilder
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.util.*
import io.github.pylonmc.rebar.util.position.BlockPosition
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.joml.Vector3f
import org.joml.Vector3i
import kotlin.math.roundToLong

internal class FluidPipePlacementTask(
    val player: Player,
    val origin: FluidPipePlacementPoint,
    val pipe: FluidPipe
) {

    class Result (val to: FluidPointDisplay, val pipesUsed: Int)

    private val display = ItemDisplayBuilder()
        .material(Material.WHITE_CONCRETE)
        .brightness(15) // transformation will be set later, make the block invisible now to prevent flash of white on place
        .transformation(TransformBuilder().scale(0f))
        .build(origin.position.location.toCenterLocation())

    var target = origin
        private set

    var isValid = false
        private set

    val task = Bukkit.getScheduler().runTaskTimer(
        Rebar,
        Runnable { tick() },
        0,
        RebarConfig.PIPE_PLACEMENT_TASK_INTERVAL_TICKS
    )

    private fun tick() {
        // Check if player is no longer holding pipes
        // This is specifically to see if the player has thrown the pipes out of their hotbar
        // (scrolling off is handled in FluidPipePlacementService)
        val hand = player.inventory.getItem(EquipmentSlot.HAND)
        val rebarItem = RebarItem.fromStack(hand)
        if (rebarItem !is FluidPipe) {
            FluidPipePlacementService.cancelConnection(player)
            return
        }

        // Check if origin and target points still exist
        if (!origin.stillActuallyExists(true) || !target.stillActuallyExists(false)) {
            FluidPipePlacementService.cancelConnection(player)
            return
        }

        // Check if player has moved too far away
        if (player.location.distance(origin.position.location) > RebarConfig.PIPE_PLACEMENT_CANCEL_DISTANCE) {
            FluidPipePlacementService.cancelConnection(player)
            return
        }

        // Recalculate target
        val previousTargetPosition = target.position
        recalculateTarget()

        // Check status of placement
        val playerHas = player.inventory.filterNotNull().filter { it.isSimilar(hand) }.sumOf { it.amount }
        val playerHasEnoughPipes = pipesUsed(origin.position, target.position) <= playerHas
        if (player.gameMode != GameMode.CREATIVE && !playerHasEnoughPipes) {
            // Player does not have enough pipes
            isValid = false
            player.sendActionBar(Component.translatable("rebar.message.pipe.not_enough_pipes"))
            display.setItemStack(ItemStack(Material.RED_CONCRETE))
        } else if (!this.isPipeTypeValid) {
            // Pipe is not of the correct type
            isValid = false
            player.sendActionBar(Component.translatable("rebar.message.pipe.not_of_same_type"))
            display.setItemStack(ItemStack(Material.RED_CONCRETE))
        } else if (!this.isPlacementValid) {
            // Points cannot be joined together (eg blocks in the way, one faces the wrong direction)
            isValid = false
            player.sendActionBar(Component.translatable("rebar.message.pipe.cannot_place_here"))
            display.setItemStack(ItemStack(Material.RED_CONCRETE))
        } else if (origin.position == target.position) {
            // Starting and ending in the same location
            isValid = false
            display.setItemStack(ItemStack(Material.CYAN_CONCRETE))
            display.setTransformationMatrix(
                TransformBuilder()
                    .scale(FluidPipeDisplay.SIZE)
                    .buildForItemDisplay()
            )
        } else {
            // Everything is fine
            isValid = true
            player.sendActionBar(Component.translatable("rebar.message.pipe.connecting"))
            display.setItemStack(ItemStack(Material.WHITE_CONCRETE))
        }

        // Update transformation
        if (origin.position != target.position) {
            val targetOffset = target.position.location.toVector().toVector3f()
                .add(target.offset)
                .sub(origin.position.location.toVector().toVector3f())
            display.setTransformationMatrix(
                LineBuilder()
                    .from(origin.offset)
                    .to(targetOffset)
                    .thickness(FluidPipeDisplay.SIZE + 0.001) // add 0.001 to prevent z-fighting with existing pipes
                    .build().buildForItemDisplay()
            )
        }

        // Interpolate only if changing on the same axis
        val difference = target.position.vector3i.sub(previousTargetPosition.vector3i)
        if (isCardinalDirection(difference)) {
            display.interpolationDelay = 0
            display.interpolationDuration = RebarConfig.PIPE_PLACEMENT_TASK_INTERVAL_TICKS.toInt()
        }
    }

    fun cancel() {
        task.cancel()
        display.remove()
        player.sendActionBar(Component.empty())
    }

    fun finish(): Result? {
        if (!isValid) {
            return null
        }

        task.cancel()
        display.remove()
        player.sendActionBar(Component.empty())

        val pipeDisplay = FluidPipePlacementService.connect(origin, target, pipe)
        return Result(pipeDisplay.getTo(), pipesUsed(origin.position, target.position))
    }

    fun pathIntersectsBlocks()
        = blocksOnPath(origin.position, target.position).any { !(it.isVanillaBlock && it.replaceableOrAir) }

    /**
     * Figures out where the player wants the pipe to end using the direction they're looking.
     */
    private fun recalculateTarget() {
        // If the player is looking at a fluid point, we should set that as the target
        val targetEntity = getTargetEntity(player, 1.5F * FluidEndpointDisplay.distanceFromFluidPointCenterToCorner)
        if (targetEntity != null) {
            val fluidPoint = EntityStorage.get(targetEntity)
            if (fluidPoint is FluidPointDisplay) {
                val newTarget = FluidPipePlacementPoint.PointDisplay(fluidPoint)
                if (isTargetInCorrectDirection(newTarget)) {
                    target = newTarget
                    return
                }
            }
        }

        // Otherwise, we should find the closest point on any axis that the player is looking at
        var distance = Float.MAX_VALUE
        distance = processAxis(Vector3i(1, 0, 0), distance)
        distance = processAxis(Vector3i(0, 1, 0), distance)
        processAxis(Vector3i(0, 0, 1), distance)
    }

    /**
     * Helper function that finds the closest block that the player is looking at on an axis, and the
     * distance to that axis. If the distance is shorter than the given [distance], it is returned and
     * the [target] is set to the block we found. Otherwise the old [distance] is returned.
     */
    private fun processAxis(axis: Vector3i, distance: Float): Float {
        val playerLookPosition = player.eyeLocation.toVector().toVector3f()
        val playerLookDirection = player.eyeLocation.getDirection().toVector3f()

        val newTargetOffset = getTargetPositionOnAxis(playerLookPosition, playerLookDirection, axis)

        val newTargetPosition = origin.position.plus(newTargetOffset)
        val newTargetBlock = BlockStorage.get(newTargetPosition)
        val newTarget = if (newTargetBlock is FluidSectionMarker) {
            FluidPipePlacementPoint.Section(newTargetBlock)
        } else if (newTargetBlock is FluidIntersectionMarker) {
            FluidPipePlacementPoint.PointDisplay(newTargetBlock.fluidIntersectionDisplay)
        } else {
            FluidPipePlacementPoint.EmptyBlock(newTargetPosition)
        }

        val newTargetDistance = findClosestDistanceBetweenLineAndPoint(
            Vector3f(origin.position.plus(newTargetOffset).vector3i),
            playerLookPosition,
            playerLookDirection
        )
        if (newTargetDistance < distance && isTargetInCorrectDirection(newTarget)) {
            target = newTarget
            return newTargetDistance
        }
        return distance
    }

    private val isPipeTypeValid: Boolean
        get() {
            val clonedTo = target // kotlin complains if no clone
            return when (clonedTo) {
                is FluidPipePlacementPoint.Section -> clonedTo.marker.pipeDisplay!!.pipe == pipe
                is FluidPipePlacementPoint.PointDisplay -> {
                    val connectedPipes = clonedTo.display.connectedPipeDisplays
                    if (connectedPipes.isEmpty()) {
                        return true // no pipes connected so any pipe type valid
                    }
                    // Slight hack - use an arbitrary connected pipe to find out the pipe type
                    val arbitraryDisplay = connectedPipes.iterator().next()
                    EntityStorage.getAs<FluidPipeDisplay>(arbitraryDisplay)!!.pipe == pipe
                }
                is FluidPipePlacementPoint.EmptyBlock -> true
            }
        }

    private val isPlacementValid: Boolean
        get() {
            val commonPipeDisplays = origin.connectedPipeDisplays.toMutableSet()
            commonPipeDisplays.retainAll(target.connectedPipeDisplays)
            val startAndEndAlreadyConnected = !commonPipeDisplays.isEmpty()

            val startAndEndEmptyIfNewBlock
                = !(origin is FluidPipePlacementPoint.EmptyBlock && !origin.position.block.let { it.isVanillaBlock && it.replaceableOrAir }
                    || target is FluidPipePlacementPoint.EmptyBlock && !target.position.block.let { it.isVanillaBlock && it.replaceableOrAir })

            return !startAndEndAlreadyConnected
                    && startAndEndEmptyIfNewBlock
                    && !pathIntersectsBlocks()
        }

    /**
     * Checks if the given [newTarget] is in the correct direction from the origin. This means
     * 1) it is on a cardinal direction
     * 2) if the origin has a specific face, the target is in the direction of that face
     */
    private fun isTargetInCorrectDirection(newTarget: FluidPipePlacementPoint): Boolean {
        val originToTarget = newTarget.position.vector3i.sub(origin.position.vector3i)
        val targetToOrigin = origin.position.vector3i.sub(newTarget.position.vector3i)
        return isCardinalDirection(originToTarget)
                && (origin.allowedFace == null || vectorToBlockFace(originToTarget) == origin.allowedFace!!)
                && (newTarget.allowedFace == null || vectorToBlockFace(targetToOrigin) == newTarget.allowedFace!!)
    }

    /**
     * Casts a ray where the player's looking and finds the closest block on a given axis.
     */
    private fun getTargetPositionOnAxis(
        playerLookPosition: Vector3f,
        playerLookDirection: Vector3f,
        axis: Vector3i
    ): Vector3i {
        val originPosition = origin.position.location.toCenterLocation().toVector().toVector3f()
        val solution = findClosestPointBetweenSkewLines(playerLookPosition, playerLookDirection, originPosition, Vector3f(axis))
        val lambda = Math.clamp(
            solution.roundToLong(),
            -RebarConfig.PIPE_PLACEMENT_MAX_LENGTH,
            RebarConfig.PIPE_PLACEMENT_MAX_LENGTH
        )
        return Vector3i(axis).mul(lambda.toInt())
    }

    companion object {
        fun pipesUsed(from: BlockPosition, to: BlockPosition)
                = blocksOnPath(from, to).size + 1
    }
}