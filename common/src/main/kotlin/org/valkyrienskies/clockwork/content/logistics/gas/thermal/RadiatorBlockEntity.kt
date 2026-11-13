package org.valkyrienskies.clockwork.content.logistics.gas.thermal

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.kelvin.util.INodeBlock
import org.valkyrienskies.kelvin.util.INodeBlockEntity
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.toWorldCoordinates

class RadiatorBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    KNodeBlockEntity(type, pos, state), IConnectable {

    private val tickPhase = Math.floorMod(pos.hashCode(), TRANSFER_INTERVAL)

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    override fun tick() {
        super.tick()
        val serverLevel = level as? ServerLevel ?: return
        if (dataToLoad != null || (serverLevel.gameTime + tickPhase) % TRANSFER_INTERVAL != 0L) return
        transferHeat(serverLevel)
    }

    private fun transferHeat(level: ServerLevel) {
        val coefficient = ClockworkConfig.SERVER.radiatorHeatTransferCoefficient
        if (coefficient <= 0.0) return
        val kelvin = ClockworkMod.getKelvin(level)
        val nodePos = getDuctNodePosition()
        if (kelvin.getNodeAt(nodePos) == null || nodePos in kelvin.unloadedNodes) return
        val worldPos = level.toWorldCoordinates(blockPos)
        val ambient = level.shipObjectWorld.aerodynamicUtils.getAirTemperatureForY(worldPos.y, level.dimensionId)
        val cooling = ThermalTransfer.cooling(
            kelvin.getHeatEnergy(nodePos), kelvin.getNodeHeatCapacity(nodePos), ambient, coefficient, TRANSFER_SECONDS
        )
        if (cooling > 0.0) kelvin.modHeatEnergy(nodePos, -cooling)
    }

    override fun updateConnection(level: Level, blockPos: BlockPos, direction: Direction) {
        if (level.isClientSide) return
        val neighbor = blockPos.relative(direction)
        if (!level.hasChunkAt(neighbor)) return
        val selfBlock = level.getBlockState(blockPos).block as? INodeBlock ?: return
        val otherBlock = level.getBlockState(neighbor).block as? INodeBlock
        if (otherBlock == null || !selfBlock.canConnectTo(blockPos, neighbor, direction, level) ||
            !otherBlock.canConnectTo(neighbor, blockPos, direction.opposite, level)) {
            val otherPos = (level.getBlockEntity(neighbor) as? INodeBlockEntity)?.getDuctNodePosition()
                ?: neighbor.toDuctNodePos(level.dimension().location())
            ClockworkMod.getKelvin(level).removeEdge(getDuctNodePosition(), otherPos)
            return
        }
        super<IConnectable>.updateConnection(level, blockPos, direction)
    }

    companion object {
        const val TRANSFER_INTERVAL = 10
        const val TRANSFER_SECONDS = TRANSFER_INTERVAL / 20.0
    }
}
