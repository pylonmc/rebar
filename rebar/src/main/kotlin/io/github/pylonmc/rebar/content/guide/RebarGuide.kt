package io.github.pylonmc.rebar.content.guide

import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.guide.button.BackButton
import io.github.pylonmc.rebar.guide.button.FluidButton
import io.github.pylonmc.rebar.guide.button.PageButton
import io.github.pylonmc.rebar.guide.button.ResearchesButton
import io.github.pylonmc.rebar.guide.pages.RootPage
import io.github.pylonmc.rebar.guide.pages.SearchItemsAndFluidsPage
import io.github.pylonmc.rebar.guide.pages.base.GuidePage
import io.github.pylonmc.rebar.guide.pages.base.SimpleDynamicGuidePage
import io.github.pylonmc.rebar.guide.pages.base.SimpleStaticGuidePage
import io.github.pylonmc.rebar.guide.pages.item.ItemIngredientsPage
import io.github.pylonmc.rebar.guide.pages.research.AddonResearchesPage
import io.github.pylonmc.rebar.guide.pages.research.ResearchItemsPage
import io.github.pylonmc.rebar.guide.pages.research.ResearchesPage
import io.github.pylonmc.rebar.guide.pages.settings.MainSettingsPage
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.base.RebarInteractor
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.item.research.Research
import io.github.pylonmc.rebar.recipe.FluidOrItem
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.rebarKey
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.inventory.ItemStack
import java.util.UUID

/**
 * The one and only Rebar guide.
 */
class RebarGuide(stack: ItemStack) : RebarItem(stack), RebarInteractor {

    @MultiHandler(priorities = [EventPriority.NORMAL, EventPriority.MONITOR])
    override fun onUsedToClick(event: PlayerInteractEvent, priority: EventPriority) {
        if (!event.action.isRightClick || event.useItemInHand() == Event.Result.DENY) return

        if (priority == EventPriority.NORMAL) {
            event.setUseInteractedBlock(Event.Result.DENY)
        } else {
            open(event.player)
        }
    }

    companion object : Listener {

        @JvmField
        val KEY = rebarKey("guide")

        @JvmField
        val STACK = ItemStackBuilder.rebar(Material.BOOK, KEY)
            .set(DataComponentTypes.ITEM_MODEL, Key.key("knowledge_book"))
            .set(DataComponentTypes.MAX_STACK_SIZE, 1)
            .build()

        /**
         * Keeps track of the pages the player last visited
         * Resets when the player ends up on the root page
         */
        @JvmStatic
        val history: MutableMap<UUID, MutableList<GuidePage>> = mutableMapOf()

        /**
         * Hidden items do not show up in searches
         */
        @JvmStatic
        val hiddenItems: MutableSet<NamespacedKey> = mutableSetOf()

        /**
         * Admin items do not show up in searches unless a player has the `rebar.guide.cheat` permission
         */
        @JvmStatic
        val adminOnlyItems: MutableSet<NamespacedKey> = mutableSetOf()

        /**
         * Hidden fluids do not show up in searches
         */
        @JvmStatic
        val hiddenFluids: MutableSet<NamespacedKey> = mutableSetOf()

        /**
         * Hidden researches do not show up in the researches category
         */
        @JvmStatic
        val hiddenResearches: MutableSet<NamespacedKey> = mutableSetOf()

        @JvmStatic
        val fluidsPage = object : SimpleDynamicGuidePage(
            rebarKey("fluids"),
            {
                RebarRegistry.FLUIDS.filter { it.key !in hiddenFluids }
                    .map { FluidButton(it) }
                    .toMutableList()
            }
        ) {}

        @JvmStatic
        val fluidsButton = PageButton(Material.WATER_BUCKET, fluidsPage)

        @JvmStatic
        val infoPage = SimpleStaticGuidePage(rebarKey("info"))

        @JvmStatic
        val infoButton = PageButton(Material.LANTERN, infoPage)

        @JvmStatic
        val researchesPage = ResearchesPage()

        @JvmStatic
        val researchesButton = PageButton(Material.BREWING_STAND, researchesPage)

        @JvmStatic
        fun addonResearchesPage(addon: RebarAddon) = AddonResearchesPage(addon)

        @JvmStatic
        fun addonResearchesButton(addon: RebarAddon) = ResearchesButton(addon)

        @JvmStatic
        fun researchItemsPage(research: Research) = ResearchItemsPage(research)

        @JvmStatic
        val rootPage = RootPage()

        @JvmStatic
        val backButton = BackButton()

        @JvmStatic
        val searchItemsAndFluidsPage = SearchItemsAndFluidsPage()

        @JvmStatic
        val searchItemsAndFluidsButton = PageButton(Material.OAK_SIGN, searchItemsAndFluidsPage)

        @JvmStatic
        val mainSettingsPage = MainSettingsPage

        @JvmStatic
        val mainSettingsButton = PageButton(Material.COMPARATOR, mainSettingsPage)

        /**
         * Lowest priority to avoid another plugin saving the players data or doing something
         * to make the player considered as having played before, before we receive the event
         */
        @EventHandler(priority = EventPriority.LOWEST)
        private fun join(event: PlayerJoinEvent) {
            if (RebarConfig.REBAR_GUIDE_ON_FIRST_JOIN && !event.player.hasPlayedBefore()) {
                event.player.give(STACK.clone())
            }
        }

        @JvmStatic
        fun ingredientsPage(input: FluidOrItem) = ItemIngredientsPage(input)

        @JvmStatic
        fun ingredientsButton(input: FluidOrItem) = PageButton(Material.SCULK_SENSOR, ingredientsPage(input))

        /**
         * Hide an item from showing up in searches
         */
        @JvmStatic
        fun hideItem(key: NamespacedKey) {
            hiddenItems.add(key)
        }

        /**
         * Hide an item from showing up in searches unless a player has the `rebar.guide.cheat` permission
         */
        @JvmStatic
        fun hideItemUnlessAdmin(key: NamespacedKey) {
            adminOnlyItems.add(key)
        }

        /**
         * Hide a fluid from showing up in searches
         */
        @JvmStatic
        fun hideFluid(key: NamespacedKey) {
            hiddenFluids.add(key)
        }

        /**
         * Hide a fluid from showing up in searches
         */
        @JvmStatic
        fun hideResearch(key: NamespacedKey) {
            hiddenResearches.add(key)
        }

        /**
         * Opens the guide to the last page that the player was on
         */
        @JvmStatic
        fun open(player: Player) {
            val history = history.getOrPut(player.uniqueId) { mutableListOf() }
            if (history.isEmpty()) {
                rootPage.open(player)
            } else {
                history.removeLast().open(player)
            }
        }
    }
}