package io.github.pylonmc.rebar.content.cargo

import io.github.pylonmc.rebar.block.BlockStorage
import io.github.pylonmc.rebar.block.RebarBlock
import io.github.pylonmc.rebar.block.base.*
import io.github.pylonmc.rebar.block.base.RebarEntityHolderBlock.Companion.holders
import io.github.pylonmc.rebar.block.context.BlockBreakContext
import io.github.pylonmc.rebar.block.context.BlockCreateContext
import io.github.pylonmc.rebar.datatypes.RebarSerializers
import io.github.pylonmc.rebar.entity.EntityStorage
import io.github.pylonmc.rebar.entity.display.ItemDisplayBuilder
import io.github.pylonmc.rebar.entity.display.transform.LineBuilder
import io.github.pylonmc.rebar.entity.display.transform.TransformBuilder
import io.github.pylonmc.rebar.event.RebarCargoConnectEvent
import io.github.pylonmc.rebar.event.RebarCargoDisconnectEvent
import io.github.pylonmc.rebar.item.builder.ItemStackBuilder
import io.github.pylonmc.rebar.util.IMMEDIATE_FACES
import io.github.pylonmc.rebar.util.position.BlockPosition
import io.github.pylonmc.rebar.util.position.position
import io.github.pylonmc.rebar.util.rebarKey
import io.github.pylonmc.rebar.util.scheduleRemove
import io.github.pylonmc.rebar.util.setNullable
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.BlockFace
import org.bukkit.entity.ItemDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityRemoveEvent
import org.bukkit.persistence.PersistentDataContainer

class CargoDuct : RebarBlock, RebarBreakHandler, RebarEntityHolderBlock, RebarEntityGroupCulledBlock, RebarFacadeBlock {
    override val facadeDefaultBlockType = Material.STRUCTURE_VOID

    var connectedFaces = mutableListOf<BlockFace>()
    val faceGroups = mutableMapOf<BlockFace, RebarEntityGroupCulledBlock.EntityCullingGroup>()
    override val cullingGroups
        get() = faceGroups.values
    override var disableBlockTextureEntity = true

    @Suppress("unused")
    constructor(block: Block, context: BlockCreateContext) : super(block) {
        updateConnectedFaces()
    }

    @Suppress("unused")
    constructor(block: Block, pdc: PersistentDataContainer) : super(block) {
        connectedFaces = pdc.get(connectedFacesKey, connectedFacesType)!!.toMutableList()
    }

    override fun postLoad() {
        for (face in connectedFaces) {
            val displayId = getHeldEntityUuid(ductDisplayName(face)) ?: continue
            EntityStorage.whenEntityLoads(displayId) { display: ItemDisplay ->
                if (faceGroups.containsKey(face)) {
                    return@whenEntityLoads
                }

                val cullingGroup = RebarEntityGroupCulledBlock.EntityCullingGroup(face.name)
                cullingGroup.entityIds.add(display.uniqueId)

                val blockPositions = display.persistentDataContainer.get(blocksKey, blocksType) ?: return@whenEntityLoads
                for (blockPos in blockPositions) {
                    val block = BlockStorage.get(blockPos) as? CargoDuct ?: continue
                    block.faceGroups[face] = cullingGroup
                    cullingGroup.blocks.add(block)
                }
            }
        }
    }

    override fun write(pdc: PersistentDataContainer) {
        pdc.setNullable(connectedFacesKey, connectedFacesType, connectedFaces)
    }

    override fun postBreak(context: BlockBreakContext) {
        for (face in connectedFaces) {
            val connectedBlock = connectedBlock(face)
            when (connectedBlock) {
                is CargoDuct -> {
                    connectedBlock.connectedFaces.remove(face.oppositeFace)
                    connectedBlock.updateConnectedFaces()
                    RebarCargoDisconnectEvent(this, connectedBlock).callEvent()
                }
                is RebarCargoBlock -> {
                    RebarCargoDisconnectEvent(this, connectedBlock).callEvent()
                }
            }
        }
    }

    fun updateConnectedFaces() {
        if (connectedFaces.size == 2) {
            return
        }

        val adjacentCargoBlocks = mutableMapOf<BlockFace, RebarBlock>()
        for (face in IMMEDIATE_FACES) {
            val adjacentBlock = BlockStorage.get(block.getRelative(face))
            if (face !in connectedFaces && (adjacentBlock is CargoDuct || adjacentBlock is RebarCargoBlock)) {
                adjacentCargoBlocks.put(face, adjacentBlock)
            }
        }

        // 1: Prioritise RebarCargoBlocks
        for ((face, block) in adjacentCargoBlocks) {
            if (connectedFaces.size != 2 && block is RebarCargoBlock && block.cargoLogisticGroups.containsKey(face.oppositeFace)) {
                if (RebarCargoConnectEvent(this, block).callEvent()) {
                    connectedFaces.add(face)
                }
            }
        }

        // 2: Prioritise RebarDucts which already have a connection
        for ((face, block) in adjacentCargoBlocks) {
            if (connectedFaces.size != 2 && block is CargoDuct && block.connectedFaces.size == 1) {
                if (RebarCargoConnectEvent(this, block).callEvent()) {
                    connectedFaces.add(face)
                    block.connectedFaces.add(face.oppositeFace)
                }
            }
        }

        // 3: Prioritise RebarDucts without connections
        for ((face, block) in adjacentCargoBlocks) {
            if (connectedFaces.size != 2 && block is CargoDuct && block.connectedFaces.isEmpty()) {
                if (RebarCargoConnectEvent(this, block).callEvent()) {
                    connectedFaces.add(face)
                    block.connectedFaces.add(face.oppositeFace)
                }
            }
        }

        updateDisplays()
    }

    fun updateDisplays() {
        // Delete any existing, outdated displays (either a single 'not connected' cube display or a
        // display that continues the same direction as any of the connected faces)
        for (face in connectedFaces) {
            (connectedBlock(face) as? CargoDuct)?.let {
                it.getHeldEntity(ductDisplayName(face))?.scheduleRemove()
                it.getHeldEntity(NOT_CONNECTED_DUCT_DISPLAY_NAME)?.scheduleRemove()
                it.faceGroups.remove(face)
                it.faceGroups.remove(BlockFace.SELF)
            }
        }
        for (entity in heldEntities.keys.toList()) { // clone to prevent concurrent modification exception
            getHeldEntity(entity)?.scheduleRemove()
            heldEntities.remove(entity)
        }
        faceGroups.clear()

        // For performance reasons, if we can use one display entity instead of
        // several, we always should. We do this by deleting any existing entities
        // on the same axis and then spawning a new display entity for the next
        // duct, and a new display entity for the previous duct. These two entities
        // span the entire line from this duct to the end of the next and previous
        // lines

        // Case 1: Duct has no connected blocks
        if (connectedFaces.isEmpty()) {
            // Spawn a cube display
            createNotConnectedDuctDisplay(block.location.toCenterLocation())
            // We are the only one using this display, so a singular group for ourselves
            faceGroups[BlockFace.SELF] = RebarEntityGroupCulledBlock.EntityCullingGroup("SELF").also {
                it.blocks.add(this)
                it.entityIds.add(getHeldEntityUuidOrThrow(NOT_CONNECTED_DUCT_DISPLAY_NAME))
            }
        }

        // Case 2: Duct has two connected blocks on opposite sides, forming a line
        else if (connectedFaces.size == 2 && connectedFaces[0] == connectedFaces[1].oppositeFace) {
            // Spawn a display spanning the entire line
            val endpoint0 = findEndOfLine(connectedFaces[0])
            val endpoint1 = findEndOfLine(connectedFaces[1])
            createDuctDisplay(endpoint0, endpoint1, connectedFaces[0].oppositeFace)
        }

        // Case 3: Duct has either one or two connected blocks, and if two blocks are
        // connected, they do not form a line across this block (this is handled in
        // case 2)
        else {
            // Spawn a display to each of the two connected blocks
            createDuctDisplay(findEndOfLine(connectedFaces[0]), this.block, connectedFaces[0].oppositeFace)
            if (connectedFaces.size == 2) {
                createDuctDisplay(findEndOfLine(connectedFaces[1]), this.block, connectedFaces[1].oppositeFace)
            }
        }
    }

    /**
     * Recursively traverses the next face only if it is the provided face.
     *
     * This has the effect of traversing to the end of the line whose direction
     * is provided by the current block and the next block.
     */
    private fun findEndOfLine(face: BlockFace): Block {
        var currentDuct = this
        while (true) {
            val nextBlock = currentDuct.connectedBlock(face)
            if (nextBlock is CargoDuct) {
                currentDuct = nextBlock
                continue
            }

            if (nextBlock is RebarCargoBlock) {
                return nextBlock.block
            }

            if (nextBlock == null) {
                return currentDuct.block
            }
        }
    }

    private fun connectedBlock(face: BlockFace): RebarBlock? {
        if (face !in connectedFaces) {
            return null
        }
        return BlockStorage.get(block.getRelative(face))
    }

    private fun createDuctDisplay(from: Block, to: Block, fromToFace: BlockFace) {
        // Need to do some detective work to find out the correct thickness. The rule
        // is that the thickness of the display connecting [from] and [to] must be
        // different to the thickness of the existing display on [from] and [to] (if
        // they exist). Note there can only be one other existing display considering
        // we're in the process of making a new connection to the duct here.
        val availableThicknesses = thicknesses.toMutableList()
        val fromDuct = if (from == this.block) {
            this // Special case: This block is not in BlockStorage yet
        } else {
            BlockStorage.getAs<CargoDuct>(from)
        }
        fromDuct?.heldEntities?.keys?.forEach { name ->
            fromDuct.getHeldEntity(name)?.persistentDataContainer?.get(thicknessKey, thicknessType).let { thickness ->
                availableThicknesses.remove(thickness)
            }
        }
        val toDuct = if (this.block == to) {
            this // Special case: This block is not in BlockStorage yet
        } else {
            BlockStorage.getAs<CargoDuct>(to)
        }
        toDuct?.heldEntities?.keys?.forEach { name ->
            toDuct.getHeldEntity(name)?.persistentDataContainer?.get(thicknessKey, thicknessType)?.let { thickness ->
                availableThicknesses.remove(thickness)
            }
        }
        val thickness = availableThicknesses[0]

        // Now to actually build the display
        // It's possible one of the displays will be a RebarCargoBlock, in which case it could be a solid block
        // This would occlude the display entity and cause it to render with brightness 0
        // To avoid this, we'll just spawn the entity at this duct, since we know it's a duct (and therefore a
        // structure void, which will not occlude the display entity)
        val spawnLocation = this.block.location.toCenterLocation()
        val display = ItemDisplayBuilder()
            .transformation(LineBuilder()
                .from(from.location.toCenterLocation().subtract(spawnLocation).toVector().toVector3d())
                .to(to.location.toCenterLocation().subtract(spawnLocation).toVector().toVector3d())
                .thickness(thickness)
                .extraLength(thickness)
                .build()
            )
            .itemStack(ItemStackBuilder.of(Material.GRAY_CONCRETE)
                .addCustomModelDataString("$key:line"))
            .build(spawnLocation)
        display.persistentDataContainer.set(thicknessKey, thicknessType, thickness)

        // Add the display to every CargoDuct on the line
        val associatedBlocks = mutableListOf<BlockPosition>()
        val cullingGroup = RebarEntityGroupCulledBlock.EntityCullingGroup(fromToFace.name)
        cullingGroup.entityIds.add(display.uniqueId)
        // (start)
        BlockStorage.getAs<CargoDuct>(from)?.let {
            it.addEntity(ductDisplayName(fromToFace), display)
            it.faceGroups[fromToFace] = cullingGroup
            cullingGroup.blocks.add(it)
        }
        if (from == this.block) {
            // Special case: This block is not in BlockStorage yet so above code will not work
            this.addEntity(ductDisplayName(fromToFace), display)
            this.faceGroups[fromToFace] = cullingGroup
            cullingGroup.blocks.add(this)
        }
        associatedBlocks.add(from.position)
        // (middle)
        var current = from
        while (true) {
            current = current.getRelative(fromToFace)
            if (current == to) {
                break
            }
            BlockStorage.getAs<CargoDuct>(current)?.let {
                it.addEntity(ductDisplayName(fromToFace), display)
                it.addEntity(ductDisplayName(fromToFace.oppositeFace), display)
                it.faceGroups[fromToFace] = cullingGroup
                it.faceGroups[fromToFace.oppositeFace] = cullingGroup
                cullingGroup.blocks.add(it)
            }
            if (current == this.block) {
                // Special case: This block is not in BlockStorage yet so above code will not work
                this.addEntity(ductDisplayName(fromToFace), display)
                this.addEntity(ductDisplayName(fromToFace.oppositeFace), display)
                this.faceGroups[fromToFace] = cullingGroup
                cullingGroup.blocks.add(this)
            }
            associatedBlocks.add(current.position)
        }
        // (end)
        BlockStorage.getAs<CargoDuct>(to)?.let {
            it.addEntity(ductDisplayName(fromToFace.oppositeFace), display)
            it.faceGroups[fromToFace.oppositeFace] = cullingGroup
            cullingGroup.blocks.add(it)
        }
        if (to == this.block) {
            // Special case: This block is not in BlockStorage yet so above code will not work
            this.addEntity(ductDisplayName(fromToFace.oppositeFace), display)
            this.faceGroups[fromToFace.oppositeFace] = cullingGroup
            cullingGroup.blocks.add(this)
        }
        associatedBlocks.add(to.position)

        // Also add the blocks to the display's PDC (see onEntityRemove in companion for explanation)
        display.persistentDataContainer.set(blocksKey, blocksType, associatedBlocks)
    }

    private fun createNotConnectedDuctDisplay(center: Location) {
        val display = ItemDisplayBuilder()
            .transformation(TransformBuilder()
                .scale(thicknesses[0])
            )
            .itemStack(ItemStackBuilder.of(Material.GRAY_CONCRETE)
                .addCustomModelDataString("$key:single"))
            .build(center)

        addEntity(NOT_CONNECTED_DUCT_DISPLAY_NAME, display)
    }

    companion object : Listener {
        const val NOT_CONNECTED_DUCT_DISPLAY_NAME = "duct-item-display:not-connected"

        // Q: 'Why the hell are there 3 different thicknesses?'
        // A: To prevent Z-fighting. It's expected that blocks trying to create a seamless connection
        // to cargo ducts will use thickness 0.35 hence why it isn't used here
        val thicknesses = mutableListOf(0.3495F, 0.3490F, 0.3485F)

        val thicknessKey = rebarKey("thickness")
        val thicknessType = RebarSerializers.FLOAT

        val connectedFacesKey = rebarKey("connected_faces")
        val connectedFacesType = RebarSerializers.LIST.listTypeFrom(RebarSerializers.BLOCK_FACE)

        val blocksKey = rebarKey("blocks")
        val blocksType = RebarSerializers.LIST.listTypeFrom(RebarSerializers.BLOCK_POSITION)

        fun ductDisplayName(face: BlockFace) = "duct-item-display:${face.name}"

        /**
         * Cargo duct displays are 'owned' by multiple blocks, but the entity removal
         * handling in [RebarEntityHolderBlock] assumes a single block holds the
         * entity. We therefore have to roll our own entity removal logic that will
         * store *all* the blocks that own the entity in the entity's PDC, and remove
         * the entity from all of those blocks when it is removed.
         */
        @EventHandler
        private fun onEntityRemove(event: EntityRemoveEvent) {
            if (event.cause == EntityRemoveEvent.Cause.UNLOAD || event.cause == EntityRemoveEvent.Cause.PLAYER_QUIT) return
            val blockPositions = event.entity.persistentDataContainer.get(blocksKey, blocksType) ?: return
            for (blockPos in blockPositions) {
                val block = BlockStorage.get(blockPos) as? RebarEntityHolderBlock ?: continue
                holders[block]?.entries?.removeIf { it.value == event.entity.uniqueId }
            }
        }
    }
}
