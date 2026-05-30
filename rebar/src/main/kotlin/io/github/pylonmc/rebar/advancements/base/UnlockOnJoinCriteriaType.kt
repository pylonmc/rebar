package io.github.pylonmc.rebar.advancements.base

import io.github.pylonmc.rebar.advancements.EmptyCriterion
import io.github.pylonmc.rebar.advancements.CriteriaType
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.NamespacedKey
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object UnlockOnJoinCriteriaType : CriteriaType<UnlockOnJoinCriterion> {
    override fun createCriterion(
        criterionKey: NamespacedKey,
        config: ConfigSection
    ): UnlockOnJoinCriterion {
        return UnlockOnJoinCriterion(criterionKey)
    }

    override fun getKey(): NamespacedKey {
        return rebarKey("unlock_on_join")
    }

    object JoinListener : Listener {
        @EventHandler
        fun onPlayerJoin(event: PlayerJoinEvent) {
            for (advancement in RebarRegistry.ADVANCEMENTS) {
                advancement.criteria.forEach {
                    if (it is UnlockOnJoinCriterion) {
                        it.grant(event.player, advancement)
                    }
                }
            }
        }
    }
}

class UnlockOnJoinCriterion(private val key: NamespacedKey) : EmptyCriterion(key) {

}