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
import org.valkyrienskies.clockwork.util.AerodynamicUtils
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class GasThrusterBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos, state), IHeatableBlockEntity {
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun getDuctNodePosition(): DuctNodePos {
        if (level != null && !level!!.isClientSide()) {
            return blockPos.toDuctNodePos(level!!.dimension().location())
        }
        return blockPos.toDuctNodePos()
    }

    override fun tick() {
        super.tick()

        if (level!!.isClientSide) return

        val ductnodepos = getDuctNodePosition()
        val kelvin = ClockworkMod.getKelvin()
        val node = kelvin.getNodeAt(ductnodepos) ?: return
        val gasMasses = kelvin.getGasMassAt(ductnodepos)
        val total = gasMasses.values.sum()

        if (total == 0.0) return

        val airPressure = AerodynamicUtils.getAirPressureForY(blockPos.y.toDouble(), 563.0)
        val gasPressure = kelvin.getPressureAt(ductnodepos)
        val temp = kelvin.getTemperatureAt(ductnodepos)
        val avgSpecificHeat = AerodynamicUtils.specificHeatAverage(kelvin.getGasMassAt(ductnodepos))


        if (gasPressure<airPressure) return

        var velocity = 0.0
        var flowrate = 0.0

        for (edge in node.nodeEdges) {
            flowrate += edge.currentFlowRate
        }

        val maxMFR = (AerodynamicUtils.DUCT_AREA * gasPressure / sqrt(temp)) * sqrt(avgSpecificHeat/AerodynamicUtils.UNIVERSAL_GAS_CONSTANT) * ((avgSpecificHeat+1)/2).pow(-(avgSpecificHeat+1)/(2*(avgSpecificHeat-1)))
        flowrate = min(maxMFR, flowrate)

        for (gas in gasMasses) {
            velocity += flowrate/(gas.key.density*AerodynamicUtils.DUCT_AREA)

            kelvin.modGasMass(ductnodepos, gas.key, -max(flowrate*0.05, gas.value))
        }

        val thrust = flowrate * velocity + (gasPressure-airPressure)
        val force = blockState.getValue(DirectionalBlock.FACING).normal.toJOMLD().mul(thrust)
        println(force)
        val serverLevel = level as ServerLevel? ?: return
        val ship = serverLevel.getShipObjectManagingPos(blockPos) ?: return
        val controller = GasThrusterController.getOrCreate(ship as ServerShip) ?: return
        controller.updateThruster(blockPos, force)



    }


}