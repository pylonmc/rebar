package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus
import java.util.UUID

/**
 * Restores Rebar advancement progress from the backup that [AdvancementsListener] keeps in each player's PDC.
 *
 * Vanilla ignores progress for advancements that aren't registered when a player's advancements are loaded, and only
 * writes back the progress it loaded. So if the server ever runs while a Rebar advancement isn't registered (Rebar
 * disabled, failed to start, or that advancement failed to load), the next save permanently deletes its progress
 * from the vanilla advancements file. The PDC backup survives that, and this puts it back.
 */
@ApiStatus.Internal
object AdvancementProgressBackup {

    /**
     * Re-awards [player] any backed-up criteria of [advancements] that vanilla has lost.
     *
     * An advancement is only restored if vanilla has no progress for it at all. Lost progress looks like that,
     * because vanilla drops an unknown advancement's whole entry, and it means revoking individual criteria is not
     * undone. Fully revoking an advancement is not tracked yet, so it will be restored.
     *
     * To avoid toasts, call this before the player's next advancements packet is sent: on join, or between
     * `PlayerAdvancements.reload` and `flushDirty`, where the next packet resets the client's advancements.
     * Chat announcements are suppressed by [AdvancementsListener] while this runs (see [isRestoring]).
     */
    @JvmStatic
    fun restore(player: Player, advancements: Iterable<RebarAdvancement>) {
        // Only add/remove if not already restoring, so a nested call can't end suppression early
        val started = restoring.add(player.uniqueId)
        try {
            restoreAll(player, advancements)
        } finally {
            if (started) {
                restoring.remove(player.uniqueId)
            }
        }
    }

    /**
     * Whether [player]'s progress is currently being restored. Advancements are completed synchronously inside
     * `awardCriteria`, so this is true during any `PlayerAdvancementDoneEvent` caused by a restore.
     */
    @JvmStatic
    fun isRestoring(player: Player): Boolean = player.uniqueId in restoring

    private val restoring = mutableSetOf<UUID>()

    private fun restoreAll(player: Player, advancements: Iterable<RebarAdvancement>) {
        val pdc = player.persistentDataContainer
        for (advancement in advancements) {
            val backup = pdc.get(advancement.key, RebarSerializers.ADVANCEMENT_PLAYER_PERSISTENT_DATA) ?: continue
            val vanilla = Bukkit.getAdvancement(advancement.key) ?: continue
            val progress = player.getAdvancementProgress(vanilla)
            if (progress.awardedCriteria.isNotEmpty()) continue

            val criterionNames = advancement.criteria.mapTo(HashSet()) { it.key }
            for (criterion in backup.completedCriteria) {
                // Skip criteria that have since been removed from the advancement's config
                if (criterion in criterionNames) {
                    progress.awardCriteria(criterion)
                }
            }
        }
    }
}
