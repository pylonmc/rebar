package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance

/**
 * One criterion of an advancement.
 *
 * @property key the criterion's name, unique within its advancement
 * @property triggers the triggers that award this criterion. Any one of them firing awards it; use the advancement's
 * requirements to require several criteria. With no triggers, the criterion can only be awarded through code or
 * `/advancement grant`.
 */
data class RebarCriteria(
    val key: String,
    val triggers: List<AdvancementTriggerInstance>
)
