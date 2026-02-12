package io.github.pylonmc.rebar.content.debug

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.block.base.RebarTickingBlock
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.RebarBlockSerializeEvent
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.base.RebarBlockInteractor
import io.github.pylonmc.rebar.item.base.RebarItemEntityInteractor
import io.github.pylonmc.rebar.item.base.RebarWeapon
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.util.position.position
import io.github.pylonmc.rebar.util.rebarKey
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.papermc.paper.datacomponent.DataComponentTypes
import me.tofaa.entitylib.meta.display.ItemDisplayMeta
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

@Suppress("UnstableApiUsage")
internal class DebugWaxedWeatheredCutCopperStairs(stack: ItemStack)
    : RebarItem(stack), RebarBlockInteractor, RebarItemEntityInteractor, RebarWeapon {

    @MultiHandler(priorities = [EventPriority.LOWEST])
    override fun onUsedToClickBlock(event: PlayerInteractEvent, priority: EventPriority) {
        event.isCancelled = true
        if (event.action == Action.PHYSICAL) return

        val block = event.clickedBlock ?: return
        val rebarBlock = BlockStorage.get(block)
        val player = event.player
        if (rebarBlock == null) {
            player.sendDebugActionBar("not_a_block")
            return
        }

        if (event.action.isLeftClick) {
            onUsedToLeftClickBlock(player, block, rebarBlock)
        } else if (event.action.isRightClick) {
            onUsedToRightClickBlock(player, block, rebarBlock)
        }
    }

    fun onUsedToLeftClickBlock(player: Player, block: Block, rebarBlock: RebarBlock) {
        if (player.currentInput.isSneak) {
            BlockStorage.deleteBlock(block.position)
            player.sendDebug(
                "deleted_data",
                RebarArgument.of("type", rebarBlock.schema.key.toString()),
                RebarArgument.of("location", block.position.toString())
            )
            return
        }
    }

    fun onUsedToRightClickBlock(player: Player, block: Block, rebarBlock: RebarBlock) {
        player.sendDebug(
            "key.block",
            RebarArgument.of("key", rebarBlock.schema.key.toString())
        )
        player.sendDebug(
            when (rebarBlock) {
                is RebarTickingBlock -> if (RebarTickingBlock.isTicking(rebarBlock)) {
                    "ticking.ticking"
                } else {
                    "ticking.error"
                }

                else -> "ticking.not_ticking"
            }
        )
        // Create a new PDC - doesn't matter what type because we won't be saving it, so we just use the block's
        // chunk to get a PDC context
        val pdc = block.chunk.persistentDataContainer.adapterContext.newPersistentDataContainer()
        if (!rebarBlock.disableBlockTextureEntity) {
            val entity = rebarBlock.blockTextureEntity
            if (entity != null) {
                pdc.set(
                    RebarBlock.rebarBlockTextureEntityKey,
                    RebarSerializers.ITEM_STACK_READABLE,
                    SpigotConversionUtil.toBukkitItemStack(entity.getEntityMeta(ItemDisplayMeta::class.java).item)
                )
            }
        }
        rebarBlock.writeDebugInfo(pdc)
        RebarBlockSerializeEvent(block, rebarBlock, pdc).callEvent()

        val serialized = NmsAccessor.instance.serializePdc(pdc)
        player.sendDebug(
            "data",
            RebarArgument.of("data", serialized)
        )
    }

    @MultiHandler(priorities = [EventPriority.LOWEST])
    override fun onUsedToDamageEntity(event: EntityDamageByEntityEvent, priority: EventPriority) {
        event.isCancelled = true
        val player = event.damager as? Player ?: return
        val rebarEntity = EntityStorage.get(event.entity)
        if (rebarEntity == null) {
            player.sendDebugActionBar("not_an_entity")
            return
        }

        if (player.currentInput.isSneak) {
            rebarEntity.entity.remove()
            player.sendDebug(
                "deleted_data",
                RebarArgument.of("type", rebarEntity.schema.key.toString()),
                RebarArgument.of("location", rebarEntity.entity.uniqueId.toString())
            )
            return
        }
    }

    @MultiHandler(priorities = [EventPriority.LOWEST])
    override fun onUsedToRightClickEntity(event: PlayerInteractEntityEvent, priority: EventPriority) {
        event.isCancelled = true
        val rebarEntity = EntityStorage.get(event.rightClicked)
        val player = event.player
        if (rebarEntity == null) {
            player.sendDebugActionBar("not_an_entity")
            return
        }

        player.sendDebug(
            "key.entity",
            RebarArgument.of("key", rebarEntity.schema.key.toString())
        )

        // TODO implement this once entities can tick
//            event.player.sendMessage(
//                MiniMessage.miniMessage().deserialize(
//                    when (rebarEntity) {
//                        is RebarTickingBlock -> if (false) {
//                            "<gold>Ticking: <green>Yes"
//                        } else {
//                            "<gold>Ticking: <red>Ticker has errored"
//                        }
//
//                        else -> "<gold>Ticking: <red>No"
//                    }
//                )
//            )
        rebarEntity.writeDebugInfo(rebarEntity.entity.persistentDataContainer)
        val serialized = NmsAccessor.instance.serializePdc(rebarEntity.entity.persistentDataContainer)
        player.sendDebug(
            "data",
            RebarArgument.of("data", serialized)
        )
    }

    companion object {
        val KEY = rebarKey("debug_waxed_weathered_cut_copper_stairs")
        val STACK = ItemStackBuilder.rebar(Material.BRICK, KEY)
            .set(DataComponentTypes.ITEM_MODEL, Material.WAXED_WEATHERED_CUT_COPPER_STAIRS.key)
            .set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            .build()
    }
}

private fun Audience.sendDebug(subkey: String, vararg args: RebarArgument) {
    return sendMessage(Component.translatable("rebar.message.debug.$subkey", *args))
}

private fun Audience.sendDebugActionBar(subkey: String, vararg args: RebarArgument) {
    return sendActionBar(Component.translatable("rebar.message.debug.$subkey", *args))
}
