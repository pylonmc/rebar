package io.github.pylonmc.rebar.guide.button

import io.github.pylonmc.rebar.guide.pages.item.ItemRecipesPage
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.rebarKey
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.item.AbstractBoundItem

/**
 * A button that takes you to the recipes of something.
 */
class ItemRecipesButton(val stack: ItemStack) : AbstractBoundItem() {

    override fun getItemProvider(player: Player) =
        ItemStackBuilder.gui(Material.CRAFTING_TABLE, rebarKey("guide_item_recipes"))
            .name(Component.translatable("rebar.guide.button.item-recipes"))

    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        val page = ItemRecipesPage(stack)
        if (page.pages.isNotEmpty()) {
            page.open(player)
        }
    }
}