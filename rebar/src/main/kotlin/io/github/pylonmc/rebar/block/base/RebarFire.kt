package io.github.pylonmc.rebar.block.base

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.block.context.BlockCreateContext
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBurnEvent
import org.bukkit.event.block.BlockIgniteEvent
import org.bukkit.event.block.BlockSpreadEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.jetbrains.annotations.ApiStatus

@Suppress("unused")
interface RebarFire {
    fun onDamageEntity(event: EntityDamageEvent, priority: EventPriority) {}

    /**
     * Called when this fire block ignites block
     * Spawns copies of this fire block itself after calling, if the event is not cancelled
     */
    fun onIgniteBlock(event: BlockIgniteEvent, priority: EventPriority) {}

    /**
     * Spawns copies of this fire block itself after calling, if the event is not cancelled
     */
    fun onFireSpread(event: BlockSpreadEvent, priority: EventPriority) {}

    /**
     * Spawns copies of this fire block itself if returns true, otherwise set the block to air.
     *
     * @see Companion.onFireEatBlock
     */
    fun onFireEatBlock(block: Block): Boolean {
        return true
    }

    /**
     * Called when a fire is spawned
     * - after onIgniteBlock
     * - after onFireSpread
     * - fire eats block
     */
    fun onFireSpawn(fire: Block) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onDamageEntity(event: EntityDamageEvent, priority: EventPriority) {
            if (event.cause != EntityDamageEvent.DamageCause.FIRE) return

            val rebarBlock = BlockStorage.get(event.entity.location)
            if (rebarBlock !is RebarFire) {
                return
            }

            try {
                MultiHandlers.handleEvent(rebarBlock, "onDamageEntity", event, priority)
            } catch (e: Exception) {
                BlockListener.logEventHandleErr(event, e, rebarBlock)
            }
        }

        @UniversalHandler
        private fun onIgniteBlock(event: BlockIgniteEvent, priority: EventPriority) {
            if (event.ignitingBlock == null || event.cause != BlockIgniteEvent.IgniteCause.SPREAD) {
                return
            }

            val rebarBlock = BlockStorage.get(event.ignitingBlock!!)
            if (rebarBlock !is RebarFire) return

            try {
                MultiHandlers.handleEvent(rebarBlock, "onIgniteBlock", event, priority)
                
                if (!event.isCancelled) {
                    trySpawnRebarFire(rebarBlock, event.block)
                }
            } catch (e: Exception) {
                BlockListener.logEventHandleErr(event, e, rebarBlock)
            }
        }

        @UniversalHandler
        private fun onFireSpread(event: BlockSpreadEvent, priority: EventPriority) {
            if (event.block.type != Material.FIRE) return

            val rebarBlock = BlockStorage.get(event.source)
            if (rebarBlock !is RebarFire) return

            try {
                MultiHandlers.handleEvent(rebarBlock, "onFireSpread", event, priority)

                if (!event.isCancelled) {
                    trySpawnRebarFire(rebarBlock, event.block)
                }
            } catch (e: Exception) {
                BlockListener.logEventHandleErr(event, e, rebarBlock)
            }
        }

        /**
         * This is a hack to make fire spread work. After calling BlockBurnEvent,
         * the fire still has a chance to spawn a new fire at the block
         * without calling events again.
         */
        @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
        private fun onFireEatBlock(event: BlockBurnEvent) {
            val rebarBlock = BlockStorage.get(event.ignitingBlock!!)
            if (rebarBlock !is RebarFire) return

            Bukkit.getScheduler().runTaskLater(Rebar, Runnable {
                if (event.block.type == Material.FIRE) { // SOUL_FIRE cannot spread, just ignore it.
                    if (rebarBlock.onFireEatBlock(event.block)) {
                        trySpawnRebarFire(rebarBlock, event.block)
                    } else {
                        event.block.type = Material.AIR
                    }
                }
            }, 1)
        }

        private fun <T> trySpawnRebarFire(fire: T, block: Block) where T : RebarFire, T : RebarBlock {
            if (BlockStorage.isRebarBlock(block)) {
                return
            }

            // place new fire
            BlockStorage.placeBlock(
                block,
                fire.key,
                BlockCreateContext.PluginGenerate(Rebar.javaPlugin, block = block, shouldSetType = false)
            )
            fire.onFireSpawn(block)
        }
    }
}