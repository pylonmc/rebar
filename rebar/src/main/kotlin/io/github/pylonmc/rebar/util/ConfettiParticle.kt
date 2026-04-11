package io.github.pylonmc.rebar.util

import io.github.pylonmc.rebar.Rebar
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.BlockDisplay
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.BoundingBox
import org.bukkit.util.Transformation
import org.bukkit.util.Vector
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.Random
import java.util.function.Consumer

class ConfettiParticle {
    private val display: BlockDisplay
    private var age = 0
    private val lifetime: Int

    private val velocity: Vector
    private val angularVelocity: Vector

    private var rotationX = 0.0
    private var rotationY = 0.0
    private var rotationZ = 0.0

    private var block: Block? = null

    constructor(location: Location, velocity: Vector, lifetime: Int, material: Material) {
        val world = location.getWorld()

        this.display = world.spawn<BlockDisplay>(location, BlockDisplay::class.java, Consumer { d: BlockDisplay ->
            d.block = material.createBlockData()
            d.transformation = Transformation(
                Vector3f(0f, 0f, 0f),
                AxisAngle4f(),
                Vector3f(0.2f, 0.01f, 0.2f),
                AxisAngle4f()
            )
            d.teleportDuration = TICK_AMOUNT.toInt()
            d.isPersistent = false
        })
        this.lifetime = lifetime

        // Random initial velocity
        this.velocity = velocity

        // Random angular velocity (degrees per tick)
        this.angularVelocity = Vector(
            (RANDOM.nextDouble() - 0.5) * 7 * TICK_AMOUNT,
            (RANDOM.nextDouble() - 0.5) * 7 * TICK_AMOUNT,
            (RANDOM.nextDouble() - 0.5) * 7 * TICK_AMOUNT
        )

        this.block = location.block

        startTickLoop()
    }

     constructor(location: Location, speed: Double, lifetime: Int, material: Material) : this(
         location, Vector(
            (RANDOM.nextDouble() - 0.5) * 0.2,
            RANDOM.nextDouble() * 0.2,
            (RANDOM.nextDouble() - 0.5) * 0.2
         ).normalize().multiply(speed), lifetime, material
     )

    private fun startTickLoop() {
        object : BukkitRunnable() {
            override fun run() {
                try {
                    if (age++ > lifetime || display.isDead) {
                        display.remove()
                        cancel()
                        return
                    }

                    val currentBlock = display.location.block
                    if (age > IMMORTAL_AGE && currentBlock != block && currentBlock.let {
                            !it.isEmpty && it.boundingBox.overlaps(
                                BoundingBox.of(display.location.toVector(), 0.1, 0.005, 0.1)
                            )
                        }) {
                        display.remove()
                        cancel()
                        return
                    }
                    block = currentBlock

                    // Apply pseudo-random drift
                    val driftX = (RANDOM.nextDouble() - 0.5) * 0.03 * TICK_AMOUNT
                    val driftZ = (RANDOM.nextDouble() - 0.5) * 0.03 * TICK_AMOUNT
                    velocity.add(Vector(driftX, 0.0, driftZ))

                    velocity.setY(velocity.getY() + GRAVITY * TICK_AMOUNT)

                    velocity.multiply(DRAG)

                    val loc = display.location.add(velocity)
                    display.teleport(loc)

                    rotationX += angularVelocity.getX() * TICK_AMOUNT
                    rotationY += angularVelocity.getY() * TICK_AMOUNT
                    rotationZ += angularVelocity.getZ() * TICK_AMOUNT

                    val leftRotation = Quaternionf().rotationXYZ(
                        Math.toRadians(rotationX).toFloat(),
                        Math.toRadians(rotationY).toFloat(),
                        Math.toRadians(rotationZ).toFloat()
                    )

                    val t = display.transformation
                    display.interpolationDelay = 0
                    display.interpolationDuration = TICK_AMOUNT.toInt()
                    display.transformation = Transformation(
                        t.translation,
                        leftRotation,
                        if (age == lifetime) Vector3f(0f) else t.scale,
                        Quaternionf()
                    )
                } catch (e: Exception) {
                    display.remove()
                    cancel()
                }
            }
        }.runTaskTimer(Rebar, 1L, TICK_AMOUNT)
    }

    companion object {
        private val RANDOM = Random()
        private const val GRAVITY = -0.02
        private const val DRAG = 0.85

        /**
         * There are a few cases where particles would all immediately die when spawning
         * this is to ensure they last at least 1 second.
         */
        private const val IMMORTAL_AGE = 10
        private const val TICK_AMOUNT = 2L

        const val DEFAULT_SPEED = 1.0
        const val DEFAULT_LIFETIME = 300

        @JvmField
        val CONCRETES: List<Material> = Material.entries
            .filter { mat: Material -> mat.name.endsWith("CONCRETE") && !mat.isLegacy }
            .toList()

        @JvmStatic
        @JvmOverloads
        fun spawnOne(loc: Location, speed: Double = DEFAULT_SPEED, lifetime: Int = DEFAULT_LIFETIME, mat: Material = CONCRETES.random()): Runnable {
            return Runnable { ConfettiParticle(loc, speed, lifetime, mat) }
        }

        @JvmStatic
        @JvmOverloads
        fun spawnMany(loc: Location, amount: Int, speed: Double = DEFAULT_SPEED, lifetime: Int = DEFAULT_LIFETIME, materials: Collection<Material> = CONCRETES): Runnable {
            val output: MutableList<Runnable> = ArrayList<Runnable>()

            repeat(amount) { _ ->
                output.add(spawnOne(loc, speed, lifetime, materials.random()))
            }

            return Runnable { output.forEach(Runnable::run) }
        }
    }
}