package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.mergeResource
import kotlin.io.path.Path

object AdvancementsManager {
    fun registerAdvancements() {
        Rebar.logger.info("Beginning to load advancements")
        val start = System.currentTimeMillis()
        for (addon in RebarRegistry.ADDONS) {
            mergeResource(addon, "advancements.yml", "advancements/${addon.key.namespace}.yml", false)
            registerAdvancementsForAddon(addon)
        }
        val end = System.currentTimeMillis()
        Rebar.logger.info("Finished loading advancements in ${(end - start) / 1000.0}s")
    }

    private fun registerAdvancementsForAddon(addon: RebarAddon) {
        try {
            val config = ConfigSection.from(Path(Rebar.dataPath.toString(), "advancements/${addon.key.namespace}.yml")) ?: return
            config.keys.forEach { it ->
                NmsAccessor.instance.registerAdvancement(
                    config.getOrThrow(it, ConfigAdapter.ADVANCEMENT)
                    , ConfigAdapter.NAMESPACED_KEY.convert(null, it)
                )
            }
        } catch (e: Exception) {
            Rebar.logger.severe("Error while loading advancement at advancements/${addon.key.namespace}.yml: \n${e.message}")
            e.printStackTrace()
        }
    }
}