package io.github.pylonmc.rebar.util.gui

import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.gui.unit.UnitFormat
import io.papermc.paper.datacomponent.DataComponentTypes
import io.papermc.paper.datacomponent.item.TooltipDisplay
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.Click
import xyz.xenondevs.invui.item.AbstractItem
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemProvider
import java.time.Duration
import kotlin.math.min

/**
 * An item that counts down (or up if [countDown] is true) from a maximum durability, symbolising progress.
 *
 * For example, this might be used in a custom furnace to show how much burn time is left on
 * the current fuel. In this case, you'd want override [setTotalTimeTicks] and set it to the the total
 * burn time of the fuel (eg: 80 seconds for coal), and then gradually increase [progress] as the
 * remaining burn time decreased.
 *
 * Using any `set` methods on here will automatically update the item in any windows that contain it.
 *
 * @param builder The item stack builder to use for the item
 * @param countDown If true, the progress bar will be inverted, meaning that 0.0 is full and 1.0 is empty.
 */
open class ProgressItem @JvmOverloads constructor(
    item: Item,
    @JvmSynthetic
    internal val countDown: Boolean = true
) : AbstractItem() {

    @JvmOverloads constructor(builder: ItemStackBuilder, inverse: Boolean = true) : this(Item.simple(builder), inverse)

    @JvmOverloads constructor(material: Material, inverse: Boolean = true) : this(ItemStackBuilder.of(material), inverse)

    @JvmOverloads constructor(stack: ItemStack, inverse: Boolean = true) : this(ItemStackBuilder.of(stack), inverse)

    /**
     * The item to be displayed
     */
    var item: Item = item
        @JvmName("setItemInternal")
        private set(value) {
            field = value
            notifyWindows()
        }

    fun setItem(item: Item) {
        this.item = item
        notifyWindows()
    }

    fun setItem(stack: ItemStack) {
        item = Item.simple(stack)
    }

    fun setItem(builder: ItemStackBuilder) {
        item = Item.simple(builder)
    }

    /**
     * The total time of whatever process this item is representing
     */
    var totalTime: Duration? = null
        set(value) {
            field = value
            notifyWindows()
            if (field == null) {
                progress = 0.0
            }
        }

    /**
     * How far through the [totalTime] we are, between 0.0 and 1.0
     */
    var progress: Double = 0.0
        set(value) {
            field = value.coerceIn(0.0, 1.0)
            notifyWindows()
        }

    var lastDisplayedItem: ItemStack = ItemStack.empty()
        private set

    /**
     * Sets how far through the [totalTime] we are
     */
    fun setRemainingTime(time: Duration) {
        check(totalTime != null) { "Remaining time can only be set if total time is not null" }
        progress = 1.0 - time.toNanos().toDouble() / totalTime!!.toNanos().toDouble()
    }

    /**
     * Sets how far through the [totalTime] we are
     */
    fun setRemainingTimeSeconds(seconds: Int) {
        setRemainingTime(Duration.ofSeconds(seconds.toLong()))
    }

    /**
     * Sets how far through the [totalTime] we are
     */
    fun setRemainingTimeTicks(ticks: Int) {
        setRemainingTime(Duration.ofMillis((ticks * 1000.0 / 20.0).toLong()))
    }

    fun setTotalTimeSeconds(seconds: Int?) {
        totalTime = seconds?.let { Duration.ofSeconds(it.toLong()) }
    }

    fun setTotalTimeTicks(ticks: Int?) {
        totalTime = ticks?.let { Duration.ofMillis((it * 1000.0 / 20.0).toLong()) }
    }

    @Suppress("UnstableApiUsage")
    override fun getItemProvider(viewer: Player): ItemProvider {
        var progressValue = progress
        if (!countDown) {
            progressValue = 1 - progressValue
        }

        val builder = ItemStackBuilder.of(item.getItemProvider(viewer).get())
            .clone()
            .set(DataComponentTypes.MAX_DAMAGE, MAX_DURABILITY)

        if (totalTime != null) {
            // +1 so the durability bar is always visible
            builder.set(DataComponentTypes.DAMAGE, min(MAX_DURABILITY, (progressValue * MAX_DURABILITY + 1).toInt()))
        }

        // Hide damage and max damage text
        val tooltipDisplay = builder.get(DataComponentTypes.TOOLTIP_DISPLAY)
        val newTooltipDisplay = TooltipDisplay.tooltipDisplay()
        if (tooltipDisplay != null) {
            // clone existing tooltip because modifying it does not work for some godforesaken reason
            newTooltipDisplay.hideTooltip(tooltipDisplay.hideTooltip())
            for (tooltip in tooltipDisplay.hiddenComponents()) {
                newTooltipDisplay.addHiddenComponents(tooltip)
            }
        }
        newTooltipDisplay.addHiddenComponents(DataComponentTypes.DAMAGE, DataComponentTypes.MAX_DAMAGE)
        builder.set(
            DataComponentTypes.TOOLTIP_DISPLAY,
            newTooltipDisplay
        )

        // Set time in lore
        totalTime?.let {
            val remaining = it - it * progress
            builder.lore(
                Component.translatable(
                    "rebar.gui.time_left",
                    RebarArgument.of("time", UnitFormat.formatDuration(remaining, abbreviate = true, useMillis = false))
                )
            )
        }

        lastDisplayedItem = builder.build()

        return builder
    }

    override fun handleClick(clickType: ClickType, player: Player, click: Click) {}

    companion object {
        private const val MAX_DURABILITY = 1000
    }
}

private operator fun Duration.times(value: Double): Duration = Duration.ofMillis((toMillis() * value).toLong())
