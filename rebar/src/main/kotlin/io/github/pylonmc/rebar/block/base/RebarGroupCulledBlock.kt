package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.resourcepack.block.BlockTextureEngine.isVisibilityInverted
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

/**
 * TODO: Come up with a better name if possible
 *
 * A variant of [RebarCulledBlock] that defines groups of [RebarGroupCulledBlock]s and entity [UUID]s
 * that should be culled together. (i.e. all blocks in the group must be culled for the entities to be culled)
 *
 * For example, [io.github.pylonmc.rebar.content.fluid.FluidPipe] and [io.github.pylonmc.rebar.content.cargo.CargoDuct] both use greedy meshing,
 * on the display entities (multiple blocks use the same entity), and therefor should
 * only cull said entities if all blocks using that entity are culled.
 *
 * TODO: allow defining multiple groups per block with entities per group
 *  likely requires not extending PylonCulledBlock because that only allows one set of culledEntityIds
 */
interface RebarGroupCulledBlock {
    val cullingGroups: Iterable<CullingGroup>

    fun showEntities(player: Player, id: String) {
        val group = cullingGroups.firstOrNull { it.id == id } ?: return
        for (entityId in group.entityIds) {
            if (player.isVisibilityInverted(entityId)) {
                Bukkit.getEntity(entityId)?.let { entity ->
                    player.showEntity(Rebar, entity)
                }
            }
        }
    }

    fun hideEntities(player: Player, id: String) {
        val group = cullingGroups.firstOrNull { it.id == id } ?: return
        for (entityId in group.entityIds) {
            if (!player.isVisibilityInverted(entityId)) {
                Bukkit.getEntity(entityId)?.let { entity ->
                    player.hideEntity(Rebar, entity)
                }
            }
        }
    }

    data class CullingGroup(
        val id: String,
        val blocks: MutableSet<RebarGroupCulledBlock> = mutableSetOf(),
        val entityIds: MutableSet<UUID> = mutableSetOf()
    )
}