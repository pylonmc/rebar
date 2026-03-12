package io.github.pylonmc.rebar.guide.pages.base

import io.github.pylonmc.rebar.fluid.RebarFluid
import io.github.pylonmc.rebar.guide.button.FluidButton
import io.github.pylonmc.rebar.guide.button.ItemButton
import io.github.pylonmc.rebar.guide.button.PageButton
import io.github.pylonmc.rebar.guide.button.ResearchButton
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.item.research.Research
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.Item

/**
 * A guide page which has a fixed set of buttons. Do not use this if the buttons
 * on the page will ever need to change.
 *
 * The title of the page will be `<youraddon>.guide.page.<key>`
 *
 * @see SimpleDynamicGuidePage
 * @see SimpleInfoPage
 */
open class SimpleStaticGuidePage @JvmOverloads constructor(
    /**
     * A key that uniquely identifies this page. Used to get the translation key for the title of this page.
     */
    key: NamespacedKey,

    /**
     * The list of buttons to be displayed on this page.
     */
    val buttons: MutableList<Item> = mutableListOf()
) : SimpleDynamicGuidePage(key, { buttons }) {

    constructor(key: NamespacedKey, vararg buttons: Item) : this(key, buttons.asList().toMutableList()) {}

    open fun addButton(button: Item) = buttons.add(button)
    open fun addPage(material: Material, page: GuidePage) = addButton(PageButton(material, page))
    open fun addPage(stack: ItemStack, page: GuidePage) = addButton(PageButton(stack.clone(), page))
    open fun addPage(builder: ItemStackBuilder, page: GuidePage) = addButton(PageButton(builder.build().clone(), page))
    open fun addItem(item: ItemStack) = addButton(ItemButton(item))
    open fun addFluid(fluid: RebarFluid) = addButton(FluidButton(fluid))
    open fun addResearch(research: Research) = addButton(ResearchButton(research))
}