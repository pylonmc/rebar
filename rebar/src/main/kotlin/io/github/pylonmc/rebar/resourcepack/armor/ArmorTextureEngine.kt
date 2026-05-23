@file:Suppress("UnstableApiUsage")

package io.github.pylonmc.rebar.resourcepack.armor

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.RebarItemSchema
import io.github.pylonmc.rebar.item.base.RebarArmor
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.Equippable
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

object ArmorTextureEngine {
    val customArmorTexturesKey = rebarKey("custom_armor_textures")

    @JvmStatic
    var Player.hasCustomArmorTextures: Boolean
        get() = RebarConfig.ArmorTextureConfig.FORCED || this.persistentDataContainer.getOrDefault(customArmorTexturesKey, PersistentDataType.BOOLEAN, false)
        set(value) = this.persistentDataContainer.set(customArmorTexturesKey, PersistentDataType.BOOLEAN, RebarConfig.ArmorTextureConfig.FORCED || value)

    fun handleItem(player: Player, stack: ItemStack) {
        if (!RebarConfig.ArmorTextureConfig.ENABLED) return
        if (!player.hasCustomArmorTextures) {
            if (stack.persistentDataContainer.has(customArmorTexturesKey)) {
                resetItem(stack)
            }
            return
        }

        val schema = RebarItemSchema.fromStack(stack)
        if (schema == null || !RebarItem.isRebarItem(stack, RebarArmor::class.java)) {
            return
        }

        val template = schema.getOriginalTemplate()
        val defaultAssetId = template.getDataOrDefault(DataComponentTypes.EQUIPPABLE, template.type.getDefaultData(DataComponentTypes.EQUIPPABLE))?.assetId()
        val component = stack.getDataOrDefault(DataComponentTypes.EQUIPPABLE, stack.type.getDefaultData(DataComponentTypes.EQUIPPABLE))
        if (component == null || component.assetId() != defaultAssetId) {
            return
        }

        val armor = RebarItem.fromStack(template, RebarArmor::class.java) ?: return
        stack.setData(DataComponentTypes.EQUIPPABLE, Equippable.equippable(component.slot())
            .assetId(armor.equipmentType)
            .swappable(component.swappable())
            .allowedEntities(component.allowedEntities())
            .cameraOverlay(component.cameraOverlay())
            .canBeSheared(component.canBeSheared())
            .damageOnHurt(component.damageOnHurt())
            .dispensable(component.dispensable())
            .equipSound(component.equipSound())
            .shearSound(component.shearSound())
            .canBeSheared(component.canBeSheared()))
        stack.editPersistentDataContainer { it.set(customArmorTexturesKey, PersistentDataType.BOOLEAN, true) }
    }

    fun resetItem(item: ItemStack) {
        val armor = RebarItem.fromStack(item, RebarArmor::class.java)
        if (armor !is RebarItem) return

        val component = item.getData(DataComponentTypes.EQUIPPABLE) ?: return
        if (component.assetId() == armor.equipmentType) {
            val template = armor.schema.getOriginalTemplate()
            if (!template.isDataOverridden(DataComponentTypes.EQUIPPABLE)) {
                item.resetData(DataComponentTypes.EQUIPPABLE)
                return
            }

            val templateComponent = template.getData(DataComponentTypes.EQUIPPABLE)
            if (templateComponent == null) {
                item.unsetData(DataComponentTypes.EQUIPPABLE)
            } else {
                item.setData(DataComponentTypes.EQUIPPABLE, templateComponent)
            }
        }
        item.editPersistentDataContainer { it.remove(customArmorTexturesKey) }
    }
}