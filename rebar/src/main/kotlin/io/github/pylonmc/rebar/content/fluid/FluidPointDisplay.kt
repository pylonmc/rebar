package io.github.pylonmc.rebar.content.fluid

import io.github.pylonmc.rebar.entity.interfaces.InteractableItemDisplayRebarEntity
import io.github.pylonmc.rebar.fluid.VirtualFluidPoint
import java.util.*

interface FluidPointDisplay : InteractableItemDisplayRebarEntity {
    val uuid: UUID
    val point: VirtualFluidPoint
    val connectedPipeDisplays: Set<UUID>

    fun connectPipeDisplay(uuid: UUID)
    fun disconnectPipeDisplay(uuid: UUID)
}