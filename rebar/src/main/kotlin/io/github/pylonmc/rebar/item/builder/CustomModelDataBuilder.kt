package io.github.pylonmc.rebar.item.builder

import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.CustomModelData
import org.bukkit.Color
import org.bukkit.inventory.ItemStack

@Suppress("UnstableApiUsage")
class CustomModelDataBuilder {
    val strings = mutableListOf<String>()
    val floats = mutableListOf<Float>()
    val flags = mutableListOf<Boolean>()
    val colors = mutableListOf<Color>()

    fun addString(string: String) = apply {
        this.strings.add(string)
    }

    fun addStrings(strings: List<String>) = apply {
        this.strings.addAll(strings)
    }

    fun addStrings(vararg strings: String) = apply {
        strings.forEach { addString(it) }
    }

    fun addFloat(float: Float) = apply {
        this.floats.add(float)
    }

    fun addFloats(floats: List<Float>) = apply {
        this.floats.addAll(floats)
    }

    fun addFloats(vararg floats: Float) = apply {
        floats.forEach { addFloat(it) }
    }

    fun addFlag(flag: Boolean) = apply {
        this.flags.add(flag)
    }

    fun addFlags(flags: List<Boolean>) = apply {
        this.flags.addAll(flags)
    }

    fun addFlags(vararg flags: Boolean) = apply {
        flags.forEach { addFlag(it) }
    }

    fun addColor(color: Color) = apply {
        this.colors.add(color)
    }

    fun addColors(colors: List<Color>) = apply {
        this.colors.addAll(colors)
    }

    fun addColors(vararg colors: Color) = apply {
        colors.forEach { addColor(it) }
    }

    fun build(): CustomModelData = CustomModelData.customModelData()
        .addStrings(this.strings)
        .addFloats(this.floats)
        .addFlags(this.flags)
        .addColors(this.colors)
        .build()

    companion object {
        fun of(customModelData: CustomModelData?): CustomModelDataBuilder {
            val builder = CustomModelDataBuilder()
            if (customModelData != null) {
                builder.addStrings(customModelData.strings())
                builder.addFloats(customModelData.floats())
                builder.addFlags(customModelData.flags())
                builder.addColors(customModelData.colors())
            }
            return builder
        }

        fun of(itemStack: ItemStack) = of(itemStack.getData(DataComponentTypes.CUSTOM_MODEL_DATA))

        fun of(builder: ItemStackBuilder) = of(builder.stack)
    }
}