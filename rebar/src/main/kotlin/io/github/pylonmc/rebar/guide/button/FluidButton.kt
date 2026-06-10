package io.github.pylonmc.rebar.guide.button

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.content.guide.RebarGuide.Companion.playGuideSound
import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.guide.pages.fluid.FluidRecipesPage
import io.github.pylonmc.rebar.guide.pages.fluid.FluidUsagesPage
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.recipe.FluidChoice
import io.github.pylonmc.rebar.recipe.FluidWithAmount
import io.github.pylonmc.rebar.util.gui.unit.UnitFormat
import io.papermc.paper.datacomponent.DataComponentTypes
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.Item

/**
 * Represents a fluid in the guide.
 *
 * @param fluids The list of fluids to display. If multiple fluids are supplied, the button automatically
 * cycles through all of them. You must supply at least one fluid
 */
open class FluidButton private constructor(
    fluids: List<Pair<RebarFluid, Double?>>,

    /**
     * A function to apply to the button item after creating it.
     */
    val preDisplayDecorator: Decorator

) : AbstractItem() {

    val fluids = fluids.shuffled()
    val currentFluid: Pair<RebarFluid, Double?>
        get() = this.fluids[(Bukkit.getCurrentTick() / 20) % this.fluids.size]

    init {
        require(fluids.isNotEmpty()) { "Fluids list cannot be empty" }
    }

    override fun getUpdatePeriod(what: Int): Int = if (fluids.size > 1) 20 else -1

    override fun getItemProvider(player: Player) = try {
        val stack = if (currentFluid.second == null) {
            preDisplayDecorator.invoke(ItemStackBuilder.of(currentFluid.first.item))
        } else {
            preDisplayDecorator.invoke(ItemStackBuilder.of(currentFluid.first.item))
                .name(
                    Component.translatable(
                        "rebar.guide.button.fluid.name",
                        RebarArgument.of("fluid", currentFluid.first.item.getData(DataComponentTypes.ITEM_NAME)!!),
                        RebarArgument.of("amount", UnitFormat.MILLIBUCKETS.format(currentFluid.second!!).decimalPlaces(2))
                    )
                )
        }
        stack
    } catch (e: Exception) {
        e.printStackTrace()
        ItemStackBuilder.of(Material.BARRIER)
            .name(Component.translatable("rebar.guide.button.fluid.error"))
    }

    override fun handleClick(clickType: ClickType, player: Player, click: Click) {
        try {
            if (clickType.isLeftClick) {
                val page = FluidRecipesPage(currentFluid.first.key)
                if (page.pages.isNotEmpty()) {
                    page.open(player)
                    player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
                }
            } else {
                val page = FluidUsagesPage(currentFluid.first)
                if (page.pages.isNotEmpty()) {
                    page.open(player)
                    player.playGuideSound(RebarConfig.GuideConfig.CLICK_BUTTON_SOUND)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private typealias Decorator = (ItemStackBuilder) -> ItemStackBuilder

        /**
         * @param fluids The list of fluids to display. If multiple fluids are supplied, the button
         * cycles through them. (if no fluids are provided, returns an empty item)
         */
        @JvmStatic
        fun of(vararg fluids: RebarFluid?): Item = of(fluids.toList(), null)

        /**
         * @param fluids The list of fluids to display. If multiple fluids are supplied, the button
         * cycles through them. (if no fluids are provided, returns an empty item)
         */
        @JvmStatic
        fun of(amount: Double?, vararg fluids: RebarFluid?): Item = of(fluids.toList(), amount)

        /**
         * @param fluids The list of fluids to display. If multiple fluids are supplied, the button
         * cycles through them. (if no fluids are provided, returns an empty item)
         */
        @JvmStatic
        @JvmOverloads
        fun of(fluids: List<RebarFluid?>, amount: Double?, preDisplayDecorator: Decorator? = null): Item = if (fluids.filterNotNull().isEmpty()) {
            EMPTY
        } else {
            FluidButton(fluids.filterNotNull().map { it to amount}, preDisplayDecorator ?: { it })
        }

        @JvmStatic
        @JvmOverloads
        fun of(fluidChoice: FluidChoice, preDisplayDecorator: Decorator? = null)
                = of(fluidChoice.fluids.toList(), fluidChoice.amount, preDisplayDecorator ?: { it })

        @JvmStatic
        @JvmOverloads
        fun of(fluid: FluidWithAmount, preDisplayDecorator: Decorator? = null)
            = of(listOf(fluid.fluid), fluid.amountMillibuckets, preDisplayDecorator ?: { it })
    }
}
