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
    fun height(height: Float): InteractionBuilder = apply { this.height = height }
    fun size(size: Float): InteractionBuilder = apply {
        this.width = size
        this.height = size
    }

    open fun build(location: Location): Interaction {
        val finalLocation = location.clone()
        // Account for Mojang deciding to center the entity on Y but not X and Z for some reason
        if (height != null) {
            finalLocation.subtract(0.0, height!! / 2.0, 0.0)
        }
        return location.getWorld().spawn(finalLocation, Interaction::class.java, this::update)
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