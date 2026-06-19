package io.github.pylonmc.rebar.config.adapter

import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.util.HSVLike
import org.bukkit.Bukkit
import java.lang.reflect.Type

object TextColorConfigAdapter : ConfigAdapter<TextColor> {
    override val type: Type = TextColor::class.java

    override fun convert(value: Any): TextColor {
        if (value is String) {
            return if (value.startsWith("#")) {
                val hexString = value.trimEnd()
                TextColor.fromHexString(hexString)
                    ?: throw IllegalArgumentException("Unable to convert hex color $hexString to TextColor")
            } else {
                val nameString = value.lowercase()
                Bukkit.getLogger().severe { "${NamedTextColor.NAMES.keys()}" }
                NamedTextColor.NAMES.valueOr(nameString, null)
                    ?: throw IllegalArgumentException("There is no TextColor with the name '$nameString'")
            }
        } else {
            val section = ConfigAdapter.CONFIG_SECTION.convert(value)

            if (section.has("h") && section.has("s") && section.has("v")) {
                val h = section.getOrThrow("h", ConfigAdapter.FLOAT)
                val s = section.getOrThrow("s", ConfigAdapter.FLOAT)
                val v = section.getOrThrow("v", ConfigAdapter.FLOAT)
                return TextColor.color(HSVLike.hsvLike(h, s, v))
            }

            if (section.has("r") && section.has("g") && section.has("b")) {
                val r = section.getOrThrow("r", ConfigAdapter.INTEGER)
                val g = section.getOrThrow("g", ConfigAdapter.INTEGER)
                val b = section.getOrThrow("b", ConfigAdapter.INTEGER)
                return TextColor.color(r, g, b)
            }

            throw IllegalArgumentException("Could not find hsv or rgb values in TextColor config section")
        }
    }

}