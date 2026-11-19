package io.github.pylonmc.rebar.advancements.trigger

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.advancements.trigger.base.*
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Bukkit

object RebarAdvancementTriggers {
    @JvmField
    val ON_JOIN = UnlockOnJoinTrigger(rebarKey("unlock_on_join"))

    init {
        Bukkit.getPluginManager().registerEvents(ON_JOIN, Rebar)
        ON_JOIN.register()
    }

    @JvmField
    val ON_ITEM = UnlockOnItemTrigger(rebarKey("unlock_on_item"))

    init {
        Bukkit.getScheduler().runTaskTimer(Rebar, ON_ITEM, 0L, RebarConfig.ADVANCEMENTS_INVENTORY_TICKER_BASE_RATE)
        ON_ITEM.register()
    }

    @JvmField
    val ON_ADVANCEMENT = UnlockOnAdvancementTrigger(rebarKey("unlock_on_advancement"))

    init {
        Bukkit.getPluginManager().registerEvents(ON_ADVANCEMENT, Rebar)
        ON_ADVANCEMENT.register()
    }

    @JvmField
    val ON_ENTER_WORLD = UnlockOnEnterWorldTrigger(rebarKey("unlock_on_enter_world"))

    init {
        Bukkit.getPluginManager().registerEvents(ON_ENTER_WORLD, Rebar)
        ON_ENTER_WORLD.register()
    }

    @JvmField
    val ON_PLAYER_STATISTIC = UnlockOnPlayerStatisticTrigger(rebarKey("unlock_on_player_statistic"))

    init {
        Bukkit.getScheduler()
            .runTaskTimer(Rebar, ON_PLAYER_STATISTIC, 0L, RebarConfig.ADVANCEMENTS_STATISTICS_TICKER_BASE_RATE)
        ON_PLAYER_STATISTIC.register()
    }

    @JvmField
    val ON_ITEM_CONSUME = UnlockOnItemConsumeTrigger(rebarKey("unlock_on_item_consume"))

    init {
        Bukkit.getPluginManager().registerEvents(ON_ITEM_CONSUME, Rebar)
        ON_ITEM_CONSUME.register()
    }

    @JvmField
    val ON_ARMOR_EQUIP = UnlockOnArmorEquipTrigger(rebarKey("unlock_on_armor_equip"))

    init {
        Bukkit.getPluginManager().registerEvents(ON_ARMOR_EQUIP, Rebar)
        ON_ARMOR_EQUIP.register()
    }

    @JvmField
    val ON_ITEM_BREAK = UnlockOnItemBreakTrigger(rebarKey("unlock_on_item_break"))
    init {
        Bukkit.getPluginManager().registerEvents(ON_ITEM_BREAK, Rebar)
        ON_ITEM_BREAK.register()
    }

    @JvmField
    val ON_ITEM_USE = UnlockOnItemUseTrigger(rebarKey("unlock_on_item_use"))
    init {
        Bukkit.getPluginManager().registerEvents(ON_ITEM_USE, Rebar)
        ON_ITEM_USE.register()
    }

    @JvmField
    val ON_ENTITY_KILL = UnlockOnEntityKillTrigger(rebarKey("unlock_on_entity_kill"))
    init {
        Bukkit.getPluginManager().registerEvents(ON_ENTITY_KILL, Rebar)
        ON_ENTITY_KILL.register()
    }

    fun init() {

    }
}