package io.github.pylonmc.rebar.nms

import com.destroystokyo.paper.event.player.PlayerRecipeBookClickEvent
import io.github.pylonmc.rebar.i18n.PlayerTranslationHandler
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.block.Block
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataContainer
import org.jetbrains.annotations.ApiStatus
import java.util.UUID

/**
 * Internal, not for innocent eyes to see, move along now.
 */
@ApiStatus.Internal
@ApiStatus.NonExtendable
interface NmsAccessor {

    fun damageItem(itemStack: ItemStack, amount: Int, world: World, onBreak: (Material) -> Unit, force: Boolean = false)

    fun damageItem(itemStack: ItemStack, amount: Int, entity: LivingEntity, slot: EquipmentSlot, force: Boolean = false)

    fun registerTranslationHandler(player: Player, handler: PlayerTranslationHandler)

    fun getTranslationHandler(playerId: UUID): PlayerTranslationHandler?

    fun unregisterTranslationHandler(player: Player)

    fun resendInventory(player: Player)

    fun resendRecipeBook(player: Player)

    fun serializePdc(pdc: PersistentDataContainer): Component

    fun getStateProperties(block: Block, custom: Map<String, Pair<String, Int>> = mutableMapOf()): Map<String, String>

    fun handleRecipeBookClick(event: PlayerRecipeBookClickEvent)

    companion object {
        val instance = Class.forName("io.github.pylonmc.rebar.nms.NmsAccessorImpl")
            .getDeclaredField("INSTANCE")
            .get(null) as NmsAccessor
    }
}