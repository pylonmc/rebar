package io.github.pylonmc.rebar.guide.button

import io.github.pylonmc.rebar.guide.pages.MachineRecipesPage
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.recipe.RecipeType
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
class MachineRecipesButton(val stack: ItemStack, val recipeType: RecipeType<*>) : AbstractBoundItem() {

    override fun getItemProvider(player: Player) =
        ItemStackBuilder.gui(Material.CRAFTER, rebarKey("guide_machine_recipes"))
            .name(Component.translatable("rebar.guide.button.machine-recipes"))

    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        val page = MachineRecipesPage(recipeType)
        if (page.pages.isNotEmpty()) {
            page.open(player)
        }
    }
}