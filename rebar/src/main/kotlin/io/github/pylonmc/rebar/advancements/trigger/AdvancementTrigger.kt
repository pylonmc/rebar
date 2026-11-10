package io.github.pylonmc.rebar.advancements.trigger

import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.registry.RebarRegistry
import org.bukkit.Keyed
import org.bukkit.NamespacedKey

abstract class AdvancementTrigger<TriggerInstanceType : AdvancementTriggerInstance>(private val key: NamespacedKey) :
    Keyed {
    private val mutableInstances = mutableListOf<TriggerInstanceType>()

    /**
     * The instances of this trigger that belong to registered advancements, so triggers don't have to search every
     * advancement for them. An instance only appears here once its advancement is registered: one created by
     * [createTriggerInstance] whose advancement then fails to load never does, as it cannot fire.
     */
    val instances: List<TriggerInstanceType> get() = mutableInstances

    /**
     * Called while the advancement [advancementKey] is being parsed, so the [io.github.pylonmc.rebar.advancements.RebarAdvancement]
     * does not exist yet. [criteriaKey] is the criterion the instance awards when triggered; pass both keys on to
     * the [AdvancementTriggerInstance] constructor. [options] is empty if the trigger was written as a bare key.
     */
    abstract fun createTriggerInstance(
        advancementKey: NamespacedKey,
        criteriaKey: String,
        options: ConfigSection
    ): TriggerInstanceType

    /**
     * Called whenever [instances] changes, so that anything derived from it can be rebuilt.
     */
    protected open fun onInstancesChanged() {}

    @JvmSynthetic
    @Suppress("UNCHECKED_CAST") // An instance's triggerKey is the key of the trigger that created it
    internal fun addInstance(instance: AdvancementTriggerInstance) {
        mutableInstances.add(instance as TriggerInstanceType)
        onInstancesChanged()
    }

    @JvmSynthetic
    internal fun removeInstance(instance: AdvancementTriggerInstance) {
        mutableInstances.remove(instance)
        onInstancesChanged()
    }

    override fun getKey(): NamespacedKey = key

    fun register() {
        RebarRegistry.ADVANCEMENT_TRIGGERS.register(this)
    }
}
