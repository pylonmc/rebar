package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.entity.RebarEntity
import io.github.pylonmc.rebar.entity.display.BlockDisplayBuilder
import io.github.pylonmc.rebar.entity.display.ItemDisplayBuilder
import io.github.pylonmc.rebar.entity.display.transform.TransformBuilder
import io.github.pylonmc.rebar.event.RebarBlockLoadEvent
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.rebarKey
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import org.bukkit.Color
import org.bukkit.NamespacedKey
import org.bukkit.block.Block
import org.bukkit.block.data.BlockData
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Entity
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.util.Vector
import org.joml.Vector3i
import java.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A wrapper over [RebarEntityHolderBlock] which allows for 'ghost blocks' (like those used
 * to show how to build a multiblock) to be managed. Each ghost block has a relative position
 * and can display vanilla blocks, rebar blocks, or both.
 *
 * Due to some annoying technicalities, vanilla blocks need to be displayed as a [BlockDisplay]
 * and Rebar items as an [ItemDisplay]. So we operate a somewhat cursed system where either a
 * BlockDisplay, ItemDisplay, or both are spawned. In the case where both are spawned, one of
 * the displays is hidden while the other is shown as all the possible states are cycled
 * through.
 *
 * You can access the individual displays with [getVanillaGhostBlockDisplay] and
 * [getRebarGhostBlockDisplay]. Keep in mind that any given position may have none, one, or both
 * displays.
 */
interface RebarGhostBlockHolder : RebarEntityHolderBlock {

    open class GhostBlock<E: Entity> : RebarEntity<E> {
        val position: Vector3i

        constructor(entity: E) : super(entity) {
            position = entity.persistentDataContainer.get(POSITION_KEY, RebarSerializers.VECTOR3I)!!
        }

        constructor(key: NamespacedKey, entity: E, position: Vector3i) : super(key, entity) {
            this.position = position
        }

        override fun write(pdc: PersistentDataContainer) {
            pdc.set(POSITION_KEY, RebarSerializers.VECTOR3I, position)
        }

        companion object {
            val POSITION_KEY = rebarKey("ghost_block_block_position")
        }
    }

    class VanillaGhostBlock : GhostBlock<BlockDisplay> {

        val vanillaBlocks: MutableList<BlockData>

        constructor(entity: BlockDisplay) : super(entity) {
            vanillaBlocks = entity.persistentDataContainer.get(VANILLA_BLOCKS_KEY, VANILLA_BLOCKS_TYPE)!!
        }

        constructor(ghostBlockHolder: Block, position: Vector3i, blockDatas: MutableList<BlockData>) : super(
            KEY,
            BlockDisplayBuilder()
                .blockData(blockDatas.first())
                .glow(Color.WHITE)
                .transformation(TransformBuilder().scale(0.501))
                .build(ghostBlockHolder.location.toCenterLocation().add(Vector.fromJOML(position))),
            position
        ) {
            this.vanillaBlocks = blockDatas
        }

        override fun write(pdc: PersistentDataContainer) {
            pdc.set(VANILLA_BLOCKS_KEY, VANILLA_BLOCKS_TYPE, vanillaBlocks)
        }

        fun setSize(size: Double) {
            entity.setTransformationMatrix(TransformBuilder().scale(size).buildForBlockDisplay())
        }

        fun setIndex(i: Int) {
            entity.block = vanillaBlocks[i]
        }

        companion object {
            val KEY = rebarKey("vanilla_ghost_block")
            val VANILLA_BLOCKS_KEY = rebarKey("vanilla_ghost_block_vanilla_blocks")
            val VANILLA_BLOCKS_TYPE = RebarSerializers.LIST.listTypeFrom(RebarSerializers.BLOCK_DATA)
        }
    }

    class RebarGhostBlock : GhostBlock<ItemDisplay> {

        val rebarBlocks: MutableList<NamespacedKey>

        constructor(entity: ItemDisplay) : super(entity) {
            rebarBlocks = entity.persistentDataContainer.get(REBAR_BLOCKS_KEY, VANILLA_BLOCKS_TYPE)!!
        }

        constructor(ghostBlockHolder: Block, position: Vector3i, items: MutableList<NamespacedKey>) : super(
            KEY,
            ItemDisplayBuilder()
                .itemStack(RebarRegistry.ITEMS.getOrThrow(items.first()).getItemStack())
                .glow(Color.WHITE)
                .transformation(TransformBuilder().scale(0.501))
                .build(ghostBlockHolder.location.toCenterLocation().add(Vector.fromJOML(position))),
            position
        ) {
            this.rebarBlocks = items
        }

        override fun write(pdc: PersistentDataContainer) {
            pdc.set(REBAR_BLOCKS_KEY, VANILLA_BLOCKS_TYPE, rebarBlocks)
        }

        fun setSize(size: Double) {
            entity.setTransformationMatrix(TransformBuilder().scale(size).buildForItemDisplay())
        }

        fun setIndex(i: Int) {
            entity.setItemStack(RebarRegistry.ITEMS.getOrThrow(rebarBlocks[i]).getItemStack())
        }

        companion object {
            val KEY = rebarKey("rebar_ghost_block")
            val REBAR_BLOCKS_KEY = rebarKey("rebar_ghost_block_rebar_blocks")
            val VANILLA_BLOCKS_TYPE = RebarSerializers.LIST.listTypeFrom(RebarSerializers.NAMESPACED_KEY)
        }
    }

    fun addGhostBlock(position: Vector3i, vanillaBlocks: List<BlockData>, rebarBlocks: List<NamespacedKey>) {
        check(!hasGhostBlockAt(position)) { "There is already a ghost block at $position" }

        if (!vanillaBlocks.isEmpty()) {
            val entity = VanillaGhostBlock(block, position, vanillaBlocks.toMutableList())
            EntityStorage.add(entity)
            addEntity(getVanillaGhostBlockName(position), entity)
        }

        if (!rebarBlocks.isEmpty()) {
            val entity = RebarGhostBlock(block, position, rebarBlocks.toMutableList())
            EntityStorage.add(entity)
            addEntity(getRebarGhostBlockName(position), entity)
        }

        startCycleTask(position)
    }

    fun removeGhostBlock(position: Vector3i) {
        getHeldEntity(getVanillaGhostBlockName(position))?.remove()
        getHeldEntity(getRebarGhostBlockName(position))?.remove()
    }

    fun hasGhostBlockAt(position: Vector3i)
            = getHeldEntity(getVanillaGhostBlockName(position)) != null
            || getHeldEntity(getRebarGhostBlockName(position)) != null

    fun getVanillaGhostBlockDisplay(position: Vector3i)
            = getHeldRebarEntity(VanillaGhostBlock::class.java, getVanillaGhostBlockName(position))

    fun getRebarGhostBlockDisplay(position: Vector3i)
            = getHeldRebarEntity(RebarGhostBlock::class.java, getRebarGhostBlockName(position))

    private fun startCycleTask(position: Vector3i) {
        val vanillaGhostBlock = getVanillaGhostBlockDisplay(position)
        val rebarGhostBlock = getRebarGhostBlockDisplay(position)
        check(vanillaGhostBlock != null || rebarGhostBlock != null)

        val updateTasks = mutableListOf<Runnable>()
        if (vanillaGhostBlock != null) {
            for (i in 0..<vanillaGhostBlock.vanillaBlocks.size) {
                if (i == 0 && rebarGhostBlock != null) {
                    updateTasks.add {
                        rebarGhostBlock.entity.setTransformationMatrix(TransformBuilder().scale(0.499).buildForItemDisplay())
                        vanillaGhostBlock.entity.setTransformationMatrix(TransformBuilder().scale(0.501).buildForBlockDisplay())
                        vanillaGhostBlock.setIndex(i)
                    }
                } else {
                    updateTasks.add {
                        vanillaGhostBlock.setIndex(i)
                    }
                }
            }
        }
        if (rebarGhostBlock != null) {
            for (i in 0..<rebarGhostBlock.rebarBlocks.size) {
                if (i == 0 && vanillaGhostBlock != null) {
                    updateTasks.add {
                        vanillaGhostBlock.setSize(0.499)
                        rebarGhostBlock.setSize(0.501)
                        rebarGhostBlock.setIndex(i)
                    }
                } else {
                    updateTasks.add {
                        rebarGhostBlock.setIndex(i)
                    }
                }
            }
        }

        if (updateTasks.size <= 1) {
            return
        }

        var i = 0
        Rebar.scope.launch(Rebar.mainThreadDispatcher) {
            while (true) {
                if (vanillaGhostBlock != null && !vanillaGhostBlock.entity.isValid
                    || rebarGhostBlock != null && !rebarGhostBlock.entity.isValid
                ) {
                    break
                }

                updateTasks[i].run()
                i++
                i %= updateTasks.size

                delay(Duration.ofMillis((RebarConfig.GHOST_BLOCK_TICK_INTERVAL * 1000 / 20).toLong()))
            }
        }
    }

    companion object : Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        fun onLoad(event: RebarBlockLoadEvent) {
            val block = event.rebarBlock
            if (block !is RebarGhostBlockHolder) {
                return
            }

            for (name in block.heldEntities.keys) {
                val entity = block.getHeldRebarEntity(name)
                if (entity !is GhostBlock) {
                    continue
                }
                val vanilla = block.getHeldRebarEntity(VanillaGhostBlock::class.java, getVanillaGhostBlockName(entity.position))
                val rebar = block.getHeldRebarEntity(RebarGhostBlock::class.java, getRebarGhostBlockName(entity.position))
                if (vanilla != null && rebar != null) {
                    if (entity is VanillaGhostBlock) { // avoid starting duplicate tasks
                        block.startCycleTask(entity.position)
                    }
                } else {
                    block.startCycleTask(entity.position)
                }
            }
        }

        private fun getVanillaGhostBlockName(position: Vector3i) = "vanilla_ghost_block_${position.x}_${position.y}_${position.z}"

        private fun getRebarGhostBlockName(position: Vector3i) = "rebar_ghost_block_${position.x}_${position.y}_${position.z}"
    }
}