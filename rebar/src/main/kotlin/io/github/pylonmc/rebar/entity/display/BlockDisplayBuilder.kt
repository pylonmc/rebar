package io.github.pylonmc.rebar.entity.display

import io.github.pylonmc.rebar.entity.display.transform.TransformBuilder
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display.Brightness
import org.joml.Matrix4f


@Suppress("unused")
open class BlockDisplayBuilder() {

    protected var material: Material? = null
    protected var blockData: BlockData? = null
    protected var transformation: Matrix4f? = null
    protected var glowColor: Color? = null
    protected var brightness: Brightness? = null
    protected var viewRange: Float? = null
    protected var interpolationDelay: Int? = null
    protected var interpolationDuration: Int? = null
    protected var displayWidth: Float? = null
    protected var displayHeight: Float? = null

    constructor(other: BlockDisplayBuilder): this() {
        this.material = other.material
        this.blockData = other.blockData
        this.transformation = other.transformation
        this.glowColor = other.glowColor
        this.brightness = other.brightness
        this.viewRange = other.viewRange
        this.interpolationDelay = other.interpolationDelay
        this.interpolationDuration = other.interpolationDuration
        this.displayWidth = other.displayWidth
        this.displayHeight = other.displayHeight
    }

    fun material(material: Material?): BlockDisplayBuilder {
        this.material = material
        return this
    }

    fun blockData(blockData: BlockData?): BlockDisplayBuilder = apply { this.blockData = blockData }
    fun transformation(transformation: Matrix4f?): BlockDisplayBuilder = apply { this.transformation = transformation }
    fun transformation(builder: TransformBuilder): BlockDisplayBuilder = apply { this.transformation = builder.buildForBlockDisplay() }
    fun brightness(brightness: Brightness): BlockDisplayBuilder = apply { this.brightness = brightness }
    fun brightness(brightness: Int): BlockDisplayBuilder = brightness(Brightness(0, brightness))
    fun glow(glowColor: Color?): BlockDisplayBuilder = apply { this.glowColor = glowColor }
    fun viewRange(viewRange: Float): BlockDisplayBuilder = apply { this.viewRange = viewRange }
    fun interpolationDelay(interpolationDelay: Int): BlockDisplayBuilder = apply { this.interpolationDelay = interpolationDelay }
    fun interpolationDuration(interpolationDuration: Int): BlockDisplayBuilder = apply { this.interpolationDuration = interpolationDuration }
    fun displayWidth(displayWidth: Float): BlockDisplayBuilder = apply { this.displayWidth = displayWidth }
    fun displayHeight(displayHeight: Float): BlockDisplayBuilder = apply { this.displayHeight = displayHeight }

    open fun build(location: Location): BlockDisplay {
        val finalLocation = location.clone()
        finalLocation.yaw = 0.0f
        finalLocation.pitch = 0.0f
        return location.world.spawn(finalLocation, BlockDisplay::class.java, this::update)
    }

    open fun update(display: BlockDisplay) {
        if (material != null) {
            display.block = material!!.createBlockData()
        }
        if (blockData != null) {
            display.block = blockData!!
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
            display.displayHeight = displayHeight!!
        }
    }
}