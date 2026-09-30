package io.github.pylonmc.rebar.entity.interfaces

import com.jogamp.common.util.WeakIdentityHashMap
import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.entity.display.transform.TransformUtil.toMatrix
import io.github.pylonmc.rebar.event.RebarEntityAddEvent
import io.github.pylonmc.rebar.event.RebarEntityRemoveEvent
import io.github.pylonmc.rebar.util.BinaryBvhTree
import io.github.pylonmc.rebar.util.PlayerTarget
import io.github.pylonmc.rebar.util.ReadWriteLockableReference
import io.github.pylonmc.rebar.util.getTargetIncludingInteractableDisplays
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.world.WorldLoadEvent
import org.jetbrains.annotations.ApiStatus
import org.joml.Matrix4fc
import org.joml.Vector3fc
import java.lang.ref.WeakReference
import java.util.*
import kotlin.time.Duration.Companion.seconds

/**
 * Implemented by item displays that wish to listen for player interactions. Normally, display entities cannot
 * listen for interactions as they have no hitbox. Rebar implements a custom interaction system specifically
 * for item displays to allow interactions to be detected.
 * **Important: this only works for item displays that are 1x1x1 cubes, such as acacia logs, sea lanterns, or crafting tables.**
 */
interface InteractableItemDisplayRebarEntity : BinaryBvhTree.Element {

    // automatically implemented by RebarEntity
    val entity: ItemDisplay

    @get:ApiStatus.NonExtendable
    override val boundingBoxTransform: Matrix4fc get() = boxes[this]!!

    @get:ApiStatus.NonExtendable
    override val position: Vector3fc get() = positions[this]!!

    fun onInteract(event: PlayerInteractEvent, interactionLocation: Location) {}

    companion object : Listener {
        private val trees = WeakHashMap<World, ReadWriteLockableReference<BinaryBvhTree<InteractableItemDisplayRebarEntity>>>()

        private val boxes = WeakIdentityHashMap<InteractableItemDisplayRebarEntity, Matrix4fc>()
        private val positions = WeakIdentityHashMap<InteractableItemDisplayRebarEntity, Vector3fc>()
        private val worlds = WeakIdentityHashMap<InteractableItemDisplayRebarEntity, World>()

        /**
         * Obtains the [InteractableItemDisplayRebarEntity]s intersected by the ray starting from [origin] and going in [direction], with a maximum
         * length equal to [direction]
         */
        @JvmStatic
        fun getIntersectedEntities(world: World, origin: Vector3fc, direction: Vector3fc): List<Pair<InteractableItemDisplayRebarEntity, Vector3fc>> {
            val tree = trees[world] ?: return emptyList()
            val elements = tree.read { it.toList() }
            elements.forEach { it.checkForUpdates() }
            return tree.read { it.getIntersections(origin, direction) }
        }

        @EventHandler
        private fun onPlayerInteract(event: PlayerInteractEvent) {
            if (event.action == Action.PHYSICAL) return

            val target = event.player.getTargetIncludingInteractableDisplays() as? PlayerTarget.InteractableItemDisplay ?: return

            event.setUseInteractedBlock(Event.Result.DENY)
            target.display.onInteract(event, target.location)
        }

        @EventHandler
        private fun onRebarEntityAdd(event: RebarEntityAddEvent) {
            val entity = event.rebarEntity as? InteractableItemDisplayRebarEntity ?: return
            val world = entity.entity.world
            boxes[entity] = entity.entity.transformation.toMatrix()
            worlds[entity] = world
            positions[entity] = entity.entity.location.toVector().toVector3f()
            trees[world]!!.write { it.get().insert(entity) }
        }

        @EventHandler
        private fun onRebarEntityRemove(event: RebarEntityRemoveEvent) {
            val entity = event.rebarEntity as? InteractableItemDisplayRebarEntity ?: return
            val world = entity.entity.world
            trees[world]!!.write { it.get().remove(entity) }
            boxes.remove(entity)
            worlds.remove(entity)
            positions.remove(entity)
        }

        @EventHandler
        private fun onWorldLoad(event: WorldLoadEvent) {
            val tree = ReadWriteLockableReference(BinaryBvhTree<InteractableItemDisplayRebarEntity>())
            trees[event.world] = tree

            // occasionally rebuild trees that have been modified a lot in order to improve performance
            val world = WeakReference(event.world)
            Rebar.scope.launch(Dispatchers.IO) {
                while (true) {
                    delay(10.seconds)
                    if (world.get() == null) break
                    val mutations = tree.read { it.mutationsSinceCreation }
                    if (mutations < 64) continue

                    val elements = tree.read { it.toList() }
                    val newTree = BinaryBvhTree.buildFromElements(elements)

                    tree.write {
                        if (tree.get().mutationsSinceCreation != mutations) continue // tree has been modified while rebuilding, try again next loop
                        tree.set(newTree)
                    }
                }
            }
        }

        private fun InteractableItemDisplayRebarEntity.checkForUpdates() {
            val entityTransform = entity.transformation.toMatrix()
            val transformChanged = entityTransform != boundingBoxTransform
            val entityPosition = entity.location.toVector().toVector3f()
            var world = worlds[this]!!
            val positionChanged = entityPosition != position || entity.world != world

            if (transformChanged || positionChanged) {
                trees[world]!!.write { it.get().remove(this) }

                boxes[this] = entityTransform
                world = entity.world
                worlds[this] = world
                positions[this] = entityPosition

                trees[world]!!.write { it.get().insert(this) }
            }
        }
    }
}