package io.github.pylonmc.rebar.entity.display

import org.bukkit.Location
import org.bukkit.entity.Interaction

@Suppress("unused")
open class InteractionBuilder() {

    protected var width: Float? = null
    protected var height: Float? = null

    constructor(other: InteractionBuilder): this() {
        this.width = other.width
        this.height = other.height
    }

    fun width(width: Float): InteractionBuilder = apply { this.width = width }
    fun width(width: Double): InteractionBuilder = width(width.toFloat())
    fun height(height: Float): InteractionBuilder = apply { this.height = height }
    fun height(height: Double): InteractionBuilder = height(height.toFloat())
    fun size(size: Float): InteractionBuilder = apply {
        this.width = size
        this.height = size
    }
    fun size(size: Double): InteractionBuilder = size(size.toFloat())

    open fun build(location: Location): Interaction {
        return location.getWorld().spawn(location, Interaction::class.java, this::update)
    }

    open fun update(interaction: Interaction) {
        if (width != null) {
            interaction.interactionWidth = width!!
        }
        if (height != null) {
            interaction.interactionHeight = height!!
        }
    }
}