package io.github.pylonmc.rebar.entity.interfaces

import com.jogamp.common.util.WeakIdentityHashMap
import io.github.pylonmc.rebar.entity.display.transform.TransformUtil.toMatrix
import io.github.pylonmc.rebar.event.RebarEntityAddEvent
import io.github.pylonmc.rebar.event.RebarEntityRemoveEvent
import io.github.pylonmc.rebar.util.BinaryBvhTree
import io.github.pylonmc.rebar.util.PlayerTarget
import io.github.pylonmc.rebar.util.getTargetIncludingInteractableDisplays
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.jetbrains.annotations.ApiStatus
import org.joml.Matrix4fc
import org.joml.Vector3fc
import java.util.*

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
        private val trees = WeakHashMap<World, BinaryBvhTree<InteractableItemDisplayRebarEntity>>()

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
            tree.toList().forEach { it.checkForUpdates() }
            return tree.getIntersections(origin, direction)
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
            boxes[entity] = entity.entity.transformation.toMatrix()
            worlds[entity] = entity.entity.world
            positions[entity] = entity.entity.location.toVector().toVector3f()
            trees.getOrPut(entity.entity.world, ::BinaryBvhTree).insert(entity)
        }

        @EventHandler
        private fun onRebarEntityRemove(event: RebarEntityRemoveEvent) {
            val entity = event.rebarEntity as? InteractableItemDisplayRebarEntity ?: return
            trees[entity.entity.world]!!.remove(entity)
            boxes.remove(entity)
            worlds.remove(entity)
            positions.remove(entity)
        }

        private fun InteractableItemDisplayRebarEntity.checkForUpdates() {
            val entityTransform = entity.transformation.toMatrix()
            val transformChanged = entityTransform != boundingBoxTransform
            val entityPosition = entity.location.toVector().toVector3f()
            var world = worlds[this]!!
            val positionChanged = entityPosition != position || entity.world != world

            if (transformChanged || positionChanged) {
                val tree = trees[world]!!
                tree.remove(this)

                boxes[this] = entityTransform
                world = entity.world
                worlds[this] = world
                positions[this] = entityPosition

                trees.getOrPut(world, ::BinaryBvhTree).insert(this)
            }
        }
    }
}