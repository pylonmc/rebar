package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.culling.BlockCullingEngine.isVisibilityInverted
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

/**
 * A variant of [EntityCulledRebarBlock] that defines groups of [EntityGroupCulledRebarBlock]s and entity [UUID]s
 * that should be culled together. (i.e. all blocks in the group must be culled for the entities to be culled)
 *
 * For example, [io.github.pylonmc.rebar.content.fluid.FluidPipe] and [io.github.pylonmc.rebar.content.cargo.CargoDuct] both use greedy meshing,
 * on the display entities (multiple blocks use the same entity), and therefor should
 * only cull said entities if all blocks using that entity are culled.
 */
interface EntityGroupCulledRebarBlock : GroupCulledRebarBLock {
    override val cullingGroups: Iterable<EntityCullingGroup>

    override val isCulledAsync: Boolean
        get() = false

    override fun isVisible(player: Player): Boolean {
        val group = cullingGroups.firstOrNull() ?: return true
        return group.entityIds.firstOrNull()?.let { player.isVisibilityInverted(it) } ?: true
    }

    override fun onGroupVisible(player: Player, group: GroupCulledRebarBLock.CullingGroup) {
        if (group !is EntityCullingGroup) return
        for (entityId in group.entityIds) {
            if (player.isVisibilityInverted(entityId)) {
                Bukkit.getEntity(entityId)?.let { entity ->
                    player.showEntity(Rebar, entity)
                }
            }
        }
    }

    override fun onGroupCulled(player: Player, group: GroupCulledRebarBLock.CullingGroup) {
        if (group !is EntityCullingGroup) return
        for (entityId in group.entityIds) {
            if (!player.isVisibilityInverted(entityId)) {
                Bukkit.getEntity(entityId)?.let { entity ->
                    player.hideEntity(Rebar, entity)
                }
            }
        }
    }

    class EntityCullingGroup(
        id: String,
        blocks: MutableSet<GroupCulledRebarBLock> = mutableSetOf(),
        val entityIds: MutableSet<UUID> = mutableSetOf()
    ) : GroupCulledRebarBLock.CullingGroup(id, blocks)
}