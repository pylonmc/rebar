package io.github.pylonmc.rebar.nms

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.entity.packet.BlockTextureEntity
import io.github.pylonmc.rebar.i18n.PlayerTranslationHandler
import io.github.pylonmc.rebar.util.delayTicks
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.papermc.paper.datacomponent.DataComponentType
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.Entity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemFactory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.Recipe
import org.bukkit.persistence.PersistentDataContainer
import org.jetbrains.annotations.ApiStatus
import java.util.UUID

/**
 * Internal, not for innocent eyes to see, move along now.
 */
@ApiStatus.Internal
@ApiStatus.NonExtendable
interface NmsAccessor {

    fun damageItem(itemStack: ItemStack, amount: Int, world: World, onBreak: (Material) -> Unit, force: Boolean = false)

    fun damageItem(itemStack: ItemStack, amount: Int, entity: LivingEntity, slot: EquipmentSlot, force: Boolean = false)

    fun registerTranslationHandler(player: Player, handler: PlayerTranslationHandler)

    fun getTranslationHandler(playerId: UUID): PlayerTranslationHandler?

    fun unregisterTranslationHandler(player: Player)

    fun resendInventory(player: Player)

    fun resendEquipment(player: Player, entity: LivingEntity)

    fun resendSlot(player: Player, slot: Int)

    fun resendRecipeBook(player: Player)

    fun serializePdc(pdc: PersistentDataContainer): Component

    fun getStateProperties(block: Block, custom: Map<String, Pair<String, Int>> = mutableMapOf()): Map<String, String>

    fun handleRecipeBookClick(event: PlayerRecipeBookClickEvent)

    fun hasTracker(entity: Entity): Boolean

    fun createBlockTextureEntity(block: RebarBlock): BlockTextureEntity

    typealias SlotListener = (inventoryView: InventoryView, slot: Int, oldItemStack: ItemStack?, newItemStack: ItemStack?) -> Unit

    fun addSlotChangedListener(key: NamespacedKey, inventoryView: InventoryView, listener: SlotListener)

    fun isOccluding(block: Block): Boolean

    fun blocksBetween(from: BlockPosition, to: BlockPosition): List<Block>

    /**
     * Furnaces have a recipe cache of the last recipe smelted, this is great
     * for performance but has an unexpected side effect once rebar items are introduced.
     *
     * Lets say you have a furnace with nothing cached and you put in a raw tin (rebar item) to smelt.
     * The furnace is going to search for a recipe that matches and there are 2 possible choices
     * 1. the raw tin -> tin ingot recipe, supplied by rebar using exact choice
     * 2. the raw iron -> iron ingot recipe, supplied by Minecraft, using material choice, so it validates even if the item is a rebar item
     *
     * Even if it picks the vanilla recipe, when it starts smelting & finishes smelting, rebar will ensure
     * the rebar recipe is used instead.
     *
     * However, the furnace itself has no way of knowing that we changed which recipe was used, so if the vanilla recipe was used
     * it will cache the vanilla recipe. Then, when it tries to start smelting another raw tin, it will check that it can fit
     * the result of the next valid recipe, which is evaluated to the cached vanilla recipe, and it will find that it can't, because a tin ingot
     * is in the result slot, not the iron ingot. This then deadlocks the furnace in a feedback loop of use the vanilla recipe, oh can't fit it.
     *
     * In order to avoid this, whenever we override the recipe being used, we set the recipe in the recipe cache so that next time
     * the furnace smelts, it uses the correct recipe and doesn't get deadlocked.
     */
    fun setFurnaceRecipeCache(block: Block, recipe: NamespacedKey)

    /**
     * Returns the weapon item for the entity
     *
     * For ex: If a player is spinning with a riptide trident, returns the trident, otherwise it may return the sword in the main hand
     */
    fun getWeaponItem(entity: Entity): ItemStack?

    /**
     * Identical to the [ItemFactory.createItemStack] method except it works with rebar ids
     */
    fun createItemStack(input: String): ItemStack

    /**
     * Notify the inventory that it has been changed, this is needed for things like Comparators and other observers
     *
     * Note: In the future we won't need this assuming the PR to paper is made & merged, for some reason they only
     * call this method on item remove, and not on set.
     */
    fun setChanged(inventory: Inventory)

    /**
     * Simulates a player interaction using the item specified, if [block] and [blockFace] are specified it simulates using the item on
     * that block.
     *
     * Note: This calls all vanilla logic, PlayerInteractEvent, BlockPlace, etc, this will **actually** use the item/block
     */
    fun simulateInteract(player: Player, itemStack: ItemStack, hand: EquipmentSlot, block: Block?, blockFace: BlockFace?)

    fun hasRecipe(key: NamespacedKey): Boolean

    fun registerRecipes(recipes: Iterable<Recipe>, finalize: Boolean)

    fun unregisterRecipes(recipes: Iterable<NamespacedKey>, finalize: Boolean)

    fun getOverriddenTypes(itemStack: ItemStack): List<DataComponentType>

    companion object {
        val instance = Class.forName("io.github.pylonmc.rebar.nms.NmsAccessorImpl")
            .getDeclaredField("INSTANCE")
            .get(null) as NmsAccessor

        private val recipeRegisterQueue = mutableSetOf<Recipe>()
        private val recipeUnregisterQueue = mutableSetOf<NamespacedKey>()

        private val registerRecipeJob = Rebar.scope.launch {
            while (true) {
                processRecipeQueue()
                delayTicks(1)
            }
        }

        @JvmStatic
        fun queueRegisterRecipe(recipe: Recipe) {
            recipeRegisterQueue.add(recipe)
        }

        @JvmStatic
        fun queueUnregisterRecipe(recipeKey: NamespacedKey) {
            recipeUnregisterQueue.add(recipeKey)
        }

        @JvmStatic
        fun processRecipeQueue() {
            if (recipeUnregisterQueue.isNotEmpty()) {
                val unregistering = recipeUnregisterQueue.toSet()
                recipeUnregisterQueue.clear()
                instance.unregisterRecipes(unregistering, false)
            }

            if (recipeRegisterQueue.isNotEmpty()) {
                val registering = recipeRegisterQueue.toSet()
                recipeRegisterQueue.clear()
                instance.registerRecipes(registering, true)
            }
        }
    }
}