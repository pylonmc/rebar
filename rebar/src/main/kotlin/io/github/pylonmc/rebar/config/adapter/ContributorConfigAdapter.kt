package io.github.pylonmc.rebar.config.adapter

import io.github.pylonmc.rebar.config.ContributorConfig
import org.bukkit.Bukkit
import java.lang.reflect.Type

object ContributorConfigAdapter : ConfigAdapter<ContributorConfig> {
    override val type: Type = ContributorConfig::class.java

    override fun convert(value: Any): ContributorConfig {
        if (value is String) {
            return ContributorConfig(displayName = value)
        }

        val section = ConfigAdapter.CONFIG_SECTION.convert(value)
        val displayName = section.getOrThrow("display-name", ConfigAdapter.STRING)
        val description = section.get("description", ConfigAdapter.STRING)
        val minecraftUUID = section.get("minecraft-uuid", ConfigAdapter.UUID) { Bukkit.getOfflinePlayer(displayName).uniqueId }
        val githubUsername = section.get("github", ConfigAdapter.STRING, displayName)
        val link = section.get("link", ConfigAdapter.STRING, "https://github.com/$githubUsername")
        return ContributorConfig(displayName, description, minecraftUUID, githubUsername, link)
    }
}