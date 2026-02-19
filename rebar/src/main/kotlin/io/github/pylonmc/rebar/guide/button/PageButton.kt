package io.github.pylonmc.rebar.guide.button

import io.github.pylonmc.rebar.guide.pages.base.GuidePage
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.rebarKey
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
open class PageButton(val stack: ItemStack, val page: GuidePage) : AbstractItem() {

    constructor(material: Material, page: GuidePage) : this(ItemStack(material), page)

    override fun getItemProvider(viewer: Player): ItemProvider = ItemStackBuilder.gui(stack, "${rebarKey("guide_page")}:${page.key}")
        .name(Component.translatable("${page.key.namespace}.guide.page.${page.key.key}"))
        .clearLore()
        .lore(Component.translatable("${page.key.namespace}.guide.button.${page.key.key}", ""))

    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        page.open(player)
    }

    open fun priority(): Double = 0.0
}
