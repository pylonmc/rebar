package io.github.pylonmc.rebar.util

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity

data class RandomizedSound(
    val keys: Collection<Key>,
    val source: Sound.Source,
    val volume: Pair<Double, Double>,
    val pitch: Pair<Double, Double>
) {
    fun create() : Sound = Sound.sound(
        keys.random(),
        source,
        (volume.first + Math.random() * (volume.second - volume.first)).toFloat(),
        (pitch.first + Math.random() * (pitch.second - pitch.first)).toFloat()
    )

    fun play(location: Location) = location.world.playSound(create(), location.x, location.y, location.z)

    fun play(block: Block) = play(block.location.toCenterLocation())

    fun playAt(entity: Entity) = play(entity.location)

    fun playFrom(entity: Entity) = entity.world.playSound(create(), entity)

    @JvmOverloads
    fun playTo(audience: Audience, emitter: Sound.Emitter? = null) = if (emitter != null) {
        audience.playSound(create(), emitter)
    } else {
        audience.playSound(create())
    }
}
