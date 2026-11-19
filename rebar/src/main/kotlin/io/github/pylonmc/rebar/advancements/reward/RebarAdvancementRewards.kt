package io.github.pylonmc.rebar.advancements.reward

import io.github.pylonmc.rebar.advancements.reward.base.GrantExperienceReward
import io.github.pylonmc.rebar.util.rebarKey

object RebarAdvancementRewards {
    @JvmField
    val GRANT_EXPERIENCE = GrantExperienceReward(rebarKey("grant_experience"))

    init {
        GRANT_EXPERIENCE.register()
    }

    fun init() {

    }
}