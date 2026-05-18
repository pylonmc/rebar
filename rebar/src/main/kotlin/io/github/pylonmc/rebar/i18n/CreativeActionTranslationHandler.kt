package io.github.pylonmc.rebar.i18n

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.nms.NmsAccessor
import io.github.pylonmc.rebar.resourcepack.armor.ArmorTextureEngine
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCreativeEvent
import org.bukkit.event.inventory.InventoryType.SlotType

internal object CreativeActionTranslationHandler : Listener {
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCreativeAction(e: InventoryCreativeEvent) {
        PlayerTranslationHandler.resetItem(e.cursor)
        ArmorTextureEngine.resetItem(e.cursor)
        e.result = Event.Result.ALLOW

        if (e.slotType == SlotType.OUTSIDE) return
        val player = e.whoClicked as? Player ?: return
        Bukkit.getScheduler().runTask(Rebar) { _ ->
            if (!e.isCancelled) {
                NmsAccessor.instance.resendSlot(player, e.rawSlot)
            }
        }
    }
}