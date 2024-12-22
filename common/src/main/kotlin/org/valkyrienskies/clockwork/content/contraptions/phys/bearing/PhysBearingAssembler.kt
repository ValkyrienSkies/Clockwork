package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d
import org.joml.Vector3i
import org.joml.Vector3ic
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.getAttachment
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.assembly.AssemblyUtil
import org.valkyrienskies.mod.common.assembly.ShipAssembler.isValidShipBlock
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.SplittingDisablerAttachment
import org.valkyrienskies.mod.common.util.toBlockPos

data class Return(val newShip: ServerShip, val previousCenter: Vector3ic, val newCenter: Vector3d, val newCenterBP: BlockPos)

//TODO this is dumb but i'm not going to use ShipAssembler cuz it sucks
object PhysBearingAssembler {
    @JvmStatic
    fun moveBlocksFromTo(level: ServerLevel, blocks: DenseBlockPosSet, removeOriginal: Boolean, originCenter: BlockPos, toCenter: BlockPos): Boolean {
        val blocks = blocks.filter { isValidShipBlock(level.getBlockState(it.toBlockPos())) }.map {it.toBlockPos()}
        for (itPos in blocks) {
            val relative: BlockPos = itPos.subtract(BlockPos(originCenter.x, originCenter.y, originCenter.z))
            val shipPos: BlockPos = toCenter.offset(relative)
            if (!level.getBlockState(shipPos).isAir) {return false}
        }

        for (itPos in blocks) {
            val relative: BlockPos = itPos.subtract(BlockPos(originCenter.x, originCenter.y, originCenter.z))
            val shipPos: BlockPos = toCenter.offset(relative)
            AssemblyUtil.copyBlock(level, itPos, shipPos)
        }

        if (removeOriginal) {
            for (itPos in blocks) {
                AssemblyUtil.removeBlock(level, itPos)
            }
        }

        for (itPos in blocks) {
            val relative: BlockPos = itPos.subtract(BlockPos(originCenter.x, originCenter.y, originCenter.z))
            val shipPos: BlockPos = toCenter.offset(relative)
            level.chunkSource.blockChanged(shipPos)
        }

        return true
    }

    @JvmStatic
    fun assembleToShip(level: ServerLevel, blocks: DenseBlockPosSet, removeOriginal: Boolean, scale: Double = 1.0, shouldDisableSplitting: Boolean = false): Return {
        if (blocks.isEmpty()) { throw IllegalArgumentException("No blocks to assemble.") }

        val existingShip = level.getShipObjectManagingPos(blocks.find { !level.getBlockState(it.toBlockPos()).isAir }?.toBlockPos() ?: throw IllegalArgumentException())

        var existingShipCouldSplit = true
        var structureCornerMin: BlockPos? = null
        var structureCornerMax: BlockPos? = null
        var hasSolids = false

        // Calculate bounds of the area containing all blocks adn check for solids and invalid blocks
        for (itPos in blocks) {
            val itPos = itPos.toBlockPos()
            if (!isValidShipBlock(level.getBlockState(itPos))) {continue}
            if (structureCornerMin == null || structureCornerMax == null) {
                structureCornerMin = itPos
                structureCornerMax = itPos
            } else {
                structureCornerMin = AssemblyUtil.getMinCorner(structureCornerMin, itPos)
                structureCornerMax = AssemblyUtil.getMaxCorner(structureCornerMax, itPos)
            }
            hasSolids = true
        }
        if (!hasSolids) throw IllegalArgumentException("No solid blocks found in the structure")
        val previousCenterBP: Vector3ic = AssemblyUtil.getMiddle(structureCornerMin!!, structureCornerMax!!)
        // Create new contraption at center of bounds
        val contraptionWorldPos: Vector3i = if (existingShip != null) {
            val doubleVer = existingShip.shipToWorld.transformPosition(Vector3d(previousCenterBP)).floor()
            Vector3i(doubleVer.x.toInt(), doubleVer.y.toInt(), doubleVer.z.toInt())
        } else {
            Vector3i(previousCenterBP)
        }

        val newShip: ServerShip = level.server.shipObjectWorld.createNewShipAtBlock(contraptionWorldPos, false, scale, level.dimensionId)

        if (shouldDisableSplitting) {
            existingShip?.let {
                existingShipCouldSplit = level.shipObjectWorld.loadedShips.getById(it.id)?.getAttachment<SplittingDisablerAttachment>()?.canSplit() ?: false
                if (existingShipCouldSplit) {level.shipObjectWorld.loadedShips.getById(it.id)?.getAttachment<SplittingDisablerAttachment>()?.disableSplitting()}
            }
            level.shipObjectWorld.loadedShips.getById(newShip.id)?.getAttachment<SplittingDisablerAttachment>()?.disableSplitting()
        }

        val newCenterPos = Vector3i(
            newShip.chunkClaim.xMiddle*16-7,
            128,
            newShip.chunkClaim.zMiddle*16-7,
        )
        val newCenter = Vector3d(newCenterPos)
        val newCenterBP = newCenterPos.toBlockPos()

        for (itPos in blocks) {
            val itPos = itPos.toBlockPos()
            if (isValidShipBlock(level.getBlockState(itPos))) {
                val relative: BlockPos = itPos.subtract(BlockPos(previousCenterBP.x(),previousCenterBP.y(),previousCenterBP.z()))
                val shipPos: BlockPos = newCenterBP.offset(relative)
                AssemblyUtil.copyBlock(level, itPos, shipPos)
            }
        }

        // Remove original blocks
        if (removeOriginal) {
            for (itPos in blocks) {
                val itPos = itPos.toBlockPos()
                if (isValidShipBlock(level.getBlockState(itPos))) {
                    AssemblyUtil.removeBlock(level, itPos)
                }
            }
        }

        // Trigger updates on both contraptions
        for (itPos in blocks) {
            val itPos = itPos.toBlockPos()
            val relative: BlockPos = itPos.subtract(BlockPos(previousCenterBP.x(),previousCenterBP.y(),previousCenterBP.z()))
            val shipPos: BlockPos = newCenterBP.offset(relative)
            AssemblyUtil.updateBlock(level, itPos, shipPos, level.getBlockState(shipPos))
        }

        val shipCenterPos = newShip.inertiaData.centerOfMassInShip.add(0.5, 0.5, 0.5, Vector3d())
        // This is giga sus, but whatever
        val shipPos = Vector3d(previousCenterBP).add(0.5, 0.5, 0.5)
        if (existingShip != null) {
            level.server.shipObjectWorld.teleportShip(newShip, ShipTeleportDataImpl(existingShip.shipToWorld.transformPosition(shipPos, Vector3d()), existingShip.transform.shipToWorldRotation, existingShip.velocity, existingShip.omega, existingShip.chunkClaimDimension, newScale = existingShip.transform.shipToWorldScaling.x(), newPosInShip = shipCenterPos))
        } else {
            level.server.shipObjectWorld.teleportShip(newShip, ShipTeleportDataImpl(newPos = shipPos, newPosInShip = shipCenterPos))
        }
        if (shouldDisableSplitting) {
            if (existingShipCouldSplit) {
                existingShip?.let { level.shipObjectWorld.loadedShips.getById(it.id)?.getAttachment<SplittingDisablerAttachment>()?.enableSplitting() }
            }
            level.shipObjectWorld.loadedShips.getById(newShip.id)?.getAttachment<SplittingDisablerAttachment>()?.enableSplitting()
        }

        return Return(newShip, previousCenterBP, newCenter, newCenterBP)
    }
}