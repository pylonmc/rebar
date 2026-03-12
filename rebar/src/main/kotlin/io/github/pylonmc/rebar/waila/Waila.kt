package io.github.pylonmc.rebar.waila

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.entity.RebarEntity
import io.github.pylonmc.rebar.event.RebarBlockWailaEvent
import io.github.pylonmc.rebar.event.RebarEntityWailaEvent
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.util.delayTicks
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.position.position
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.waila.Waila.Companion.addWailaOverride
import io.papermc.paper.raytracing.RayTraceTarget
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.kyori.adventure.bossbar.BossBar
import net.kyori.adventure.text.Component
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import kotlin.math.max
import kotlin.math.pow

/**
 * Handles WAILAs (the text that displays a block's name when looking
 * at the block).
 *
 * If you want to change the WAILA display for your [RebarBlock] or [RebarEntity], see
 * [RebarBlock.getWaila] and [RebarEntity.getWaila], if you need to change the WAILA
 * display for a different block/entity, see [addWailaOverride].
 */
class Waila private constructor(private val player: Player, playerConfig: PlayerWailaConfig, private val job: Job) {

    private var config = playerConfig
        set(value) {
            if (field.type != value.type) {
                hide()
            }
            field = value
        }

    private val bossBar = BossBar.bossBar(
        Component.empty(),
        RebarConfig.WailaConfig.DEFAULT_DISPLAY.progress,
        RebarConfig.WailaConfig.DEFAULT_DISPLAY.color,
        RebarConfig.WailaConfig.DEFAULT_DISPLAY.overlay
    )

    private fun send(display: WailaDisplay) {
        when (config.type) {
            Type.BOSSBAR -> {
                player.hideBossBar(bossBar)
                val color = if (display.color in RebarConfig.WailaConfig.ALLOWED_BOSS_BAR_COLORS) {
                    display.color
                } else {
                    RebarConfig.WailaConfig.DEFAULT_DISPLAY.color
                }
                val overlay = if (display.overlay in RebarConfig.WailaConfig.ALLOWED_BOSS_BAR_OVERLAYS) {
                    display.overlay
                } else {
                    RebarConfig.WailaConfig.DEFAULT_DISPLAY.overlay
                }

                bossBar.name(display.text)
                bossBar.color(color)
                bossBar.overlay(overlay)
                bossBar.progress(display.progress)
                player.showBossBar(bossBar)
            }
            Type.ACTIONBAR -> player.sendActionBar(display.text)
        }
    }

    private fun hide() {
        when (config.type) {
            Type.BOSSBAR -> {
                if (player.activeBossBars().contains(bossBar)) {
                    player.hideBossBar(bossBar)
                }
            }
            Type.ACTIONBAR -> player.sendActionBar(Component.empty())
        }
    }

    private fun destroy() {
        hide()
        job.cancel()
    }

    private fun updateDisplay() {
        val entityReach = player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)?.value ?: 3.0
        val blockReach = player.getAttribute(Attribute.BLOCK_INTERACTION_RANGE)?.value ?: 4.5

        val rayTraceResult = player.world.rayTrace { builder ->
            builder.start(player.eyeLocation)
            builder.direction(player.eyeLocation.direction)
            builder.maxDistance(max(entityReach, blockReach))
            builder.entityFilter { entity ->
                entity != player && entity.location.distanceSquared(player.eyeLocation) <= entityReach * entityReach
            }
            // Add 0.707 (approximate distance from center to corner of block) and use center location to make sure that all locations on blocks
            // within range are considered. Without this, looking for example at the further end of the side of a block may be filtered out.
            // The real solution would be to check if the point on the block that the player is looking at is within blockReach distance, but
            // this is annoying
            builder.blockFilter { block ->
                block.location.toCenterLocation().distanceSquared(player.eyeLocation) <= (blockReach + 0.707).pow(2)
            }
            builder.targets(RayTraceTarget.ENTITY, RayTraceTarget.BLOCK)
        }

        if (rayTraceResult == null) {
            hide()
            return
        }

        rayTraceResult.hitEntity?.let { entity ->
            try {
                var display = entityOverrides[entity.uniqueId]?.invoke(player)
                    ?: entity.let(EntityStorage::get)?.getWaila(player)

                if (display == null && player.wailaConfig.vanillaWailaEnabled) {
                    display = WailaDisplay(Component.translatable(entity.type.translationKey()))
                }

                if (display != null) {
                    val event = RebarEntityWailaEvent(player, entity, display)
                    event.callEvent()
                    if (!event.isCancelled && event.display != null) {
                        send(event.display!!)
                    } else {
                        hide()
                    }
                } else {
                    hide()
                }
            } catch(e: Exception) {
                e.printStackTrace()
                hide()
            }
        }

        rayTraceResult.hitBlock?.let { block ->
            try {
                var display = blockOverrides[block.position]?.invoke(player)
                    ?: block.let(BlockStorage::get)?.getWaila(player)

                if (display == null && player.wailaConfig.vanillaWailaEnabled) {
                    display = WailaDisplay(Component.translatable(block.type.translationKey()))
                }

                if (display != null) {
                    val event = RebarBlockWailaEvent(player, block, display)
                    event.callEvent()
                    if (!event.isCancelled && event.display != null) {
                        send(event.display!!)
                    } else {
                        hide()
                    }
                } else {
                    hide()
                }
            } catch(e: Exception) {
                e.printStackTrace()
                hide()
            }
        }
    }

    enum class Type {
        BOSSBAR,
        ACTIONBAR
    }

    companion object : Listener {

        private val wailaKey = rebarKey("waila")
        private val wailas = mutableMapOf<UUID, Waila>()

        private val blockOverrides = mutableMapOf<BlockPosition, (Player) -> WailaDisplay?>()
        private val entityOverrides = mutableMapOf<UUID, (Player) -> WailaDisplay?>()

        /**
         * Forcibly adds a WAILA display for the given player.
         */
        @JvmStatic
        fun addPlayer(player: Player, config: PlayerWailaConfig = player.wailaConfig) {
            if (wailas.containsKey(player.uniqueId) || !config.enabled) {
                return
            }

            wailas[player.uniqueId] = Waila(player, config, Rebar.scope.launch {
                delayTicks(1)
                val waila = wailas[player.uniqueId]!!
                while (true) {
                    waila.updateDisplay()
                    delayTicks(RebarConfig.WailaConfig.TICK_INTERVAL.toLong())
                }
            })
        }

        /**
         * Forcibly removes a WAILA display for the given player.
         */
        @JvmStatic
        fun removePlayer(player: Player) {
            wailas.remove(player.uniqueId)?.destroy()
        }

        @JvmStatic
        var Player.wailaConfig: PlayerWailaConfig
            get() = this.persistentDataContainer.getOrDefault(wailaKey, RebarSerializers.PLAYER_WAILA_CONFIG, PlayerWailaConfig()).apply {
                player = this@wailaConfig
                if (!RebarConfig.WailaConfig.ENABLED_TYPES.contains(type)) {
                    sendMessage(Component.translatable("rebar.message.waila.type-disabled").arguments(
                        RebarArgument.of("type", type.name.lowercase())
                    ))
                    type = RebarConfig.WailaConfig.DEFAULT_TYPE
                }
            }
            set(value) {
                this.persistentDataContainer.set(wailaKey, RebarSerializers.PLAYER_WAILA_CONFIG, value)
                if (value.enabled) {
                    if (!wailas.containsKey(uniqueId)) {
                        addPlayer(this, value)
                    } else {
                        wailas[this.uniqueId]?.config = value
                    }
                } else {
                    removePlayer(this)
                }
            }

        /**
         * Adds a WAILA override for the given position. This will always show the
         * provided WAILA config when a WAILA-enabled player looks at the block at
         * the given position, regardless of the block type or even if the block is
         * not a Rebar block.
         *
         * If an override is added for a position that already has an override, the
         * old override will be replaced.
         */
        @JvmStatic
        fun addWailaOverride(position: BlockPosition, provider: (Player) -> WailaDisplay?) {
            blockOverrides[position] = provider
        }

        /**
         * Adds a WAILA override for the given position. This will always show the
         * provided WAILA config when a WAILA-enabled player looks at the block at
         * the given position, regardless of the block type or even if the block is
         * not a Rebar block.
         *
         * If an override is added for a position that already has an override, the
         * old override will be replaced.
         */
        @JvmStatic
        fun addWailaOverride(block: Block, provider: (Player) -> WailaDisplay?)
                = addWailaOverride(block.position, provider)

        /**
         * Adds a WAILA override for the given entity. This will always show the
         * provided WAILA config when a WAILA-enabled player looks at the entity
         * regardless of any other factors.
         *
         * If an override is added for an entity that already has an override, the
         * old override will be replaced.
         */
        @JvmStatic
        fun addWailaOverride(entity: Entity, provider: (Player) -> WailaDisplay?) {
            entityOverrides[entity.uniqueId] = provider
        }

        /**
         * Removes any existing WAILA override for the given position.
         */
        @JvmStatic
        fun removeWailaOverride(position: BlockPosition) {
            blockOverrides.remove(position)
        }

        /**
         * Removes any existing WAILA override for the given position.
         */
        @JvmStatic
        fun removeWailaOverride(block: Block)
                = removeWailaOverride(block.position)

        /**
         * Removes any existing WAILA override for the given entity.
         */
        @JvmStatic
        fun removeWailaOverride(entity: Entity) {
            entityOverrides.remove(entity.uniqueId)
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private fun onPlayerJoin(event: PlayerJoinEvent) {
            val player = event.player
            if (player.wailaConfig.enabled) {
                addPlayer(player)
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        private fun onPlayerQuit(event: PlayerQuitEvent) {
            removePlayer(event.player)
        }
    }
}