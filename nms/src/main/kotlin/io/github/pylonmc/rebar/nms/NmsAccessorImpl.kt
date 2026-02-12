package io.github.pylonmc.rebar.nms

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent
import com.github.shynixn.mccoroutine.bukkit.asyncDispatcher
import com.github.shynixn.mccoroutine.bukkit.launch
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.i18n.PlayerTranslationHandler
import io.github.pylonmc.rebar.i18n.packet.PlayerPacketHandler
import io.github.pylonmc.rebar.nms.recipe.HandlerRecipeBookClick
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.Component
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.TextComponentTagVisitor
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.world.inventory.AbstractCraftingMenu
import net.minecraft.world.inventory.RecipeBookMenu.PostPlaceAction
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.state.properties.Property
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.craftbukkit.CraftEquipmentSlot
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.inventory.CraftItemType
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer
import org.bukkit.craftbukkit.util.CraftNamespacedKey
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

@Suppress("unused")
object NmsAccessorImpl : NmsAccessor {

    private val players = ConcurrentHashMap<UUID, PlayerPacketHandler>()

    override fun damageItem(itemStack: ItemStack, amount: Int, world: World, onBreak: (Material) -> Unit, force: Boolean) {
        (itemStack as CraftItemStack).handle.hurtAndBreak(amount, (world as CraftWorld).handle, null, { item: Item ->
            onBreak(CraftItemType.minecraftToBukkit(item))
        }, force)
    }

    override fun damageItem(itemStack: ItemStack, amount: Int, entity: LivingEntity, slot: EquipmentSlot, force: Boolean) {
        (itemStack as CraftItemStack).handle.hurtAndBreak(amount, (entity as CraftLivingEntity).handle, CraftEquipmentSlot.getNMS(slot), force)
    }

    override fun registerTranslationHandler(player: Player, handler: PlayerTranslationHandler) {
        if (players.containsKey(player.uniqueId)) return
        val handler = PlayerPacketHandler((player as CraftPlayer).handle, handler)
        players[player.uniqueId] = handler
        handler.register()
    }

    override fun unregisterTranslationHandler(player: Player) {
        val handler = players.remove(player.uniqueId) ?: return
        handler.unregister()
    }

    override fun resendInventory(player: Player) {
        val player = (player as CraftPlayer).handle
        val inventory = player.containerMenu
        for (slot in 0..45) {
            val item = inventory.getSlot(slot).item
            player.containerSynchronizer.sendSlotChange(inventory, slot, item)
        }
    }

    override fun resendRecipeBook(player: Player) {
        val player = (player as CraftPlayer).handle
        player.recipeBook.sendInitialRecipeBook(player)
    }

    override fun serializePdc(pdc: PersistentDataContainer): Component
        = PaperAdventure.asAdventure(TextComponentTagVisitor("  ").visit((pdc as CraftPersistentDataContainer).toTagCompound()))

    override fun getStateProperties(block: Block, custom: Map<String, Pair<String, Int>>): Map<String, String> {
        val state = (block as CraftBlock).nms
        val map = mutableMapOf<String, String>()
        val possibleValues = mutableMapOf<String, Int>()
        for (property in state.block.stateDefinition.properties) {
            @Suppress("UNCHECKED_CAST")
            property as Property<Comparable<Any>>
            map[property.name] = state.getOptionalValue(property).map(property::getName).orElse("none")
            possibleValues[property.name] = property.possibleValues.size
        }
        for ((name, pair) in custom) {
            map[name] = pair.first
            possibleValues[name] = pair.second
        }
        return map.toSortedMap(compareByDescending<String> { possibleValues[it] ?: 0 }.thenBy { it })
    }

    override fun handleRecipeBookClick(event: PlayerRecipeBookClickEvent) {
        val serverPlayer = (event.player as CraftPlayer).handle
        val menu = serverPlayer.containerMenu

        if (menu !is AbstractCraftingMenu) return
        val server = MinecraftServer.getServer()
        val recipeName = event.recipe
        val recipeHolder = server.recipeManager
            .byKey(ResourceKey.create(
                Registries.RECIPE, CraftNamespacedKey.toMinecraft(recipeName)
            ))
            .orElse(null) ?: return

        val postPlaceAction = HandlerRecipeBookClick(serverPlayer).handleRebarItemPlacement(
            menu,
            event.isMakeAll,
            recipeHolder,
            serverPlayer.level(),
        )


        val displayRecipes = recipeHolder.value().display()
        event.isCancelled = true
        if (postPlaceAction != PostPlaceAction.PLACE_GHOST_RECIPE || displayRecipes.isEmpty()) return

        Rebar.javaPlugin.launch(Rebar.asyncDispatcher) {
            val max = displayRecipes.size
            for (i in 0..<max) {
                serverPlayer.connection.send(
                    ClientboundPlaceGhostRecipePacket(
                        serverPlayer.containerMenu.containerId,
                        displayRecipes[i]
                    )
                )
            }
        }
    }
}