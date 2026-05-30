package io.github.pylonmc.rebar.block.interfaces

import org.bukkit.entity.Player

/**
 * A variant of [CulledRebarBlock] that defines groups of [GroupCulledRebarBLock]s that should be culled together.
 * (i.e. all blocks in the group must be culled for any of the blocks in the group to be culled)
 *
 * For an example use case, see [EntityGroupCulledRebarBlock]
 */
interface GroupCulledRebarBLock : CulledRebarBlock {
    val cullingGroups: Iterable<CullingGroup>

    /**
     * Does not do anything for group culled blocks, see [onGroupVisible]
     */
    override fun onVisible(player: Player) {}

    /**
     * Does not do anything for group culled blocks, see [onGroupCulled]
     */
    override fun onCulled(player: Player) {}

    fun onGroupVisible(player: Player, group: CullingGroup)

    fun onGroupCulled(player: Player, group: CullingGroup)

    open class CullingGroup(
        val id: String,
        val blocks: MutableSet<GroupCulledRebarBLock> = mutableSetOf()
    )
}