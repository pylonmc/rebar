package io.github.pylonmc.rebar.advancements.base

import io.github.pylonmc.rebar.advancements.EmptyCriterion
import io.github.pylonmc.rebar.advancements.CriteriaType
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.rebarKey
import kotlinx.coroutines.Runnable
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.Registry
import org.bukkit.inventory.ItemStack

object UnlockOnItemCriteriaType : CriteriaType<UnlockOnItemCriterion> {
    override fun createCriterion(
        criterionKey: NamespacedKey,
        config: ConfigSection
    ): UnlockOnItemCriterion {
        val itemKey = config.getOrThrow("item", ConfigAdapter.NAMESPACED_KEY)
        if (RebarRegistry.ITEMS[itemKey] == null && Registry.MATERIAL[itemKey] == null) {
            throw ExceptionInInitializerError("Item key $itemKey not found in rebar or vanilla registry")
        }
        return UnlockOnItemCriterion(
            criterionKey,
            itemKey,
            config.get("count", ConfigAdapter.INTEGER) ?: 1
        )
    }

    override fun getKey(): NamespacedKey {
        return rebarKey("unlock_on_item")
    }

    object Ticker : Runnable {
        // I don't even want to think about the performance of this method...
        override fun run() {
            for (advancement in RebarRegistry.ADVANCEMENTS) {
                for (criteria in advancement.criteria.filterIsInstance<UnlockOnItemCriterion>()) {
                    val item = RebarRegistry.ITEMS[criteria.itemKey]?.getItemStack() ?: ItemStack.of(
                        Registry.MATERIAL.get(
                            criteria.itemKey
                        )!!
                    )
                    val itemIsVanilla = RebarItem.isRebarItem(item)
                    item.amount = criteria.itemCount
                    for (player in Bukkit.getOnlinePlayers()) {
                        var sum = 0
                        val stacks: ArrayList<ItemStack> = arrayListOf()
                        player.inventory.forEach {
                            if (itemIsVanilla) {
                                if (it.type == item.type) {
                                    sum += it.amount
                                    stacks.add(it)
                                }
                            } else {
                                if (RebarItem.fromStack(it)?.key?.equals(criteria.itemKey) == true) {
                                    sum += it.amount
                                    stacks.add(it)
                                }
                            }
                        }
                        if (sum >= criteria.itemCount) {
                            criteria.grant(player, advancement)
                        }
                    }
                }
            }
        }

    }
}

class UnlockOnItemCriterion(private val key: NamespacedKey, val itemKey: NamespacedKey, val itemCount: Int) :
    EmptyCriterion(key) {

}