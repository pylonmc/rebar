package io.github.pylonmc.rebar.advancements

import com.destroystokyo.paper.event.player.PlayerAdvancementCriterionGrantEvent
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.papermc.paper.event.server.ServerResourcesReloadedEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerAdvancementDoneEvent
import org.bukkit.persistence.PersistentDataType

object AdvancementsListener : Listener {
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: ServerResourcesReloadedEvent){
        TODO() // Reregister advancements
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerAdvancementCriterionGrantEvent){
        val advancement = RebarAdvancement.fromVanilla(event.advancement) ?: return
        val currentPdcData = event.player.persistentDataContainer.get(advancement.key, RebarSerializers.ADVANCEMENT_PLAYER_PERSISTENT_DATA)
            ?: AdvancementPlayerPersistentData(advancement.key, mutableListOf(), false)
        val newCriterionList = mutableListOf(event.criterion)
        newCriterionList.addAll(currentPdcData.completedCriteria)
        val newPdcData = AdvancementPlayerPersistentData(advancement.key, newCriterionList, currentPdcData.completed)
        event.player.persistentDataContainer.set(advancement.key, RebarSerializers.ADVANCEMENT_PLAYER_PERSISTENT_DATA, newPdcData)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    private fun handle(event: PlayerAdvancementDoneEvent){
        val advancement = RebarAdvancement.fromVanilla(event.advancement) ?: return
        val advancementPdcData = event.player.persistentDataContainer.get(advancement.key, RebarSerializers.ADVANCEMENT_PLAYER_PERSISTENT_DATA)
        if(advancementPdcData != null && advancementPdcData.completed) return
        advancement.grantRewards(event.player)
        val newPdcData = AdvancementPlayerPersistentData(advancement.key, advancementPdcData?.completedCriteria ?: listOf(), true)
        event.player.persistentDataContainer.set(advancement.key, RebarSerializers.ADVANCEMENT_PLAYER_PERSISTENT_DATA, newPdcData)
    }
}