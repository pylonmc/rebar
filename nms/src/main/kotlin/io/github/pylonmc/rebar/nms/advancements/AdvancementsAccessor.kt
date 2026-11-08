package io.github.pylonmc.rebar.nms.advancements

import com.google.common.collect.ImmutableMap
import io.github.pylonmc.rebar.advancements.RebarAdvancement
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.minecraft.advancements.*
import net.minecraft.advancements.triggers.ImpossibleTrigger
import net.minecraft.core.ClientAsset
import net.minecraft.core.HolderSet
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.ServerAdvancementManager
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStackTemplate
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.util.CraftNamespacedKey
import java.util.*
import java.util.function.Consumer
import kotlin.collections.map

object AdvancementsAccessor {

    fun registerRebarAdvancement(
        advancement: RebarAdvancement,
        key: NamespacedKey
    ): org.bukkit.advancement.Advancement {
        val nmsAdvancement = convertRebarAdvancementToNms(advancement, key)
        val bukkitAdvancement = addNmsAdvancementToServer(key, nmsAdvancement)
        advancement.register()
        resyncAllAdvancements()
        return bukkitAdvancement
    }

    private fun convertRebarAdvancementToNms(advancement: RebarAdvancement, key: NamespacedKey): Advancement = Advancement(
        Optional.ofNullable(advancement.parent).map { parent -> CraftNamespacedKey.toMinecraft(parent) },
        Optional.ofNullable(advancement.displayInfo).map { info ->
            DisplayInfo(
                ItemStackTemplate.fromNonEmptyStack(
                    (info.icon.itemType.createItemStack() as CraftItemStack).handle
                ),
                PaperAdventure.asVanilla(
                    info.title
                ),
                PaperAdventure.asVanilla(
                    info.description
                ),
                Optional.ofNullable(info.iconBackground)
                    .map { background -> ClientAsset.ResourceTexture(CraftNamespacedKey.toMinecraft(background)) },
                AdvancementType.valueOf(info.iconFrame.uppercase()),
                info.showToast,
                info.announceToChat,
                info.hidden
            )
        },
        AdvancementRewards(
            0,
            HolderSet.direct(),
            listOf(),
            Optional.ofNullable(null)
        ),
        advancement.criteria.map { it.key }.associateWith { key ->
            ImpossibleTrigger().createCriterion(ImpossibleTrigger.TriggerInstance())
        },
        AdvancementRequirements(
            advancement.requirements
        ),
        false // Not allowing this to be configured to avoid spamming Mojang's servers with junk data about fake advancements
    )

    private fun addNmsAdvancementToServer(key: NamespacedKey, nmsAdvancement: Advancement) : org.bukkit.advancement.Advancement {
        val advancementsManagerNms = MinecraftServer.getServer().advancements
        val resourceKey = CraftNamespacedKey.toMinecraft(key)
        val holder = AdvancementHolder(resourceKey, nmsAdvancement)
        val mapBuilder: ImmutableMap.Builder<Identifier, AdvancementHolder> =
            ImmutableMap.builderWithExpectedSize(advancementsManagerNms.advancements.size + 1)
        mapBuilder.putAll(advancementsManagerNms.advancements)
        mapBuilder.put(resourceKey, holder)
        advancementsManagerNms.advancements = mapBuilder.buildKeepingLast()
        val tree = advancementsManagerNms.tree()
        tree.addAll(listOf(holder))
        tree.repositionNodes()
        resyncAllAdvancements()
        return Bukkit.getAdvancement(key)!!
    }

    private fun resyncAllAdvancements(){
        val advancementsManagerNms = MinecraftServer.getServer().advancements
        for (player in MinecraftServer.getServer().playerList.players){
            player.advancements.reload(advancementsManagerNms)
            player.advancements.flushDirty(player, false)
        }
    }
}