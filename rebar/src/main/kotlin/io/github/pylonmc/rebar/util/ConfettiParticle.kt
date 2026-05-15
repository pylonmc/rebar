package io.github.pylonmc.rebar.util

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.async.BukkitMainThreadDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Display
import org.bukkit.util.BoundingBox
import org.bukkit.util.Transformation
import org.bukkit.util.Vector
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.Random
import java.util.function.Consumer
import java.util.function.Supplier

class ConfettiParticle {
    val display: BlockDisplay
    var age = 0
        private set
    val lifetime: Int
    val job: Job

    val velocity: Vector
    val angularVelocity: Vector

    var rotationX = 0.0
    var rotationY = 0.0
    var rotationZ = 0.0

    var block: Block? = null
        private set

    constructor(location: Location, velocity: Vector, lifetime: Int, material: Material) {
        val world = location.getWorld()

        this.display = world.spawn(location, BlockDisplay::class.java, Consumer { d: BlockDisplay ->
            d.block = material.createBlockData()
            d.transformation = Transformation(
                Vector3f(0f, 0f, 0f),
                AxisAngle4f(),
                Vector3f(0.2f, 0.01f, 0.2f),
                AxisAngle4f()
            )
            d.teleportDuration = TICK_AMOUNT
            d.isPersistent = false
        })
        this.lifetime = Math.ceilDiv(lifetime, TICK_AMOUNT)

        // Random initial velocity
        this.velocity = velocity.clone()

        // Random angular velocity (degrees per tick)
        this.angularVelocity = Vector(
            (RANDOM.nextDouble() - 0.5) * 7 * TICK_AMOUNT,
            (RANDOM.nextDouble() - 0.5) * 7 * TICK_AMOUNT,
            (RANDOM.nextDouble() - 0.5) * 7 * TICK_AMOUNT
        )

        this.block = location.block
        this.job = startTickLoop()
    }

    constructor(location: Location, speed: Double, lifetime: Int, material: Material) : this(
        location, Vector(
            (RANDOM.nextDouble() - 0.5) * 0.2,
            RANDOM.nextDouble() * 0.2,
            (RANDOM.nextDouble() - 0.5) * 0.2
        ).normalize().multiply(speed), lifetime, material
    )

    private fun startTickLoop() : Job {
        return Rebar.scope.launch(Rebar.mainThreadDispatcher) {
            while (true) {
                try {
                    if (age++ > lifetime || display.isDead) {
                        remove()
                        break
                    }

                    val currentBlock = display.location.block
                    if (age > IMMORTAL_AGE && currentBlock != block && currentBlock.let {
                            !it.isEmpty && it.boundingBox.overlaps(
                                BoundingBox.of(display.location.toVector(), 0.1, 0.005, 0.1)
                            )
                        }) {
                        remove()
                        break
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

                    val despawning = age == lifetime
                    val t = display.transformation
                    display.interpolationDelay = 0
                    display.interpolationDuration = if (despawning) 10 else TICK_AMOUNT
                    display.transformation = Transformation(
                        t.translation,
                        leftRotation,
                        if (despawning) Vector3f(0f) else t.scale,
                        t.rightRotation
                    )
                    delayTicks(if (despawning) 10 else TICK_AMOUNT.toLong())
                } catch (_: Exception) {
                    remove()
                    break
                }
            }
        }
    }

    fun remove() {
        this.job.cancel()
        try {
            this.display.remove()
        } catch (_: Exception) {}
    }

    companion object {
        private val RANDOM = Random()
        private const val GRAVITY = -0.02
        private const val DRAG = 0.85

        /**
         * There are a few cases where particles would all immediately die when spawning
         * this is to ensure they last at least 1 second.
         */
        private const val IMMORTAL_AGE = 7
        private const val TICK_AMOUNT = 3

        const val DEFAULT_SPEED = 1.0
        const val DEFAULT_LIFETIME = 600

        @JvmField
        val CONCRETES: List<Material> = Material.entries
            .filter { mat: Material -> mat.name.endsWith("CONCRETE") && !mat.isLegacy }
            .toList()

        @JvmStatic
        @JvmOverloads
        fun spawnOne(loc: Location, speed: Double = DEFAULT_SPEED, lifetime: Int = DEFAULT_LIFETIME, mat: Material = CONCRETES.random()): Supplier<ConfettiParticle> {
            return Supplier { ConfettiParticle(loc, speed, lifetime, mat) }
        }

        @JvmStatic
        @JvmOverloads
        fun spawnMany(loc: Location, amount: Int, speed: Double = DEFAULT_SPEED, lifetime: Int = DEFAULT_LIFETIME, materials: Collection<Material> = CONCRETES): Supplier<List<ConfettiParticle>> {
            return spawnMany(loc, amount, speed, speed, lifetime, materials)
        }

        @JvmStatic
        fun spawnMany(loc: Location, amount: Int, minSpeed: Double = DEFAULT_SPEED, maxSpeed: Double = DEFAULT_SPEED, lifetime: Int = DEFAULT_LIFETIME, materials: Collection<Material> = CONCRETES): Supplier<List<ConfettiParticle>> {
            val output: MutableList<Supplier<ConfettiParticle>> = ArrayList()

            repeat(amount) { _ ->
                output.add(spawnOne(loc, minSpeed + (Math.random() * (maxSpeed - minSpeed)), lifetime, materials.random()))
            }

            return Supplier { output.map(Supplier<ConfettiParticle>::get) }
        }

        @JvmStatic
        fun randomMaterial() : Material {
            return CONCRETES.random()
        }
    }
}