package io.github.pylonmc.rebar.item.research

import com.github.shynixn.mccoroutine.bukkit.launch
import com.github.shynixn.mccoroutine.bukkit.ticks
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.research.Research.Companion.canPickUp
import io.github.pylonmc.rebar.particles.ConfettiParticle
import io.github.pylonmc.rebar.recipe.FluidOrItem
import io.github.pylonmc.rebar.recipe.RecipeType
import io.github.pylonmc.rebar.recipe.vanilla.VanillaRecipeType
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.persistentData
import io.github.pylonmc.rebar.util.rebarKey
import kotlinx.coroutines.delay
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.Bukkit
import org.bukkit.Keyed
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import kotlin.math.min

/**
 * A Rebar research as seem in the 'researches' guide section.
 *
 * Researches typically have a research point [cost] specified. However, this
 * is optional, and you can implement your own methods to unlock a research.
 *
 * @property cost If null, the research cannot be unlocked using points
 * @property unlocks The keys of the items that are unlocked by this research
 */
class Research(
    private val key: NamespacedKey,
    private val itemTemplate: ItemStack,
    val name: Component,
    val cost: Long?,
    val unlocks: Set<NamespacedKey>
) : Keyed {

    val item: ItemStack
        get() {
            val item = itemTemplate.clone()
            item.lore(listOf())
            return item
        }

    /**
     * A constructor that sets the name to a default language file key.
     */
    constructor(key: NamespacedKey, item: ItemStack, cost: Long?, vararg unlocks: NamespacedKey) : this(
        key,
        item,
        Component.translatable("${key.namespace}.research.${key.key}"),
        cost,
        unlocks.toSet()
    )

    fun register() {
        RebarRegistry.RESEARCHES.register(this)
    }

    /**
     * Adds the research to a player.
     *
     * @param sendMessage If set, sends a message to notify the player that they
     * have unlocked the research
     */
    @JvmOverloads
    fun addTo(player: Player, sendMessage: Boolean = true, effects: Boolean = true) {
        if (this in getResearches(player)) return

        addResearch(player, this)
        for (recipe in RecipeType.vanillaCraftingRecipes()) {
            val rebarItem = RebarItem.fromStack(recipe.craftingRecipe.result) ?: continue
            if (rebarItem.key in unlocks) {
                player.discoverRecipe(recipe.key)
            }
        }

        if (sendMessage) {
            player.sendMessage(
                Component.translatable(
                    "rebar.message.research.unlocked",
                    RebarArgument.of("research", name)
                )
            )
        }

        if (effects) {
            if (player.researchConfetti) {
                val multiplier = (cost?.toDouble() ?: 0.0) * RebarConfig.ResearchConfig.MULTIPLIER_CONFETTI_AMOUNT
                val amount = (RebarConfig.ResearchConfig.BASE_CONFETTI_AMOUNT * multiplier).toInt()
                val spawnedConfetti = min(amount, RebarConfig.ResearchConfig.MAX_CONFETTI_AMOUNT)
                ConfettiParticle.spawnMany(player.location, spawnedConfetti).run()
            }

            if (player.researchSounds) {
                for ((delay, sound) in RebarConfig.ResearchConfig.SOUNDS) {
                    Bukkit.getScheduler().runTaskLater(Rebar, Runnable {
                        player.playSound(sound.create(), Sound.Emitter.self())
                    }, delay)
                }
            }
        }
    }

    /**
     * Removes a research from a player.
     */
    fun removeFrom(player: Player) {
        if (this !in getResearches(player)) return

        removeResearch(player, this)
        for (recipe in RecipeType.vanillaCraftingRecipes()) {
            val rebarItem = RebarItem.fromStack(recipe.craftingRecipe.result) ?: continue
            if (rebarItem.key in unlocks) {
                player.undiscoverRecipe(recipe.key)
            }
        }
    }

    /**
     * Returns whether a research has been researched by the given player.
     */
    fun isResearchedBy(player: Player): Boolean {
        return this in getResearches(player)
    }

    override fun getKey() = key

    override fun equals(other: Any?): Boolean = other is Research && this.key == other.key

    override fun hashCode(): Int = key.hashCode()

    companion object : Listener {
        private val researchesKey = rebarKey("researches")
        private val researchPointsKey = rebarKey("research_points")
        private val researchConfettiKey = rebarKey("research_confetti")
        private val researchSoundsKey = rebarKey("research_sounds")
        private val guideHintsKey = rebarKey("guide_hints")
        private val researchesType =
            RebarSerializers.SET.setTypeFrom(RebarSerializers.KEYED.keyedTypeFrom(RebarRegistry.RESEARCHES::getOrThrow))

        @JvmStatic
        var Player.researchPoints: Long by persistentData(researchPointsKey, RebarSerializers.LONG, 0)

        @JvmStatic
        var Player.researchConfetti: Boolean by persistentData(researchConfettiKey, RebarSerializers.BOOLEAN, true)

        @JvmStatic
        var Player.researchSounds: Boolean by persistentData(researchSoundsKey, RebarSerializers.BOOLEAN, true)

        @JvmStatic
        var Player.guideHints: Boolean by persistentData(guideHintsKey, RebarSerializers.BOOLEAN, true)

        @JvmStatic
        fun getResearches(player: OfflinePlayer): Set<Research> {
            val researches = player.persistentDataContainer.get(researchesKey, researchesType)
            if (researches == null && player is Player) {
                setResearches(player, setOf())
                return setOf()
            }
            return researches!!
        }

        @JvmStatic
        fun setResearches(player: Player, researches: Set<Research>)
            = player.persistentDataContainer.set(researchesKey, researchesType, researches)

        @JvmStatic
        fun addResearch(player: Player, research: Research)
            = setResearches(player, getResearches(player) + research)

        @JvmStatic
        fun removeResearch(player: Player, research: Research)
            = setResearches(player, getResearches(player) - research)

        /**
         * Checks whether a player can craft an item (ie has the associated research, or
         * has permission to bypass research.
         *
         * @param sendMessage Whether, if the player cannot craft the item, a message should be sent to them
         * to notify them of this fact
         */
        @JvmStatic
        @JvmOverloads
        @JvmName("canPlayerCraft")
        fun Player.canCraft(item: RebarItem, sendMessage: Boolean = false, respectBypass: Boolean = true): Boolean {
            if (!RebarConfig.ResearchConfig.ENABLED || (respectBypass && this.hasPermission(item.researchBypassPermission))) return true

            val research = item.research ?: return true

            val canUse = this.hasResearch(research)
            if (!canUse && sendMessage) {
                var researchName = research.name
                if (research.cost != null) {
                    researchName = researchName
                        .hoverEvent(
                            HoverEvent.showText(
                                Component.translatable(
                                    "rebar.message.research.click_to_research",
                                    RebarArgument.of("points", research.cost)
                                )
                            )
                        )
                        .clickEvent(ClickEvent.runCommand("/rebar research discover ${research.key}"))
                }
                this.sendMessage(
                    Component.translatable(
                        "rebar.message.research.unknown",
                        RebarArgument.of("item", item.stack.effectiveName()),
                        RebarArgument.of("research", researchName)
                    )
                )
            }

            return canUse
        }

        /**
         * Checks whether a player can pick up an item (ie has the associated research, or
         * has permission to bypass research.
         *
         * @param sendMessage Whether, if the player cannot pick up the item, a message should be sent to them
         * to notify them of this fact
         */
        @JvmStatic
        @JvmOverloads
        @JvmName("canPlayerPickUp")
        fun Player.canPickUp(item: RebarItem, sendMessage: Boolean = false): Boolean = canCraft(item, sendMessage)

        /**
         * Checks whether a player can use an item (ie has the associated research, or
         * has permission to bypass research.
         *
         * @param sendMessage Whether, if the player cannot use the item, a message should be sent to them
         * to notify them of this fact
         */
        @JvmStatic
        @JvmOverloads
        @JvmName("canPlayerUse")
        fun Player.canUse(item: RebarItem, sendMessage: Boolean = false): Boolean {
            if (RebarConfig.DISABLED_ITEMS.contains(item.key)) {
                if (sendMessage) {
                    this.sendMessage(
                        Component.translatable(
                            "rebar.message.disabled.message",
                            RebarArgument.of("item", item.stack.effectiveName()),
                        )
                    )
                }
                return false
            }

            return canCraft(item, sendMessage)
        }

        @EventHandler
        private fun onPlayerPickup(event: EntityPickupItemEvent) {
            val entity = event.entity
            if (entity is Player) {
                Rebar.launch {
                    delay(1.ticks)
                    entity.ejectUnknownItems()
                }
            }
        }

        @EventHandler
        private fun onJoin(e: PlayerJoinEvent) {
            if (!RebarConfig.ResearchConfig.ENABLED) return
            val player = e.player

            // discover only the recipes that have no research whenever an ingredient is added to the inventory
            for (recipeType in RebarRegistry.RECIPE_TYPES) {
                if (recipeType !is VanillaRecipeType<*>) continue
                for (recipe in recipeType) {
                    if (recipe.key in VanillaRecipeType.nonRebarRecipes) continue
                    val researches = recipe.results
                        .filterIsInstance<FluidOrItem.Item>()
                        .mapNotNull { RebarItem.fromStack(it.item)?.research }
                    if (researches.isNotEmpty()) continue
                    player.discoverRecipe(recipe.key)
                }
            }
        }


        @JvmStatic
        fun loadFromConfig(section: ConfigSection, key : NamespacedKey) : Research {

            try {
                val item = section.getOrThrow("item", ConfigAdapter.ITEM_STACK)
                val name = section.get("name", ConfigAdapter.STRING) ?: "${key.namespace}.research.${key.key}"
                val cost = section.get("cost", ConfigAdapter.LONG)
                val unlocks = section.get("unlocks", ConfigAdapter.SET.from(ConfigAdapter.NAMESPACED_KEY)) ?: emptySet()

                return Research(key, item, Component.translatable(name), cost, unlocks)
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Failed to load research '$key' from config",
                    e
                )
            }
        }
    }
}

@JvmSynthetic
private fun Player.ejectUnknownItems() {
    val toRemove = inventory.contents.filterNotNull().filter { item ->
        val rebarItem = RebarItem.fromStack(item)
        rebarItem != null && !canPickUp(rebarItem, sendMessage = true)
    }
    for (item in toRemove) {
        inventory.remove(item)
        dropItem(item)
    }
}

@JvmSynthetic
fun Player.addResearch(research: Research, sendMessage: Boolean = false, confetti: Boolean = true) {
    research.addTo(this, sendMessage, confetti)
}

@JvmSynthetic
fun Player.removeResearch(research: Research) {
    research.removeFrom(this)
}

@JvmSynthetic
fun Player.hasResearch(research: Research): Boolean {
    return research.isResearchedBy(this)
}
