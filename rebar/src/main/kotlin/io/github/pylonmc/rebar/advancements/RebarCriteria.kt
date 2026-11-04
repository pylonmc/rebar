package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance

data class RebarCriteria(
    val key: String,
    val triggers: List<AdvancementTriggerInstance>
    )
