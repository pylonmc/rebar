package io.github.pylonmc.rebar.advancements.reward

import io.github.pylonmc.rebar.advancements.reward.base.AdvancementRewardItem
import io.github.pylonmc.rebar.util.rebarKey

object RebarAdvancementRewards {
    @JvmField val GIVE_ITEM_REWARD = AdvancementRewardItem(rebarKey("give_item_reward"))
    init {
        GIVE_ITEM_REWARD.register()
    }

    fun init(){

    }
}