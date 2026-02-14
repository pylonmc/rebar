@file:JvmName("RebarUtils")
@file:Suppress("UnstableApiUsage")

package io.github.pylonmc.rebar.util

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.config.Config
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.papermc.paper.datacomponent.DataComponentType
import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.TranslationArgumentLike
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.World
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import org.joml.Matrix3f
import org.joml.RoundingMode
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.invui.inventory.event.PlayerUpdateReason
import xyz.xenondevs.invui.inventory.event.UpdateReason
import java.lang.Math
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.function.Consumer
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Checks whether a [NamespacedKey] is from [addon]
 */
@JvmName("isKeyFromAddon")
fun NamespacedKey.isFromAddon(addon: RebarAddon): Boolean
    = namespace == addon.key.namespace

/**
 * Converts an orthogonal vector to a [BlockFace]
 *
 *  @return The face that the vector is facing
 *  @throws IllegalStateException if the vector is not pointing in a cardinal direction
 */
fun vectorToBlockFace(vector: Vector3i): BlockFace {
    return if (vector.x > 0 && vector.y == 0 && vector.z == 0) {
        BlockFace.EAST
    } else if (vector.x < 0 && vector.y == 0 && vector.z == 0) {
        BlockFace.WEST
    } else if (vector.x == 0 && vector.y > 0 && vector.z == 0) {
        BlockFace.UP
    } else if (vector.x == 0 && vector.y < 0 && vector.z == 0) {
        BlockFace.DOWN
    } else if (vector.x == 0 && vector.y == 0 && vector.z > 0) {
        BlockFace.SOUTH
    } else if (vector.x == 0 && vector.y == 0 && vector.z < 0) {
        BlockFace.NORTH
    } else {
        throw IllegalStateException("Vector $vector cannot be turned into a block face")
    }
}

/**
 * Returns the yaw (in radians) that a face has, starting at NORTH and
 * going counterclockwise.
 *
 * Only works for cardinal directions.
 *
 *  @throws IllegalStateException if [face] is not a cardinal direction
 */
fun faceToYaw(face: BlockFace) = when (face) {
    BlockFace.NORTH -> 0.0
    BlockFace.EAST -> -Math.PI / 2
    BlockFace.SOUTH -> Math.PI
    BlockFace.WEST -> Math.PI / 2
    else -> throw IllegalArgumentException("$face is not a cardinal direction")
}

/**
 * Converts an orthogonal vector to a [BlockFace]
 *
 *  @return The face that the vector is facing
 *  @throws IllegalStateException if the vector is not pointing in a cardinal direction
 */
fun vectorToBlockFace(vector: Vector3f) = vectorToBlockFace(Vector3i(vector, RoundingMode.HALF_DOWN))

/**
 * Converts an orthogonal vector to a [BlockFace]
 *
 *  @return The face that the vector is facing
 *  @throws IllegalStateException if the vector is not pointing in a cardinal direction
 */
fun vectorToBlockFace(vector: Vector3d) = vectorToBlockFace(Vector3i(vector, RoundingMode.HALF_DOWN))

/**
 * Converts an orthogonal vector to a [BlockFace]
 *
 *  @return The face that the vector is facing
 *  @throws IllegalStateException if the vector is not pointing in a cardinal direction
 */
// use toVector3f rather than toVector3i because toVector3i will floor components
fun vectorToBlockFace(vector: Vector) = vectorToBlockFace(vector.toVector3f())

/**
 * Rotates [vector] to face a direction
 *
 * Assumes north to be the default direction (i.e. supplying north will result in no rotation)
 *
 * @param face Must be a immediate direction (north, east, south, west, up, down)
 * @return The rotated vector
 */
fun rotateVectorToFace(vector: Vector3i, face: BlockFace) = when (face) {
    BlockFace.NORTH -> vector
    BlockFace.EAST -> Vector3i(-vector.z, vector.y, vector.x)
    BlockFace.SOUTH -> Vector3i(-vector.x, vector.y, -vector.z)
    BlockFace.WEST -> Vector3i(vector.z, vector.y, -vector.x)
    BlockFace.UP -> Vector3i(0, 1, 0)
    BlockFace.DOWN -> Vector3i(0, -1, 0)
    else -> throw IllegalArgumentException("$face is not a cardinal direction")
}

/**
 * Rotates [vector] to face a direction
 *
 * Assumes north to be the default direction (i.e. supplying north will result in no rotation)
 *
 * @param face Must be a immediate direction (north, east, south, west, up, down)
 * @return The rotated vector
 */
fun rotateVectorToFace(vector: Vector3d, face: BlockFace) = when (face) {
    BlockFace.NORTH -> vector
    BlockFace.EAST -> Vector3d(-vector.z, vector.y, vector.x)
    BlockFace.SOUTH -> Vector3d(-vector.x, vector.y, -vector.z)
    BlockFace.WEST -> Vector3d(vector.z, vector.y, -vector.x)
    BlockFace.UP -> Vector3d(0.0, 1.0, 0.0)
    BlockFace.DOWN -> Vector3d(0.0, -1.0, 0.0)
    else -> throw IllegalArgumentException("$face is not a horizontal cardinal direction")
}

/**
 * Rotates [face] to be relative to [referenceFace].
 *
 * Assumes north to be the default direction (i.e. supplying north will result in no rotation)
 *
 * Think of this like changing the direction of North. For example, if you change North to
 * point where East would be, then suddenly East in your coordinate system becomes South.
 *
 * @param face Must be a horizontal cardinal direction (north, east, south, west)
 * @return The rotated vector
 */
fun rotateFaceToReference(referenceFace: BlockFace, face: BlockFace)
    = vectorToBlockFace(rotateVectorToFace(face.direction.toVector3d(), referenceFace))

/**
 * @return Whether [vector] is a cardinal direction
 */
fun isCardinalDirection(vector: Vector3i) = (vector.x != 0 && vector.y == 0 && vector.z == 0)
        || (vector.x == 0 && vector.y != 0 && vector.z == 0)
        || (vector.x == 0 && vector.y == 0 && vector.z != 0)

/**
 * @return Whether [vector] is a cardinal direction
 */
fun isCardinalDirection(vector: Vector3f)
    = (vector.x.absoluteValue > 1.0e-6 && vector.y.absoluteValue < 1.0e-6 && vector.z.absoluteValue < 1.0e-6)
        || (vector.x.absoluteValue < 1.0e-6 && vector.y.absoluteValue > 1.0e-6 && vector.z.absoluteValue < 1.0e-6)
        || (vector.x.absoluteValue < 1.0e-6 && vector.y.absoluteValue < 1.0e-6 && vector.z.absoluteValue > 1.0e-6)

/**
 * @return The addon that [key] belongs to
 */
fun getAddon(key: NamespacedKey): RebarAddon =
    RebarRegistry.ADDONS.find { addon -> addon.key.namespace == key.namespace }
        ?: error("Key does not have a corresponding addon; does your addon call registerWithRebar()?")

/**
 * Attaches arguments to a component and all its children.
 *
 * @param args List of arguments to attach
 * @return The component with the arguments attached
 */
@JvmName("attachArguments")
fun Component.withArguments(args: List<TranslationArgumentLike>): Component {
    if (args.isEmpty()) return this
    var result = this
    if (this is TranslatableComponent) {
        result = this.arguments(args)
    }
    return result.children(result.children().map { it.withArguments(args) })
}

/**
 * (Heuristically) checks whether an event is 'fake' (by checking if it has 'Fake' in its name)
 *
 * 'Fake' events are often used to check actions before performing them.
 */
fun isFakeEvent(event: Event): Boolean {
    return event.javaClass.name.contains("Fake")
}

/**
 * [BlockFace.NORTH], [BlockFace.EAST], [BlockFace.SOUTH], [BlockFace.WEST]
 */
@JvmField
val CARDINAL_FACES: Array<BlockFace> = arrayOf(
    BlockFace.NORTH,
    BlockFace.EAST,
    BlockFace.SOUTH,
    BlockFace.WEST
)

/**
 * [BlockFace.UP], [BlockFace.DOWN], [BlockFace.EAST], [BlockFace.WEST], [BlockFace.SOUTH], [BlockFace.NORTH]
 */
@JvmField
val IMMEDIATE_FACES: Array<BlockFace> = arrayOf(
    BlockFace.UP,
    BlockFace.DOWN,
    BlockFace.EAST,
    BlockFace.WEST,
    BlockFace.SOUTH,
    BlockFace.NORTH
)

/**
 * Same as [IMMEDIATE_FACES] but includes diagonal faces, not including the vertical directions.
 */
@JvmField
val IMMEDIATE_FACES_WITH_DIAGONALS: Array<BlockFace> = arrayOf(
    BlockFace.UP,
    BlockFace.DOWN,
    BlockFace.EAST,
    BlockFace.WEST,
    BlockFace.SOUTH,
    BlockFace.NORTH,
    BlockFace.NORTH_EAST,
    BlockFace.NORTH_WEST,
    BlockFace.SOUTH_EAST,
    BlockFace.SOUTH_WEST,
    BlockFace.EAST
)

/**
 * Returns all the immediate faces that are perpendicular to the given [face]
 *
 * @see IMMEDIATE_FACES
 */
fun perpendicularImmediateFaces(face: BlockFace): List<BlockFace> {
    val faces = IMMEDIATE_FACES.toMutableList()
    faces.remove(face)
    faces.remove(face.oppositeFace)
    return faces
}

@JvmSynthetic
internal fun rebarKey(key: String): NamespacedKey = NamespacedKey(Rebar, key)

@JvmSynthetic
internal fun Class<*>.findConstructorMatching(vararg types: Class<*>): MethodHandle? {
    return declaredConstructors.firstOrNull {
        it.parameterTypes.size == types.size &&
                it.parameterTypes.zip(types).all { (param, given) -> given.isSubclassOf(param) }
    }?.let(MethodHandles.lookup()::unreflectConstructor)
}

// I can never remember which way around `isAssignableFrom` goes,
// so this is a helper function to make it more readable
@JvmSynthetic
private fun Class<*>.isSubclassOf(other: Class<*>): Boolean = other.isAssignableFrom(this)

/**
 * Small helper function to convert a minimessage string (eg: '<red>bruh') into a component
 * @param string The string to turn into a component
 * @returns The string as a component
 */
@JvmSynthetic
fun fromMiniMessage(string: String): Component = MiniMessage.miniMessage().deserialize(string)

/**
 * Finds a Rebar item in an inventory. Use this to find Rebar items instead of traditional
 * find methods, because this will compare Rebar IDs.
 *
 * @param inventory The inventory to search
 * @param targetItem The item to find. Items will be compared by their Rebar ID
 * @return The slot containing the item, or null if no item was found
 */
fun findRebarItemInInventory(inventory: Inventory, targetItem: RebarItem): Int? {
    for (i in 0..<inventory.size) {
        val item = inventory.getItem(i)?.let {
            RebarItem.fromStack(it)
        }
        if (item == targetItem) {
            return i
        }
    }
    return null
}

@JvmSynthetic
inline fun <reified T> ItemStack?.isRebarAndIsNot(): Boolean {
    val rebarItem = RebarItem.fromStack(this)
    return rebarItem != null && rebarItem !is T
}

@JvmSynthetic
@Suppress("UnstableApiUsage")
inline fun <T : Any> ItemStack.editData(
    type: DataComponentType.Valued<T>,
    block: (T) -> T
): ItemStack {
    val data = getData(type) ?: return this
    setData(type, block(data))
    return this
}

@JvmSynthetic
@Suppress("UnstableApiUsage")
inline fun <T : Any> ItemStack.editDataOrDefault(
    type: DataComponentType.Valued<T>,
    block: (T) -> T
): ItemStack {
    val data = getData(type) ?: this.type.getDefaultData(type) ?: return this
    setData(type, block(data))
    return this
}

@JvmSynthetic
@Suppress("UnstableApiUsage")
inline fun <T : Any> ItemStack.editDataOrSet(
    type: DataComponentType.Valued<T>,
    block: (T?) -> T
): ItemStack {
    setData(type, block(getData(type)))
    return this
}

/**
 * Wrapper around [PersistentDataContainer.set] that allows nullable values to be passed
 *
 * @param value The value to set. If this is null, the key will be removed from the container
 */
fun <P, C> PersistentDataContainer.setNullable(key: NamespacedKey, type: PersistentDataType<P, C>, value: C?) {
    if (value != null) {
        set(key, type, value)
    } else {
        remove(key)
    }
}

/**
 * Acts as a property delegate for stuff contained inside a [PersistentDataContainer]
 * For example:
 * ```
 * val numberOfTimesJumped: Int by persistentData(NamespacedKey(yourPlugin, "jumped"), PersistentDataType.INTEGER) { 0 }
 * ```
 */
@JvmSynthetic
inline fun <T> persistentData(
    key: NamespacedKey,
    type: PersistentDataType<*, T & Any>,
    crossinline default: () -> T
) = object : ReadWriteProperty<PersistentDataHolder, T> {

    override fun getValue(thisRef: PersistentDataHolder, property: KProperty<*>): T {
        return thisRef.persistentDataContainer.get(key, type) ?: default()
    }

    override fun setValue(thisRef: PersistentDataHolder, property: KProperty<*>, value: T) {
        if (value == null) {
            thisRef.persistentDataContainer.remove(key)
        } else {
            thisRef.persistentDataContainer.set(key, type, value)
        }
    }
}

/**
 * Same as [persistentData] but with a default value that is constant
 */
@JvmSynthetic
fun <T> persistentData(
    key: NamespacedKey,
    type: PersistentDataType<*, T & Any>,
    default: T
) = persistentData(key, type) { default }

@get:JvmSynthetic
val Player.pdc: PersistentDataContainer
    get() = this.persistentDataContainer

/**
 * Merges config from addons to the Rebar config directory.
 * Used for stuff like item settings and language files.
 *
 * Returns the configuration read and merged from the resource.
 * If the file does not exist in the resource but already exists
 * at the [to] path, reads and returns the file at the [to] path.
 *
 * @param from The path to the config file. Must be a YAML file.
 * @param warnMissing if set to true, the logger will warn if the resource in [from] is missing
 * @return The merged config
 */
internal fun mergeGlobalConfig(addon: RebarAddon, from: String, to: String, warnMissing: Boolean = true): Config {
    require(from.endsWith(".yml")) { "Config file must be a YAML file" }
    require(to.endsWith(".yml")) { "Config file must be a YAML file" }
    val cached = globalConfigCache[from to to]
    if (cached != null) {
        return cached
    }
    val globalConfig = Rebar.dataFolder.resolve(to)
    if (!globalConfig.exists()) {
        globalConfig.parentFile.mkdirs()
        globalConfig.createNewFile()
    }
    val config = Config(globalConfig)
    val resource = addon.javaPlugin.getResource(from)
    if (resource == null) {
        if (warnMissing) Rebar.logger.warning("Resource not found: $from")
    } else {
        val newConfig = resource.reader().use(YamlConfiguration::loadConfiguration)
        config.internalConfig.setDefaults(newConfig)
        config.internalConfig.options().copyDefaults(true)
        config.merge(ConfigSection(newConfig))
        config.save()
    }
    globalConfigCache[from to to] = config
    return config
}

private val globalConfigCache: MutableMap<Pair<String, String>, Config> = mutableMapOf()

val Block.replaceableOrAir: Boolean
    get() = type.isAir || isReplaceable

fun ItemStack.vanillaDisplayName(): Component
    = effectiveName().let {
        val wrapped = Component.translatable("chat.square_brackets", it)
        if (!this.isEmpty) {
            wrapped.hoverEvent(this.asHoverEvent())
        }
        return wrapped
    }

val Component.plainText: String
    get() = PlainTextComponentSerializer.plainText().serialize(this)

/**
 * Does not include first or last block
 */
fun blocksOnPath(from: BlockPosition, to: BlockPosition): List<Block> {
    val originBlock = from.block
    val offset = to.location
        .subtract(originBlock.location)
        .toVector().toVector3i()

    val blocks = mutableListOf<Block>()
    var block = originBlock
    // math.round to make it an integer - the length will already be an integer
    for (i in 0..<offset.length().roundToInt() - 1) {
        block = block.getRelative(vectorToBlockFace(offset))
        blocks.add(block)
    }

    return blocks
}

/* Returns lambda where
 * r1 = p1 + lambda*d1 (line 1)
 * r2 = p2 + mu*d2 (line 2)
 * r3 = p3 + phi*d3 (an imagined perpendicular line between them used to solve for closest points)
 */
fun findClosestPointBetweenSkewLines(p1: Vector3f, d1: Vector3f, p2: Vector3f, d2: Vector3f): Float {
    val d3 = Vector3f(d1).cross(d2)
    // solve for lamdba, mu, phi using the matrix inversion method
    val mat = Matrix3f(d1, Vector3f(d2).mul(-1f), d3)
        .invert()
    val solution = Vector3f(p2).sub(p1).mul(mat)
    return solution.y
}

/**
 * @param p The point
 * @param p1 The starting point of the line
 * @param d1 The direction of the line
 *
 * @return Supposing the equation of the line is p1 + t*d1, returns the t representing the closest point
 *
 * @see <a href="https://math.stackexchange.com/questions/1905533/find-perpendicular-distance-from-point-to-line-in-3d">
 */
fun findClosestPointToOtherPointOnLine(p: Vector3f, p1: Vector3f, d1: Vector3f): Float {
    val v = Vector3f(p).sub(p1)
    return Vector3f(v).dot(d1)
}

/**
 * Unidirectional, meaning if the closest point is 'behind' the starting point, returns the distance
 * from the starting point.
 *
 * @param p The point
 * @param p1 The starting point of the line
 * @param d1 The direction of the line
 *
 * @see <a href="https://math.stackexchange.com/questions/1905533/find-perpendicular-distance-from-point-to-line-in-3d">
 */
fun findClosestDistanceBetweenLineAndPoint(p: Vector3f, p1: Vector3f, d1: Vector3f): Float {
    val t = max(0.0F, findClosestPointToOtherPointOnLine(p, p1, d1))
    val closestPoint = Vector3f(p1).add(Vector3f(d1).mul(t))
    return (Vector3f(closestPoint).sub(p)).length()
}

@JvmSynthetic
internal fun getTargetEntity(player: Player, maxDistanceBetweenRayAndEntity: Float): Entity? {
    val range = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)!!.value
    val entities = player.getNearbyEntities(range, range, range)

    for (entity in entities) {
        val distance = findClosestDistanceBetweenLineAndPoint(
            entity.location.toVector().toVector3f(),
            player.eyeLocation.toVector().toVector3f(),
            player.eyeLocation.getDirection().toVector3f()
        )
        if (distance <= maxDistanceBetweenRayAndEntity) {
            return entity
        }
    }

    return null
}

fun pickaxeMineable() = Registry.BLOCK.getTag(BlockTypeTagKeys.MINEABLE_PICKAXE)
fun axeMineable() = Registry.BLOCK.getTag(BlockTypeTagKeys.MINEABLE_AXE)
fun shovelMineable() = Registry.BLOCK.getTag(BlockTypeTagKeys.MINEABLE_SHOVEL)
fun hoeMineable() = Registry.BLOCK.getTag(BlockTypeTagKeys.MINEABLE_HOE)

@JvmOverloads
fun damageItem(itemStack: ItemStack, amount: Int, world: World, onBreak: (Material) -> Unit = {}, force: Boolean = false) =
    NmsAccessor.instance.damageItem(itemStack, amount, world, onBreak, force)

@JvmOverloads
fun damageItem(itemStack: ItemStack, amount: Int, entity: LivingEntity, slot: EquipmentSlot, force: Boolean = false) =
    NmsAccessor.instance.damageItem(itemStack, amount, entity, slot, force)


/**
 * A shorthand for a commonly used [VirtualInventory] handler which prevents players
 * from removing items from it.
 *
 * Usage: Call [VirtualInventory.addPreUpdateHandler] and supply this function to it
 */
@JvmField
val DISALLOW_PLAYERS_FROM_ADDING_ITEMS_HANDLER = Consumer<ItemPreUpdateEvent> { event: ItemPreUpdateEvent ->
    if (!event.isRemove && event.updateReason is PlayerUpdateReason) {
        event.isCancelled = true
    }
}

/**
 * Indicates a machine has updated an inventory slot.
 */
class MachineUpdateReason : UpdateReason

// https://minecraft.wiki/w/Breaking#Calculation
fun getBlockBreakTicks(tool: ItemStack, block: Block)
    = round(100 * block.type.getHardness() / block.getDestroySpeed(tool, true))

fun Entity.scheduleRemove() = Bukkit.getScheduler().runTask(Rebar, this::remove)