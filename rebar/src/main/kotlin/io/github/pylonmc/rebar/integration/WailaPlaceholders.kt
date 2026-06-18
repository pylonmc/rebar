package io.github.pylonmc.rebar.integration

import io.github.pylonmc.rebar.i18n.customMiniMessage
import io.github.pylonmc.rebar.util.plainText
import io.github.pylonmc.rebar.waila.Waila
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import net.kyori.adventure.bossbar.BossBar
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player

/**
 * Please update https://pylonmc.github.io/home/placeholders if adding/removing/modifying any placeholders.
 */
object WailaPlaceholders : PlaceholderExpansion() {
    @JvmField
    val placeholders: List<String> = listOf("text", "plain_text", "color", "overlay", "progress")

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        if (player == null || player !is Player) return null
        val waila = Waila.getWaila(player) ?: return null
        return when(params) {
            "text" -> waila.lastText?.let { customMiniMessage.serialize(it) } ?: ""
            "plain_text" -> waila.lastText?.plainText ?: ""
            "color" -> waila.lastColor?.let { BossBar.Color.NAMES.key(it) } ?: ""
            "overlay" -> waila.lastOverlay?.let { BossBar.Overlay.NAMES.key(it) } ?: ""
            "progress" -> waila.lastProgress?.toString() ?: ""
            else -> null
        }
    }

    override fun getPlaceholders(): List<String?> = placeholders

    override fun getIdentifier(): String = "rebarwaila"

    override fun getAuthor(): String = "Rebar"

    override fun getVersion(): String = "1.0.0"
}