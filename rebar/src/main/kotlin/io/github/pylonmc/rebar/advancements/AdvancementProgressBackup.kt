package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.annotations.ApiStatus

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
     */
    @JvmStatic
    fun restore(player: Player, advancements: Iterable<RebarAdvancement>) {
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
