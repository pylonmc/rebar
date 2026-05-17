package io.github.pylonmc.rebar.nms.entity

import io.github.pylonmc.rebar.Rebar
import net.minecraft.network.chat.Component
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SyncedDataHolder
import net.minecraft.network.syncher.SynchedEntityData
import net.minecraft.util.ClassTreeIdRegistry
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.ItemStack
import org.joml.Quaternionfc
import org.joml.Vector3fc
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.util.*

object EntityDataAccess {

    val ENTITY_DATA_SHARED_FLAGS_ID: EntityDataAccessor<Byte> = getAccessor("DATA_SHARED_FLAGS_ID", Entity::class.java)
    val ENTITY_DATA_AIR_SUPPLY_ID: EntityDataAccessor<Int> = getAccessor("DATA_AIR_SUPPLY_ID", Entity::class.java)
    val ENTITY_DATA_CUSTOM_NAME_VISIBLE: EntityDataAccessor<Boolean> = getAccessor("DATA_CUSTOM_NAME_VISIBLE", Entity::class.java)
    val ENTITY_DATA_CUSTOM_NAME: EntityDataAccessor<Optional<Component>> = getAccessor("DATA_CUSTOM_NAME", Entity::class.java)
    val ENTITY_DATA_SILENT: EntityDataAccessor<Boolean> = getAccessor("DATA_SILENT", Entity::class.java)
    val ENTITY_DATA_NO_GRAVITY: EntityDataAccessor<Boolean> = getAccessor("DATA_NO_GRAVITY", Entity::class.java)
    val ENTITY_DATA_POSE: EntityDataAccessor<Pose> = getAccessor("DATA_POSE", Entity::class.java)
    val ENTITY_DATA_TICKS_FROZEN: EntityDataAccessor<Int> = getAccessor("DATA_TICKS_FROZEN", Entity::class.java)

    val DISPLAY_DATA_POS_ROT_INTERPOLATION_DURATION_ID: EntityDataAccessor<Int> = getAccessor("DATA_POS_ROT_INTERPOLATION_DURATION_ID", Display::class.java)
    val DISPLAY_DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID: EntityDataAccessor<Int> = getAccessor("DATA_TRANSFORMATION_INTERPOLATION_START_DELTA_TICKS_ID", Display::class.java)
    val DISPLAY_DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID: EntityDataAccessor<Int> = getAccessor("DATA_TRANSFORMATION_INTERPOLATION_DURATION_ID", Display::class.java)
    val DISPLAY_DATA_TRANSLATION_ID: EntityDataAccessor<Vector3fc> = getAccessor("DATA_TRANSLATION_ID", Display::class.java)
    val DISPLAY_DATA_SCALE_ID: EntityDataAccessor<Vector3fc> = getAccessor("DATA_SCALE_ID", Display::class.java)
    val DISPLAY_DATA_RIGHT_ROTATION_ID: EntityDataAccessor<Quaternionfc> = getAccessor("DATA_RIGHT_ROTATION_ID", Display::class.java)
    val DISPLAY_DATA_LEFT_ROTATION_ID: EntityDataAccessor<Quaternionfc> = getAccessor("DATA_LEFT_ROTATION_ID", Display::class.java)
    val DISPLAY_DATA_BILLBOARD_RENDER_CONSTRAINTS_ID: EntityDataAccessor<Byte> = getAccessor("DATA_BILLBOARD_RENDER_CONSTRAINTS_ID", Display::class.java)
    val DISPLAY_DATA_BRIGHTNESS_OVERRIDE_ID: EntityDataAccessor<Int> = getAccessor("DATA_BRIGHTNESS_OVERRIDE_ID", Display::class.java)
    val DISPLAY_DATA_VIEW_RANGE_ID: EntityDataAccessor<Float> = getAccessor("DATA_VIEW_RANGE_ID", Display::class.java)
    val DISPLAY_DATA_SHADOW_RADIUS_ID: EntityDataAccessor<Float> = getAccessor("DATA_SHADOW_RADIUS_ID", Display::class.java)
    val DISPLAY_DATA_SHADOW_STRENGTH_ID: EntityDataAccessor<Float> = getAccessor("DATA_SHADOW_STRENGTH_ID", Display::class.java)
    val DISPLAY_DATA_WIDTH_ID: EntityDataAccessor<Float> = getAccessor("DATA_WIDTH_ID", Display::class.java)
    val DISPLAY_DATA_HEIGHT_ID: EntityDataAccessor<Float> = getAccessor("DATA_HEIGHT_ID", Display::class.java)
    val DISPLAY_DATA_GLOW_COLOR_OVERRIDE_ID: EntityDataAccessor<Int> = getAccessor("DATA_GLOW_COLOR_OVERRIDE_ID", Display::class.java)
    
    val ITEM_DISPLAY_DATA_ITEM_STACK_ID: EntityDataAccessor<ItemStack> = getAccessor("DATA_ITEM_STACK_ID", Display.ItemDisplay::class.java)
    val ITEM_DISPLAY_DATA_ITEM_DISPLAY_ID: EntityDataAccessor<Byte> = getAccessor("DATA_ITEM_DISPLAY_ID", Display.ItemDisplay::class.java)

    private val ID_REGISTRY: ClassTreeIdRegistry = run {
        try {
            val field = SynchedEntityData::class.java.getDeclaredField("ID_REGISTRY")
            field.isAccessible = true
            field[null] as ClassTreeIdRegistry
        } catch (t: Throwable) {
            Rebar.logger.severe("Failed to access SynchedEntityData ID registry!")
            throw t
        }
    }

    private val ITEMS_HANDLE: MethodHandle = run {
        try {
            val field = SynchedEntityData.Builder::class.java.getDeclaredField("itemsById")
            field.isAccessible = true
            val lookup = MethodHandles.privateLookupIn(SynchedEntityData.Builder::class.java, MethodHandles.lookup())
            lookup.unreflectSetter(field)
        } catch (t: Throwable) {
            Rebar.logger.severe("Failed to access SynchedEntityData.Builder itemsById field!")
            throw t
        }
    }

    /**
     * When creating a packet entity based on a real entity, you need a builder scaled for the real entity type but backed by the packet based implementation
     * @return A [SynchedEntityData.Builder] scaled for the real entity type but backed by the packet based implementation
     */
    internal fun <T : SyncedDataHolder> fakedDataBuilder(real: SyncedDataHolder, faked: Class<T>) : SynchedEntityData.Builder {
        try {
            val builder = SynchedEntityData.Builder(real)
            val fakedSize = ID_REGISTRY.getCount(faked)
            ITEMS_HANDLE.invoke(builder, arrayOfNulls<SynchedEntityData.DataItem<*>>(fakedSize))
            return builder
        } catch (t: Throwable) {
            Rebar.logger.severe("Failed to create sized entity data builder")
            throw t
        }
    }

    private fun <T : Any> getAccessor(name: String, clazz: Class<*>) : EntityDataAccessor<T> {
        try {
            val field = clazz.getDeclaredField(name)
            field.isAccessible = true
            @Suppress("UNCHECKED_CAST")
            return field[null] as EntityDataAccessor<T>
        } catch (t: Throwable) {
            Rebar.logger.severe("Failed to access entity data '$name'!")
            throw t
        }
    }
}