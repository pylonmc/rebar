package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.block.RebarBlockSchema
import io.github.pylonmc.rebar.datatypes.NamespacedKeyPersistentDataType
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.entity.RebarEntity
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.position.position
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.block.Block
import org.bukkit.entity.FallingBlock
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.bukkit.event.entity.EntityDropItemEvent
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import java.util.*

/**
 * Interface meant to be used for all Rebar blocks affected by gravity, like sand, gravel etc.
 *
 * If you implement this interface, and the block can't fall, then the methods will never be called.
 *
 * Beware of how you modify the passed data in the entity, because of the order of serialization and deserialization
 * some modification need to be applied directly to the PDC stored in the entity, or they will be lost.
 *
 * Also at some steps, the entity or the block might not exist yet in their relative storage,
 * handle this interface with caution as it needs to handle state using internals most of the time.
 *
 * As a suggestion, don't make important blocks with lots of data affected by gravity,
 * or it might become a nightmare to apply the correct changes.
 *
 * Note: Currently unlike most RebarHandler's all of [FallingRebarBlockHandler] methods are processed ONLY
 * on [EventPriority.NORMAL], you cannot use the [MultiHandler] annotation on these methods.
 */
interface FallingRebarBlockHandler {
    /**
     * When calling this, the entity doesn't exist yet in [EntityStorage]
     * Called after serialization
     *
     * Can also be used to change the fallback item on deletion
     */
    fun onFallStart(event: EntityChangeBlockEvent, spawnedEntity: RebarFallingBlockEntity) {}

    /**
     * Called after deserialization
     * Cancelling the event at this step does nothing, and the entity is about to be removed
     */
    fun onFallStop(event: EntityChangeBlockEvent, entity: RebarFallingBlockEntity) {}

    /**
     * When called the block doesn't exist in the world and in [BlockStorage]
     * @return the item to drop from this falling block
     */
    fun onFallDropItem(event: EntityDropItemEvent, entity: RebarFallingBlockEntity) = RebarRegistry.ITEMS[(this as RebarBlock).key]?.getItemStack()

    class RebarFallingBlockEntity : RebarEntity<FallingBlock> {
        val fallStartPosition: BlockPosition
        val blockSchema: RebarBlockSchema
        val blockData: PersistentDataContainer

        constructor(blockSchema: RebarBlockSchema, blockData: PersistentDataContainer, fallingStart: BlockPosition, entity: FallingBlock) : super(KEY, entity) {
            this.blockSchema = blockSchema
            this.blockData = blockData
            this.fallStartPosition = fallingStart
        }

        constructor(entity: FallingBlock) : super(entity) {
            val pdc = entity.persistentDataContainer

            val fallingBlockType = pdc.get(fallingBlockTypeKey, NamespacedKeyPersistentDataType)!!
            this.blockSchema = RebarRegistry.BLOCKS[fallingBlockType]!!
            this.blockData = pdc.get(fallingBlockDataKey, RebarSerializers.TAG_CONTAINER)!!
            this.fallStartPosition = pdc.get(fallingBlockStartPositionKey, RebarSerializers.BLOCK_POSITION)!!
        }

        override fun write(pdc: PersistentDataContainer) {
            pdc.set(fallingBlockTypeKey, NamespacedKeyPersistentDataType, blockSchema.key)
            pdc.set(fallingBlockDataKey, RebarSerializers.TAG_CONTAINER, blockData)
            pdc.set(fallingBlockStartPositionKey, RebarSerializers.BLOCK_POSITION, fallStartPosition)
        }

        fun fallbackItem() : ItemStack? {
            return this.entity.persistentDataContainer.get(fallingBlockFallbackItemKey, RebarSerializers.ITEM_STACK) ?: RebarRegistry.ITEMS[blockSchema.key]?.getItemStack()
        }
    }

    companion object : Listener {
        // TODO: decide case convention, all other entity keys are all caps like this but we do not consistently do screaming snake case VS camel case for companion object fields
        val KEY = rebarKey("falling_rebar_block")

        val fallingBlockDataKey = rebarKey("falling_rebar_block_data")

        val fallingBlockTypeKey = rebarKey("falling_rebar_block_type")

        val fallingBlockStartPositionKey = rebarKey("falling_rebar_block_start")

        val fallingBlockFallbackItemKey = rebarKey("fallback_item")

        private val fallMap = HashMap<UUID, Pair<FallingRebarBlockHandler, RebarFallingBlockEntity>>()

        @EventHandler(ignoreCancelled = true)
        private fun entityBlockChange(event: EntityChangeBlockEvent) {
            val entity = event.entity
            if (entity !is FallingBlock) return

            val block = event.block
            if (!entity.isInWorld) {
                handleFallStart(block, event, entity)
            } else {
                handleFallStop(block, event, entity)
            }
        }

        private fun handleFallStop(block: Block, event: EntityChangeBlockEvent, entity: FallingBlock) {
            val rebarEntity = EntityStorage.get(entity) as? RebarFallingBlockEntity

            // falling onto another pylon block
            if (BlockStorage.get(block) != null) {
                val drop = if (rebarEntity == null) {
                    ItemStack.of(entity.blockData.material)
                } else {
                    rebarEntity.fallbackItem()
                }

                if (drop != null) {
                    entity.world.dropItemNaturally(entity.location, drop)
                }

                entity.remove()
                event.isCancelled = true
                return
            }

            // if everything is valid, place the block
            rebarEntity ?: return
            val rebarBlock = BlockStorage.loadBlock(
                block.position,
                rebarEntity.blockSchema,
                rebarEntity.blockData
            ) as FallingRebarBlockHandler

            rebarBlock.onFallStop(event, rebarEntity)
        }

        private fun handleFallStart(block: Block, event: EntityChangeBlockEvent, entity: FallingBlock) {
            val rebarBlock = BlockStorage.get(block) ?: return
            val rebarFallingBlock = rebarBlock as? FallingRebarBlockHandler
            if (rebarFallingBlock == null) {
                event.isCancelled = true
                return
            }

            val blockPdc = RebarBlock.serialize(rebarBlock, block.chunk.persistentDataContainer.adapterContext) ?: return
            val fallingEntity = RebarFallingBlockEntity(
                rebarBlock.schema,
                blockPdc,
                block.position,
                entity
            )

            rebarFallingBlock.onFallStart(event, fallingEntity)

            if (event.isCancelled) return

            BlockStorage.deleteBlock(block.position)
            EntityStorage.add(fallingEntity)
            // save this here as the entity storage is going to nuke it if the item drops
            fallMap[entity.uniqueId] = Pair(rebarFallingBlock, fallingEntity)
        }

        @EventHandler
        private fun entityDespawn(event: EntityRemoveEvent) {
            // DESPAWN = Fell and created block ; OUT_OF_WORLD = Fell and dropped item
            if (event.cause != EntityRemoveEvent.Cause.DESPAWN || event.entity !is FallingBlock) return
            fallMap.remove(event.entity.uniqueId)
        }

        @EventHandler(ignoreCancelled = true)
        private fun fallingBlockDrop(event: EntityDropItemEvent) {
            val entity = event.entity
            if (entity !is FallingBlock) return

            val (rebarFallingBlock, rebarFallingEntity) = fallMap[entity.uniqueId] ?: return
            fallMap.remove(entity.uniqueId)

            val droppedItem = rebarFallingBlock.onFallDropItem(event, rebarFallingEntity)
            if (event.isCancelled) return
            if (droppedItem == null) {
                event.isCancelled = true
                return
            }

            event.itemDrop.itemStack = droppedItem
        }
    }
}
