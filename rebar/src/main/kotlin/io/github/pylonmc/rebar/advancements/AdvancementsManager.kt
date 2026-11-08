package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.addon.RebarAddon
import io.github.pylonmc.rebar.config.ConfigSection
import io.github.pylonmc.rebar.config.adapter.ConfigAdapter
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.registry.RebarRegistry
import io.github.pylonmc.rebar.util.mergeResource
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import java.util.logging.Level
import kotlin.io.path.Path

/**
 * Loads Rebar advancements from addon configs and registers them with the server.
 */
internal object AdvancementsManager {

    /**
     * Reads the configuration files of every plugin and loads their [RebarAdvancement]s from config and registers them with the server.
     */
    @JvmSynthetic
    internal fun loadAdvancements() {
        Rebar.logger.info("Beginning to load advancements")
        val start = System.currentTimeMillis()
        // Parse every addon's advancements before registering any, so that an advancement's parent may be
        // defined later in the file or by another addon
        val advancements = mutableListOf<RebarAdvancement>()
        for (addon in RebarRegistry.ADDONS) {
            mergeResource(addon, Rebar, "advancements.yml", "advancements/${addon.key.namespace}.yml", false)
            loadAdvancementsForAddon(addon, advancements)
        }
        val orphans = findOrphans(advancements)
        warnAboutOrphans(
            orphans,
            "${orphans.size} Rebar advancement(s) will not be shown to players because their parent does not exist:",
            "They can still be earned and grant their rewards, but won't appear on the advancements screen until " +
                    "their parent exists."
        )
        // Into the Rebar registry first, so that they can be looked up while the server registration restores progress
        advancements.forEach { it.register() }
        NmsAccessor.instance.registerAdvancements(advancements)
        val end = System.currentTimeMillis()
        Rebar.logger.info("Finished loading advancements in ${(end - start) / 1000.0}s")
    }

    /**
     * Adds the already loaded Rebar advancements back to the server after server resources are reloaded, which
     * replaces the server's advancements with only those from datapacks. Config files are not re-read.
     */
    @JvmSynthetic
    internal fun reregisterAdvancements() {
        // Orphans are checked again, since a datapack advancement used as a parent may have been removed
        val advancements = RebarRegistry.ADVANCEMENTS.toList()
        val orphans = findOrphans(advancements)
        warnAboutOrphans(
            orphans,
            "After reloading server resources, ${orphans.size} Rebar advancement(s) have no parent and will not be shown to players:",
            "They can still be earned and grant their rewards, and will appear on the advancements screen again if " +
                    "their parent comes back in a later reload."
        )
        NmsAccessor.instance.registerAdvancements(advancements)
    }

    /**
     * Loads all the [RebarAdvancement]s for an addon from the config file.
     */
    private fun loadAdvancementsForAddon(addon: RebarAddon, into: MutableList<RebarAdvancement>) {
        val path = "advancements/${addon.key.namespace}.yml"
        val config = try {
            ConfigSection.from(Path(Rebar.dataPath.toString(), path)) ?: return
        } catch (e: Exception) {
            Rebar.logger.log(Level.SEVERE, "Error while reading $path; no advancements from it will be loaded", e)
            return
        }
        // Each advancement is loaded separately so that one bad advancement doesn't prevent the rest from loading
        for (key in config.keys) {
            try {
                into.add(config.getOrThrow(key, ConfigAdapter.ADVANCEMENT))
            } catch (e: Exception) {
                Rebar.logger.log(Level.SEVERE, "Error while loading advancement $key in $path; it will not be registered", e)
            }
        }
    }

    /**
     * Finds the orphans in [advancements]: advancements whose parent doesn't exist, including advancements whose parent
     * is itself an orphan. This is only used to warn about them. Orphans are registered like any other advancement:
     * vanilla keeps them in its list of advancements, so they still work, but leaves them out of the advancement tree,
     * so they aren't sent to clients.
     *
     * Must be called before [advancements] are added to the server, as [Bukkit.getAdvancement] also finds orphans.
     */
    private fun findOrphans(advancements: List<RebarAdvancement>): List<RebarAdvancement> {
        val resolvedKeys = mutableSetOf<NamespacedKey>()
        var pending = advancements
        while (pending.isNotEmpty()) {
            val (ready, blocked) = pending.partition {
                val parent = it.parent
                parent == null || parent in resolvedKeys || Bukkit.getAdvancement(parent) != null
            }
            if (ready.isEmpty()) break
            resolvedKeys += ready.map { it.key }
            pending = blocked
        }
        return pending
    }

    /**
     * Prints out a logging warning that the [orphans] advancements have no parent and so won't be shown to players.
     */
    private fun warnAboutOrphans(orphans: List<RebarAdvancement>, header: String, footer: String) {
        if (orphans.isEmpty()) return
        val orphanKeys = orphans.mapTo(HashSet()) { it.key }
        val logger = Rebar.logger
        logger.severe("!!!!!!!!!!!!!!!!!!!! WARNING !!!!!!!!!!!!!!!!!!!!")
        logger.severe(header)
        for (orphan in orphans) {
            val reason = if (orphan.parent in orphanKeys) {
                "its parent ${orphan.parent} is itself in this list"
            } else {
                "its parent ${orphan.parent} does not exist (check for typos, or a removed datapack or addon)"
            }
            logger.severe("  - ${orphan.key}: $reason")
        }
        logger.severe(footer)
        logger.severe("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    }
}
