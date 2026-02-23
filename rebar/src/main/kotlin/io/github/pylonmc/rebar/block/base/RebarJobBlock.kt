package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandler
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.entity.memory.MemoryKey
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.VillagerCareerChangeEvent

interface RebarJobBlock {
    fun onVillagerAcquireJob(event: VillagerCareerChangeEvent, priority: EventPriority)

    companion object : MultiListener {
        @UniversalHandler
        private fun onVillagerChangeProfession(event: VillagerCareerChangeEvent, priority: EventPriority) {
            if(event.reason != VillagerCareerChangeEvent.ChangeReason.EMPLOYED) return;
            val jobSite = event.entity.getMemory(MemoryKey.JOB_SITE) ?: return
            val rebarBlock = BlockStorage.get(jobSite)
            if (rebarBlock is RebarJobBlock) {
                try {
                    MultiHandler.handleEvent(rebarBlock, "onVillagerAcquireJob", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}