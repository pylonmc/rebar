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
        val (toRegister, orphans) = splitOrphans(advancements)
        warnAboutOrphans(
            orphans,
            "${orphans.size} Rebar advancement(s) could not be registered because their parent does not exist:",
            "They will not be registered until the problem is fixed and the server is restarted."
        )
        // Into the Rebar registry first, so that they can be looked up while the server registration restores progress
        toRegister.forEach { it.register() }
        NmsAccessor.instance.registerAdvancements(toRegister)
        val end = System.currentTimeMillis()
        Rebar.logger.info("Finished loading advancements in ${(end - start) / 1000.0}s")
    }

    /**
     * Adds the already loaded Rebar advancements back to the server after server resources are reloaded, which
     * replaces the server's advancements with only those from datapacks. Config files are not re-read.
     */
    @JvmSynthetic
    internal fun reregisterAdvancements() {
        // Orphans are checked again, since a datapack advancement used as a parent may have been removed. They are
        // unregistered, so nothing can find them while they aren't on the server.
        val (toRegister, orphans) = splitOrphans(RebarRegistry.ADVANCEMENTS.toList())
        if (orphans.isNotEmpty()) {
            RebarRegistry.ADVANCEMENTS.unregister(*orphans.map { it.key }.toTypedArray())
        }
        warnAboutOrphans(
            orphans,
            "After reloading server resources, ${orphans.size} Rebar advancement(s) no longer have a parent and have been unregistered:",
            "They will stay unregistered until the server is restarted, even if their parent comes back. Anything that " +
                    "uses them will no longer find them. Players keep their progress, which is restored once they are " +
                    "registered again."
        )
        NmsAccessor.instance.registerAdvancements(toRegister)
    }

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
     * Splits [advancements] into those that can be registered and orphans: advancements whose parent doesn't exist,
     * including advancements whose parent is itself an orphan. Vanilla would otherwise leave orphans out of the
     * advancement tree with only a generic error.
     */
    private fun splitOrphans(advancements: List<RebarAdvancement>): Pair<List<RebarAdvancement>, List<RebarAdvancement>> {
        val resolved = mutableListOf<RebarAdvancement>()
        val resolvedKeys = mutableSetOf<NamespacedKey>()
        var pending = advancements
        while (pending.isNotEmpty()) {
            val (ready, blocked) = pending.partition {
                val parent = it.parent
                parent == null || parent in resolvedKeys || Bukkit.getAdvancement(parent) != null
            }
            if (ready.isEmpty()) break
            resolved += ready
            resolvedKeys += ready.map { it.key }
            pending = blocked
        }
        return resolved to pending
    }

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
