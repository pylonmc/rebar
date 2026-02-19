package io.github.pylonmc.rebar.config

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.waila.Waila
import net.kyori.adventure.bossbar.BossBar

/**
 * The config options for Rebar.
 */
object RebarConfig {

    private val config = Config(Rebar, "config.yml")

    @JvmField
    val REBAR_GUIDE_ON_FIRST_JOIN = config.getOrThrow("rebar-guide-on-first-join", ConfigAdapter.BOOLEAN)

    @JvmField
    val DEFAULT_TICK_INTERVAL = config.getOrThrow("default-tick-interval", ConfigAdapter.INTEGER)

    @JvmField
    val ALLOWED_BLOCK_ERRORS = config.getOrThrow("allowed-block-errors", ConfigAdapter.INTEGER)

    @JvmField
    val ALLOWED_ENTITY_ERRORS = config.getOrThrow("allowed-entity-errors", ConfigAdapter.INTEGER)

    @JvmField
    val FLUID_TICK_INTERVAL = config.getOrThrow("fluid-tick-interval", ConfigAdapter.INTEGER)

    @JvmField
    val BLOCK_DATA_AUTOSAVE_INTERVAL_SECONDS = config.getOrThrow("block-data-autosave-interval-seconds", ConfigAdapter.LONG)

    @JvmField
    val ENTITY_DATA_AUTOSAVE_INTERVAL_SECONDS = config.getOrThrow("entity-data-autosave-interval-seconds", ConfigAdapter.LONG)

    @JvmField
    val RESEARCHES_ENABLED = config.getOrThrow("research.enabled", ConfigAdapter.BOOLEAN)

    @JvmField
    val RESEARCH_BASE_CONFETTI_AMOUNT = config.get("research.confetti.base-amount", ConfigAdapter.DOUBLE, 70.0)

    @JvmField
    val RESEARCH_MULTIPLIER_CONFETTI_AMOUNT = config.get("research.confetti.multiplier", ConfigAdapter.DOUBLE, 0.2)

    @JvmField
    val RESEARCH_MAX_CONFETTI_AMOUNT = config.get("research.confetti.max-amount", ConfigAdapter.INTEGER, 700)

    @JvmField
    val RESEARCH_SOUNDS = config.getOrThrow("research.sounds", ConfigAdapter.MAP.from(ConfigAdapter.LONG, ConfigAdapter.RANDOMIZED_SOUND))

    @JvmField
    val PIPE_PLACEMENT_TASK_INTERVAL_TICKS = config.getOrThrow("pipe-placement.tick-interval", ConfigAdapter.LONG)

    @JvmField
    val PIPE_PLACEMENT_MAX_LENGTH = config.getOrThrow("pipe-placement.max-length", ConfigAdapter.LONG)

    @JvmField
    val PIPE_PLACEMENT_CANCEL_DISTANCE = config.getOrThrow("pipe-placement.cancel-distance", ConfigAdapter.INTEGER)

    @JvmField
    val TRANSLATION_WRAP_LIMIT = config.getOrThrow("translation-wrap-limit", ConfigAdapter.INTEGER)

    @JvmField
    val METRICS_SAVE_INTERVAL_TICKS = config.getOrThrow("metrics-save-interval-ticks", ConfigAdapter.LONG)

    @JvmField
    val DISABLED_ITEMS = config.getOrThrow("disabled-items", ConfigAdapter.SET.from(ConfigAdapter.NAMESPACED_KEY))

    @JvmField
    val INVENTORY_TICKER_BASE_RATE = config.getOrThrow("inventory-ticker-base-rate", ConfigAdapter.LONG)

    @JvmField
    val CARGO_TICK_INTERVAL = config.getOrThrow("cargo-tick-interval", ConfigAdapter.INTEGER)

    @JvmField
    val CARGO_TRANSFER_RATE_MULTIPLIER = config.getOrThrow("cargo-transfer-rate-multiplier", ConfigAdapter.INTEGER)

    object WailaConfig {

        @JvmStatic
        val ENABLED
            get() = TICK_INTERVAL > 0 && ENABLED_TYPES.isNotEmpty()

        @JvmField
        val TICK_INTERVAL = config.getOrThrow("waila.tick-interval", ConfigAdapter.INTEGER)

        @JvmField
        val ENABLED_TYPES = config.getOrThrow("waila.enabled-types", ConfigAdapter.LIST.from(ConfigAdapter.ENUM.from(Waila.Type::class.java)))

        @JvmField
        val DEFAULT_TYPE = config.getOrThrow("waila.default-type", ConfigAdapter.ENUM.from(Waila.Type::class.java)).apply {
            if (!ENABLED_TYPES.contains(this)) {
                throw IllegalStateException("Default Waila type $this is not in the list of enabled types: $ENABLED_TYPES")
            }
        }

        @JvmField
        val ALLOWED_BOSS_BAR_COLORS = config.getOrThrow("waila.bossbar.allowed-colors", ConfigAdapter.SET.from(ConfigAdapter.ENUM.from(BossBar.Color::class.java)))

        @JvmField
        val ALLOWED_BOSS_BAR_OVERLAYS = config.getOrThrow("waila.bossbar.allowed-overlays", ConfigAdapter.SET.from(ConfigAdapter.ENUM.from(BossBar.Overlay::class.java)))

        @JvmField
        val DEFAULT_DISPLAY = config.getOrThrow("waila.default-display.bossbar", ConfigAdapter.WAILA_DISPLAY).apply {
            if (!ALLOWED_BOSS_BAR_COLORS.contains(color)) {
                throw IllegalStateException("Default bossbar color $color is not in the list of allowed colors: $ALLOWED_BOSS_BAR_COLORS")
            }
            if (!ALLOWED_BOSS_BAR_OVERLAYS.contains(overlay)) {
                throw IllegalStateException("Default bossbar overlay $overlay is not in the list of allowed overlays: $ALLOWED_BOSS_BAR_OVERLAYS")
            }
        }
    }

    object ArmorTextureConfig {

        @JvmField
        val ENABLED = config.getOrThrow("custom-armor-textures.enabled", ConfigAdapter.BOOLEAN)

        @JvmField
        val FORCED = config.getOrThrow("custom-armor-textures.force", ConfigAdapter.BOOLEAN)

    }

    object BlockTextureConfig {

        @JvmField
        val ENABLED = config.getOrThrow("custom-block-textures.enabled", ConfigAdapter.BOOLEAN)

        @JvmField
        val DEFAULT = config.getOrThrow("custom-block-textures.default", ConfigAdapter.BOOLEAN)

        @JvmField
        val FORCED = config.getOrThrow("custom-block-textures.force", ConfigAdapter.BOOLEAN)

    }

    object CullingEngineConfig {
        @JvmField
        val ENABLED = config.getOrThrow("block-culling-engine.enabled", ConfigAdapter.BOOLEAN)

        @JvmField
        val OCCLUDING_CACHE_INVALIDATE_INTERVAL = config.getOrThrow("block-culling-engine.occluding-cache-invalidate-interval", ConfigAdapter.INTEGER)

        @JvmField
        val OCCLUDING_CACHE_INVALIDATE_SHARE = config.getOrThrow("block-culling-engine.occluding-cache-invalidate-share", ConfigAdapter.DOUBLE)

        @JvmField
        val SYNC_APPLY_INTERVAL = config.getOrThrow("block-culling-engine.sync-apply-interval", ConfigAdapter.INTEGER)
        
        @JvmField
        val PRESETS_ONLY = config.getOrThrow("block-culling-engine.presets-only", ConfigAdapter.BOOLEAN)

        @JvmField
        val DISABLED_UPDATE_INTERVAL = config.getOrThrow("block-culling-engine.disabled-update-interval", ConfigAdapter.INTEGER)

        @JvmField
        val UPDATE_INTERVAL_LIMIT = config.getOrThrow("block-culling-engine.limits.update-interval", ConfigAdapter.INT_RANGE)

        @JvmField
        val HIDDEN_INTERVAL_LIMIT = config.getOrThrow("block-culling-engine.limits.hidden-interval", ConfigAdapter.INT_RANGE)

        @JvmField
        val VISIBLE_INTERVAL_LIMIT = config.getOrThrow("block-culling-engine.limits.visible-interval", ConfigAdapter.INT_RANGE)

        @JvmField
        val ALWAYS_SHOW_RADIUS_LIMIT = config.getOrThrow("block-culling-engine.limits.always-show-radius", ConfigAdapter.INT_RANGE)

        @JvmField
        val CULL_RADIUS_LIMIT = config.getOrThrow("block-culling-engine.limits.cull-radius", ConfigAdapter.INT_RANGE)

        @JvmField
        val MAX_OCCLUDING_COUNT_LIMIT = config.getOrThrow("block-culling-engine.limits.max-occluding-count", ConfigAdapter.INT_RANGE)

        @JvmField
        val CULLING_PRESETS = config.getOrThrow("block-culling-engine.presets", ConfigAdapter.MAP.from(ConfigAdapter.STRING, ConfigAdapter.CULLING_PRESET)).apply {
            if (isEmpty()) {
                throw IllegalStateException("At least one culling preset must be defined")
            }
        }

        @JvmField
        val FORCE_DISABLED = config.getOrThrow("block-culling-engine.forced", ConfigAdapter.STRING) == "disabled"

        @JvmField
        val FORCED_CULLING_PRESET = run {
            when (val key = config.getOrThrow("block-culling-engine.forced", ConfigAdapter.STRING)) {
                "none", "disabled" -> null
                else -> CULLING_PRESETS[key] ?: error("No culling preset with forced-preset id '$key' found")
            }
        }

        @JvmField
        val DEFAULT = config.getOrThrow("block-culling-engine.default", ConfigAdapter.STRING) != "disabled"

        @JvmField
        val DEFAULT_CULLING_PRESET = run {
            when (DEFAULT) {
                true -> CULLING_PRESETS.getValue(CULLING_PRESETS.keys.first())
                false -> {
                    val key = config.getOrThrow("block-culling-engine.default-preset", ConfigAdapter.STRING)
                    CULLING_PRESETS[key] ?: error("No culling preset with id '$key' found")
                }
            }
        }
    }

}