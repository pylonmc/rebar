package io.github.pylonmc.rebar.config.adapter

import net.kyori.adventure.sound.Sound
import java.lang.reflect.Type

object SoundConfigAdapter : ConfigAdapter<Sound> {
    override val type: Type = Sound::class.java

    override fun convert(value: Any): Sound {
        val section = ConfigAdapter.CONFIG_SECTION.convert(value)
        return Sound.sound(
            section.getOrThrow("name", ConfigAdapter.NAMESPACED_KEY),
            section.getOrThrow("source", ConfigAdapter.ENUM.from<Sound.Source>()),
            section.getOrThrow("volume", ConfigAdapter.FLOAT),
            section.getOrThrow("pitch", ConfigAdapter.FLOAT)
        )
    }
}