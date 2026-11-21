package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.interfaces.WireRebarItem
import io.github.pylonmc.rebar.util.Either
import io.github.pylonmc.rebar.util.delayTicks
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerChangedWorldEvent
import org.bukkit.event.player.PlayerItemHeldEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.*

object WireConnectionService : Listener {

    private val connecting = mutableMapOf<UUID, WireEntity>()
    private val jobs = mutableMapOf<UUID, Job>()

    @JvmStatic
    fun startConnectingWire(player: Player, wire: WireEntity) {
        connecting[player.uniqueId] = wire
        jobs[player.uniqueId] = Rebar.scope.launch {
            while (true) {
                when (val connection = wire.canConnect()) {
                    is Either.Left -> {
                        val needed = connection.value
                        val mainHandItem = player.inventory.itemInMainHand
                        val total = if (RebarItem.isRebarItem<WireRebarItem>(mainHandItem)) mainHandItem.amount else 0
                        val color =
                            if (player.gameMode != GameMode.CREATIVE && needed > total) NamedTextColor.RED else NamedTextColor.GREEN
                        player.sendActionBar(
                            Component.translatable(
                                "rebar.message.wiring.wiring",
                                RebarArgument.of("wires", needed),
                                RebarArgument.of("total", total)
                            ).color(color)
                        )
                    }

                    is Either.Right -> player.sendActionBar(connection.value.errorMessage)
                }
                delayTicks(RebarConfig.WIRING_TICK_INTERVAL.toLong())
            }
        }
        player.sendMessage(Component.translatable("rebar.message.wiring.instructions"))
    }

    @JvmStatic
    fun getWirePlayerIsConnecting(player: Player) = connecting[player.uniqueId]

    @JvmStatic
    @JvmOverloads
    fun stopConnectingWire(player: Player, delete: Boolean = true) {
        val wire = connecting.remove(player.uniqueId)
        if (delete) wire?.remove()
        jobs.remove(player.uniqueId)?.cancel()
        player.sendActionBar(Component.empty())
    }

    @EventHandler
    private fun onPlayerMove(event: PlayerMoveEvent) {
        if (!event.hasChangedPosition()) return
        getWirePlayerIsConnecting(event.player)?.update()
    }

    @EventHandler
    private fun onPlayerLeave(event: PlayerQuitEvent) {
        stopConnectingWire(event.player)
    }

    @EventHandler
    private fun onPlayerDeath(event: PlayerDeathEvent) {
        stopConnectingWire(event.player)
    }

    @EventHandler
    private fun onPlayerChangeWorld(event: PlayerChangedWorldEvent) {
        stopConnectingWire(event.player)
    }

    @EventHandler
    private fun onPlayerSlotChange(event: PlayerInventorySlotChangeEvent) {
        val player = event.player
        val wire = getWirePlayerIsConnecting(player) ?: return
        val wireItem = RebarItem.fromStack<WireRebarItem>(event.newItemStack)
        if (wireItem == null) {
            stopConnectingWire(player)
        } else {
            wire.setWireItem(wireItem)
        }
    }

    @EventHandler
    private fun onPlayerScroll(event: PlayerItemHeldEvent) {
        val player = event.player
        val wire = getWirePlayerIsConnecting(player) ?: return
        val wireItem = RebarItem.fromStack<WireRebarItem>(player.inventory.getItem(event.newSlot))
        if (wireItem == null) {
            stopConnectingWire(player)
        } else {
            wire.setWireItem(wireItem)
        }
    }

    @EventHandler
    private fun onBlockPlace(@Suppress("unused") unused: BlockPlaceEvent) {
        Rebar.scope.launch {
            delayTicks(1)
            for (wire in WireEntity.loadedWires) {
                if (wire.isObstructed && !wire.isHeldByPlayer) {
                    val loc = wire.port.location
                    loc.world.dropItemNaturally(loc, wire.wire.createNewItemStack(wire.wireCount))
                    wire.remove()
                }
            }
        }
    }
}