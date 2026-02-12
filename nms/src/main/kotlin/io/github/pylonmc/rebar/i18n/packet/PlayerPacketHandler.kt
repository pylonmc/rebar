@file:Suppress("UnstableApiUsage")

package io.github.pylonmc.rebar.i18n.packet

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.i18n.PlayerTranslationHandler
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.util.editData
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.papermc.paper.datacomponent.DataComponentTypes
import net.minecraft.network.HashedPatchMap
import net.minecraft.network.HashedStack
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.*
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.HashOps
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.display.*
import org.bukkit.craftbukkit.inventory.CraftItemStack
import java.util.logging.Level


// Much inspiration has been taken from https://github.com/GuizhanCraft/SlimefunTranslation
// with permission from the author
class PlayerPacketHandler(private val player: ServerPlayer, private val handler: PlayerTranslationHandler) {

    private val channel = player.connection.connection.channel

    private val hashGenerator: HashedPatchMap.HashGenerator

    init {
        // (PaperMC server) https://discord.com/channels/289587909051416579/555462289851940864/1371651093972385823
        val registryOps = player.registryAccess().createSerializationContext(HashOps.CRC32C_INSTANCE)
        hashGenerator = HashedPatchMap.HashGenerator { component ->
            component.encodeValue(registryOps)
                .getOrThrow { IllegalArgumentException("Failed to hash $component: $it") }
                .asInt()
        }
    }

    fun register() {
        channel.pipeline().addBefore("packet_handler", HANDLER_NAME, PacketHandler())
    }

    fun unregister() {
        channel.eventLoop().submit {
            channel.pipeline().remove(HANDLER_NAME)
        }
    }

    private inner class PacketHandler : ChannelDuplexHandler() {
        override fun write(ctx: ChannelHandlerContext, packet: Any, promise: ChannelPromise) {
            @Suppress("UNCHECKED_CAST")
            val packet = packet as? Packet<in ClientGamePacketListener> ?: return super.write(ctx, packet, promise)
            super.write(ctx, handleOutgoingPacket(packet), promise)
        }

        override fun channelRead(ctx: ChannelHandlerContext, packet: Any) {
            @Suppress("UNCHECKED_CAST")
            val packet = packet as? Packet<in ServerGamePacketListener> ?: return super.channelRead(ctx, packet)
            super.channelRead(ctx, handleIncomingPacket(packet))
        }
    }

    private fun handleOutgoingPacket(packet: Packet<in ClientGamePacketListener>): Packet<in ClientGamePacketListener> =
        when (packet) {
            is ClientboundBundlePacket -> ClientboundBundlePacket(packet.subPackets().map(::handleOutgoingPacket))

            is ClientboundContainerSetContentPacket -> packet.apply {
                items.forEach(::translate)
                translate(packet.carriedItem)
            }

            is ClientboundContainerSetSlotPacket -> packet.apply { translate(item) }
            is ClientboundSetCursorItemPacket -> packet.apply { translate(contents) }
            is ClientboundRecipeBookAddPacket -> ClientboundRecipeBookAddPacket(
                packet.entries.map {
                    ClientboundRecipeBookAddPacket.Entry(
                        RecipeDisplayEntry(
                            it.contents.id,
                            handleRecipeDisplay(it.contents.display),
                            it.contents.group,
                            it.contents.category,
                            it.contents.craftingRequirements
                        ),
                        it.flags
                    )
                },
                packet.replace
            )

            is ClientboundMerchantOffersPacket -> packet.apply {
                for (offer in offers) {
                    translate(offer.baseCostA.itemStack)
                    offer.costB.ifPresent {
                        translate(it.itemStack)
                    }
                    translate(offer.result)
                }
            }

            is ClientboundPlaceGhostRecipePacket -> ClientboundPlaceGhostRecipePacket(
                packet.containerId,
                handleRecipeDisplay(packet.recipeDisplay)
            )

            else -> packet
        }

    private fun handleIncomingPacket(packet: Packet<in ServerGamePacketListener>): Packet<in ServerGamePacketListener> =
        when (packet) {
            is ServerboundContainerClickPacket -> ServerboundContainerClickPacket(
                packet.containerId,
                packet.stateId,
                packet.slotNum,
                packet.buttonNum,
                packet.clickType,
                packet.changedSlots,
                if (packet.changedSlots.size == 1) {
                    val slot = packet.changedSlots.keys.single()
                    val menu = player.containerMenu
                    if (menu.isValidSlotIndex(slot)) {
                        HashedStack.create(menu.getSlot(slot).item, hashGenerator)
                    } else {
                        HashedStack.EMPTY
                    }
                } else {
                    HashedStack.create(player.containerMenu.carried, hashGenerator)
                }
            )

            is ServerboundSetCreativeModeSlotPacket -> ServerboundSetCreativeModeSlotPacket(
                packet.slotNum,
                reset(packet.itemStack)
            )

            else -> packet
        }

    private fun handleRecipeDisplay(display: RecipeDisplay): RecipeDisplay {
        return when (display) {
            is FurnaceRecipeDisplay -> FurnaceRecipeDisplay(
                handleSlotDisplay(display.ingredient),
                handleSlotDisplay(display.fuel),
                handleSlotDisplay(display.result),
                handleSlotDisplay(display.craftingStation),
                display.duration,
                display.experience
            )

            is ShapedCraftingRecipeDisplay -> ShapedCraftingRecipeDisplay(
                display.width,
                display.height,
                display.ingredients.map(::handleSlotDisplay),
                handleSlotDisplay(display.result),
                handleSlotDisplay(display.craftingStation)
            )

            is ShapelessCraftingRecipeDisplay -> ShapelessCraftingRecipeDisplay(
                display.ingredients.map(::handleSlotDisplay),
                handleSlotDisplay(display.result),
                handleSlotDisplay(display.craftingStation)
            )

            is SmithingRecipeDisplay -> SmithingRecipeDisplay(
                handleSlotDisplay(display.template),
                handleSlotDisplay(display.base),
                handleSlotDisplay(display.addition),
                handleSlotDisplay(display.result),
                handleSlotDisplay(display.craftingStation)
            )

            is StonecutterRecipeDisplay -> display
            else -> throw IllegalArgumentException("Unknown recipe display type: ${display::class.simpleName}")
        }
    }

    private fun handleSlotDisplay(display: SlotDisplay): SlotDisplay {
        return when (display) {
            is SlotDisplay.AnyFuel,
            is SlotDisplay.ItemSlotDisplay,
            is SlotDisplay.TagSlotDisplay,
            is SlotDisplay.Empty -> display

            is SlotDisplay.Composite -> SlotDisplay.Composite(display.contents.map(::handleSlotDisplay))
            is SlotDisplay.ItemStackSlotDisplay -> SlotDisplay.ItemStackSlotDisplay(
                display.stack.copy().apply(::translate)
            )

            is SlotDisplay.SmithingTrimDemoSlotDisplay -> SlotDisplay.SmithingTrimDemoSlotDisplay(
                handleSlotDisplay(display.base),
                handleSlotDisplay(display.material),
                display.pattern
            )

            is SlotDisplay.WithRemainder -> SlotDisplay.WithRemainder(
                handleSlotDisplay(display.input),
                handleSlotDisplay(display.remainder)
            )

            else -> throw IllegalArgumentException("Unknown slot display type: ${display::class.simpleName}")
        }
    }

    private fun translate(item: ItemStack) {
        if (item.isEmpty) return
        try {
            handler.handleItem(CraftItemStack.asCraftMirror(item))
        } catch (e: Throwable) {
            // Log the error nicely instead of kicking the player off
            // and causing two days of headache. True story.
            Rebar.logger.log(
                Level.SEVERE,
                "An error occurred while handling item translations",
                e
            )
        }
    }

    // no, I have no idea what this does either
    private fun reset(stack: ItemStack): ItemStack {
        if (stack.isEmpty) return stack
        val bukkitStack = CraftItemStack.asCraftMirror(stack)
        val item = RebarItem.fromStack(bukkitStack) ?: return stack
        val prototype = item.schema.getItemStack()
        prototype.copyDataFrom(bukkitStack) { it != DataComponentTypes.ITEM_NAME && it != DataComponentTypes.LORE }
        prototype.amount = bukkitStack.amount
        val translatedPrototype = prototype.clone()
        try {
            handler.handleItem(translatedPrototype)
        } catch (e: Throwable) {
            Rebar.logger.log(
                Level.SEVERE,
                "An error occurred while handling item translations",
                e
            )
            return stack
        }
        prototype.editData(DataComponentTypes.ITEM_NAME) {
            val protoName = translatedPrototype.getData(DataComponentTypes.ITEM_NAME)!!
            val newName = bukkitStack.getData(DataComponentTypes.ITEM_NAME)!!
            if (protoName != newName) newName else it
        }
        prototype.editData(DataComponentTypes.LORE) {
            val protoLore = translatedPrototype.getData(DataComponentTypes.LORE)!!
            val newLore = bukkitStack.getData(DataComponentTypes.LORE)!!
            if (protoLore != newLore) newLore else it
        }
        return CraftItemStack.unwrap(prototype)
    }

    companion object {
        private const val HANDLER_NAME = "rebar_packet_handler"
    }
}