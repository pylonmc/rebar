package io.github.pylonmc.rebar.nms

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.exceptions.CommandSyntaxException
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.async.PlayerScope
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.entity.packet.BlockTextureEntity
import io.github.pylonmc.rebar.i18n.PlayerTranslationHandler
import io.github.pylonmc.rebar.nms.packet.PlayerPacketHandler
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import io.github.pylonmc.rebar.nms.entity.BlockTextureEntityImpl
import io.github.pylonmc.rebar.nms.inventory.KeyedContainerListener
import io.github.pylonmc.rebar.nms.recipe.AccessibleCachedCheck
import io.github.pylonmc.rebar.nms.recipe.HandlerRecipeBookClick
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.papermc.paper.adventure.PaperAdventure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.minecraft.commands.arguments.item.ItemParser
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.TextComponentTagVisitor
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.world.inventory.AbstractCraftingMenu
import net.minecraft.world.inventory.RecipeBookMenu.PostPlaceAction
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.RecipeManager
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.world.phys.BlockHitResult
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.craftbukkit.CraftEquipmentSlot
import org.bukkit.craftbukkit.CraftRegistry
import org.bukkit.craftbukkit.CraftWorld
import org.bukkit.craftbukkit.block.CraftBlock
import org.bukkit.craftbukkit.entity.CraftEntity
import org.bukkit.craftbukkit.entity.CraftLivingEntity
import org.bukkit.craftbukkit.entity.CraftPlayer
import org.bukkit.craftbukkit.inventory.CraftInventory
import org.bukkit.craftbukkit.inventory.CraftInventoryView
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.craftbukkit.inventory.CraftItemType
import org.bukkit.craftbukkit.persistence.CraftPersistentDataContainer
import org.bukkit.craftbukkit.util.CraftNamespacedKey
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Field
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.max
import kotlin.math.min
import com.mojang.datafixers.util.Pair as NmsPair
import net.minecraft.world.entity.EquipmentSlot as NmsEquipmentSlot

@Suppress("unused")
object NmsAccessorImpl : NmsAccessor {

    // We use both the field and the handle because the handle will have significantly better performance
    // getting the field value but cannot be used for setting so we still need the raw field.
    // (even if we used a VarHandle, because the field is normally final, setting will not work)
    private val furnaceQuickCheckField: Field
    private val furnaceQuickCheckHandle: MethodHandle

    init {
        try {
            furnaceQuickCheckField = AbstractFurnaceBlockEntity::class.java.getDeclaredField("quickCheck")
            furnaceQuickCheckField.isAccessible = true

            val methodHandles = MethodHandles.privateLookupIn(AbstractFurnaceBlockEntity::class.java, MethodHandles.lookup())
            furnaceQuickCheckHandle = methodHandles.unreflectGetter(furnaceQuickCheckField)
        } catch (e: Throwable) {
            Rebar.logger.severe("Failed to access furnace quick check: ${e.message}")
            throw RuntimeException(e)
        }
    }

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

    override fun getTranslationHandler(playerId: UUID): PlayerTranslationHandler? {
        return players[playerId]?.handler
    }

    override fun unregisterTranslationHandler(player: Player) {
        val handler = players.remove(player.uniqueId) ?: return
        handler.unregister()
    }

    override fun resendInventory(player: Player) {
        resendEquipment(player, player)
        val player = (player as CraftPlayer).handle
        val inventory = player.containerMenu
        for (slot in 0..45) {
            val item = inventory.getSlot(slot).item
            player.containerSynchronizer.sendSlotChange(inventory, slot, item)
        }
    }

    override fun resendEquipment(player: Player, entity: LivingEntity) {
        val player = (player as CraftPlayer).handle
        val entity = (entity as CraftLivingEntity).handle
        player.connection.send(ClientboundSetEquipmentPacket(entity.id, listOf(
            NmsPair.of(NmsEquipmentSlot.HEAD, entity.getItemBySlot(NmsEquipmentSlot.HEAD)),
            NmsPair.of(NmsEquipmentSlot.CHEST, entity.getItemBySlot(NmsEquipmentSlot.CHEST)),
            NmsPair.of(NmsEquipmentSlot.LEGS, entity.getItemBySlot(NmsEquipmentSlot.LEGS)),
            NmsPair.of(NmsEquipmentSlot.FEET, entity.getItemBySlot(NmsEquipmentSlot.FEET)),
        )))
    }

    override fun resendSlot(player: Player, slot: Int) {
        val player = (player as CraftPlayer).handle
        player.connection.send(
            ClientboundContainerSetSlotPacket(
                player.inventoryMenu.containerId,
                player.inventoryMenu.incrementStateId(),
                slot,
                player.inventoryMenu.getSlot(slot).item
            )
        )
    }

    override fun resendRecipeBook(player: Player) {
        val player = (player as CraftPlayer).handle
        player.recipeBook.sendInitialRecipeBook(player)
    }

    override fun serializePdc(pdc: PersistentDataContainer): Component
        = PaperAdventure.asAdventure(TextComponentTagVisitor("  ").visit((pdc as CraftPersistentDataContainer).toTagCompound()))

    override fun getStateProperties(block: Block, custom: Map<String, Pair<String, Int>>): Map<String, String> {
        val state = (block as CraftBlock).blockState
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

        PlayerScope(EmptyCoroutineContext, event.player).launch(Dispatchers.Default) {
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

    override fun hasTracker(entity: Entity): Boolean {
        val id = entity.entityId
        return (entity.world as CraftWorld).handle.chunkSource.chunkMap.entityMap.get(id)?.seenBy?.isNotEmpty() ?: false
    }

    override fun createBlockTextureEntity(block: RebarBlock): BlockTextureEntity = BlockTextureEntityImpl(block)

    override fun addSlotChangedListener(key: NamespacedKey, inventoryView: InventoryView, listener: NmsAccessor.SlotListener) {
        val inventoryView = inventoryView as CraftInventoryView<*, *>
        inventoryView.handle.addSlotListener(KeyedContainerListener(CraftNamespacedKey.toMinecraft(key), listener))
    }

    override fun isOccluding(block: Block) = (block as CraftBlock).blockState.canOcclude()

    override fun blocksBetween(from: BlockPosition, to: BlockPosition) = BlockPos.betweenClosedStream(
        min(from.x, to.x), min(from.y, to.y), min(from.z, to.z),
        max(from.x, to.x), max(from.y, to.y), max(from.z, to.z)
    ).let {
        val blocks = mutableListOf<Block>()
        for (pos in it) {
            blocks.add(from.world?.getBlockAt(pos.x, pos.y, pos.z) ?: continue)
        }
        blocks
    }

    override fun setFurnaceRecipeCache(block: Block, recipe: NamespacedKey) {
        val block = block as CraftBlock
        val blockEntity = block.level.getBlockEntity(block.position) as? AbstractFurnaceBlockEntity ?: return
        try {
            val currentQuickCheck = furnaceQuickCheckHandle.invoke(blockEntity) as? RecipeManager.CachedCheck<*, *> ?: return
            if (currentQuickCheck is AccessibleCachedCheck<*, *>) {
                currentQuickCheck.lastRecipe = CraftNamespacedKey.toResourceKey(Registries.RECIPE, recipe)
            } else {
                val newQuickCheck = AccessibleCachedCheck(blockEntity.recipeType)
                newQuickCheck.lastRecipe = CraftNamespacedKey.toResourceKey(Registries.RECIPE, recipe)
                furnaceQuickCheckField.set(blockEntity, newQuickCheck)
            }
        } catch (e: Throwable) {
            Rebar.logger.severe("Failed to set furnace recipe cache: ${e.message}")
            e.printStackTrace()
        }
    }

    override fun getWeaponItem(entity: Entity): ItemStack? = (entity as CraftEntity).handle.weaponItem?.asBukkitMirror()

    override fun createItemStack(input: String): ItemStack {
        var input = input
        var idEnd = input.indexOf('[')
        if (idEnd == -1) idEnd = input.length

        val typeString = input.substring(0, idEnd)
        val type = ItemTypeWrapper(NamespacedKey.fromString(typeString) ?: throw IllegalArgumentException("Could not find item $typeString"))
        if (type is ItemTypeWrapper.Rebar) {
            input = "minecraft:air" + input.substring(idEnd)
        }

        try {
            val reader = StringReader(input)
            val itemInput = ItemParser(CraftRegistry.getMinecraftRegistry()).parse(reader);
            if (reader.canRead()) {
                throw IllegalArgumentException("Trailing input found when parsing ItemStack: " + reader.remaining);
            } else {
                val stack = type.createItemStack()
                val nmsStack = (stack as CraftItemStack).handle
                nmsStack.applyComponents(itemInput.components)
                return nmsStack.asBukkitMirror()
            }
        } catch (ex: CommandSyntaxException) {
            throw IllegalArgumentException("Could not parse ItemStack: $input", ex);
        }
    }

    override fun setChanged(inventory: Inventory) {
        val inventory = inventory as CraftInventory
        inventory.inventory.setChanged()
    }

    override fun simulateInteract(player: Player, itemStack: ItemStack, hand: EquipmentSlot, block: Block?, blockFace: BlockFace?) {
        val nmsPlayer = (player as CraftPlayer).handle
        val level = nmsPlayer.level()
        val nmsStack = (itemStack as CraftItemStack).handle
        val nmsHand = CraftEquipmentSlot.getHand(hand)
        if (block == null || blockFace == null) {
            nmsPlayer.gameMode.useItem(nmsPlayer, level, nmsStack, nmsHand)
        } else {
            val nmsPos = (block as CraftBlock).position
            val nmsDirection = CraftBlock.blockFaceToNotch(blockFace) ?: throw IllegalArgumentException("Invalid block face $blockFace")
            val nmsLoc = nmsPos.center.add(nmsDirection.unitVec3.scale(0.5))
            nmsPlayer.gameMode.useItemOn(nmsPlayer, level, nmsStack, nmsHand, BlockHitResult(nmsLoc, nmsDirection, nmsPos, false))
        }
    }
}