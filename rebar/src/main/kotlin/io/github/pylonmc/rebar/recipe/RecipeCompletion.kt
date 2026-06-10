package io.github.pylonmc.rebar.recipe

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent
import io.github.pylonmc.rebar.nms.NmsAccessor
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener

object RecipeCompletion : Listener {

    @EventHandler
    private fun onRecipeBookClick(e: PlayerRecipeBookClickEvent) = NmsAccessor.instance.handleRecipeBookClick(e)
}