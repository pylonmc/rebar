package io.github.pylonmc.rebar.advancements.trigger

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.advancements.trigger.base.UnlockOnAdvancementTrigger
import io.github.pylonmc.rebar.advancements.trigger.base.UnlockOnEnterWorldTrigger
import io.github.pylonmc.rebar.advancements.trigger.base.UnlockOnItemTrigger
import io.github.pylonmc.rebar.advancements.trigger.base.UnlockOnJoinTrigger
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

    fun init() {

    }
}