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
import io.github.pylonmc.rebar.util.breakProgress
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
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.attribute.Attribute
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
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
class Waila private constructor(
    private val player: Player,
    playerConfig: PlayerWailaConfig,
    private val updateContentsJob: Job,
    private val updateTargetJob: Job,
) {

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

    private var playerEyeLocationAtLastTargetUpdate: Location? = null

    // always null if targetEntity is not null
    private var targetBlock: BlockPosition? = null

    // always null if targetBlock is not null
    private var targetEntity: UUID? = null

    var lastText: Component? = null
        private set
    var lastColor: BossBar.Color? = null
        private set
    var lastOverlay: BossBar.Overlay? = null
        private set
    var lastProgress: Float? = null
        private set

    private var wasVisible = false

    private fun send(display: WailaDisplay) {
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

        when (config.type) {
            Type.BOSSBAR -> {
                player.hideBossBar(bossBar)
                bossBar.name(display.text)
                bossBar.color(color)
                bossBar.overlay(overlay)
                bossBar.progress(display.progress)
                player.showBossBar(bossBar)
            }
            Type.ACTIONBAR -> player.sendActionBar(display.text)
        }

        lastText = display.text
        lastColor = color
        lastOverlay = overlay
        lastProgress = display.progress
        wasVisible = true
    }

    private fun hide() {
        if (!wasVisible) {
            return
        }

        when (config.type) {
            Type.BOSSBAR -> player.hideBossBar(bossBar)
            Type.ACTIONBAR -> player.sendActionBar(Component.empty())
        }
        lastText = null
        lastColor = null
        lastOverlay = null
        lastProgress = null
        wasVisible = false
    }

    private fun destroy() {
        hide()
        updateContentsJob.cancel()
        updateTargetJob.cancel()
    }

    // Note: Raytracing is quite expensive especially when done frequently, so we try to
    // limit target recalculation as much as possible
    private fun updateTarget() {
        playerEyeLocationAtLastTargetUpdate = player.eyeLocation

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

        this.targetEntity = null
        this.targetBlock = null

        rayTraceResult?.hitEntity?.let { entity -> this.targetEntity = entity.uniqueId }
        rayTraceResult?.hitBlock?.let { block -> this.targetBlock = block.position }

        updateContents()
    }

    private fun updateContents() {
        if (targetEntity != null) {
            try {
                val entity = Bukkit.getEntity(targetEntity!!)
                if (entity == null || !entity.isValid) {
                    targetEntity = null
                    updateTarget()
                    hide()
                    return
                }

                var display = entityOverrides[targetEntity]?.invoke(player)
                    ?: entity.let(EntityStorage::get)?.getWaila(player)

                if (display == null && player.wailaConfig.vanillaWailaEnabled) {
                    display = if (entity is Item) {
                        WailaDisplay(entity.itemStack.effectiveName())
                    } else {
                        WailaDisplay(Component.translatable(entity.type.translationKey()))
                    }
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
        } else if (targetBlock != null) {
            try {
                val block = targetBlock!!.block

                if (block.isEmpty) {
                    targetBlock = null
                    updateTarget()
                    hide()
                    return
                }

                var display = blockOverrides[targetBlock]?.invoke(player)
                    ?: block.let(BlockStorage::get)?.getWaila(player)

                if (!BlockStorage.isRebarBlock(block) && display == null && player.wailaConfig.vanillaWailaEnabled) {
                    val name = Component.translatable(block.type.translationKey())
                    val prefix = WailaDisplay.getWailaBlockPrefix(block, player)
                    display = if (prefix != null) {
                        WailaDisplay(prefix).add(name)
                    } else {
                        WailaDisplay(name)
                    }
                    display = display.progress(1.0F - block.breakProgress)
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
        } else {
            hide()
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

        @JvmStatic
        fun getWaila(player: Player): Waila? {
            return wailas[player.uniqueId]
        }

        /**
         * Forcibly adds a WAILA display for the given player.
         */
        @JvmStatic
        fun addPlayer(player: Player, config: PlayerWailaConfig = player.wailaConfig) {
            if (wailas.containsKey(player.uniqueId) || !config.enabled) {
                return
            }

            val updateContentsJob = Rebar.scope.launch {
                delayTicks(1)
                val waila = wailas[player.uniqueId]!!
                while (true) {
                    waila.updateContents()
                    delayTicks(RebarConfig.WailaConfig.CONTENTS_TICK_INTERVAL.toLong())
                }
            }

            val updateTargetJob = Rebar.scope.launch {
                delayTicks(1)
                val waila = wailas[player.uniqueId]!!
                while (true) {
                    waila.updateTarget()

                    // Delay for at most TARGET_TICK_INTERVAL * STATIONARY_TARGET_TICK_INTERVAL_MULTIPLIER ticks,
                    // until the player moves their eyes
                    for (i in 0..<RebarConfig.WailaConfig.STATIONARY_TARGET_TICK_INTERVAL_MULTIPLIER) {
                        delayTicks(RebarConfig.WailaConfig.TARGET_TICK_INTERVAL.toLong())
                        if (waila.playerEyeLocationAtLastTargetUpdate != player.eyeLocation) {
                            break
                        }
                    }
                }
            }

            wailas[player.uniqueId] = Waila(player, config, updateContentsJob, updateTargetJob)
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