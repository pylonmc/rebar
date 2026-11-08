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

    /**
     * Registers all of [advancements] with the server in one batch. They may be in any order: vanilla's
     * [AdvancementTree.addAll] keeps retrying until every advancement whose parent exists has been inserted.
     */
    fun registerRebarAdvancements(advancements: Collection<RebarAdvancement>) {
        if (advancements.isEmpty()) return
        val holders = advancements.map {
            AdvancementHolder(CraftNamespacedKey.toMinecraft(it.key), convertRebarAdvancementToNms(it))
        }

        val advancementsManagerNms = MinecraftServer.getServer().advancements
        val mapBuilder: ImmutableMap.Builder<Identifier, AdvancementHolder> =
            ImmutableMap.builderWithExpectedSize(advancementsManagerNms.advancements.size + holders.size)
        mapBuilder.putAll(advancementsManagerNms.advancements)
        for (holder in holders) {
            mapBuilder.put(holder.id(), holder)
        }
        advancementsManagerNms.advancements = mapBuilder.buildKeepingLast()

        val tree = advancementsManagerNms.tree()
        tree.addAll(holders)
        tree.repositionNodes()

        advancements.forEach { it.register() }
        resyncAllAdvancements()
    }

    private fun convertRebarAdvancementToNms(advancement: RebarAdvancement): Advancement = Advancement(
        Optional.ofNullable(advancement.parent).map { parent -> CraftNamespacedKey.toMinecraft(parent) },
        Optional.ofNullable(advancement.displayInfo).map { info ->
            DisplayInfo(
                ItemStackTemplate.fromNonEmptyStack(
                    (info.icon.itemType.createItemStack(info.icon.count) as CraftItemStack).handle
                ),
                PaperAdventure.asVanilla(
                    info.title
                ),
                PaperAdventure.asVanilla(
                    info.description
                ),
                Optional.ofNullable(info.iconBackground)
                    .map { background -> ClientAsset.ResourceTexture(CraftNamespacedKey.toMinecraft(background)) },
                AdvancementType.valueOf(info.iconFrame.name),
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
        advancement.criteria.map { it.key }.associateWith {
            ImpossibleTrigger().createCriterion(ImpossibleTrigger.TriggerInstance())
        },
        AdvancementRequirements(
            advancement.requirements
        ),
        false // Not allowing this to be configured to avoid spamming Mojang's servers with junk data about fake advancements
    )

    private fun resyncAllAdvancements(){
        val advancementsManagerNms = MinecraftServer.getServer().advancements
        for (player in MinecraftServer.getServer().playerList.players){
            player.advancements.reload(advancementsManagerNms)
            player.advancements.flushDirty(player, false)
        }
    }
}