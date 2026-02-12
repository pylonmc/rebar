package io.github.pylonmc.rebar.block

import com.github.retrooper.packetevents.protocol.world.Location
import com.github.retrooper.packetevents.util.Vector3f
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.RebarBlock.Companion.rebarBlockTextureEntityKey
import io.github.pylonmc.rebar.block.RebarBlock.Companion.register
import io.github.pylonmc.rebar.block.base.RebarDirectionalBlock
import io.github.pylonmc.rebar.block.base.RebarEntityHolderBlock
import io.github.pylonmc.rebar.block.base.RebarGuiBlock
import io.github.pylonmc.rebar.block.context.BlockBreakContext
import io.github.pylonmc.rebar.block.context.BlockCreateContext
import io.github.pylonmc.rebar.config.Config
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.config.Settings
import io.github.pylonmc.rebar.content.debug.DebugWaxedWeatheredCutCopperStairs
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.packet.BlockTextureEntity
import io.github.pylonmc.rebar.event.RebarBlockDeserializeEvent
import io.github.pylonmc.rebar.event.RebarBlockSerializeEvent
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.IMMEDIATE_FACES
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.position.position
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.waila.WailaDisplay
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.papermc.paper.datacomponent.DataComponentTypes
import me.tofaa.entitylib.meta.display.ItemDisplayMeta
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.ItemDisplay
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataAdapterContext
import org.bukkit.persistence.PersistentDataContainer

/**
 * Represents a Rebar block in the world.
 *
 * All custom Rebar blocks extend this class. Every instance of this class is wrapping a real block
 * in the world, and is stored in [BlockStorage]. All new block *types* must be registered using [register].
 *
 * An implementation of RebarBlock must have two constructors: one that takes a [Block] and a
 * [BlockCreateContext], and one that takes a [Block] and a [PersistentDataContainer]. The first
 * constructor is known as the "create constructor", and is used when the block is created in the world.
 * The second constructor is known as the "load constructor", and is used to reconstruct the block when
 * the chunk containing the block is loaded.
 *
 * @see BlockStorage
 */
open class RebarBlock internal constructor(val block: Block) {

    /**
     * All the data needed to create or load the block.
     */
    val schema = RebarBlockSchema.schemaCache.remove(block.position)!!

    val key = schema.key

    val nameTranslationKey = schema.nameTranslationKey
    val loreTranslationKey = schema.loreTranslationKey
    val defaultWailaTranslationKey = schema.defaultWailaTranslationKey

    /**
     * Set this to `true` if your block should not have a [blockTextureEntity] for custom models/textures.
     *
     * For example, if your block is comprised fully of [ItemDisplay]s, then you may have no need for a texture
     * entity as your existing entities could already support custom models/textures.
     */
    open var disableBlockTextureEntity = false

    /**
     * A packet based [ItemDisplay] sent to players who have `customBlockTextures` enabled.
     *
     * You can override [getBlockTextureProperties] if you have any custom block state properties
     * you want to expose to resource packs for custom models/textures. If those properties change
     * you can call [refreshBlockTextureItem] to update the model accordingly.
     *
     * Upon initialization the entity is set up by [setupBlockTexture] (which can be overridden),
     * and modifications afterward can be done using [updateBlockTexture].
     *
     * Being lazily initialized, if you do not access the entity directly it will only be created
     * when a player with `customBlockTextures` comes within range for the first time. This is to
     * avoid unnecessary entity creation, memory usage, and entity update overhead when no players
     * can actually see it.
     */
    open val blockTextureEntity: BlockTextureEntity? by lazy {
        if (!RebarConfig.BlockTextureConfig.ENABLED || disableBlockTextureEntity) {
            null
        } else {
            val entity = BlockTextureEntity(this)
            val meta = entity.getEntityMeta(ItemDisplayMeta::class.java)
            setupBlockTexture(entity, meta)
        }
    }

    val defaultItem = RebarRegistry.ITEMS[schema.key]

    /**
     * This constructor is called when a *new* block is created in the world.
     */
    constructor(block: Block, context: BlockCreateContext) : this(block)

    /**
     * This constructor is called when the block is loaded. For example, if the server
     * restarts, we need to create a new RebarBlock instance, and we'll do it with this
     * constructor.
     *
     * You should only load data in this constructor. If you need to do any extra logic on
     * load for whatever reason, it's recommended to do it in [postLoad] to make sure all
     * data associated with your block that you don't directly control (such as inventories,
     * associated entities, fluid tank data, etc) has been loaded.
     *
     * @see PersistentDataContainer
     */
    constructor(block: Block, pdc: PersistentDataContainer) : this(block)

    /**
     * Called after the load constructor.
     *
     * This is necessary because "external" stuff like [RebarGuiBlock], [io.github.pylonmc.rebar.block.base.RebarFluidBufferBlock]
     * and [RebarEntityHolderBlock] load their data *after* the load constructor is called.
     * If you need to use data from these interfaces (such as the amount of fluid stored in
     * a [io.github.pylonmc.rebar.block.base.RebarFluidBufferBlock], you must use this
     * instead of using the data in the load constructor.
     */
    protected open fun postLoad() {}

    /**
     * Called after both the create constructor and the load constructor.
     *
     * Use this to initialise stuff which must always be initialised, like creating logistics
     * groups (see [io.github.pylonmc.rebar.block.base.RebarLogisticBlock]).
     *
     * Called before [postLoad], after [io.github.pylonmc.rebar.event.RebarBlockPlaceEvent],
     * after [RebarBlockDeserializeEvent], and
     * before [io.github.pylonmc.rebar.event.RebarBlockLoadEvent]
     */
    open fun postInitialise() {}

    /**
     * Used to initialize [blockTextureEntity], if you need to modify the entity post-initialization,
     * use [updateBlockTexture].
     *
     * By default, this method sets the item display to be at the center of the block, using the
     * item returned by [getBlockTextureItem] (or a barrier if none is provided), set's its item
     * model to air, making it invisible for players without a resource pack, scales it to
     * 1.00085f in all directions to prevent z-fighting with the vanilla block model, and maxes its
     * brightness. It sets the display type to be "fixed".
     */
    protected open fun setupBlockTexture(entity: BlockTextureEntity, meta: ItemDisplayMeta): BlockTextureEntity = entity.apply {
        // TODO: Add a way to easily just change the transformation of the entity, without having to override this method entirely
        entity.spawn(Location(this@RebarBlock.block.x + 0.5, this@RebarBlock.block.y + 0.5, this@RebarBlock.block.z + 0.5, 0f, 0f))

        val item = getBlockTextureItem() ?: ItemStack(Material.BARRIER)
        item.setData(DataComponentTypes.ITEM_MODEL, Key.key("air"))
        meta.item = SpigotConversionUtil.fromBukkitItemStack(item)
        meta.displayType = ItemDisplayMeta.DisplayType.FIXED
        meta.brightnessOverride = 15 shl 4 or 15 shl 20;
        meta.scale = Vector3f(BlockTextureEntity.BLOCK_OVERLAP_SCALE, BlockTextureEntity.BLOCK_OVERLAP_SCALE, BlockTextureEntity.BLOCK_OVERLAP_SCALE)
        meta.width = 0f
        meta.height = 0f
    }

    /**
     * Use this method to make any changes to the block texture entity, such as changing its item,
     * transformation, etc, after initialization. (see [setupBlockTexture])
     */
    protected fun updateBlockTexture(updater: (BlockTextureEntity, ItemDisplayMeta) -> Unit) {
        blockTextureEntity?.let {
            val meta = it.getEntityMeta(ItemDisplayMeta::class.java)
            updater(it, meta)
        }
    }

    /**
     * Schedules the block texture item to be refreshed on the next server tick.
     * See [refreshBlockTextureItem].
     */
    fun scheduleBlockTextureItemRefresh() {
        Bukkit.getScheduler().runTask(Rebar) { _ ->
            refreshBlockTextureItem()
        }
    }

    /**
     * Call this method to refresh the block texture entity's item to be the result of
     * [getBlockTextureItem], or a barrier if that returns null.
     */
    fun refreshBlockTextureItem() {
        updateBlockTexture { _, meta ->
            val item = getBlockTextureItem() ?: ItemStack(Material.BARRIER)
            item.setData(DataComponentTypes.ITEM_MODEL, Key.key("air"))
            meta.item = SpigotConversionUtil.fromBukkitItemStack(item)
        }
    }

    /**
     * Returns a map of custom block state properties to be used for the block texture item.
     * These properties will be merged with the vanilla block state properties of the block.
     *
     * Your map should be in the form of `propertyName -> (propertyValue, numberOfPossibleValues)`.
     * For example, if you have a property called "facing" that can be "up", "down", "north", "south", "east", or "west",
     * you may return `mapOf("facing" to ("north", 6))`.
     *
     * When overriding this method you most likely want to work off the result of `super.getBlockTextureProperties()`
     * instead of returning a new map entirely, to ensure that any properties provided by superclasses
     * are preserved. (e.g. [RebarDirectionalBlock])
     */
    open fun getBlockTextureProperties(): MutableMap<String, Pair<String, Int>> {
        val properties = mutableMapOf<String, Pair<String, Int>>()
        if (this is RebarDirectionalBlock) {
            properties["facing"] = facing.name.lowercase() to IMMEDIATE_FACES.size
        }
        return properties
    }

    /**
     * Returns the item that should be used to display the block's texture.
     *
     * By default, returns the item with the same key as the block, marked with the
     * [rebarBlockTextureEntityKey]. The item will also have custom model data with
     * the vanilla block state properties of the block, merged with any custom
     * properties provided by the block. (see [getBlockTextureProperties])
     * This allows resource packs to provide different models/textures for different
     * block states.
     *
     * It is recommended to only override this method if you definitely need to, for
     * most use cases you should only ever need to override [getBlockTextureProperties].
     *
     * @return the item that should be used to display the block's texture
     */
    open fun getBlockTextureItem() = defaultItem?.getItemStack()?.let { ItemStackBuilder(it) }?.apply {
        editPdc { it.set(rebarBlockTextureEntityKey, RebarSerializers.BOOLEAN, true) }
        val properties = NmsAccessor.instance.getStateProperties(block, getBlockTextureProperties())
        for ((property, value) in properties) {
            addCustomModelDataString("$property=$value")
        }
    }?.build()

    /**
     * WAILA is the text that shows up when looking at a block to tell you what the block is.
     *
     * This will only be called for the player if the player has WAILA enabled.
     *
     * @return the WAILA configuration, or null if WAILA should not be shown for this block.
     */
    open fun getWaila(player: Player): WailaDisplay? {
        return WailaDisplay(defaultWailaTranslationKey)
    }

    /**
     * Returns the item that the block should drop.
     *
     * By default, returns the item with the same key as the block only if
     * [BlockBreakContext.normallyDrops] is true, and null otherwise.
     *
     * @return the item the block should drop, or null if none
     */
    open fun getDropItem(context: BlockBreakContext): ItemStack? {
        return if (context.normallyDrops) {
            defaultItem?.getItemStack()
        } else {
            null
        }
    }

    /**
     * Returns the item that should be given when the block is middle clicked.
     *
     * By default, returns the item with the same key as the block only if
     * [BlockBreakContext.normallyDrops] is true, and null otherwise.
     *
     * @return the item the block should give when middle clicked, or null if none
     */
    open fun getPickItem() = defaultItem?.getItemStack()

    /**
     * Called when debug info is requested for the block by someone
     * using the [DebugWaxedWeatheredCutCopperStairs]. If there is
     * any transient data that can be useful for debugging, you're
     * encouraged to serialize it here.
     *
     * Defaults to a normal [write] call.
     */
    open fun writeDebugInfo(pdc: PersistentDataContainer) = write(pdc)

    /**
     * Called when the block is saved.
     *
     * Put any logic to save the data in the block here.
     *
     * *Do not assume that when this is called, the block is being unloaded.* This
     * may be called for other reasons, such as when a player right clicks with
     * [DebugWaxedWeatheredCutCopperStairs].
     * Instead, implement [io.github.pylonmc.rebar.block.base.RebarUnloadBlock] and
     * use [io.github.pylonmc.rebar.block.base.RebarUnloadBlock.onUnload].
     */
    open fun write(pdc: PersistentDataContainer) {}

    /**
     * Returns settings associated with the block.
     *
     * Shorthand for `Settings.get(getKey())`
     */
    fun getSettings(): Config = Settings.get(key)

    companion object {

        @JvmStatic
        val rebarBlockTextureEntityKey = rebarKey("rebar_block_texture_entity")

        @JvmStatic
        val rebarBlockKeyKey = rebarKey("rebar_block_key")

        @JvmStatic
        val rebarBlockPositionKey = rebarKey("position")

        @get:JvmStatic
        val Block.rebarBlock: RebarBlock?
            get() = BlockStorage.get(this)

        @get:JvmStatic
        val Block.isVanillaBlock: Boolean
            get() = BlockStorage.get(this) == null

        /**
         * Registers a new block type with Rebar.
         *
         * @param key A unique key that identifies this type of block
         * @param material The material to use as the block. This must match the material
         * of the item(s) that place the block.
         * @param blockClass The class extending [RebarBlock] that represents a block
         * of this type in the world.
         */
        @JvmStatic
        fun register(key: NamespacedKey, material: Material, blockClass: Class<out RebarBlock>) {
            val schema = RebarBlockSchema(key, material, blockClass)
            RebarRegistry.BLOCKS.register(schema)
        }

        @JvmSynthetic
        inline fun <reified T : RebarBlock> register(key: NamespacedKey, material: Material) =
            register(key, material, T::class.java)

        @JvmSynthetic
        internal fun serialize(
            block: RebarBlock,
            context: PersistentDataAdapterContext
        ): PersistentDataContainer {
            // See PhantomBlock docs for why we do this
            if (block is PhantomBlock) {
                return block.pdc
            }

            val pdc = context.newPersistentDataContainer()
            pdc.set(rebarBlockKeyKey, RebarSerializers.NAMESPACED_KEY, block.schema.key)
            pdc.set(rebarBlockPositionKey, RebarSerializers.LONG, block.block.position.asLong)

            block.write(pdc)
            RebarBlockSerializeEvent(block.block, block, pdc).callEvent()

            return pdc
        }

        @JvmSynthetic
        internal fun deserialize(
            world: World,
            pdc: PersistentDataContainer
        ): RebarBlock? {
            // Stored outside of the try block so they are displayed in error messages once acquired
            var key: NamespacedKey? = null
            var position: BlockPosition? = null

            try {
                key = pdc.get(rebarBlockKeyKey, RebarSerializers.NAMESPACED_KEY)
                    ?: error("Block PDC does not contain ID")

                position = pdc.get(rebarBlockPositionKey, RebarSerializers.LONG)?.let {
                    BlockPosition(world, it)
                } ?: error("Block PDC does not contain position")

                // We fail silently here because this may trigger if an addon is removed or fails to load.
                // In this case, we don't want to delete the data, and we also don't want to spam errors.
                // See PhantomBlock docs for why PhantomBlock is returned rather than null.
                val schema = RebarRegistry.BLOCKS[key]
                if (schema == null) {
                    RebarBlockSchema.schemaCache[position] = PhantomBlock.schema
                    return PhantomBlock(pdc, key, position.block)
                }

                // We can assume this function is only going to be called when the block's world is loaded, hence the asBlock!!
                @Suppress("UNCHECKED_CAST") // The cast will work - this is checked in the schema constructor
                val block = schema.load(position.block, pdc)

                RebarBlockDeserializeEvent(block.block, block, pdc).callEvent()
                block.postInitialise()
                block.postLoad()
                return block
            } catch (t: Throwable) {
                Rebar.logger.severe("Error while loading block $key at $position")
                t.printStackTrace()
                return if (key != null && position != null) {
                    RebarBlockSchema.schemaCache[position] = PhantomBlock.schema
                    PhantomBlock(pdc, key, position.block)
                } else {
                    null
                }
            }
        }
    }
}