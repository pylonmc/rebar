@file:JvmName("RebarUtils")
@file:Suppress("UnstableApiUsage")

package io.github.pylonmc.rebar.util

import com.google.common.base.Preconditions
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.ContributorConfig
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.i18n.customMiniMessage
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemSchema
import io.github.pylonmc.rebar.item.interfaces.ProjectileRebarItemHandler
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.position.position
import io.papermc.paper.datacomponent.DataComponentType
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.registry.keys.tags.BlockTypeTagKeys
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.TranslationArgumentLike
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.block.data.BlockData
import org.bukkit.entity.*
import org.bukkit.event.Event
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.BoundingBox
import org.bukkit.util.Vector
import org.joml.Intersectionf
import org.joml.Matrix3f
import org.joml.Quaternionf
import org.joml.RoundingMode
import org.joml.Vector2f
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
import kotlin.coroutines.CoroutineContext
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
fun Class<*>.isSubclassOf(other: Class<*>): Boolean = other.isAssignableFrom(this)

/**
 * Small helper function to convert a minimessage string (eg: '<red>bruh') into a component
 * @param string The string to turn into a component
 * @returns The string as a component
 */
@JvmSynthetic
fun fromMiniMessage(string: String): Component = customMiniMessage.deserialize(string)

/**
 * Finds a Rebar item in this inventory. Use this to find Rebar items instead of traditional
 * find methods, because this will compare Rebar IDs.
 *
 * @param targetItemId The item id to find. Items will be compared by their Rebar ID
 * @return The slot containing the item, or null if no item was found
 */
fun Inventory.findRebar(targetItemId: NamespacedKey): Int? = RebarRegistry.ITEMS[targetItemId]?.let { findRebar(it) }

/**
 * Finds a Rebar item in this inventory. Use this to find Rebar items instead of traditional
 * find methods, because this will compare Rebar IDs.
 *
 * @param targetItem The item to find. Items will be compared by their Rebar ID
 * @return The slot containing the item, or null if no item was found
 */
fun Inventory.findRebar(targetItem: RebarItemSchema): Int? {
    for (i in 0..<size) {
        val item = getItem(i)?.let {
            RebarItemSchema.fromStack(it)
        }
        if (item == targetItem) {
            return i
        }
    }
    return null
}

/**
 * Finds an item of the right type in this inventory. Use this to find items purely based on Vanilla / Rebar IDs.
 *
 * @param targetType The type to find. Items will be compared by their Vanilla / Rebar ID
 * @return The slot containing the item, or null if no item was found
 */
fun Inventory.findType(targetType: ItemTypeWrapper): Int? {
    for (i in 0..<size) {
        val item = getItem(i)?.let {
            ItemTypeWrapper(it)
        }
        if (item == targetType) {
            return i
        }
    }
    return null
}

fun Inventory.swapItem(from: Int, to: Int) {
    val fromItem = getItem(from)
    val toItem = getItem(to)
    setItem(from, toItem)
    setItem(to, fromItem)
}

@JvmSynthetic
inline fun <reified T> ItemStack?.isRebarAndIsNot(): Boolean {
    val schema = RebarItemSchema.fromStack(this)
    return schema != null && !schema.isType(T::class.java)
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
fun <P, C : Any> PersistentDataContainer.setNullable(key: NamespacedKey, type: PersistentDataType<P, C>, value: C?) {
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
@JvmSynthetic
internal fun mergeResource(
    fromAddon: RebarAddon,
    toAddon: RebarAddon,
    from: String,
    to: String,
    warnMissing: Boolean = true
): ConfigSection {
    require(from.endsWith(".yml") || from.endsWith(".yaml")) {
        "Config file must be a YAML file (addon: ${fromAddon.javaClass.simpleName}, path: $from)"
    }
    require(to.endsWith(".yml") || to.endsWith(".yaml")) {
        "Config file must be a YAML file (addon: ${fromAddon.javaClass.simpleName}, path: $to)"
    }

    val cached = globalConfigCache[from to to]
    if (cached != null) {
        return cached
    }

    val toConfigFile = toAddon.javaPlugin.dataFolder.resolve(to)
    if (!toConfigFile.exists()) {
        toConfigFile.parentFile.mkdirs()
        toConfigFile.createNewFile()
    }

    check(toConfigFile.exists()) { "Unable to create file ${toConfigFile.absolutePath}" }
    val toConfig = ConfigSection.fromOrThrow(toConfigFile)
    val fromConfig = ConfigSection.fromResource(fromAddon.javaPlugin, from)
    if (fromConfig == null) {
        if (warnMissing) toAddon.javaPlugin.logger.warning("Resource not found: $from")
    } else {
        toConfig.merge(fromConfig)
        toConfig.save(toConfigFile)
    }
    globalConfigCache[from to to] = toConfig
    return toConfig
}

private val globalConfigCache: MutableMap<Pair<String, String>, ConfigSection> = mutableMapOf()

@JvmSynthetic
internal fun getContributors(addon: RebarAddon): List<ContributorConfig> {
    val cached = contributorsCache[addon]
    if (cached != null) {
        return cached
    }

    val config = ConfigSection.fromResource(addon.javaPlugin, "contributors.yml")
    val contributors = config?.get(
        "contributors",
        ConfigAdapter.LIST.from(ConfigAdapter.CONTRIBUTOR),
        emptyList()
    ) ?: emptyList()
    contributorsCache[addon] = contributors
    return contributors
}

private val contributorsCache: MutableMap<RebarAddon, List<ContributorConfig>> = mutableMapOf()

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

fun blocksWithin(world: World, boundingBox: BoundingBox) = blocksBetween(
    BlockPosition(world, boundingBox.min),
    BlockPosition(world, boundingBox.max)
)

fun blocksBetween(from: BlockPosition, to: BlockPosition): List<Block> = NmsAccessor.instance.blocksBetween(from, to)

/**
 * Does not include first or last block
 */
fun blocksOnPath(from: BlockPosition, to: BlockPosition): List<Block> {
    val originBlock = from.block
    val offset = to.toLocation()
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
 * @see <a href="https://math.stackexchange.com/questions/1905533/find-perpendicular-distance-from-point-to-line-in-3d">https://math.stackexchange.com/questions/1905533/find-perpendicular-distance-from-point-to-line-in-3d</a>
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
 * @see <a href="https://math.stackexchange.com/questions/1905533/find-perpendicular-distance-from-point-to-line-in-3d">https://math.stackexchange.com/questions/1905533/find-perpendicular-distance-from-point-to-line-in-3d</a>
 */
fun findClosestDistanceBetweenLineAndPoint(p: Vector3f, p1: Vector3f, d1: Vector3f): Float {
    val t = max(0.0F, findClosestPointToOtherPointOnLine(p, p1, d1))
    val closestPoint = Vector3f(p1).add(Vector3f(d1).mul(t))
    return (Vector3f(closestPoint).sub(p)).length()
}

/**
 * Returns the entity the player is "looking" at if the entity's location is within [maxDistanceBetweenRayAndEntity]
 * of a ray extending from the player's eyes, going in the direction the player is looking at, and terminating at the
 * player's entity interaction range.
 *
 * This is useful for determining interaction with relatively symmetrical display entities, as those don't have
 * hitboxes and thus aren't targetable by methods like [Player.getTargetEntity].
 */
fun Player.getTargetEntityByLocation(maxDistanceBetweenRayAndEntity: Float): Entity? {
    val range = getAttribute(Attribute.ENTITY_INTERACTION_RANGE)!!.value
    val entities = getNearbyEntities(range, range, range)
    val eyeLocation = this.eyeLocation.toVector().toVector3f()
    val eyeDirection = this.eyeLocation.getDirection().toVector3f()

    for (entity in entities) {
        val distance = findClosestDistanceBetweenLineAndPoint(
            entity.location.toVector().toVector3f(),
            eyeLocation,
            eyeDirection
        )
        if (distance <= maxDistanceBetweenRayAndEntity) {
            return entity
        }
    }

    return null
}

/**
 * Returns the closest intersection of a line and a cylinder, if it exists.
 * Also returns null if the line is parallel to the cylinder.
 *
 * @param cyPos position of cylinder's origin
 * @param cyVec vector of cylinder (with length being cylinder length)
 * @param cyRad radius of cylinder
 * @param linPos position of line's origin
 * @param linVec vector of line (with length being line length)
 *
 * @see <a href="https://math.stackexchange.com/a/2613826/1291722">https://math.stackexchange.com/a/2613826/1291722</a>
 */
fun intersectionOfLineAndCylinder(cyPos: Vector3f, cyVec: Vector3f, cyRad: Float, linPos: Vector3f, linVec: Vector3f): Vector3f? {
    val linPosOffset = linPos - cyPos

    // rotate coordinate system such that the problem becomes a line-circle intersection problem in 2d
    val cyAxis = cyVec.normalize(Vector3f())
    val cyRotation = Quaternionf().rotationTo(cyAxis, Vector3f(0f, 0f, 1f))

    val rotLinPosOffset = linPosOffset.rotate(cyRotation, Vector3f())
    val rotLinVec = linVec.rotate(cyRotation, Vector3f())
    if (rotLinVec.x == 0f && rotLinVec.y == 0f) return null

    // project to 2d
    val rotLinPosOffset2d = Vector2f(rotLinPosOffset.x(), rotLinPosOffset.y())
    val linDir2d = Vector2f(rotLinVec.x, rotLinVec.y).normalize()

    val result = Vector2f()
    if (!Intersectionf.intersectRayCircle(rotLinPosOffset2d, linDir2d, Vector2f(0f, 0f), cyRad * cyRad, result)) return null
    val closestT = result.x
    // intersection is outside our line segment
    if (closestT < 0 || closestT * closestT > rotLinVec.lengthSquared()) return null

    // reproject to 3d
    val intersection3d = (rotLinPosOffset + rotLinVec.normalize(Vector3f()) * closestT).rotate(cyRotation.conjugate())
    val z = intersection3d.dot(cyAxis)
    if (z < 0 || z > cyVec.length()) return null

    return intersection3d + cyPos
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

/**
 * Schedules the entity to be removed next tick
 */
fun Entity.scheduleRemove() = Bukkit.getScheduler().runTask(Rebar, this::remove)

fun Block.getRelative(vector: Vector3i) = this.getRelative(vector.x, vector.y, vector.z)

@JvmSynthetic
suspend fun delayTicks(ticks: Long) = delay(ticks * 50)

/**
 * Creates a [CoroutineContext] for a child coroutine, with a [Job] that is a child of the current context's [Job]
 */
@JvmSynthetic
fun CoroutineContext.createChildContext(): CoroutineContext = this + Job(this[Job])

/**
 * @return Whether the entity has at least one tracking player, a tracking player is just a player who has & is receiving packets for the entity.
 */
fun Entity.hasTracker() = NmsAccessor.instance.hasTracker(this)

fun Projectile.sourceItem(): ItemStack? {
    return when(this) {
        is ThrowableProjectile -> this.item
        is SizedFireball -> this.displayItem
        is AbstractArrow -> this.itemStack
        is Firework -> this.item
        else -> persistentDataContainer.get(ProjectileRebarItemHandler.sourceItemKey, RebarSerializers.ITEM_STACK)
    }
}

fun Entity.getWeaponItem(): ItemStack? {
    return NmsAccessor.instance.getWeaponItem(this)
}

@JvmName("colorToTextColor")
fun Color.toTextColor(): TextColor {
    return TextColor.color(this.red, this.green, this.blue);
}

/**
 * Checks whether two items are of the same type, comparing Rebar IDs if they are Rebar items and vanilla IDs if they are not.
 */
fun rebarTypeSimilar(item1: ItemStack, item2: ItemStack): Boolean {
    // This does not use ItemTypeWrapper because that would be useless allocation
    val schema1 = RebarItemSchema.fromStack(item1)
    val schema2 = RebarItemSchema.fromStack(item2)
    if ((schema1 != null && schema2 == null) || (schema1 == null && schema2 == null)) {
        return false
    } else if (schema1 != null && schema2 != null) {
        return schema1 === schema2
    }
    return item1.type == item2.type
}

@JvmSynthetic // java should just use RebarItem#isRebarItem
fun ItemStack.isRebarItem(key: NamespacedKey) = RebarItem.isRebarItem(this, key)

fun <T : Any> ItemStack.forceSetData(type: DataComponentType.Valued<T>, value: Any?) {
    if (value == null) {
        unsetData(type)
        return
    }

    @Suppress("UNCHECKED_CAST")
    setData(type, value as? T ?: return)
}

fun ItemStack.setComponents(components: Map<DataComponentType, Any?>) {
    for (entry in components) {
        when (val key = entry.key) {
            is DataComponentType.NonValued -> setData(key)
            is DataComponentType.Valued<*> -> forceSetData(key, entry.value)
        }
    }
}

fun ItemStack.overriddenDataTypes(): List<DataComponentType> {
    return NmsAccessor.instance.getOverriddenTypes(this)
}

fun ItemStack.overriddenComponents(exact: Boolean): Map<DataComponentType, Any?>
    = NmsAccessor.instance.overriddenComponents(this, exact)

fun ItemStack.matchesComponents(components: Map<DataComponentType, Any?>)
    = NmsAccessor.instance.componentsMatch(this, components)

fun ItemStack.componentsEqual(components: Map<DataComponentType, Any?>)
    = NmsAccessor.instance.componentsEqual(this, components)

fun ItemStack.hasDefaultComponents(components: Set<DataComponentType>)
    = NmsAccessor.instance.hasDefaultComponents(this, components)

val ItemStack.isDefaultComponents: Boolean
    get() = NmsAccessor.instance.isDefaultComponents(this)

fun Material.getPreferredTool(): Material? {
    if (Tag.MINEABLE_AXE.isTagged(this)) {
        if (Tag.NEEDS_DIAMOND_TOOL.isTagged(this)) {
            return Material.DIAMOND_AXE
        }
        if (Tag.NEEDS_STONE_TOOL.isTagged(this)) {
            return Material.STONE_AXE
        }
        if (Tag.NEEDS_IRON_TOOL.isTagged(this)) {
            return Material.IRON_AXE
        }
        return Material.WOODEN_AXE
    }

    if (Tag.MINEABLE_PICKAXE.isTagged(this)) {
        if (Tag.NEEDS_DIAMOND_TOOL.isTagged(this)) {
            return Material.DIAMOND_PICKAXE
        }
        if (Tag.NEEDS_STONE_TOOL.isTagged(this)) {
            return Material.STONE_PICKAXE
        }
        if (Tag.NEEDS_IRON_TOOL.isTagged(this)) {
            return Material.IRON_PICKAXE
        }
        return Material.WOODEN_PICKAXE
    }

    if (Tag.MINEABLE_SHOVEL.isTagged(this)) {
        if (Tag.NEEDS_DIAMOND_TOOL.isTagged(this)) {
            return Material.DIAMOND_SHOVEL
        }
        if (Tag.NEEDS_STONE_TOOL.isTagged(this)) {
            return Material.STONE_SHOVEL
        }
        if (Tag.NEEDS_IRON_TOOL.isTagged(this)) {
            return Material.IRON_SHOVEL
        }
        return Material.WOODEN_SHOVEL
    }

    if (Tag.MINEABLE_HOE.isTagged(this)) {
        if (Tag.NEEDS_DIAMOND_TOOL.isTagged(this)) {
            return Material.DIAMOND_HOE
        }
        if (Tag.NEEDS_STONE_TOOL.isTagged(this)) {
            return Material.STONE_HOE
        }
        if (Tag.NEEDS_IRON_TOOL.isTagged(this)) {
            return Material.IRON_HOE
        }
        return Material.WOODEN_HOE
    }

    if (Tag.SWORD_EFFICIENT.isTagged(this) || Tag.SWORD_INSTANTLY_MINES.isTagged(this)) {
        return Material.WOODEN_SWORD
    }

    // TODO shears - tags will be added next update, we can't do this yet

    return null
}

val Block.breakProgress
    get() = BlockListener.blockBreakProgressMap[position] ?: 0.0F

val Block.isChunkLoaded: Boolean
    get() = world.isChunkLoaded(x shr 4, z shr 4)

fun isSymmetrical(width: Int, height: Int, list: List<*>): Boolean {
    if (width == 1) return true
    val center = width / 2
    for (y in 0..<height) {
        for (left in 0..<center) {
            val right = width - 1 - left
            if (list[left + y * width] != list[right + y * width]) {
                return false
            }
        }
    }
    return true
}

/**
 * Sets the raw [ItemStack] at [slot] without any checks or validation. This will not call any events called by normal methods.
 * This will still notify any windows backed by this inventory.
 *
 * Note: The main reason this method exists is to improve performance over the typical [setItem] method, which calls events and clones [ItemStack]s.
 * You should only call this event when you know you can ignore all events and handlers.
 */
fun VirtualInventory.unsafeSet(slot: Int, stack: ItemStack?) {
    unsafeItems[slot] = stack
    notifyWindows(slot)
}

/**
 * Sets the raw [item amount][ItemStack.getAmount] at [slot] without any checks or validation. This will not call any events called by normal methods.
 * This will still notify any windows backed by this inventory.
 *
 * Note: The main reason this method exists is to improve performance over the typical [setItemAmount] method, which calls events and clones [ItemStack]s.
 * You should only call this event when you know you can ignore all events and handlers.
 */
fun VirtualInventory.unsafeSetAmount(slot: Int, amount: Int) {
    val item = getUnsafeItem(slot)!!
    item.amount = amount
    notifyWindows(slot)
}

/**
 * Adds to the raw [item amount][ItemStack.getAmount] at [slot] without any checks or validation. This will not call any events called by normal methods.
 * This will still notify any windows backed by this inventory.
 *
 * Note: The main reason this method exists is to improve performance over the typical [addItemAmount] method, which calls events and clones [ItemStack]s.
 * You should only call this event when you know you can ignore all events and handlers.
 */
fun VirtualInventory.unsafeAdd(slot: Int, amount: Int) {
    val item = getUnsafeItem(slot)!!
    item.add(amount)
    notifyWindows(slot)
}

/**
 * Subtracts from the raw [item amount][ItemStack.getAmount] at [slot] without any checks or validation. This will not call any events called by normal methods.
 * This will still notify any windows backed by this inventory.
 *
 * Note: The main reason this method exists is to improve performance over the typical [setItem] and [addItemAmount] methods, which calls events and clones [ItemStack]s.
 * You should only call this event when you know you can ignore all events and handlers.
 */
fun VirtualInventory.unsafeSubtract(slot: Int, amount: Int) {
    val item = getUnsafeItem(slot)!!
    check(item.amount >= amount) { "Cannot subtract $amount from item with amount ${item.amount}" }
    item.subtract(amount)
    if (item.isEmpty) unsafeItems[slot] = null
    notifyWindows(slot)
}

@JvmOverloads
fun Block.editBlockData(editor: Consumer<BlockData>, applyPhysics: Boolean = true) = editBlockDataAs(BlockData::class.java, editor, applyPhysics)

@JvmOverloads
fun <D: BlockData> Block.editBlockDataAs(dataType: Class<D>, editor: Consumer<D>, applyPhysics: Boolean = true) {
    val blockData = getBlockData(dataType)
    editor.accept(blockData)
    setBlockData(blockData, applyPhysics)
}

@JvmSynthetic
inline fun <reified D: BlockData> Block.editBlockDataAs(editor: Consumer<D>, applyPhysics: Boolean = true) = editBlockDataAs(D::class.java, editor, applyPhysics)

fun <D: BlockData> Block.getBlockData(dataType: Class<D>): D {
    val blockData = this.blockData
    Preconditions.checkState(dataType.isInstance(blockData))
    return dataType.cast(this.blockData)
}

fun ItemStack.isBroken(): Boolean {
    val maxDamage = getData(DataComponentTypes.MAX_DAMAGE) ?: return false
    val damage = getData(DataComponentTypes.DAMAGE) ?: return false
    return !hasData(DataComponentTypes.UNBREAKABLE) && damage >= maxDamage
}

/**
 * This is used commonly when you are manipulating an [ItemStack] before it has been damaged, but when you know it
 * will be damaged,and you want to check if it will break after the damage is applied.
 *
 * @return if this [ItemStack] has only 1 durability left.
 */
fun ItemStack.hasOneDurabilityLeft(): Boolean {
    val maxDamage = getData(DataComponentTypes.MAX_DAMAGE) ?: return false
    val damage = getData(DataComponentTypes.DAMAGE) ?: return false
    return !hasData(DataComponentTypes.UNBREAKABLE) && damage == maxDamage - 1
}

fun Player.addToInventoryOrDrop(vararg items: ItemStack) {
    for (item in inventory.addItem(*items).values) {
        location.world.dropItemNaturally(location, item)
    }
}

const val FLUID_EPSILON = 1.0e-6

fun Component.removeStyle(): Component = this.style(Style.empty()).children(this.children().map(Component::removeStyle))
