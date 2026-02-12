package io.github.pylonmc.rebar.content.fluid

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.fluid.FluidManager
import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.fluid.placement.FluidPipePlacementPoint
import io.github.pylonmc.rebar.fluid.placement.FluidPipePlacementService
import io.github.pylonmc.rebar.fluid.tags.FluidTemperature
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.base.RebarInteractor
import io.github.pylonmc.rebar.util.getTargetEntity
import io.github.pylonmc.rebar.util.gui.unit.UnitFormat
import io.github.pylonmc.rebar.util.position.BlockPosition
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack

/**
 * An item that represents a fluid pipe. All the logic regarding placing the pipe is handled automatically.
 *
 * Expects the pipe's config to contain a `material`, a `fluid-per-second`, and optionally a
 * `allowed-temperatures` value. If `allowed-temperatures` is not set, any fluid will be allowed through.
 * To override this behaviour, override [canPass].
 *
 * You should generally not need to extend this class. Instead, simply register an item with class
 * FluidPipe.class to create a new pipe type.
 */
open class FluidPipe(stack: ItemStack) : RebarItem(stack), RebarInteractor {
    val material = getSettings().getOrThrow("material", ConfigAdapter.MATERIAL)
    val fluidPerSecond = getSettings().getOrThrow("fluid-per-second", ConfigAdapter.DOUBLE)
    val allowedTemperatures = getSettings().get(
        "allowed-temperatures",
        ConfigAdapter.LIST.from(ConfigAdapter.FLUID_TEMPERATURE)
    )

    override fun getPlaceholders(): List<RebarArgument> = listOf(
        RebarArgument.of("fluid_per_second", UnitFormat.MILLIBUCKETS_PER_SECOND.format(fluidPerSecond)),
        RebarArgument.of(
            "temperatures", Component.join(
                JoinConfiguration.separator(Component.text(", ")),
                allowedTemperatures?.map(FluidTemperature::valueText) ?: listOf()
            )
        )
    )

    /**
     * Returns whether the pipe is capable of moving the given fluid through it.
     */
    open fun canPass(fluid: RebarFluid) = allowedTemperatures == null
            || fluid.hasTag<FluidTemperature>() && fluid.getTag<FluidTemperature>() in allowedTemperatures

    override fun onUsedToClick(event: PlayerInteractEvent, priority: EventPriority) {
        if (event.hand != EquipmentSlot.HAND) {
            return
        }

        val action = event.action
        val block = event.clickedBlock
        val player = event.player

        if (FluidPipePlacementService.connectedLastTick(player)) {
            return
        }

        if (FluidPipePlacementService.isConnecting(player))  {
            // Player is already connecting; see if we can finish the connection
            if (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) {
                val segment = FluidPipePlacementService.placeConnection(player)
                if (segment != null) {
                    FluidManager.setFluidPerSecond(segment, fluidPerSecond)
                    FluidManager.setFluidPredicate(segment, this::canPass)
                }
            }
        } else {
            // Player is not yet connecting a pipe; see if they have right clicked a endpoint display, then see if we can start a connection
            val targetEntity = getTargetEntity(player, 1.5F * FluidEndpointDisplay.distanceFromFluidPointCenterToCorner)
            if (targetEntity != null) {
                if (tryStartConnection(player, targetEntity)) {
                    return
                }
            }

            // Player is not yet connecting a pipe; see if they have right clicked a block, then see if we can start a connection
            if (block != null && action == Action.RIGHT_CLICK_BLOCK) {
                if (!tryStartConnection(player, block)) {
                    tryStartConnection(player, block.getRelative(event.blockFace))
                }
            }
        }
    }

    private fun tryStartConnection(player: Player, block: Block): Boolean {
        val rebarBlock = BlockStorage.get(block)

        if (rebarBlock is FluidIntersectionMarker) {
            if (rebarBlock.pipe == this) {
                // This pipe matches the pipe we right clicked; start a connection
                FluidPipePlacementService.startConnection(player, FluidPipePlacementPoint.PointDisplay(rebarBlock.fluidIntersectionDisplay), this)
            } else {
                // This pipe does not match the pipe we right clicked
                player.sendActionBar(Component.translatable("rebar.message.pipe.not_of_same_type"))
            }
            return true
        }

        if (rebarBlock is FluidSectionMarker) {
            if (rebarBlock.pipeDisplay!!.pipe == this) {
                // This pipe matches the pipe we right clicked; start a connection
                FluidPipePlacementService.startConnection(player, FluidPipePlacementPoint.Section(rebarBlock), this)
            } else {
                // This pipe does not match the pipe we right clicked
                player.sendActionBar(Component.translatable("rebar.message.pipe.not_of_same_type"))
            }
            return true
        }

        if (block.type.isAir()) {
            FluidPipePlacementService.startConnection(player, FluidPipePlacementPoint.EmptyBlock(BlockPosition(block)), this)
            return true
        }

        return false
    }

    private fun tryStartConnection(player: Player, entity: Entity): Boolean {
        val hitRebarEntity = EntityStorage.get(entity)
        if (hitRebarEntity is FluidPointDisplay) {
            if (hitRebarEntity.connectedPipeDisplays.isEmpty()) {
                FluidPipePlacementService.startConnection(player, FluidPipePlacementPoint.PointDisplay(hitRebarEntity), this)
                return true
            }
        }
        return false
    }
}