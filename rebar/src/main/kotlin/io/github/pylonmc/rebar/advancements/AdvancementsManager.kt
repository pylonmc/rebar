package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.config.Config
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.mergeGlobalConfig
import org.bukkit.NamespacedKey
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object AdvancementsManager {
    fun registerAdvancements() {
        for (addon in RebarRegistry.ADDONS) {
            mergeGlobalConfig(addon, "advancements.yml", "advancements/${addon.key.namespace}.yml", false)
            registerAdvancementsForAddon(addon)
        }
    }

    private fun registerAdvancementsForAddon(addon: RebarAddon) {
        val advancementsDir = File(Rebar.dataPath.resolve("advancements").toString())
        if (!advancementsDir.exists() || !advancementsDir.isDirectory) {
            return
        }
        val advancementsFile = advancementsDir.resolve("${addon.key.namespace}.yml")
        if (!advancementsFile.exists()) return

        try {
            val config = Config(advancementsFile)
            config.keys.forEach { it ->
                NmsAccessor.instance.registerAdvancement(
                    config.getOrThrow(it, ConfigAdapter.ADVANCEMENT)
                    , ConfigAdapter.NAMESPACED_KEY.convert(null, it)
                )
            }
        } catch (e: Exception) {
            Rebar.logger.severe("Error while loading advancement $advancementsFile at ${advancementsFile.absolutePath}: \n${e.message}")
            e.printStackTrace()
        }
    }
}