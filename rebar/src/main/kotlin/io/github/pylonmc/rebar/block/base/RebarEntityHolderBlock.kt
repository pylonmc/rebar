package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.PhantomBlock
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.entity.RebarEntity
import io.github.pylonmc.rebar.event.RebarBlockBreakEvent
import io.github.pylonmc.rebar.event.RebarBlockDeserializeEvent
import io.github.pylonmc.rebar.event.RebarBlockSerializeEvent
import io.github.pylonmc.rebar.event.RebarBlockUnloadEvent
import io.github.pylonmc.rebar.util.position.position
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Bukkit
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.persistence.PersistentDataContainer
import org.jetbrains.annotations.ApiStatus
import java.util.IdentityHashMap
import java.util.UUID
import java.util.function.Consumer

/**
 * A block that has one or more associated Rebar entities. For example, a pedestal that
 * uses an item display to show the item would implement this to keep track of the
 * item display.
 *
 * Note that the Rebar entities may not be loaded at the same time that the block is loaded.
 */
interface RebarEntityHolderBlock {
    val block: Block

    @get:ApiStatus.NonExtendable
    val heldEntities: MutableMap<String, UUID>
        get() = holders.getOrPut(this) { mutableMapOf() }

    fun addEntity(name: String, entity: Entity) {
        heldEntities[name] = entity.uniqueId
        entity.persistentDataContainer.set(blockKey, RebarSerializers.BLOCK_POSITION, block.position)
    }

    fun addEntity(name: String, entity: RebarEntity<*>)
        = addEntity(name, entity.entity)

    fun tryRemoveEntity(name: String) {
        val uuid = heldEntities[name] ?: return
        Bukkit.getEntity(uuid)?.remove()
    }

    fun tryRemoveAllEntities() {
        val list = heldEntities.values.toList()
        list.forEach {
            Bukkit.getEntity(it)?.let { if (it.isValid) it.remove() }
        }
    }

    @ApiStatus.NonExtendable
    fun getHeldEntityUuid(name: String) = heldEntities[name]

    @ApiStatus.NonExtendable
    fun getHeldEntityUuidOrThrow(name: String) = getHeldEntityUuid(name)
        ?: throw IllegalArgumentException("Entity $name not found")

    @ApiStatus.NonExtendable
    fun getHeldEntity(name: String): Entity?
            = getHeldEntityUuid(name)?.let { Bukkit.getEntity(it) }

    @ApiStatus.NonExtendable
    fun getHeldEntityOrThrow(name: String): Entity
            = getHeldEntity(name)
        ?: throw IllegalArgumentException("Entity $name does not exist")

    @ApiStatus.NonExtendable
    fun <T: Entity> getHeldEntity(clazz: Class<T>, name: String): T? {
        val entity = getHeldEntity(name)
        if (!clazz.isInstance(entity)) {
            return null
        }
        return clazz.cast(entity)
    }

    @ApiStatus.NonExtendable
    fun <T: Entity> getHeldEntityOrThrow(clazz: Class<T>, name: String): T
            = getHeldEntity(clazz, name)
        ?: throw IllegalArgumentException("Entity $name does not exist or is not of type ${clazz.simpleName}")

    @ApiStatus.NonExtendable
    fun <T: RebarEntity<*>> getHeldRebarEntity(clazz: Class<T>, name: String): T?
            = getHeldEntityUuid(name)?.let { EntityStorage.getAs(clazz, it) }

    @ApiStatus.NonExtendable
    fun <T: RebarEntity<*>> getHeldRebarEntityOrThrow(clazz: Class<T>, name: String): T
            = EntityStorage.getAs(clazz, getHeldEntityUuidOrThrow(name))
        ?: throw IllegalArgumentException("Entity $name is not of type ${clazz.simpleName}")

    @ApiStatus.NonExtendable
    fun whenHeldRebarEntityLoads(name: String, consumer: Consumer<RebarEntity<*>>) {
        EntityStorage.whenEntityLoads(getHeldEntityUuidOrThrow(name), consumer)
    }

    @ApiStatus.NonExtendable
    fun <T: RebarEntity<*>> whenHeldRebarEntityLoads(clazz: Class<T>, name: String, consumer: Consumer<T>) {
        EntityStorage.whenEntityLoads(getHeldEntityUuidOrThrow(name), clazz, consumer)
    }

    /**
     * Returns false if the block holds no entity with the provided name, the entity is unloaded or does not
     * physically exist.
     */
    @ApiStatus.NonExtendable
    fun isHeldEntityPresent(name: String) = getHeldEntityUuid(name) != null

    /**
     * Returns false if any entity is unloaded or does not exist.
     */
    @ApiStatus.NonExtendable
    fun areAllHeldEntitiesLoaded() = heldEntities.keys.all { isHeldEntityPresent(it) }

    @ApiStatus.Internal
    companion object : Listener {
        @JvmStatic
        val entityKey = rebarKey("entity_holder_entity_uuids")
        @JvmStatic
        val blockKey = rebarKey("entity_holder_block")
        @JvmStatic
        val entityType = RebarSerializers.MAP.mapTypeFrom(RebarSerializers.STRING, RebarSerializers.UUID)
        @JvmStatic
        val debugEntityType = RebarSerializers.MAP.mapTypeFrom(RebarSerializers.STRING, RebarSerializers.TAG_CONTAINER)

        @JvmSynthetic
        internal val holders = IdentityHashMap<RebarEntityHolderBlock, MutableMap<String, UUID>>()

        @EventHandler
        private fun onDeserialize(event: RebarBlockDeserializeEvent) {
            val block = event.rebarBlock
            if (block !is RebarEntityHolderBlock) return
            holders[block] = event.pdc.get(entityKey, entityType)?.toMutableMap()
                ?: error("Held entities not found for ${block.key}")
        }

        @EventHandler
        private fun onSerialize(event: RebarBlockSerializeEvent) {
            val block = event.rebarBlock
            if (block !is RebarEntityHolderBlock) return
            if (event.debug) {
                val debugMap = mutableMapOf<String, PersistentDataContainer>()
                holders[block]?.forEach { (name, uuid) ->
                    val entityPdc = event.pdc.adapterContext.newPersistentDataContainer()
                    val entity = EntityStorage.get(uuid)
                    if (entity != null) {
                        entity.writeDebugInfo(entityPdc)
                    } else {
                        entityPdc.set(rebarKey("rebar_entity_not_found"), RebarSerializers.BOOLEAN, true)
                    }
                    debugMap[name] = entityPdc
                }
                event.pdc.set(entityKey, debugEntityType, debugMap)
            } else {
                holders[block]?.let { event.pdc.set(entityKey, entityType, it) }
            }
        }

        @EventHandler
        private fun onUnload(event: RebarBlockUnloadEvent) {
            val block = event.rebarBlock
            if (block !is RebarEntityHolderBlock) return
            holders.remove(block)
        }

        @EventHandler
        private fun onBreak(event: RebarBlockBreakEvent) {
            val block = event.rebarBlock
            if (block is RebarEntityHolderBlock) {
                // Best-effort removal; unlikely to cause issues
                block.tryRemoveAllEntities()
                holders.remove(block)
            } else if (block is PhantomBlock) {
                block.pdc.get(entityKey, entityType)?.values?.forEach {
                    Bukkit.getEntity(it)?.let { if (it.isValid) it.remove() }
                }
            }
        }

        @EventHandler
        private fun onEntityRemove(event: EntityRemoveEvent) {
            if (event.cause == EntityRemoveEvent.Cause.UNLOAD || event.cause == EntityRemoveEvent.Cause.PLAYER_QUIT) return
            val blockPos = event.entity.persistentDataContainer.get(blockKey, RebarSerializers.BLOCK_POSITION) ?: return
            val block = BlockStorage.get(blockPos) as? RebarEntityHolderBlock ?: return
            holders[block]?.entries?.removeIf { it.value == event.entity.uniqueId }
        }
    }
}