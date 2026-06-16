package io.github.pylonmc.rebar.nms.advancements

import com.google.common.collect.ImmutableMap
import io.github.pylonmc.rebar.advancements.RebarAdvancement
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.minecraft.advancements.*
import net.minecraft.advancements.criterion.ImpossibleTrigger
import net.minecraft.core.ClientAsset
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStackTemplate
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.util.CraftNamespacedKey
import java.util.*
import java.util.function.Consumer

object AdvancementsAccessor {

    fun registerRebarAdvancement(
        advancement: RebarAdvancement,
        key: NamespacedKey
    ): org.bukkit.advancement.Advancement? {

        // Construct the advancement from the config data
        val nmsAdvancement = Advancement(
            Optional.ofNullable(advancement.parent).map { parent -> CraftNamespacedKey.toMinecraft(parent) },
            Optional.ofNullable(advancement.displayInfo).map { info ->
                DisplayInfo(
                    ItemStackTemplate.fromNonEmptyStack(
                        (info.icon.itemType.createItemStack() as CraftItemStack).handle
                    ),
                    PaperAdventure.asVanilla(
                        info.title ?: Component.translatable("${key.namespace}.advancements.${key.key}.title")
                    ),
                    PaperAdventure.asVanilla(
                        info.description
                            ?: Component.translatable("${key.namespace}.advancements.${key.key}.description")
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
                advancement.rewards.experience,
                advancement.rewards.loot.map { table ->
                    ResourceKey.create(
                        Registries.LOOT_TABLE,
                        CraftNamespacedKey.toMinecraft(table.key)
                    )
                },
                advancement.rewards.recipes.map { recipe ->
                    ResourceKey.create(
                        Registries.RECIPE,
                        CraftNamespacedKey.toMinecraft((recipe as Keyed).key) // Safe since they were fetched from registry in config adapter and therefore must implement Keyed
                    )
                },
                Optional.ofNullable(null)
            ),
            advancement.criteria.map { key.toString() }.associateWith { key ->
                ImpossibleTrigger().createCriterion(ImpossibleTrigger.TriggerInstance())
            },
            AdvancementRequirements(
                advancement.requirements.map { it.map { key -> key.toString() } }
            ),
            false // Not allowing this to be configured to avoid spamming Mojang's servers with junk data about fake advancements
        )

        // Add the advancement to the tree
        val resourceKey = CraftNamespacedKey.toMinecraft(key)
        val mapBuilder = ImmutableMap.builder<Identifier, AdvancementHolder>()
        mapBuilder.putAll(MinecraftServer.getServer().advancements.advancements)
        val advancementHolder = AdvancementHolder(resourceKey, nmsAdvancement)
        mapBuilder.put(resourceKey, advancementHolder)
        MinecraftServer.getServer().advancements.advancements = mapBuilder.build()
        val tree = MinecraftServer.getServer().advancements.tree()
        tree.addAll(listOf(advancementHolder))
        val node: AdvancementNode? = tree.get(resourceKey)
        if (node != null) {
            val root = node.root()
            if (root.holder().value().display().isPresent) {
                TreeNodePosition.run(root)
            }
        }

        val bukkitAdvancement = Bukkit.getAdvancement(key)
        MinecraftServer.getServer().playerList.getPlayers().forEach(Consumer { player: ServerPlayer ->
            player.advancements.reload(MinecraftServer.getServer().advancements)
            player.advancements.flushDirty(player, false)
        })
        return bukkitAdvancement
    }
}