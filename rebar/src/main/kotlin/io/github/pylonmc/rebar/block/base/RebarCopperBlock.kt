package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.Registry
import org.bukkit.entity.Player
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityChangeBlockEvent

interface RebarCopperBlock {

    fun applyWaxOn(event: EntityChangeBlockEvent, priority: EventPriority)
    fun scrapeWaxOff(event: EntityChangeBlockEvent, priority: EventPriority)
    fun scrapeOxidation(event: EntityChangeBlockEvent, priority: EventPriority)

    companion object : MultiListener {

        private val COPPER_TYPES = setOf(
            "copper",
            "lightning_rod"
        )

        private val STAGES = setOf(
            "exposed",
            "weathered",
            "oxidized"
        )

        private val WAXING = run {
            val map = mutableMapOf<Material, Material>()
            for (material in Material.entries) {
                if (material.isLegacy || !material.isBlock || COPPER_TYPES.all { !material.key.key.contains(it) }) continue
                val waxedType = Registry.MATERIAL.get(Key.key(material.key.namespace, "waxed_${material.key.key}")) ?: continue
                map[material] = waxedType
            }
            return@run map
        }

        private val SCRAPE_OXIDATION = run {
            val map = mutableMapOf<Material, Material>()
            materials@ for (material in Material.entries) {
                if (material.isLegacy || !material.isBlock || COPPER_TYPES.all { !material.key.key.contains(it) }) continue
                for (stage in STAGES) {
                    if (material.key.key.contains(stage)) {
                        continue@materials
                    }
                }

                var lastStageMaterial = material
                for (stage in STAGES) {
                    val oxidizedType = Registry.MATERIAL.get(Key.key(material.key.namespace, "${stage}_${material.key.key}")) ?: continue
                    map[oxidizedType] = lastStageMaterial
                    lastStageMaterial = oxidizedType
                }
            }
            return@run map
        }

        @UniversalHandler
        private fun onInteractScrape(event: EntityChangeBlockEvent, priority: EventPriority) {
            val player = event.entity as? Player ?: return
            val from = event.block.type
            val to = event.to
            if (from == to || (!WAXING.containsKey(from) && !WAXING.containsKey(to) && !SCRAPE_OXIDATION.containsKey(from))) return

            val rebarBlock = BlockStorage.get(event.block) ?: return

            val method = when {
                WAXING.containsKey(from) && to == WAXING[from] -> "applyWaxOn"
                WAXING.containsKey(to) && from == WAXING[to] -> "scrapeWaxOff"
                SCRAPE_OXIDATION.containsKey(from) && to == SCRAPE_OXIDATION[from] -> "scrapeOxidation"
                else -> return
            }

            if (rebarBlock is RebarCopperBlock) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, method, event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            } else {
                event.isCancelled = true
            }
        }
    }
}