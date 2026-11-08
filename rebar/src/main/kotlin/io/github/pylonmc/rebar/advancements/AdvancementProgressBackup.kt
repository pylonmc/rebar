package io.github.pylonmc.rebar.advancements

import io.github.pylonmc.rebar.datatypes.RebarSerializers
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
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
 *
 * For Rebar advancements the PDC is the source of truth; vanilla's progress is only a copy of it. The PDC is updated
 * whenever a criterion is granted or an advancement is completed, so it never needs vanilla's progress to be saved.
 */
@ApiStatus.Internal
object AdvancementProgressBackup {

    /**
     * Awards [player] every criterion of [advancements] that is in the PDC but missing from vanilla's progress.
     *
     * Revokes made through [RebarAdvancement.revokeCriteria] or [RebarAdvancement.revoke] update the PDC. Revokes
     * made through Bukkit or `/advancement revoke` can't be detected, so those criteria are re-awarded here.
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

    // The only code that touches the PDC backup; everything else goes through these

    @JvmSynthetic
    internal fun read(player: Player, advancementKey: NamespacedKey): AdvancementPlayerPersistentData? =
        player.persistentDataContainer.get(advancementKey, RebarSerializers.ADVANCEMENT_PLAYER_PERSISTENT_DATA)

    @JvmSynthetic
    internal fun write(player: Player, data: AdvancementPlayerPersistentData) =
        player.persistentDataContainer.set(data.key, RebarSerializers.ADVANCEMENT_PLAYER_PERSISTENT_DATA, data)

    @JvmSynthetic
    internal fun remove(player: Player, advancementKey: NamespacedKey) =
        player.persistentDataContainer.remove(advancementKey)

    private fun restoreAll(player: Player, advancements: Iterable<RebarAdvancement>) {
        for (advancement in advancements) {
            val backup = read(player, advancement.key) ?: continue
            val vanilla = Bukkit.getAdvancement(advancement.key) ?: continue
            val progress = player.getAdvancementProgress(vanilla)
            val awarded = progress.awardedCriteria
            val criterionNames = advancement.criteria.mapTo(HashSet()) { it.key }
            for (criterion in backup.completedCriteria) {
                // Skip criteria that have since been removed from the advancement's config
                if (criterion in criterionNames && criterion !in awarded) {
                    progress.awardCriteria(criterion)
                }
            }
        }
    }
}
