package io.github.pylonmc.rebar.advancements

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.papermc.paper.event.server.ServerResourcesReloadedEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent

object AdvancementsListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    private fun handle(event: ServerResourcesReloadedEvent) {
        AdvancementsManager.reregisterAdvancements()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun handle(event: PlayerJoinEvent) {
        AdvancementProgressManager.restore(event.player, RebarRegistry.ADVANCEMENTS)
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handle(event: PlayerAdvancementCriterionGrantEvent) {
        val advancement = RebarAdvancement.fromVanilla(event.advancement) ?: return
        val currentPdcData = event.player.persistentDataContainer.get(
            advancement.key,
            RebarSerializers.ADVANCEMENT_PLAYER_PERSISTENT_DATA
        )
            ?: AdvancementPlayerPersistentData(advancement.key, emptySet(), false)
        event.player.persistentDataContainer.set(
            advancement.key,
            RebarSerializers.ADVANCEMENT_PLAYER_PERSISTENT_DATA,
            currentPdcData.copy(completedCriteria = currentPdcData.completedCriteria + event.criterion)
        )
    }

    // Restoring lost progress re-completes advancements the player already had, so don't announce them again.
    // Paper only broadcasts the message if it is still non-null after this event. HIGHEST rather than MONITOR
    // because this changes the event, and late so that other plugins can't set the message again afterwards.
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun suppressRestoredAnnouncement(event: PlayerAdvancementDoneEvent) {
        if (AdvancementProgressManager.isRestoring(event.player)) {
            event.message(null)
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    private fun handle(event: PlayerAdvancementDoneEvent) {
        val advancement = RebarAdvancement.fromVanilla(event.advancement) ?: return
        val advancementPdcData = event.player.persistentDataContainer.get(
            advancement.key,
            RebarSerializers.ADVANCEMENT_PLAYER_PERSISTENT_DATA
        )
        if (advancementPdcData != null && advancementPdcData.completed) return
        advancement.grantRewards(event.player)
        event.player.persistentDataContainer.set(
            advancement.key,
            RebarSerializers.ADVANCEMENT_PLAYER_PERSISTENT_DATA,
            AdvancementPlayerPersistentData(advancement.key, advancementPdcData?.completedCriteria ?: emptySet(), true)
        )
    }
}
