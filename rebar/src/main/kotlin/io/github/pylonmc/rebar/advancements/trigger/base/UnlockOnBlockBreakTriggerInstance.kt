package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance
import io.github.pylonmc.rebar.block.interfaces.SimpleRebarMultiblock
import org.bukkit.NamespacedKey

class UnlockOnBlockBreakTriggerInstance(
    triggerKey: NamespacedKey,
    advancementKey: NamespacedKey,
    criteriaKey: String,
    val block: SimpleRebarMultiblock.MultiblockComponent
) : AdvancementTriggerInstance(triggerKey, advancementKey, criteriaKey)