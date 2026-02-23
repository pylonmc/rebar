package io.github.pylonmc.rebar.block.base

import org.bukkit.event.EventPriority
import org.bukkit.event.entity.VillagerCareerChangeEvent

interface RebarNoJobBlock : RebarJobBlock {
    override fun onVillagerAcquireJob(event: VillagerCareerChangeEvent, priority: EventPriority) {
        event.isCancelled = true
    }
}