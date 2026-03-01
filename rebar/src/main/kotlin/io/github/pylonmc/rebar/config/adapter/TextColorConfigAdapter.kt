package io.github.pylonmc.rebar.config.adapter

import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.util.HSVLike
import java.lang.reflect.Type

object TextColorConfigAdapter : ConfigAdapter<TextColor> {
    override val type: Type = TextColor::class.java

    override fun convert(value: Any): TextColor {
        if(value is String){
            val color = TextColor.fromHexString("#${value.trimStart().trimEnd()}")
            if(color != null){
                return color
            } else {
                throw RuntimeException("Unable to convert hex color #${value.trimStart().trimEnd()} to TextColor")
            }
        } else {
            val section = ConfigAdapter.CONFIG_SECTION.convert(value)
            val h = section.get("h", ConfigAdapter.FLOAT)
            val s = section.get("s", ConfigAdapter.FLOAT)
            val v = section.get("v", ConfigAdapter.FLOAT)
            if(h != null && s != null && v != null){
                return TextColor.color(HSVLike.hsvLike(h, s, v))
            }
            val r = section.get("r", ConfigAdapter.INTEGER)
            val g = section.get("g", ConfigAdapter.INTEGER)
            val b = section.get("b", ConfigAdapter.INTEGER)
            if(r != null && g != null && b != null && v != null){
                return TextColor.color(r, g, b)
            }
            throw RuntimeException("Could not find hsv or rgb values in TextColor config section")
        }
    }

}