package io.github.pylonmc.rebar.item

import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.content.guide.RebarGuide
import io.github.pylonmc.rebar.guide.pages.base.SimpleInfoPage
import io.github.pylonmc.rebar.guide.pages.base.SimpleStaticGuidePage
import org.bukkit.Material
import org.bukkit.NamespacedKey
import java.util.function.Consumer

/**
 * This is a class which exists only to provide shorthands for common tasks you want to do after
 * registering an item. It is purely for ergonomics and does not actually represent anything important!
 */
class RebarItemRegistrationContext internal constructor(private val schema: RebarItemSchema) {

    /**
     * Adds the item to a guide page. The guide page must be a [SimpleStaticGuidePage].
     *
     * This is just a more convenient way to call [SimpleStaticGuidePage.addItem]
     */
    fun withGuidePage(page: SimpleStaticGuidePage) = apply {
        page.addItem(schema.createNewItemStack())
    }

    /**
     * Sets the block with the given [key] as the item's placed block.
     */
    fun withBlock(key: NamespacedKey) {
        schema.rebarBlockKey = key
    }

    /**
     * Registers a block with the same key as the item and sets it as the item's placed block.
     */
    fun withBlock(clazz: Class<out RebarBlock>, material: Material) = apply {
        RebarBlock.register(clazz, material, schema.key)
        withBlock(schema.key)
    }

    /**
     * Registers a block with the same key as the item and sets it as the item's placed block.
     *
     * The type of block defaults to the type of the item.
     */
    fun withBlock(clazz: Class<out RebarBlock>)
        = withBlock(clazz, schema.createNewItemStack().type)

    /**
     * Creates an info page in the guide if it does not already exist, and lets you edit it
     * using the given [editPage] function.
     */
    fun withInfoPage(editPage: Consumer<SimpleInfoPage>) = apply {
        editPage.accept(RebarGuide.getOrCreateInfoPage(schema.key))
    }

    /**
     * Hide an item from showing up in searches
     */
    fun hideFromGuide() = apply {
        RebarGuide.hideItem(schema.key)
    }

    /**
     * Hide an item from showing up in searches unless a player has the `rebar.guide.cheat` permission
     */
    fun hideFromGuideUnlessAdmin() = apply {
        RebarGuide.hideItemUnlessAdmin(schema.key)
    }
}