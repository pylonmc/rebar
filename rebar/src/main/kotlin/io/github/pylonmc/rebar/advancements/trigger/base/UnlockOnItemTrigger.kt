package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack

class UnlockOnItemTrigger(key: NamespacedKey) : AdvancementTrigger<UnlockOnItemTriggerInstance>(key) {
    override fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): UnlockOnItemTriggerInstance {
        return UnlockOnItemTriggerInstance(key, advancementKey, criteriaKey, options.getOrThrow("item", ConfigAdapter.ITEM_CHOICE))
    }

    object Ticker : Runnable {
        // I don't even want to think about the performance of this method...
        override fun run() {
            for (advancement in RebarRegistry.ADVANCEMENTS) {
                for (triggerInstance in advancement.criteria.flatMap { crit -> crit.triggers.filterIsInstance<UnlockOnItemTriggerInstance>() }) {
                    for (player in Bukkit.getOnlinePlayers()) {
                        var sum = 0
                        val stacks: ArrayList<ItemStack> = arrayListOf()

                        player.inventory.forEach {
                            if(triggerInstance.itemChoice.matchesIgnoringAmount(it)){
                                sum += it.amount
                                stacks.add(it)
                            }
                        }
                        if (sum >= triggerInstance.itemChoice.amount) {
                            triggerInstance.trigger(player)
                        }
                    }
                }
            }
        }

    }
}