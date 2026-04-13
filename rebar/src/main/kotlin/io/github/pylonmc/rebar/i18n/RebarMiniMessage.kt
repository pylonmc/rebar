@file:JvmName("RebarMiniMessage")

package io.github.pylonmc.rebar.i18n

import io.github.pylonmc.rebar.util.gui.unit.MetricPrefix
import io.github.pylonmc.rebar.util.gui.unit.UnitFormat
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.Context
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Modifying
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.NamespacedKey

/**
 * Rebar's custom MiniMessage instance with custom tags. This instance is used when translating
 * any Rebar translation keys.
 *
 * ### Custom Tags
 * - `<arrow>`|`<arrow:\[color\]>` - Inserts a right arrow (→) with the specified color (default: 0x666666)
 * - `<guidearrow> - Shorthand for `<arrow:0x3a293>`
 * - `<diamond>`|`<diamond:\[color\]>` - Inserts a diamond (◆) with the specified color (default: 0x666666)
 * - `<star>`|`<star:\[color\]>` - Inserts a star (★) with the specified color (default: [NamedTextColor.BLUE])
 * - `<insn></insn>` - Applies a yellow styling (0xf9d104), used for instructions
 * - `<guideinsn></guideinsn>` - Applies a purple styling (0xc907f4), used for guide instructions
 * - `<attr></attr>` - Applies a cyan styling (0xa9d9e8), used for attributes
 * - `<unit:\[prefix\]:[unit]></unit>` - Formats a **constant** number as a unit, with an optional metric prefix
 * - `<nbsp></nbsp>` - Replaces spaces with non-breaking spaces ( ), useful for preventing line breaks in lore
 * - `<item:\[item_name\]>` - Renders the translated name of a vanilla/rebar item (e.g., `<item:stone>` → "Stone", `<item:pylon:loupe>` → "Loupe")
 * - `<entity:\[entity_type\]>` - Renders the translated name of an entity type (e.g., `<entity:creeper>` → "Creeper")
 * - `<effect:\[effect_type\]>` - Renders the translated name of a potion effect (e.g., `<effect:speed>` → "Speed")
 * - `<enchant:\[enchant_name\]>` - Renders the translated name of an enchantment (e.g., `<enchant:sharpness>` → "Sharpness")
 * - `<biome:\[biome_name\]>` - Renders the translated name of a biome (e.g., `<biome:plains>` → "Plains")
 *   
 * For item, entity, effect, enchant, and biome tags:
 * - If no namespace is provided (e.g., `stone`), it defaults to `minecraft:stone`
 * - Full namespaced keys are supported (e.g., `myplugin:custom_item`)
 * - For custom items without a minecraft translation, falls back to rebar-style key (e.g., `rebar:custom`)
 */
val customMiniMessage = MiniMessage.builder()
    .tags(TagResolver.standard())
    .editTags {
        it.tag("arrow", ::arrow)
        it.tag("guidearrow", ::guidearrow)
        it.tag("diamond", ::diamond)
        it.tag("star", ::star)
        it.tag("insn") { _, _ -> Tag.styling(TextColor.color(0xf9d104)) }
        it.tag("guideinsn") { _, _ -> Tag.styling(TextColor.color(0xc907f4)) }
        it.tag("story") { _, _ ->
            Tag.styling { builder ->
                builder.color(TextColor.color(0xcc9bf2)).decorate(TextDecoration.ITALIC)
            }
        }
        it.tag("attr") { _, _ -> Tag.styling(TextColor.color(0xa9d9e8)) }
        it.tag("unit", ::unit)
        it.tag("nbsp", ::nbsp)
        it.tag("item", ::item)
        it.tag("entity", ::entity)
        it.tag("effect", ::effect)
        it.tag("enchant", ::enchantment)
        it.tag("biome", ::biome)
    }
    .strict(false)
    .build()

private fun arrow(args: ArgumentQueue, @Suppress("unused") ctx: Context): Tag {
    val color = args.peek()?.value()?.let(::parseColor) ?: TextColor.color(0x666666)
    return Tag.selfClosingInserting(Component.text("\u2192").color(color))
}

private fun guidearrow(args: ArgumentQueue, @Suppress("unused") ctx: Context): Tag {
    val color = TextColor.color(0x3a293)
    return Tag.selfClosingInserting(Component.text("\u2192").color(color))
}

private fun diamond(args: ArgumentQueue, @Suppress("unused") ctx: Context): Tag {
    val color = args.peek()?.value()?.let(::parseColor) ?: TextColor.color(0x666666)
    return Tag.selfClosingInserting(Component.text("\u25C6").color(color))
}

private fun star(args: ArgumentQueue, @Suppress("unused") ctx: Context): Tag {
    val color = args.peek()?.value()?.let(::parseColor) ?: NamedTextColor.BLUE
    return Tag.selfClosingInserting(Component.text("\u2605").color(color))
}

private fun unit(args: ArgumentQueue, @Suppress("unused") ctx: Context): Tag {
    val args = args.iterator().asSequence().toList()
    val (prefix, unitName) = when (args.size) {
        2 -> enumValueOf<MetricPrefix>(args[0].value().uppercase()) to args[1].value()
        1 -> null to args[0].value()
        else -> throw ctx.newException("Expected 1 or 2 arguments, got ${args.size}")
    }
    val unit = UnitFormat.allUnits[unitName]
        ?: throw ctx.newException("No such unit: $unitName")
    return Replacing {
        val content = PlainTextComponentSerializer.plainText().serialize(it).trim()
        val number = content.toBigDecimalOrNull() ?: throw ctx.newException("Expected a number, got '$content'")
        unit.format(number)
            .prefix(prefix ?: MetricPrefix.NONE)
            .asComponent()
    }
}

@Suppress("unused")
private fun nbsp(args: ArgumentQueue, ctx: Context): Tag {
    return Replacing { it.replaceText(nbspReplacement) }
}

private val nbspReplacement = TextReplacementConfig.builder()
    .matchLiteral(" ")
    .replacement(Typography.nbsp.toString())
    .build()

private fun item(args: ArgumentQueue, ctx: Context): Tag {
    val nsKey = parseNamespacedKey(args, ctx)
    val translationKey = if (nsKey.namespace == NamespacedKey.MINECRAFT) {
        val material = Material.matchMaterial(nsKey.key)
            ?: throw ctx.newException("Unknown material: $nsKey")
        material.translationKey()
    } else {
        "${nsKey.namespace}.item.${nsKey.key}.name"
    }

    return Tag.selfClosingInserting(Component.translatable(translationKey))
}

private fun entity(args: ArgumentQueue, ctx: Context): Tag {
    val nsKey = parseNamespacedKey(args, ctx)
    val entityType = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENTITY_TYPE).get(nsKey)
        ?: throw ctx.newException("Unknown entity type: $nsKey")

    return Tag.selfClosingInserting(Component.translatable(entityType.translationKey()))
}

private fun effect(args: ArgumentQueue, ctx: Context): Tag {
    val nsKey = parseNamespacedKey(args, ctx)
    val effect = RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT).get(nsKey)
        ?: throw ctx.newException("Unknown potion effect type: $nsKey")

    return Tag.selfClosingInserting(Component.translatable(effect.translationKey()))
}

private fun enchantment(args: ArgumentQueue, ctx: Context): Tag {
    val nsKey = parseNamespacedKey(args, ctx)
    val enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(nsKey)
        ?: throw ctx.newException("Unknown enchantment: $nsKey")

    return Tag.selfClosingInserting(Component.translatable(enchantment.translationKey()))
}

private fun biome(args: ArgumentQueue, ctx: Context): Tag {
    val nsKey = parseNamespacedKey(args, ctx)
    val biome = RegistryAccess.registryAccess().getRegistry(RegistryKey.BIOME).get(nsKey)
        ?: throw ctx.newException("Unknown biome: $nsKey")

    return Tag.selfClosingInserting(Component.translatable(biome.translationKey()))
}

private fun parseColor(color: String): TextColor {
    val theOnlyTrueWayToSpellGray = color.replace("grey", "gray")
    return TextColor.fromHexString(theOnlyTrueWayToSpellGray)
        ?: NamedTextColor.NAMES.value(theOnlyTrueWayToSpellGray)
        ?: throw IllegalArgumentException("No such color: $color")
}

private fun parseNamespacedKey(args: ArgumentQueue, ctx: Context): NamespacedKey {
    val argsList = args.iterator().asSequence().toList()
    val arg = when (argsList.size) {
        2 -> "${argsList[0].value()}:${argsList[1].value()}"
        1 -> argsList[0].value()
        else -> throw ctx.newException("Expected 1 or 2 arguments, got ${argsList.size}")
    }
    return NamespacedKey.fromString(arg) ?: throw ctx.newException("Invalid NamespacedKey: $arg")
}

@Suppress("FunctionName")
private inline fun Replacing(crossinline block: (Component) -> ComponentLike): Tag {
    return Modifying { current, depth ->
        if (depth == 0) block(current).asComponent()
        else Component.empty()
    }
}

private operator fun ArgumentQueue.iterator(): Iterator<Tag.Argument> {
    return object : Iterator<Tag.Argument> {
        override fun hasNext() = peek() != null
        override fun next() = popOr("No more arguments available")
    }
}
