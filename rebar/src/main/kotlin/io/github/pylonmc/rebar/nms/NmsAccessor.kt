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
import org.bukkit.Bukkit
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

    /**
     * A quicker alternative to using [Bukkit.getRecipe] != null
     *
     * Note: This and related methods will be removed when [Paper PR#13945](https://github.com/PaperMC/Paper/pull/13945) is merged
     */
    fun hasRecipe(key: NamespacedKey): Boolean

    /**
     * A method to register multiple recipes all at once, avoiding the exponential
     * performance cost when using [Bukkit.addRecipe] repeatedly
     *
     * Note: This and related methods will be removed when [Paper PR#13945](https://github.com/PaperMC/Paper/pull/13945) is merged
     */
    fun registerRecipes(recipes: Iterable<Recipe>, finalize: Boolean)

    /**
     * A method to unregister multiple recipes all at once, avoiding the exponential
     * performance cost when using [Bukkit.removeRecipe] repeatedly
     *
     * Note: This and related methods will be removed when [Paper PR#13945](https://github.com/PaperMC/Paper/pull/13945) is merged
     */
    fun unregisterRecipes(recipes: Iterable<NamespacedKey>, finalize: Boolean)

    /**
     * Note: this exists purely for the performance of rebar's recipe system,
     * it is significantly faster than taking [ItemStack.getDataTypes] and
     * filtering which ones of which have been overridden.
     */
    fun getOverriddenTypes(itemStack: ItemStack): List<DataComponentType>

    /**
     * Note: this exists purely for the performance of rebar's recipe system,
     * it is significantly faster than taking [ItemStack.getDataTypes] and
     * filtering then mapping which ones of which have been overridden.
     */
    fun overriddenComponents(itemStack: ItemStack, exact: Boolean): Map<DataComponentType, Any?>

    /**
     * Note: this exists purely for the performance of rebar's recipe system,
     * it is significantly faster than checking against the default values for each
     * type using the [ItemStack.isDataOverridden] for vanilla items, or comparing the
     * results of [ItemStack.getData] for rebar items.
     */
    fun hasDefaultComponents(itemStack: ItemStack, components: Set<DataComponentType>) : Boolean

    /**
     * Note: this exists purely for the performance of rebar's recipe system,
     * it is significantly faster than taking [Material.getDefaultDataTypes] for vanilla items
     * or using [ItemStack.getDataTypes] for rebar items. And checking if they are set to their
     * effective default value.
     */
    fun isDefaultComponents(itemStack: ItemStack): Boolean

    /**
     * Checks if the given [itemStack] has the given values of all the [components].
     *
     * Note: this exists purely for the performance of rebar's recipe system,
     * it is significantly faster than checking everything with the paper api equivalents.
     */
    fun componentsMatch(itemStack: ItemStack, components: Map<DataComponentType, Any?>): Boolean

    /**
     * Checks if the given [itemStack] matches the given [components] and does not have any
     * additional components.
     *
     * Note: this exists purely for the performance of rebar's recipe system,
     * it is significantly faster than checking everything with the paper api equivalents.
     */
    fun componentsEqual(itemStack: ItemStack, components: Map<DataComponentType, Any?>): Boolean

    companion object {
        val instance = Class.forName("io.github.pylonmc.rebar.nms.NmsAccessorImpl")
            .getDeclaredField("INSTANCE")
            .get(null) as NmsAccessor

        private val recipeRegisterQueue = mutableSetOf<Recipe>()
        private val recipeUnregisterQueue = mutableSetOf<NamespacedKey>()

        /**
         * Note: This will be removed when [Paper PR#13945](https://github.com/PaperMC/Paper/pull/13945) is merged
         */
        private val registerRecipeJob = Rebar.scope.launch {
            while (true) {
                processRecipeQueue()
                delayTicks(1)
            }
        }

        /**
         * Used to queue a recipe to be registered, the next time the queue is processed
         * all recipes queued will be registered at once for performance. By default, the queue
         * is processed once per tick, but it can be manually triggered.
         *
         * Note: This and related methods will be removed when [Paper PR#13945](https://github.com/PaperMC/Paper/pull/13945) is merged
         *
         * @see NmsAccessor.registerRecipes
         */
        @JvmStatic
        fun queueRegisterRecipe(recipe: Recipe) {
            recipeRegisterQueue.add(recipe)
        }

        /**
         * Used to queue a recipe to be unregistered, the next time the queue is processed
         * all recipes queued will be unregistered at once for performance. By default, the queue
         * is processed once per tick, but it can be manually triggered.
         *
         * Note: This and related methods will be removed when [Paper PR#13945](https://github.com/PaperMC/Paper/pull/13945) is merged
         *
         * @see NmsAccessor.unregisterRecipes
         */
        @JvmStatic
        fun queueUnregisterRecipe(recipeKey: NamespacedKey) {
            recipeUnregisterQueue.add(recipeKey)
        }

        /**
         * Immediately processes all recipes in the register & unregister queue
         *
         * Note: This and related methods will be removed when [Paper PR#13945](https://github.com/PaperMC/Paper/pull/13945) is merged
         *
         * @see queueRegisterRecipe
         * @see queueUnregisterRecipe
         */
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