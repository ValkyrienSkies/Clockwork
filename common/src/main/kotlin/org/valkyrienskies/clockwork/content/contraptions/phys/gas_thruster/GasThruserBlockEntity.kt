package org.valkyrienskies.clockwork.content.contraptions.phys.gas_thruster

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.forces.GasThrusterController
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD

class GasThruserBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos, state), IHeatableBlockEntity {
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun getDuctNodePosition(): DuctNodePos {
        return this.blockPos.toJOMLD()
    }

    val area = 0.11045

    override fun tick() {
        super.tick()

        if (level!!.isClientSide) return

        val ductnodepos = getDuctNodePosition()
        val node = ClockworkMod.getKelvin().getNodeAt(ductnodepos) ?: return
        val gasMasses = node.network.getGasMassAt(ductnodepos)
        val total = gasMasses.values.sum()

        if (total == 0.0) return

        val stpPressure = 100000 // TODO: Different height pressures + Different dimension pressures
        val gasPressure = node.network.getPressureAt(ductnodepos)

        if (gasPressure<stpPressure) return

        var velocity = 0.0
        var flowrate = 0.0

        for (edge in node.nodeEdges) {
            flowrate += edge.currentFlowRate
        }

        for (gas in gasMasses) {
            velocity += flowrate/(gas.key.density*area)

            node.network.modGasMass(ductnodepos, gas.key, -gas.value)
        }

        val thrust = flowrate * velocity + (gasPressure-stpPressure)*area
        val force = blockState.getValue(DirectionalBlock.FACING).normal.toJOMLD().mul(thrust)
        println(force)
        val serverLevel = level as ServerLevel? ?: return
        val ship = serverLevel.getShipObjectManagingPos(blockPos) ?: return
        val controller = GasThrusterController.getOrCreate(ship as ServerShip) ?: return
        controller.updateThruster(blockPos, force)



    }
}