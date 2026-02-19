package io.github.pylonmc.rebar.resourcepack.block

import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.util.pdc
import io.github.pylonmc.rebar.util.rebarKey
import org.bukkit.entity.Player

object BlockTextureEngine {
    val customBlockTexturesKey = rebarKey("custom_block_textures")

    @JvmStatic
    var Player.hasCustomBlockTextures: Boolean
        get() = this.pdc.getOrDefault(customBlockTexturesKey, RebarSerializers.BOOLEAN, RebarConfig.BlockTextureConfig.DEFAULT) || RebarConfig.BlockTextureConfig.FORCED
        set(value) = this.pdc.set(customBlockTexturesKey, RebarSerializers.BOOLEAN, value || RebarConfig.BlockTextureConfig.FORCED)

}
