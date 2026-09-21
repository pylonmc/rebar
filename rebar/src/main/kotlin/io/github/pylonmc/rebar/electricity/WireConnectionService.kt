package io.github.pylonmc.rebar.electricity

import io.github.pylonmc.rebar.Rebar
import io.github.pylonmc.rebar.config.RebarConfig
import io.github.pylonmc.rebar.electricity.nodes.ElectricPortEntity
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.event.RebarPlayerInteractWireEvent
import io.github.pylonmc.rebar.i18n.RebarArgument
import io.github.pylonmc.rebar.item.RebarItem
import io.github.pylonmc.rebar.item.interfaces.WireRebarItem
import io.github.pylonmc.rebar.util.*
import io.papermc.paper.event.player.PlayerInventorySlotChangeEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.attribute.Attribute
import org.bukkit.entity.Player
import org.bukkit.event.Event
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.*
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.util.Vector
import org.joml.Vector3f
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

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onPlayerInteract(event: PlayerInteractEvent) {
        if (event.hand != EquipmentSlot.HAND) return

        // prevent wires from blocking ports
        val usedPort = run {
            val target = event.player.getTargetEntityByLocation(ElectricPortEntity.SCALE.toFloat()) ?: return@run false
            val port = EntityStorage.getAs<ElectricPortEntity>(target) ?: return@run false
            port.onInteractedWith(event)
            event.setUseInteractedBlock(Event.Result.DENY)
            event.setUseItemInHand(Event.Result.DENY)
            true
        }
        if (usedPort) return

        val player = event.player
        val eyePos = player.eyeLocation.toVector().toVector3f()
        val eyeVec =
            player.eyeLocation.direction.toVector3f() * player.getAttribute(Attribute.ENTITY_INTERACTION_RANGE)!!.value.toFloat()
        val intersections = mutableListOf<Pair<WireEntity, Vector3f>>()
        for (wire in WireEntity.loadedWires) {
            if (wire.isHeldByPlayer) continue
            val wirePos = wire.port.location.toVector().toVector3f()
            val wireOtherEndPos = (wire.otherEnd as Either.Right).value.location.toVector().toVector3f()
            val wireVec = wireOtherEndPos - wirePos
            val intersection = intersectionOfLineAndCylinder(wirePos, wireVec, WireEntity.THICKNESS, eyePos, eyeVec)
            if (intersection != null) {
                intersections.add(wire to intersection)
            }
        }
        val closestIntersection = intersections.minByOrNull { it.second.distanceSquared(eyePos) } ?: return
        event.setUseInteractedBlock(Event.Result.DENY)
        RebarPlayerInteractWireEvent(
            closestIntersection.first,
            event,
            Vector.fromJOML(closestIntersection.second).toLocation(player.world)
        ).callEvent()
    }

    @EventHandler
    private fun onPlayerBreakWire(event: RebarPlayerInteractWireEvent) {
        if (!event.interaction.action.isLeftClick || !RebarItem.isRebarItem<WireRebarItem>(event.player.inventory.itemInMainHand)) return
        event.wire.dropItemsAt(event.interactionPoint)
        event.wire.remove()
    }
}