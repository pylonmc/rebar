package io.github.pylonmc.rebar.advancements.trigger

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.advancements.trigger.base.UnlockOnJoinTrigger
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.Bukkit

object RebarAdvancementTriggers {
    @JvmField
    val ON_JOIN = UnlockOnJoinTrigger(rebarKey("unlock_on_join"))
    init {
        Bukkit.getPluginManager().registerEvents(ON_JOIN, Rebar)
        ON_JOIN.register()
    }

    fun init(){

    }
}