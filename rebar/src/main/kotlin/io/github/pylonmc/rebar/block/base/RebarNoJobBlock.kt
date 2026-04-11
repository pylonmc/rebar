package io.github.pylonmc.rebar.block.base

import org.bukkit.event.EventPriority
import org.bukkit.event.entity.VillagerCareerChangeEvent

/**
 * Prevents villagers from using this block as a job block.
 */
interface RebarNoJobBlock : RebarJobBlock {
    override fun onVillagerAcquireJob(event: VillagerCareerChangeEvent, priority: EventPriority) {
        event.isCancelled = true
    }
}