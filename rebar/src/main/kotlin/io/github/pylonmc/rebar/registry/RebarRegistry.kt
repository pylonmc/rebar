package io.github.pylonmc.rebar.registry

import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.block.RebarBlockSchema
import io.github.pylonmc.rebar.entity.RebarEntitySchema
import io.github.pylonmc.rebar.event.RebarRegisterEvent
import io.github.pylonmc.rebar.event.RebarUnregisterEvent
import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.gametest.GameTestConfig
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.item.RebarItemSchema
import io.github.pylonmc.rebar.item.research.Research
import io.github.pylonmc.rebar.recipe.RecipeType
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.Tag
import java.util.stream.Stream

/**
 * Represents a list of things that can be registered and looked up by [NamespacedKey].
 * This class is not thread safe and any concurrent access must be synchronized externally.
 *
 * @param T the type of the registered values
 * @property key the key of this registry
 */
class RebarRegistry<T : Keyed>(val key: NamespacedKey) : Iterable<T> {

    private val values: MutableMap<NamespacedKey, T> = LinkedHashMap()

    private val keyMapping = mutableMapOf<NamespacedKey, NamespacedKey>()

    fun register(vararg values: T) {
        for (value in values) {
            val key = value.key
            check(key !in this.values) { "Value with key $key is already registered in registry $this" }
            this.values[key] = value
            if (value is RegistryHandler) {
                value.onRegister(this)
            }
            RebarRegisterEvent(this, value).callEvent()
        }
    }

    fun register(tag: Tag<T>) = register(*tag.values.toTypedArray())

    fun unregister(vararg values: T) = unregister(*values.map { it.key }.toTypedArray())

    fun unregister(tag: Tag<T>) = unregister(*tag.values.toTypedArray())

    fun unregister(vararg keys: NamespacedKey) {
        for (key in keys) {
            check(key in this.values) { "Value with key $key is not registered in registry $this" }
            val value = this.values.remove(key)
            if (value is RegistryHandler) {
                value.onUnregister(this)
            }
            RebarUnregisterEvent(this, value!!).callEvent()
        }
    }

    fun unregisterAllFromAddon(addon: RebarAddon) {
        val namespace = addon.key.namespace
        values.keys.removeIf { it.namespace == namespace }
    }

    operator fun get(key: NamespacedKey): T? {
        var key = key
        while (key in keyMapping) {
            key = keyMapping[key]!!
        }
        return values[key]
    }

    fun getOrThrow(key: NamespacedKey): T {
        return get(key) ?: throw NoSuchElementException("No value found for key $key in registry $this")
    }

    fun getOrCreate(key: NamespacedKey, creator: () -> T): T {
        val value = get(key)
        if (value != null) {
            return value
        }
        val newValue = creator()
        register(newValue)
        return newValue
    }

    /**
     * Maps a key to another key, allowing values to be looked up by either key. This is useful for updating
     * keys without breaking existing references. For example, if an item is renamed from "addon:old_item" to
     * "addon:new_item", you can map "addon:old_item" to "addon:new_item" so that both keys will return the same item.
     */
    fun mapKey(from: NamespacedKey, to: NamespacedKey) {
        keyMapping[from] = to
    }

    fun getKeys(): Set<NamespacedKey> {
        return values.keys
    }

    fun getValues(): Collection<T> {
        return values.values
    }

    operator fun contains(key: NamespacedKey): Boolean {
        return values.containsKey(key)
    }

    operator fun contains(tag: Tag<T>): Boolean {
        return tag.values.all { it.key in values }
    }

    override fun iterator(): Iterator<T> {
        return values.values.iterator()
    }

    fun stream(): Stream<T> = values.values.stream()

    override fun equals(other: Any?): Boolean = other is RebarRegistry<*> && key == other.key
    override fun hashCode(): Int = key.hashCode()
    override fun toString(): String = key.toString()

    companion object {
        private val registries: MutableMap<NamespacedKey, RebarRegistry<*>> = mutableMapOf()

        // @formatter:off
        @JvmField val ITEMS_KEY = rebarKey("items")
        @JvmField val BLOCKS_KEY = rebarKey("blocks")
        @JvmField val ENTITIES_KEY = rebarKey("entities")
        @JvmField val FLUIDS_KEY = rebarKey("fluids")
        @JvmField val GAMETESTS_KEY = rebarKey("gametests")
        @JvmField val ADDONS_KEY = rebarKey("addons")
        @JvmField val RECIPE_TYPES_KEY = rebarKey("recipe_types")
        @JvmField val RESEARCHES_KEY = rebarKey("researches")
        @JvmField val ITEM_TAGS_KEY = rebarKey("tags")
        // @formatter:on

        // @formatter:off
        @JvmField val ITEMS = RebarRegistry<RebarItemSchema>(ITEMS_KEY).also(::addRegistry)
        @JvmField val BLOCKS = RebarRegistry<RebarBlockSchema>(BLOCKS_KEY).also(::addRegistry)
        @JvmField val ENTITIES = RebarRegistry<RebarEntitySchema>(ENTITIES_KEY).also(::addRegistry)
        @JvmField val FLUIDS = RebarRegistry<RebarFluid>(FLUIDS_KEY).also(::addRegistry)
        @JvmField val ADDONS = RebarRegistry<RebarAddon>(ADDONS_KEY).also(::addRegistry)
        @JvmField val GAMETESTS = RebarRegistry<GameTestConfig>(GAMETESTS_KEY).also(::addRegistry)
        @JvmField val RECIPE_TYPES = RebarRegistry<RecipeType<*>>(RECIPE_TYPES_KEY).also(::addRegistry)
        @JvmField val RESEARCHES = RebarRegistry<Research>(RESEARCHES_KEY).also(::addRegistry)
        @JvmField val ITEM_TAGS = RebarRegistry<Tag<ItemTypeWrapper>>(ITEM_TAGS_KEY).also(::addRegistry)
        // @formatter:on

        @JvmStatic
        fun <T : Keyed> getRegistry(key: NamespacedKey): RebarRegistry<T> {
            return getRegistryOrNull(key) ?: throw IllegalArgumentException("Registry $key not found")
        }

        @JvmStatic
        fun <T : Keyed> getRegistryOrNull(key: NamespacedKey): RebarRegistry<T>? {
            @Suppress("UNCHECKED_CAST")
            return registries[key] as? RebarRegistry<T>
        }

        @JvmStatic
        fun addRegistry(registry: RebarRegistry<*>) {
            val key = registry.key
            check(key !in registries) { "Registry $key is already registered" }
            registries[key] = registry
        }
    }
}