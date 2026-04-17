package io.github.pylonmc.rebar.entity.display

import io.github.pylonmc.rebar.entity.display.transform.TransformBuilder
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Display
import org.bukkit.entity.Display.Billboard
import org.bukkit.entity.Display.Brightness
import org.bukkit.entity.ItemDisplay
import org.bukkit.inventory.ItemStack
import org.joml.Matrix4f


@Suppress("unused")
open class ItemDisplayBuilder() {

    protected var itemStack: ItemStack? = null
    protected var itemDisplayTransform : ItemDisplay.ItemDisplayTransform? = null
    protected var transformation: Matrix4f? = null
    protected var brightness: Brightness? = null
    protected var glowColor: Color? = null
    protected var billboard: Billboard? = null
    protected var viewRange: Float? = null
    protected var interpolationDelay: Int? = null
    protected var interpolationDuration: Int? = null
    protected var displayWidth: Float? = null
    protected var displayHeight: Float? = null
    protected var persistent: Boolean? = null

    constructor(other: ItemDisplayBuilder): this() {
        this.itemStack = other.itemStack
        this.itemDisplayTransform = other.itemDisplayTransform
        this.transformation = other.transformation
        this.brightness = other.brightness
        this.glowColor = other.glowColor
        this.billboard = other.billboard
        this.viewRange = other.viewRange
        this.interpolationDelay = other.interpolationDelay
        this.interpolationDuration = other.interpolationDuration
        this.displayWidth = other.displayWidth
        this.displayHeight = other.displayHeight
        this.persistent = other.persistent
    }

    fun material(material: Material) = apply { this.itemStack = ItemStack(material) }
    fun itemStack(itemStack: ItemStack?) = apply { this.itemStack = itemStack }
    fun itemStack(builder: ItemStackBuilder) = apply { this.itemStack = builder.build() }
    fun itemDisplayTransform(itemDisplayTransform: ItemDisplay.ItemDisplayTransform?) = apply { this.itemDisplayTransform = itemDisplayTransform }
    fun transformation(transformation: Matrix4f?) = apply { this.transformation = transformation }
    fun transformation(builder: TransformBuilder) = apply { this.transformation = builder.buildForItemDisplay() }
    fun brightness(brightness: Brightness) = apply { this.brightness = brightness }
    fun brightness(brightness: Int) = brightness(Brightness(0, brightness))
    fun glow(glowColor: Color?)  = apply { this.glowColor = glowColor }
    fun billboard(billboard: Billboard?) = apply { this.billboard = billboard }
    fun viewRange(viewRange: Float) = apply { this.viewRange = viewRange }
    fun interpolationDelay(interpolationDelay: Int) = apply { this.interpolationDelay = interpolationDelay }
    fun interpolationDuration(interpolationDuration: Int) = apply { this.interpolationDuration = interpolationDuration }
    fun displayWidth(displayWidth: Float): ItemDisplayBuilder = apply { this.displayWidth = displayWidth }
    fun displayHeight(displayHeight: Float): ItemDisplayBuilder = apply { this.displayHeight = displayHeight }
    fun persistent(persistent: Boolean): ItemDisplayBuilder = apply { this.persistent = persistent }

    open fun build(location: Location): ItemDisplay {
        val finalLocation = location.clone()
        finalLocation.yaw = 0.0F
        finalLocation.pitch = 0.0F
        return finalLocation.getWorld().spawn(finalLocation, ItemDisplay::class.java, this::update)
    }

    open fun update(display: Display) {
        if (display !is ItemDisplay) {
            throw IllegalArgumentException("Must provide an ItemDisplay")
        }
        if (itemStack != null) {
            display.setItemStack(itemStack)
        }
        if (itemDisplayTransform != null) {
            display.itemDisplayTransform = itemDisplayTransform!!
        }
        if (transformation != null) {
            display.setTransformationMatrix(transformation!!)
        }
        if (glowColor != null) {
            display.isGlowing = true
            display.glowColorOverride = glowColor
        }
        if (brightness != null) {
            display.brightness = brightness
        }
        if (billboard != null) {
            display.billboard = billboard!!
        }
        if (viewRange != null) {
            display.viewRange = viewRange!!
        }
        if (interpolationDelay != null) {
            display.interpolationDelay = interpolationDelay!!
        }
        if (interpolationDuration != null) {
            display.interpolationDuration = interpolationDuration!!
        }
        if (displayWidth != null) {
            display.displayWidth = displayWidth!!
        }
        if (displayHeight != null) {
            display.displayWidth = displayHeight!!
        }
        if (persistent != null) {
            display.isPersistent = persistent!!
        }
    }
}