package io.github.pylonmc.rebar.entity

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.Config
import io.github.pylonmc.rebar.config.Settings
import io.github.pylonmc.rebar.content.debug.DebugWaxedWeatheredCutCopperStairs
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.RebarEntity.Companion.register
import io.github.pylonmc.rebar.event.RebarEntitySerializeEvent
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.waila.WailaDisplay
import org.bukkit.NamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.persistence.PersistentDataContainer


/**
 * Represents a Rebar entity in the world.
 *
 * All custom Rebar entities extend this class. Every instance of this class is wrapping a real entity
 * in the world, and is stored in [EntityStorage]. All new block *types* must be registered using [register],
 * and all new Rebar entities must be added to [EntityStorage] with [EntityStorage.add].
 *
 * You are responsible for creating your Rebar entities; there are no place constructors as with
 * Rebar blocks. This is because it doesn't make sense for Rebar to manage spawning entities. However, your
 * entity must still have a load constructor that takes a single parameter of type [E].
 */
abstract class RebarEntity<out E: Entity>(val entity: E) {

    val key = entity.persistentDataContainer.get(rebarEntityKeyKey, RebarSerializers.NAMESPACED_KEY)
        ?: throw IllegalStateException("Entity did not have a Rebar key; did you mean to call RebarEntity(NamespacedKey, Entity) instead of RebarEntity(Entity)?")
    val schema = RebarRegistry.ENTITIES.getOrThrow(key)
    val uuid = entity.uniqueId

    constructor(key: NamespacedKey, entity: E): this(initialiseRebarEntity<E>(key, entity))

    /**
     * WAILA is the text that shows up when looking at a block to tell you what the block is. It
     * can also be used for entities.
     *
     * This will only be called for the player if the player has WAILA enabled.
     *
     * @return the WAILA configuration, or null if WAILA should not be shown for this block.
     */
    open fun getWaila(player: Player): WailaDisplay? = null

    /**
     * Called when debug info is requested for the entity by someone
     * using the [DebugWaxedWeatheredCutCopperStairs]. If there is
     * any transient data that can be useful for debugging, you're
     * encouraged to save it here.
     *
     * Defaults to a normal [write] call.
     */
    open fun writeDebugInfo(pdc: PersistentDataContainer) = write(pdc)

    /**
     * Called when the entity is saved.
     *
     * Put any logic to save the data in the entity here.
     *
     * *Do not assume that when this is called, the entity is being unloaded.* This
     * may be called for other reasons, such as when a player right clicks with
     * [DebugWaxedWeatheredCutCopperStairs]. Instead, use [onUnload]
     */
    open fun write(pdc: PersistentDataContainer) {}

    /**
     * Called when the entity is unloaded, not including when it is deleted.
     */
    open fun onUnload() {}

    /**
     * Returns settings associated with the block.
     *
     * Shorthand for `Settings.get(getKey())`
     */
    fun getSettings(): Config
            = Settings.get(key)

    companion object {

        val rebarEntityKeyKey = rebarKey("rebar_entity_key")

        @JvmOverloads
        @JvmStatic
        fun register(
            key: NamespacedKey,
            entityClass: Class<*>,
            rebarEntityClass: Class<out RebarEntity<*>>,
            isPersistent: Boolean = true,
        ) {
            RebarRegistry.ENTITIES.register(RebarEntitySchema(key, entityClass, rebarEntityClass, isPersistent))
        }

        @JvmSynthetic
        inline fun <reified E: Entity, reified T: RebarEntity<E>> register(key: NamespacedKey, isPersistent: Boolean = true) {
            RebarRegistry.ENTITIES.register(RebarEntitySchema(key, E::class.java, T::class.java, isPersistent))
        }

        @JvmSynthetic
        internal fun <E: Entity> initialiseRebarEntity(key: NamespacedKey, entity: E): E {
            entity.persistentDataContainer.set(rebarEntityKeyKey, RebarSerializers.NAMESPACED_KEY, key)
            return entity
        }

        @JvmSynthetic
        internal fun serialize(rebarEntity: RebarEntity<*>) {
            rebarEntity.write(rebarEntity.entity.persistentDataContainer)
            RebarEntitySerializeEvent(rebarEntity.entity, rebarEntity, rebarEntity.entity.persistentDataContainer).callEvent()
        }

        @JvmSynthetic
        internal fun deserialize(entity: Entity): RebarEntity<*>? {
            // Stored outside of the try block so it is displayed in error messages once acquired
            var key: NamespacedKey? = null

            try {
                key = entity.persistentDataContainer.get(rebarEntityKeyKey, RebarSerializers.NAMESPACED_KEY)
                    ?: return null

                // We fail silently here because this may trigger if an addon is removed or fails to load.
                // In this case, we don't want to delete the data, and we also don't want to spam errors.
                val schema = RebarRegistry.ENTITIES[key]
                    ?: return null

                if (!schema.entityClass.isInstance(entity) || schema.loadConstructor == null) {
                    return null
                }

                @Suppress("UNCHECKED_CAST") // The cast will work - this is checked in the schema constructor
                val rebarEntity = schema.loadConstructor.invoke(entity) as RebarEntity<*>
                RebarEntitySerializeEvent(entity, rebarEntity, entity.persistentDataContainer).callEvent()
                return rebarEntity
            } catch (t: Throwable) {
                Rebar.logger.severe("Error while loading entity $key with UUID ${entity.uniqueId}")
                t.printStackTrace()
                return null
            }
        }
    }
}