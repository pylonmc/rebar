package io.github.pylonmc.rebar.block.interfaces

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
import org.bukkit.event.block.BlockFormEvent
import org.bukkit.event.entity.EntityChangeBlockEvent
import org.jetbrains.annotations.ApiStatus

interface CopperRebarBlockHandler : VanillaCopperBlock {
    fun onCopperOxidize(event: BlockFormEvent, priority: EventPriority) {}
    fun onCopperWax(event: EntityChangeBlockEvent, priority: EventPriority) {}
    fun onScrapeWax(event: EntityChangeBlockEvent, priority: EventPriority) {}
    fun onScrapeOxidation(event: EntityChangeBlockEvent, priority: EventPriority) {}

    @ApiStatus.Internal
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

        private val OXIDIZE = run {
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
                    map[lastStageMaterial] = oxidizedType
                    lastStageMaterial = oxidizedType
                }
            }
            return@run map
        }

        @UniversalHandler
        private fun onCopperOxidize(event: BlockFormEvent, priority: EventPriority) {
            val from = event.block.type
            val to = event.newState.type
            if (from !in OXIDIZE || OXIDIZE[from] != to) return

            val rebarBlock = BlockStorage.get(event.block) ?: return

            if (rebarBlock is CopperRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onCopperOxidize", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            } else if (rebarBlock !is VanillaCopperBlock) {
                event.isCancelled = true
            }
        }

        @UniversalHandler
        private fun onCopperChange(event: EntityChangeBlockEvent, priority: EventPriority) {
            if (event.entity !is Player) return

            val from = event.block.type
            val to = event.to
            if (from == to || (from !in WAXING && to !in WAXING && to !in OXIDIZE)) return

            val rebarBlock = BlockStorage.get(event.block) ?: return

            val method = when {
                from in WAXING && WAXING[from] == to -> "onCopperWax"
                to in WAXING && WAXING[to] == from -> "onScrapeWax"
                to in OXIDIZE && OXIDIZE[to] == from -> "onScrapeOxidation"
                else -> return
            }

            if (rebarBlock is CopperRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, method, event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            } else if (rebarBlock !is VanillaCopperBlock) {
                event.isCancelled = true
            }
        }
    }
}