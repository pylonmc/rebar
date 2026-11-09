package io.github.pylonmc.rebar.nms.advancements

import com.google.common.collect.ImmutableMap
import io.github.pylonmc.rebar.advancements.AdvancementProgressManager
import io.github.pylonmc.rebar.advancements.RebarAdvancement
import io.papermc.paper.adventure.PaperAdventure
import net.minecraft.advancements.*
import net.minecraft.advancements.triggers.ImpossibleTrigger
import net.minecraft.core.ClientAsset
import net.minecraft.core.HolderSet
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.world.item.ItemStackTemplate
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.util.CraftNamespacedKey
import java.util.*

object AdvancementsAccessor {

    /**
     * Adds all of [advancements] to the server in one batch, then resyncs online players and restores their
     * backed-up progress. They may be in any order: vanilla's [AdvancementTree.addAll] keeps retrying until every
     * advancement whose parent exists has been inserted. Those with a missing parent are left out of the tree, so
     * they aren't sent to clients, but stay in the list of advancements, so they still work on the server.
     *
     * This does not register them in the Rebar registry, so it can be called again after server resources are
     * reloaded.
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

        resyncAllAdvancements(advancements)
    }

    /**
     * Converts a RebarAdvancement to the NMS vanilla advancement for registration in the vanilla advancement tree.
     */
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
                Optional.ofNullable(info.background)
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

    /**
     * Resends the advancements tree to clients and restores the progress of those advancements from the player PDC data.
     */
    private fun resyncAllAdvancements(restoring: Collection<RebarAdvancement>) {
        val advancementsManagerNms = MinecraftServer.getServer().advancements
        for (player in MinecraftServer.getServer().playerList.players) {
            player.advancements.reload(advancementsManagerNms)
            // Between reload and flushDirty: reload makes the next packet reset the client's advancements, so
            // restored advancements don't show toasts
            AdvancementProgressManager.restore(player.bukkitEntity, restoring)
            player.advancements.flushDirty(player, false)
        }
    }
}