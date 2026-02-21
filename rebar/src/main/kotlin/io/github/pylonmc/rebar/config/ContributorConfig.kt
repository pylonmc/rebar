package io.github.pylonmc.rebar.config

import org.bukkit.Bukkit
import java.util.UUID

data class ContributorConfig(
    val displayName: String,
    val description: String?,
    val minecraftUUID: UUID? = Bukkit.getOfflinePlayer(displayName).uniqueId,
    val githubUsername: String?,
    val link: String?
) {
    constructor(displayName: String) : this(
        displayName,
        description = null,
        githubUsername = displayName,
        link = "https://github.com/$displayName"
    )
}