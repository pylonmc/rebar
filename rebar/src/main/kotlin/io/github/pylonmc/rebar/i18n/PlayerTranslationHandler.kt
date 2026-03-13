@file:Suppress("UnstableApiUsage")

package io.github.pylonmc.rebar.i18n

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.i18n.RebarTranslator.Companion.translate
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.util.editData
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.ChargedProjectiles
import io.papermc.paper.datacomponent.item.ItemContainerContents
import io.papermc.paper.datacomponent.item.ItemLore
import net.kyori.adventure.text.Component
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class PlayerTranslationHandler internal constructor(private val player: Player) {
    companion object {
        val STOP_ADDING_THAT = rebarKey("stop_adding_that")
    }

    fun handleItem(stack: ItemStack) {
        val rebarItem = RebarItem.fromStack(stack)
        val placeholders = rebarItem?.getPlaceholders().orEmpty()

        stack.translate(player.locale(), placeholders)

        if (rebarItem != null) {
            val isPresent = stack.persistentDataContainer.get(STOP_ADDING_THAT, RebarSerializers.BOOLEAN) ?: false

            if (!isPresent) {
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

                stack.editPersistentDataContainer {
                    it.set(STOP_ADDING_THAT, RebarSerializers.BOOLEAN, true)
                }
            }
        }

        stack.editData(DataComponentTypes.CHARGED_PROJECTILES) { chargedProjectiles ->
            val translated = chargedProjectiles.projectiles().map { projectile ->
                handleItem(projectile)
                projectile
            }
            ChargedProjectiles.chargedProjectiles(translated)
        }

        stack.editData(DataComponentTypes.CONTAINER) { container ->
            val translated = container.contents().map { item ->
                handleItem(item)
                item
            }
            ItemContainerContents.containerContents(translated)
        }
    }
}