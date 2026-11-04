package io.github.pylonmc.rebar.advancements.trigger

import io.github.pylonmc.rebar.advancements.trigger.base.PlayerAcquireItemAdvancementTrigger
import io.github.pylonmc.rebar.advancements.trigger.base.PlayerJoinAdvancementTrigger
import io.github.pylonmc.rebar.util.rebarKey

object RebarAdvancementTriggers {
    @JvmField val ACQUIRE_ITEM_TRIGGER = PlayerAcquireItemAdvancementTrigger(rebarKey("player_acquire_item_advancement_trigger"))
    init {
        ACQUIRE_ITEM_TRIGGER.register()
    }
    @JvmField val PLAYER_JOIN_TRIGGER = PlayerJoinAdvancementTrigger(rebarKey("player_join_advancement_trigger"))
    init {
        PLAYER_JOIN_TRIGGER.register()
    }

    fun init(){

    }
}