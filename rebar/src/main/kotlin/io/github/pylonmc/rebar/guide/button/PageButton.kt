package io.github.pylonmc.rebar.guide.button

import io.github.pylonmc.rebar.guide.pages.base.GuidePage
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.TooltipDisplay
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.ItemProvider

/**
 * A button that opens another page in the guide.
 *
 * The name and lore of [stack] are ignored, and overwritten by the supplied name and lore.
 *
 * The name will be inherited from the page name. The lore will be blank, unless you add it
 * at `<your-addon>.guide.button.<button-key>: "your name here"`
 *
 * @see GuidePage
 */
open class PageButton(val stack: ItemStack, val page: GuidePage) : GuideButton() {

    constructor(builder: ItemStackBuilder, page: GuidePage) : this(builder.build(), page)

    constructor(material: Material, page: GuidePage) : this(ItemStack(material), page)

    @Suppress("UnstableApiUsage")
    override fun getItemProvider(viewer: Player): ItemProvider = ItemStackBuilder.gui(stack.clone(), "${rebarKey("guide_page")}:${page.key}")
        .name(Component.translatable("${page.key.namespace}.guide.page.${page.key.key}"))
        .clearLore()
        .lore(Component.translatable("${page.key.namespace}.guide.button.${page.key.key}.lore", ""))
        .set(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hiddenComponents(hiddenComponents))

    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        page.open(player)
    }

    override fun shouldDisplay(player: Player) = page.shouldDisplay(player)

    override fun priority(): Double = 0.0

    companion object {
        /**
         * Page buttons are just icons in the guide, we don't want any random component data to be
         * appended to the tooltip. (for ex: +5 attack damage)
         *
         * Here we specify all the components that may add to the tooltip that we want to hide.
         */
        @Suppress("UnstableApiUsage")
        val hiddenComponents = setOf(
            DataComponentTypes.DAMAGE, DataComponentTypes.MAX_DAMAGE, DataComponentTypes.UNBREAKABLE,
            DataComponentTypes.ENCHANTMENTS, DataComponentTypes.STORED_ENCHANTMENTS,
            DataComponentTypes.ATTRIBUTE_MODIFIERS,
            DataComponentTypes.BANNER_PATTERNS, DataComponentTypes.PROVIDES_BANNER_PATTERNS,
            DataComponentTypes.CAN_PLACE_ON, DataComponentTypes.CAN_BREAK,
            DataComponentTypes.FOOD, DataComponentTypes.CONSUMABLE,
            DataComponentTypes.TOOL, DataComponentTypes.WEAPON,
            DataComponentTypes.DYED_COLOR,
            DataComponentTypes.MAP_ID, DataComponentTypes.MAP_DECORATIONS,
            DataComponentTypes.CHARGED_PROJECTILES, DataComponentTypes.BUNDLE_CONTENTS, DataComponentTypes.POTION_CONTENTS, DataComponentTypes.POT_DECORATIONS,
            DataComponentTypes.CONTAINER, DataComponentTypes.CONTAINER_LOOT,
            DataComponentTypes.TRIM,
            DataComponentTypes.FIREWORK_EXPLOSION, DataComponentTypes.FIREWORKS,
            DataComponentTypes.PROFILE
        )
    }

}
