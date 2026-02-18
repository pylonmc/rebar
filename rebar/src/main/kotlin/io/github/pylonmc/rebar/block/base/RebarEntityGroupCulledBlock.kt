package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.content.fluid.FluidPipe
import io.github.pylonmc.rebar.content.cargo.CargoDuct
import io.github.pylonmc.rebar.culling.BlockCullingEngine.isVisibilityInverted
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

/**
 * A variant of [RebarEntityCulledBlock] that defines groups of [RebarEntityGroupCulledBlock]s and entity [UUID]s
 * that should be culled together. (i.e. all blocks in the group must be culled for the entities to be culled)
 *
 * For example, [FluidPipe] and [CargoDuct] both use greedy meshing,
 * on the display entities (multiple blocks use the same entity), and therefor should
 * only cull said entities if all blocks using that entity are culled.
 */
interface RebarEntityGroupCulledBlock : RebarGroupCulledBlock {
    override val cullingGroups: Iterable<EntityCullingGroup>

    override val isCulledAsync: Boolean
        get() = false

    override fun isVisible(player: Player): Boolean {
        val group = cullingGroups.firstOrNull() ?: return true
        return group.entityIds.firstOrNull()?.let { player.isVisibilityInverted(it) } ?: true
    }

    override fun onGroupVisible(player: Player, group: RebarGroupCulledBlock.CullingGroup) {
        if (group !is EntityCullingGroup) return
        for (entityId in group.entityIds) {
            if (player.isVisibilityInverted(entityId)) {
                Bukkit.getEntity(entityId)?.let { entity ->
                    player.showEntity(Rebar, entity)
                }
            }
        }
    }

    override fun onGroupCulled(player: Player, group: RebarGroupCulledBlock.CullingGroup) {
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
        blocks: MutableSet<RebarGroupCulledBlock> = mutableSetOf(),
        val entityIds: MutableSet<UUID> = mutableSetOf()
    ) : RebarGroupCulledBlock.CullingGroup(id, blocks)
}