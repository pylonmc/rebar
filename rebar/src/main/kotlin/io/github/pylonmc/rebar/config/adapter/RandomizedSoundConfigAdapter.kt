package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.util.RandomizedSound
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.configuration.ConfigurationSection
import java.lang.reflect.Type

object RandomizedSoundConfigAdapter : ConfigAdapter<RandomizedSound> {
    override val type: Type = RandomizedSound::class.java

    override fun convert(value: Any): RandomizedSound {
        val section = ConfigAdapter.CONFIG_SECTION.convert(value)
        val keys = mutableListOf<Key>()
        section.get("sound", ConfigAdapter.NAMESPACED_KEY)?.let(keys::add)
        section.get("sounds", ConfigAdapter.LIST.from(ConfigAdapter.NAMESPACED_KEY))?.let(keys::addAll)

        if (keys.isEmpty()) {
            section.get("sound", ConfigAdapter.STRING) // will report the error
            throw AssertionError()
        }

        return RandomizedSound(
            keys,
            section.getOrThrow("source", ConfigAdapter.ENUM.from<Sound.Source>()),
            getRange(section, "volume"),
            getRange(section, "pitch")
        )
    }

    private fun getRange(section: ConfigSection, key: String): Pair<Double, Double> {
        return when (val range = section.getOrThrow(key, ConfigAdapter.ANY)) {
            is ConfigurationSection, is Map<*, *> -> {
                val range = ConfigAdapter.CONFIG_SECTION.convert(range)
                range.getOrThrow("min", ConfigAdapter.DOUBLE) to range.getOrThrow("max", ConfigAdapter.DOUBLE)
            }

            is List<*> -> ConfigAdapter.DOUBLE.convert(range[0]!!) to ConfigAdapter.DOUBLE.convert(range[1]!!)

            else -> try {
                val value = range.toString().toDouble()
                Pair(value, value)
            } catch (_: Throwable) {
                throw IllegalArgumentException("Sound '$key' field is not a valid number or range: $range")
            }
        }
    }
}