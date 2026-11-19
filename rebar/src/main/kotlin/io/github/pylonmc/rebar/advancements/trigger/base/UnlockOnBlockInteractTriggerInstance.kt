package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTriggerInstance
import io.github.pylonmc.rebar.block.interfaces.SimpleRebarMultiblock
import org.bukkit.NamespacedKey
import org.bukkit.event.block.Action

class UnlockOnBlockInteractTriggerInstance(
    triggerKey: NamespacedKey,
    advancementKey: NamespacedKey,
    criteriaKey: String,
    val block: SimpleRebarMultiblock.MultiblockComponent,
    val actions: Set<Action>
) : AdvancementTriggerInstance(triggerKey, advancementKey, criteriaKey)