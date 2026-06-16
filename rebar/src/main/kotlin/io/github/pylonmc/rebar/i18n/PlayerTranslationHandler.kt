@file:Suppress("UnstableApiUsage")

package io.github.pylonmc.rebar.i18n

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.i18n.RebarTranslator.Companion.translate
import io.github.pylonmc.rebar.i18n.RebarTranslator.Companion.untranslate
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.resourcepack.armor.ArmorTextureEngine
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
    fun handleItem(stack: ItemStack) {
        val rebarItem = RebarItem.fromStack(stack)
        val rebarFluid = RebarFluid.fromStack(stack)
        val placeholders = rebarItem?.getPlaceholders().orEmpty()

        stack.translate(player, placeholders)

        if ((rebarItem != null || rebarFluid != null) && !stack.persistentDataContainer.has(FOOTER_APPENDED)) {
            stack.editData(DataComponentTypes.LORE) { lore ->
                val newLore = lore.lines().toMutableList()
                if (!stack.enchantments.isEmpty()) {
                    newLore.addFirst(Component.empty())
                }
                val addon = rebarItem?.addon ?: rebarFluid?.addon!!
                newLore.add(GlobalTranslator.render(addon.footerName, player.locale()))
                if (rebarItem?.isDisabled ?: false) {
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
                it.set(FOOTER_APPENDED, RebarSerializers.BOOLEAN, true)
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

        ArmorTextureEngine.handleItem(player, stack)
    }

    companion object {
        val FOOTER_APPENDED = rebarKey("footer_appended")

        fun resetItem(stack: ItemStack) {
            stack.untranslate()

            stack.editData(DataComponentTypes.CHARGED_PROJECTILES) { chargedProjectiles ->
                val translated = chargedProjectiles.projectiles().map { projectile ->
                    resetItem(projectile)
                    projectile
                }
                ChargedProjectiles.chargedProjectiles(translated)
            }

            stack.editData(DataComponentTypes.CONTAINER) { container ->
                val translated = container.contents().map { item ->
                    resetItem(item)
                    item
                }
                ItemContainerContents.containerContents(translated)
            }

            stack.editPersistentDataContainer {
                it.remove(FOOTER_APPENDED)
            }
        }
    }
}