package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.datatypes.RebarSerializers.KEYED
import io.github.pylonmc.rebar.fluid.tags.FluidTemperature
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.RandomizedSound
import net.kyori.adventure.sound.Sound
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.joml.Vector2d
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import java.lang.reflect.Type

interface ConfigAdapter<T> {

    val type: Type

    /**
     * Converts the given value obtained from config to the target type [T].
     */
    fun convert(value: Any): T

    @Suppress("unused")
    companion object {
        // @formatter:off
        @JvmField val BYTE = numberAdapter(Byte.MIN_VALUE, Byte.MAX_VALUE, Number::toByte)
        @JvmField val SHORT = numberAdapter(Short.MIN_VALUE, Short.MAX_VALUE, Number::toShort)
        @JvmField val INTEGER = numberAdapter(Int.MIN_VALUE, Int.MAX_VALUE, Number::toInt)
        @JvmField val LONG = numberAdapter(Long.MIN_VALUE, Long.MAX_VALUE, Number::toLong)
        @JvmField val FLOAT = numberAdapter(Float.MIN_VALUE, Float.MAX_VALUE, Number::toFloat)
        @JvmField val DOUBLE = numberAdapter(Double.MIN_VALUE, Double.MAX_VALUE, Number::toDouble)
        @JvmField val INT_RANGE = IntRangeAdapter
        @JvmField val CHAR = ConfigAdapter { (it as String).single() }
        @JvmField val BOOLEAN = ConfigAdapter { it as Boolean }
        @JvmField val ANY = ConfigAdapter { it }

        @JvmField val STRING = ConfigAdapter { it.toString() }
        @JvmField val LIST = ListConfigAdapter
        @JvmField val SET = SetConfigAdapter
        @JvmField val MAP = MapConfigAdapter
        @JvmField val ENUM = EnumConfigAdapter

        @JvmField val UUID = UUIDConfigAdapter

        @JvmField val KEYED = KeyedConfigAdapter
        @JvmField val NAMESPACED_KEY = ConfigAdapter { NamespacedKey.fromString(STRING.convert(it))!! }
        @JvmField val MATERIAL = KEYED.fromRegistry(Registry.MATERIAL)
        @JvmField val ITEM_STACK = ItemStackConfigAdapter
        @JvmField val BLOCK_DATA = ConfigAdapter { Bukkit.createBlockData(STRING.convert(it)) }

         @JvmField val VECTOR_2I = ConfigAdapter {
            val list = (it as List<*>).filterIsInstance<Int>()
            check(list.size == 2) { "List must be of size 2" }
            Vector2i(list[0], list[1])
        }
        @JvmField val VECTOR_2F = ConfigAdapter {
            val list = (it as List<*>).filterIsInstance<Float>()
            check(list.size == 2) { "List must be of size 2" }
            Vector2f(list[0], list[1])
        }
        @JvmField val VECTOR_2D = ConfigAdapter {
            val list = (it as List<*>).filterIsInstance<Double>()
            check(list.size == 2) { "List must be of size 2" }
            Vector2d(list[0], list[1])
        }
        @JvmField val VECTOR_3I = ConfigAdapter {
            val list = (it as List<*>).filterIsInstance<Int>()
            check(list.size == 3) { "List must be of size 3" }
            Vector3i(list[0], list[1], list[2])
        }
        @JvmField val VECTOR_3F = ConfigAdapter {
            val list = (it as List<*>).filterIsInstance<Float>()
            check(list.size == 3) { "List must be of size 3" }
            Vector3f(list[0], list[1], list[2])
        }
        @JvmField val VECTOR_3D = ConfigAdapter {
            val list = (it as List<*>).filterIsInstance<Double>()
            check(list.size == 3) { "List must be of size 3" }
            Vector3d(list[0], list[1], list[2])
        }

        /**
         * A [ConfigAdapter] for in game [Sound]s,
         * comprised of a key, source, volume and pitch.
         *
         * For example:
         * ```yaml
         * hammer-sound:
         *   sound: minecraft:block.anvil.use
         *   source: player
         *   volume: 0.5
         *   pitch: 1.0
         * ```
         */
        @JvmField val SOUND = SoundConfigAdapter

        /**
         * A [ConfigAdapter] for [RandomizedSound]s,
         * which accept either a single sound or a list of sounds, a source,
         * and ranges for volume and pitch.
         *
         * Picking a random sound and a random volume and pitch from the ranges
         * when played.
         *
         * The volume and pitch can either be specified as a single value,
         * a list of two values (min and max), or specific min and max keys.
         *
         * For example:
         * ```yaml
         * hammer-sound:
         *   sounds:
         *   - minecraft:block.anvil.use
         *   - minecraft:block.anvil.land
         *   source: player
         *   volume:
         *     min: 0.3
         *     max: 0.7
         *   pitch:
         *     - 0.8
         *     - 1.2
         *```
         */
        @JvmField val RANDOMIZED_SOUND = RandomizedSoundConfigAdapter

        @JvmField val REBAR_FLUID = KEYED.fromRegistry(RebarRegistry.FLUIDS)
        @JvmField val FLUID_TEMPERATURE = ENUM.from<FluidTemperature>()
        @JvmField val FLUID_OR_ITEM = FluidOrItemConfigAdapter
        @JvmField val RECIPE_INPUT = RecipeInputConfigAdapter
        @JvmField val RECIPE_INPUT_ITEM = RecipeInputItemAdapter
        @JvmField val RECIPE_INPUT_FLUID = RecipeInputFluidAdapter
        @JvmField val ITEM_TAG = ItemTagConfigAdapter
        @JvmField val WEIGHTED_SET = WeightedSetConfigAdapter
        @JvmField val CULLING_PRESET = CullingPresetConfigAdapter
        @JvmField val WAILA_DISPLAY = WailaDisplayConfigAdapter
        @JvmField val CONFIG_SECTION = ConfigSectionConfigAdapter
        @JvmField val CONTRIBUTOR = ContributorConfigAdapter
        @JvmField val TEXT_COLOR = TextColorConfigAdapter
        // @formatter:on

        private inline fun <reified T : Number> numberAdapter(
            min: T,
            max: T,
            crossinline toType: Number.() -> T
        ): ConfigAdapter<T> = ConfigAdapter { value ->
            if (value is String) return@ConfigAdapter value.toDouble().toType()

            check (value is Number) { "Expected ${Number::class.java}, got ${value::class.java}" }
            if (value is Float || value is Double) {
                check(min is Float || min is Double) { "Expected ${T::class.java.canonicalName}, got ${value::class.java.canonicalName}" }
            }

            if (min !is Double) {
                check(value.toDouble() in (min.toDouble())..(max.toDouble())) { "Expected value between $min and $max, got $value" }
            }
            return@ConfigAdapter value.toDouble().toType()
        }
    }
}

@JvmSynthetic
inline fun <reified T> ConfigAdapter(crossinline convert: (Any) -> T): ConfigAdapter<T> =
    object : ConfigAdapter<T> {
        override val type: Type = T::class.java
        override fun convert(value: Any): T = convert(value)
    }