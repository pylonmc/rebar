@file:Suppress("UnstableApiUsage")

package io.github.pylonmc.rebar.i18n

import io.github.pylonmc.rebar.i18n.RebarTranslator.Companion.translate
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.util.editData
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.text.Component
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class PlayerTranslationHandler internal constructor(private val player: Player) {

    fun handleItem(stack: ItemStack) {
        val rebarItem = RebarItem.fromStack(stack)
        val placeholders = rebarItem?.getPlaceholders().orEmpty()

        stack.translate(player.locale(), placeholders)

        if (rebarItem != null) {
            stack.editData(DataComponentTypes.LORE) { lore ->
                val newLore = lore.lines().toMutableList()
                newLore.add(GlobalTranslator.render(rebarItem.addon.footerName, player.locale()))
                if (rebarItem.isDisabled) {
                    newLore.add(
                        GlobalTranslator.render(
                            Component.translatable("rebar.message.disabled.lore"),
                            player.locale()
                        )
                    )
                }
                ItemLore.lore(newLore)
            }
        }
    }
}