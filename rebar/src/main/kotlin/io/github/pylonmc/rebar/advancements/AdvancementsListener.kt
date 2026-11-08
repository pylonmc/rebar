package io.github.pylonmc.rebar.advancements

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.papermc.paper.event.server.ServerResourcesReloadedEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.event.player.PlayerJoinEvent

object AdvancementsListener : Listener {

    // Reloading server resources (/minecraft:reload) rebuilds the server's advancements from datapacks, which drops
    // Rebar's. This is called after the rebuild, so add them back. LOWEST so that other plugins' handlers see them.
    @EventHandler(priority = EventPriority.LOWEST)
    private fun handle(event: ServerResourcesReloadedEvent) {
        AdvancementsManager.reregisterAdvancements()
    }

    // LOWEST so that other plugins' join handlers see the restored progress. This runs before the player's first
    // advancements packet is sent, so restored advancements don't show toasts.
    @EventHandler(priority = EventPriority.LOWEST)
    private fun handle(event: PlayerJoinEvent) {
        AdvancementProgressBackup.restore(event.player, RebarRegistry.ADVANCEMENTS)
    }

    // Records every granted criterion, whether it came from RebarAdvancement, a trigger, Bukkit's AdvancementProgress
    // or /advancement grant. MONITOR so that this only records criteria that were actually granted: the event is
    // cancellable, and handlers run from LOWEST to HIGHEST, so at any earlier priority another plugin could still
    // cancel it after we recorded it
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private fun handle(event: PlayerAdvancementCriterionGrantEvent){
        val advancement = RebarAdvancement.fromVanilla(event.advancement) ?: return
        val currentPdcData = AdvancementProgressBackup.read(event.player, advancement.key)
            ?: AdvancementPlayerPersistentData(advancement.key, emptySet(), false)
        AdvancementProgressBackup.write(
            event.player,
            currentPdcData.copy(completedCriteria = currentPdcData.completedCriteria + event.criterion)
        )
    }

    // Restoring lost progress re-completes advancements the player already had, so don't announce them again.
    // Paper only broadcasts the message if it is still non-null after this event. HIGHEST rather than MONITOR
    // because this changes the event, and late so that other plugins can't set the message again afterwards.
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun suppressRestoredAnnouncement(event: PlayerAdvancementDoneEvent) {
        if (AdvancementProgressBackup.isRestoring(event.player)) {
            event.message(null)
        }
    }

    // MONITOR so rewards are granted after every other plugin has handled the completion
    @EventHandler(priority = EventPriority.MONITOR)
    private fun handle(event: PlayerAdvancementDoneEvent){
        val advancement = RebarAdvancement.fromVanilla(event.advancement) ?: return
        val advancementPdcData = AdvancementProgressBackup.read(event.player, advancement.key)
        if(advancementPdcData != null && advancementPdcData.completed) return
        advancement.grantRewards(event.player)
        AdvancementProgressBackup.write(
            event.player,
            AdvancementPlayerPersistentData(advancement.key, advancementPdcData?.completedCriteria ?: emptySet(), true)
        )
    }
}
