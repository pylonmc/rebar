package io.github.pylonmc.rebar.config.adapter

import java.util.UUID

object UUIDConfigAdapter : ConfigAdapter<UUID> {
    private val longListAdapter = ConfigAdapter.LIST.from(ConfigAdapter.LONG)

    override val type: Class<UUID> = UUID::class.java

    override fun convert(value: Any): UUID {
        return if (value is String) {
            UUID.fromString(value)
        } else {
            val longs = longListAdapter.convert(value)
            require(longs.size == 2) { "Expected a list of 2 longs for UUID, got ${longs.size}" }
            UUID(longs[0], longs[1])
        }
    }
}