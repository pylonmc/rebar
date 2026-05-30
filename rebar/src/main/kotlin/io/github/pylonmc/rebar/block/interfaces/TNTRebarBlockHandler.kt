package io.github.pylonmc.rebar.block.interfaces

import io.github.pylonmc.rebar.block.BlockListener
import io.github.pylonmc.rebar.block.BlockListener.logEventHandleErr
import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.event.api.MultiListener
import io.github.pylonmc.rebar.event.api.annotation.MultiHandlers
import io.github.pylonmc.rebar.event.api.annotation.UniversalHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.TNTPrimeEvent
import org.jetbrains.annotations.ApiStatus

interface TNTRebarBlockHandler {
    fun onTNTPrime(event: TNTPrimeEvent, priority: EventPriority) {}

    @ApiStatus.Internal
    companion object : MultiListener {
        @UniversalHandler
        private fun onTNTPrime(event: TNTPrimeEvent, priority: EventPriority) {
            val rebarBlock = BlockStorage.get(event.block)
            if (rebarBlock is TNTRebarBlockHandler) {
                try {
                    MultiHandlers.handleEvent(rebarBlock, "onTNTPrime", event, priority)
                } catch (e: Exception) {
                    BlockListener.logEventHandleErr(event, e, rebarBlock)
                }
            }
        }
    }
}