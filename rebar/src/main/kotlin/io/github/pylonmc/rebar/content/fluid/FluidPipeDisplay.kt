package io.github.pylonmc.rebar.content.fluid

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.entity.RebarEntity
import io.github.pylonmc.rebar.entity.display.ItemDisplayBuilder
import io.github.pylonmc.rebar.entity.display.transform.LineBuilder
import io.github.pylonmc.rebar.fluid.FluidManager
import io.github.pylonmc.rebar.fluid.placement.FluidPipePlacementService
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.GameMode
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import java.util.UUID

/**
 * A display that visually represents a pipe.
 */
class FluidPipeDisplay : RebarEntity<ItemDisplay> {
    val fromDisplay: UUID
    val toDisplay: UUID
    val pipe: FluidPipe
    val pipeAmount: Int

    constructor(
        pipe: FluidPipe, pipeAmount: Int, from: FluidPointDisplay, to: FluidPointDisplay
    ) : super(KEY, makeDisplay(pipe, pipeAmount, from, to)) {
        this.pipe = pipe
        this.pipeAmount = pipeAmount
        this.fromDisplay = from.uuid
        this.toDisplay = to.uuid
        EntityStorage.add(this)
    }

    @Suppress("unused")
    constructor(entity: ItemDisplay) : super(entity) {
        val pdc = entity.persistentDataContainer

        this.fromDisplay = pdc.get(FROM_DISPLAY_KEY, RebarSerializers.UUID)!!
        this.toDisplay = pdc.get(TO_DISPLAY_KEY, RebarSerializers.UUID)!!

        // will fail to load if schema not found; no way around this
        val pipeSchema = pdc.get(PIPE_KEY, PIPE_TYPE)!!
        pipe = RebarItem.fromStack(pipeSchema.getItemStack()) as FluidPipe

        this.pipeAmount = pdc.get(PIPE_AMOUNT_KEY, RebarSerializers.INTEGER)!!

        // When fluid points are loaded back, their segment's fluid per second and predicate won't be preserved, so
        // we wait for them to load and then set their segments' fluid per second and predicate
        EntityStorage.whenEntityLoads<FluidPointDisplay>(fromDisplay) { display ->
            FluidManager.setFluidPerSecond(display.point.segment, pipe.fluidPerSecond)
            FluidManager.setFluidPredicate(display.point.segment, pipe::canPass)
        }

        // Technically only need to do this for one of the end points since they're part of the same segment, but
        // we do it twice just to be safe
        EntityStorage.whenEntityLoads<FluidPointDisplay>(toDisplay) { display ->
            FluidManager.setFluidPerSecond(display.point.segment, pipe.fluidPerSecond)
            FluidManager.setFluidPredicate(display.point.segment, pipe::canPass)
        }
    }

    override fun write(pdc: PersistentDataContainer) {
        pdc.set(FROM_DISPLAY_KEY, RebarSerializers.UUID, fromDisplay)
        pdc.set(TO_DISPLAY_KEY, RebarSerializers.UUID, toDisplay)
        pdc.set(PIPE_KEY, PIPE_TYPE, pipe.schema)
        pdc.set(PIPE_AMOUNT_KEY, RebarSerializers.INTEGER, pipeAmount)
    }

    override fun writeDebugInfo(pdc: PersistentDataContainer) {
        super.writeDebugInfo(pdc)
        pdc.set(rebarKey("display_item"), RebarSerializers.ITEM_STACK_READABLE, entity.itemStack)
    }

    fun getFrom(): FluidPointDisplay
        = EntityStorage.getAs<FluidPointDisplay>(fromDisplay)!!

    fun getTo(): FluidPointDisplay
        = EntityStorage.getAs<FluidPointDisplay>(toDisplay)!!

    fun delete(player: Player?, drops: MutableList<ItemStack>?) {
        if (!entity.isValid) {
            // already deleted
            return
        }

        val from = getFrom()
        val to = getTo()

        val itemToGive = pipe.stack.clone()
        itemToGive.amount = pipeAmount
        if (player != null) {
            if (player.gameMode != GameMode.CREATIVE) {
                player.give(itemToGive)
            }
        } else if (drops != null) {
            drops.add(itemToGive)
        } else {
            val location = to.point.position.plus(from.point.position).location.multiply(0.5)
            location.getWorld().dropItemNaturally(location, itemToGive)
        }

        FluidPipePlacementService.disconnect(from, to, true)
    }

    companion object {

        val KEY = rebarKey("fluid_pipe_display")
        const val SIZE = 0.1

        private val PIPE_AMOUNT_KEY = rebarKey("pipe_amount")
        private val PIPE_KEY = rebarKey("pipe")
        private val PIPE_TYPE = RebarSerializers.KEYED.keyedTypeFrom { RebarRegistry.ITEMS.getOrThrow(it) }
        private val FROM_DISPLAY_KEY = rebarKey("from_display")
        private val TO_DISPLAY_KEY = rebarKey("to_display")

        private fun makeDisplay(pipe: FluidPipe, pipeAmount: Int, from: FluidPointDisplay, to: FluidPointDisplay): ItemDisplay {
            val height = from.entity.height
            val fromLocation = from.entity.location.add(0.0, height / 2.0, 0.0)
            val toLocation = to.entity.location.add(0.0, height / 2.0, 0.0)
            // We use a center location rather than just spawning at fromLocation or toLocation to prevent the entity
            // from being spawned just inside a block - this causes it to render as black due to being inside the block
            val centerLocation = fromLocation.clone().add(toLocation).multiply(0.5)
            val fromOffset = centerLocation.clone().subtract(fromLocation).toVector().toVector3f()
            val toOffset = centerLocation.clone().subtract(toLocation).toVector().toVector3f()

            return ItemDisplayBuilder()
                .transformation(LineBuilder()
                    .from(fromOffset)
                    .to(toOffset)
                    .thickness(SIZE)
                    .build()
                    .buildForItemDisplay()
                )
                .itemStack(ItemStackBuilder.of(pipe.material)
                    .addCustomModelDataString("fluid_pipe_display:${pipe.key.key}")
                    .addCustomModelDataString("fluid_pipe_length:${pipeAmount}")
                )
                .build(centerLocation)
        }
    }
}
