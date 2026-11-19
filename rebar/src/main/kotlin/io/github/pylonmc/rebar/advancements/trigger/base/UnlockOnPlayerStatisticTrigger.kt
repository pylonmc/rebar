package io.github.pylonmc.rebar.advancements.trigger.base

import io.github.pylonmc.rebar.advancements.trigger.AdvancementTrigger
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Statistic

class UnlockOnPlayerStatisticTrigger(key: NamespacedKey) : AdvancementTrigger<UnlockOnPlayerStatisticTriggerInstance>(
    key
), Runnable {
    private var instancesByStatistic: Map<Statistic, List<UnlockOnPlayerStatisticTriggerInstance>>? = null

    override fun onInstancesChanged() {
        instancesByStatistic = null
    }

    private fun buildIndex(): Map<Statistic, List<UnlockOnPlayerStatisticTriggerInstance>> =
        instances.groupBy { it.statistic }

    override fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): UnlockOnPlayerStatisticTriggerInstance {
        return UnlockOnPlayerStatisticTriggerInstance(
            key,
            advancementKey,
            criteriaKey,
            options.getOrThrow("statistic", ConfigAdapter.ENUM.from<Statistic>()),
            options.getOrThrow("value", ConfigAdapter.INTEGER)
        )
    }

    override fun run() {
        if (instancesByStatistic == null) {
            instancesByStatistic = buildIndex()
        }
        for ((statistic, triggerList) in instancesByStatistic!!) {
            for (player in Bukkit.getOnlinePlayers()) {
                val statisticValue = player.getStatistic(statistic)
                triggerList.filter { statisticValue >= it.value }.forEach {
                    it.trigger(player)
                }
            }
        }
    }
}