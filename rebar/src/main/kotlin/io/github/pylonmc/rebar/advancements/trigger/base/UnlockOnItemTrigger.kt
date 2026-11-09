package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.item.ItemTypeWrapper
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player

/**
 * Awards criteria to players holding enough of an item. Run this periodically to check for them.
 *
 * Each inventory is only walked once per run, looking up each stack in an index of which instances care about that
 * item, so the cost of a run does not grow with the number of instances.
 */
class UnlockOnItemTrigger(key: NamespacedKey) : AdvancementTrigger<UnlockOnItemTriggerInstance>(key), Runnable {
    private var instancesByItem: Map<ItemTypeWrapper, List<UnlockOnItemTriggerInstance>>? = null

    override fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): UnlockOnItemTriggerInstance {
        return UnlockOnItemTriggerInstance(key, advancementKey, criteriaKey, options.getOrThrow("item", ConfigAdapter.ITEM_CHOICE))
    }

    override fun onInstancesChanged() {
        instancesByItem = null
    }

    override fun run() {
        val index = instancesByItem ?: buildIndex().also { instancesByItem = it }
        if (index.isEmpty()) return
        for (player in Bukkit.getOnlinePlayers()) {
            scan(player, index)
        }
    }

    private fun buildIndex(): Map<ItemTypeWrapper, List<UnlockOnItemTriggerInstance>> =
        instances.flatMap { instance -> instance.itemChoice.internalChoices.map { it.wrapper to instance } }
            .groupBy({ it.first }, { it.second })
            // A choice listing the same item twice must not count its stacks twice
            .mapValues { it.value.distinct() }

    private fun scan(player: Player, index: Map<ItemTypeWrapper, List<UnlockOnItemTriggerInstance>>) {
        val amounts = LinkedHashMap<UnlockOnItemTriggerInstance, Int>()
        for (stack in player.inventory) {
            val candidates = index[ItemTypeWrapper(stack ?: continue)] ?: continue
            // The index only narrows down the candidates: itemChoice still decides, as it may also check components
            for (instance in candidates) {
                if (instance.itemChoice.matchesIgnoringAmount(stack)) {
                    amounts.merge(instance, stack.amount, Int::plus)
                }
            }
        }
        // Not during the walk above, because rewards are granted synchronously and may change the inventory
        for ((instance, amount) in amounts) {
            if (amount >= instance.itemChoice.amount) {
                instance.trigger(player)
            }
        }
    }
}
