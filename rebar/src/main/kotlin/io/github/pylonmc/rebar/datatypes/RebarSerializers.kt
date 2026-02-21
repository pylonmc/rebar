package io.github.pylonmc.rebar.datatypes

import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.logistics.LogisticGroupType
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.waila.Waila
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.block.BlockFace
import org.bukkit.persistence.PersistentDataType

/**
 * A collection of various [PersistentDataType]s for use by Rebar and Rebar addons
 */
@Suppress("unused")
object RebarSerializers {
    @JvmField
    val BYTE = PersistentDataType.BYTE!!

    @JvmField
    val SHORT = PersistentDataType.SHORT!!

    @JvmField
    val INTEGER = PersistentDataType.INTEGER!!

    @JvmField
    val LONG = PersistentDataType.LONG!!

    @JvmField
    val FLOAT = PersistentDataType.FLOAT!!

    @JvmField
    val DOUBLE = PersistentDataType.DOUBLE!!

    @JvmField
    val BOOLEAN = PersistentDataType.BOOLEAN!!

    @JvmField
    val STRING = PersistentDataType.STRING!!

    @JvmField
    val CHAR = CharPersistentDataType

    @JvmField
    val BYTE_ARRAY = PersistentDataType.BYTE_ARRAY!!

    @JvmField
    val INTEGER_ARRAY = PersistentDataType.INTEGER_ARRAY!!

    @JvmField
    val LONG_ARRAY = PersistentDataType.LONG_ARRAY!!

    @JvmField
    val TAG_CONTAINER = PersistentDataType.TAG_CONTAINER!!

    @JvmField
    val LIST = PersistentDataType.LIST!!

    @JvmField
    val SET = SetPersistentDataType

    @JvmField
    val MAP = MapPersistentDataType

    @JvmField
    val ENUM = EnumPersistentDataType

    @JvmField
    val NAMESPACED_KEY = NamespacedKeyPersistentDataType

    @JvmField
    val UUID = UUIDPersistentDataType

    @JvmField
    val VECTOR = VectorPersistentDataType

    @JvmField
    val WORLD = WorldPersistentDataType

    @JvmField
    val BLOCK_POSITION = BlockPositionPersistentDataType

    @JvmField
    val BLOCK_FACE = EnumPersistentDataType(BlockFace::class.java)

    @JvmField
    val CHUNK_POSITION = ChunkPositionPersistentDataType

    @JvmField
    val LOCATION = LocationPersistentDataType

    @JvmField
    val ITEM_STACK = ItemStackPersistentDataType

    /**
     * Unlike the regular [ItemStackPersistentDataType], which serializes directly to a byte array,
     * this serializes item stacks to a human-readable format backed by vanilla item serialization.
     */
    @JvmField
    val ITEM_STACK_READABLE = ReadableItemStackPersistentDataType

    @JvmField
    val INVENTORY = InventoryPersistentDataType

    @JvmField
    val VIRTUAL_INVENTORY = VirtualInventoryPersistentDataType

    @JvmField
    val KEYED = KeyedPersistentDataType

    @JvmField
    val MATERIAL = KEYED.keyedTypeFrom<Material>(Registry.MATERIAL::getOrThrow)

    @JvmField
    val REBAR_FLUID = KEYED.keyedTypeFrom<RebarFluid>(RebarRegistry.FLUIDS::getOrThrow)

    @JvmField
    val FLUID_CONNECTION_POINT = FluidConnectionPointPersistentDataType

    @JvmField
    val LOGISTIC_POINT_TYPE = EnumPersistentDataType(LogisticGroupType::class.java)

    @JvmField
    val DURATION = DurationPersistentDataType

    @JvmField
    val PROGRESS_ITEM = ProgressItemPersistentDataType

    @JvmField
    val COMPONENT = ComponentPersistentDataType

    @JvmSynthetic
    internal val CARGO_BLOCK_DATA = CargoBlockPersistentDataType

    @JvmSynthetic
    internal val WAILA_TYPE = EnumPersistentDataType(Waila.Type::class.java)

    @JvmSynthetic
    internal val PLAYER_WAILA_CONFIG = PlayerWailaConfigPersistentDataType

    @JvmSynthetic
    internal val PLAYER_CULLING_CONFIG = PlayerCullingConfigPersistentDataType

    @JvmSynthetic
    internal val FLUID_BUFFER_DATA = FluidBufferDataPersistentDataType

    @JvmSynthetic
    internal val FLUID_TANK_DATA = FluidTankDataPersistentDataType

    @JvmSynthetic
    internal val PROCESSOR_DATA = ProcessorDataPersistentDataType

    @JvmSynthetic
    internal val RECIPE_PROCESSOR_DATA = RecipeProcessorDataPersistentDataType

    @JvmSynthetic
    internal val SIMPLE_MULTIBLOCK_DATA = SimpleMultiblockDataPersistentDataType

    @JvmSynthetic
    internal val TICKING_BLOCK_DATA = TickingBlockPersistentDataType

    @JvmSynthetic
    internal val TICKING_ENTITY_DATA = TickingEntityPersistentDataType

}
