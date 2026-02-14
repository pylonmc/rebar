package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.culling.BlockCullingEngine.isVisibilityInverted
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

interface RebarEntityCulledBlock : RebarCulledBlock {
    /**
     * Any entity id's that should be culled when the block is considered culled by the BlockTextureEngine.
     * The default implementation of [onVisible] and [onCulled] assumes that these are real entities
     * When using real entities you **cannot** include entities in this list that are invisible by default,
     * if you do they will not be culled properly.
     *
     * You can include uuids of fake/packet entities, but you **must** override [onVisible] and [onCulled]
     * If that is the case you may also be able to set [isCulledAsync] to true.
     */
    val culledEntityIds: Iterable<UUID>

    /**
     * If the entities should be shown/hidden asynchronously instead of scheduled to be shown/hidden on the main thread.
     * By default, this is false and the entities will be shown/hidden on the main thread because they are real entities.
     *
     * If the entries in [culledEntityIds] are fake/packet entities, and you have overridden [isVisible], [onVisible], and [onCulled]
     * to handle showing/hiding them & it supports it, you can set this to true.
     */
    override val isCulledAsync: Boolean
        get() = false

    /**
     * If the block is currently visible to the player.
     * By default, this checks that the first entity from [culledEntityIds] is not toggled invisible for the player
     * If you are using fake/packet entities you must override this method.
     */
    override fun isVisible(player: Player): Boolean {
        return culledEntityIds.firstOrNull()?.let { !player.isVisibilityInverted(it) } ?: true
    }

    /**
     * Reveals all entities in [culledEntityIds] **not currently visible** to the player.
     * By default, this assumes that all entities in [culledEntityIds] are real entities,
     * if they are not you must override this method.
     */
    override fun onVisible(player: Player) {
        for (entityId in culledEntityIds) {
            if (player.isVisibilityInverted(entityId)) {
                Bukkit.getEntity(entityId)?.let { entity ->
                    player.showEntity(Rebar, entity)
                }
            }
        }
    }

    /**
     * Hides all entities in [culledEntityIds] **currently visible** to the player.
     * By default, this assumes that all entities in [culledEntityIds] are real entities,
     * if they are not you must override this method.
     */
    override fun onCulled(player: Player) {
        for (entityId in culledEntityIds) {
            if (!player.isVisibilityInverted(entityId)) {
                Bukkit.getEntity(entityId)?.let { entity ->
                    player.hideEntity(Rebar, entity)
                }
            }
        }
    }
}