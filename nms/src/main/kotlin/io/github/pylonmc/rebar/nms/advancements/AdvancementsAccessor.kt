package io.github.pylonmc.rebar.nms.advancements

import com.google.common.collect.ImmutableMap
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.advancements.RebarAdvancement
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.minecraft.advancements.*
import net.minecraft.advancements.criterion.ImpossibleTrigger
import net.minecraft.commands.CacheableFunction
import net.minecraft.core.ClientAsset
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.crafting.display.SlotDisplay
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.function.Consumer

object AdvancementsAccessor {

    /** Functions for converting NamespacedKeys and Strings to Minecraft's internal Identifier structure */
    private fun identifierFromKey(key: NamespacedKey?): Identifier? {
        if (key != null) {
            return Identifier.fromNamespaceAndPath(key.namespace, key.key)
        }
        return null
    }

    private fun identifierFromString(key: String?): Identifier? {
        if (key != null) {
            return Identifier.fromNamespaceAndPath("minecraft", key)
        }
        return null
    }

    @JvmName("identifierFromKeyNonNull")
    private fun identifierFromKey(key: NamespacedKey) = Identifier.fromNamespaceAndPath(key.namespace, key.key)

    @JvmName("identifierFromStringNonNull")
    private fun identifierFromString(key: String) = Identifier.fromNamespaceAndPath("minecraft", key)

    fun registerRebarAdvancement(
        advancement: RebarAdvancement,
        key: NamespacedKey
    ): org.bukkit.advancement.Advancement? {
        if (advancement.displayInfo != null && RebarRegistry.ITEMS[advancement.displayInfo!!.icon.id] == null && Registry.MATERIAL.get(
                advancement.displayInfo!!.icon.id
            ) == null
        ) {
            Rebar.logger.warning("Failed to register advancement $key due to failing to find the item with the key for the icon")
            return null
        }

        // Construct the advancement from the config data
        val nmsAdvancement = Advancement(
            Optional.ofNullable(advancement.parent).map { parent -> identifierFromKey(parent) },
            Optional.ofNullable(advancement.displayInfo).map { info ->
                DisplayInfo(
                    ItemStackTemplate(
                        CraftItemStack.asNMSCopy(
                            RebarRegistry.ITEMS[info.icon.id]?.getItemStack() ?: ItemStack.of(
                                Registry.MATERIAL.get(info.icon.id)!!
                            )
                        ).item
                    ),
                    PaperAdventure.asVanilla(
                        info.title ?: Component.translatable("${key.namespace}.advancements.${key.key}.title")
                    ),
                    PaperAdventure.asVanilla(
                        info.description
                            ?: Component.translatable("${key.namespace}.advancements.${key.key}.description")
                    ),
                    Optional.ofNullable(info.iconBackground)
                        .map { background -> ClientAsset.ResourceTexture(identifierFromKey(background)) },
                    AdvancementType.valueOf(info.iconFrame.uppercase()),
                    info.showToast,
                    info.announceToChat,
                    info.hidden
                )
            },
            AdvancementRewards(
                advancement.rewards.experience,
                advancement.rewards.loot.map { key ->
                    ResourceKey.create(
                        Registries.LOOT_TABLE,
                        identifierFromKey(key)
                    )
                },
                advancement.rewards.recipes.map { key ->
                    ResourceKey.create(
                        Registries.RECIPE,
                        identifierFromKey(key)
                    )
                },
                Optional.ofNullable(identifierFromKey(advancement.rewards.function)?.let { CacheableFunction(it) })
            ),
            advancement.criteria.map { key.toString() }.associateWith { key ->
                ImpossibleTrigger().createCriterion(ImpossibleTrigger.TriggerInstance()) // TODO: Allow the usage of Mojang's criterions
            },
            AdvancementRequirements(
                advancement.requirements.map { it.map { key -> key.toString() } }
            ),
            false // Not allowing this to be configured to avoid spamming Mojang's servers with junk data about fake advancements
        )

        // Add the advancement to the tree
        val resourceKey = identifierFromKey(key)
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