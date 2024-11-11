package org.valkyrienskies.clockwork.content.physicalities.ballooner

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkAugmentations
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.AerodynamicUtils.calcPressure
import org.valkyrienskies.clockwork.util.AerodynamicUtils.calculateFlow
import org.valkyrienskies.clockwork.util.AerodynamicUtils.densityAverage
import org.valkyrienskies.clockwork.util.AerodynamicUtils.specificHeatAverage
import org.valkyrienskies.clockwork.util.AerodynamicUtils.viscosityAverage
import org.valkyrienskies.clockwork.util.ClockworkUtils.retrieveGasInfoFromPocket
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.abs
import kotlin.math.max

class BalloonerBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?): SmartBlockEntity(type, pos, state), IHeatableBlockEntity {

    var hasPocket = false

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun tick() {
        super.tick()
        if (level == null || level!!.isClientSide) return

        val serverLevel = level!! as ServerLevel

        hasPocket = try {
            serverLevel.shipObjectWorld.getAirComponentSize(blockPos.x, blockPos.y+1, blockPos.z, serverLevel.dimensionId) > 0
        } catch (e: IllegalArgumentException) {
            false
        }

        if (hasPocket) {
            flowIntoPocket()
        }

    }

    fun flowIntoPocket() {
        val serverLevel = level!! as ServerLevel

        val currentNodeTemperature = ClockworkMod.getKelvin().getTemperatureAt(blockPos.toJOMLD())
        val currentNodePressure = ClockworkMod.getKelvin().getPressureAt(blockPos.toJOMLD())
        val currentNodeGasVolumes = ClockworkMod.getKelvin().getGasMassAt(blockPos.toJOMLD())
        val currentNodeTotalMass = currentNodeGasVolumes.values.sum()
        val currentNodeAvgDensity = densityAverage(currentNodeGasVolumes)
        val currentNodeAvgViscosity = viscosityAverage(currentNodeGasVolumes)
        val currentNodeAvgSpecificHeat = specificHeatAverage(currentNodeGasVolumes)


        val pocketRef = blockPos.above()
        val (pocketGasVolumes, pocketTemperature) = retrieveGasInfoFromPocket(pocketRef.toJOML(), serverLevel)
        val pocketVolume = serverLevel.shipObjectWorld.getAirComponentSize(pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId).toDouble()
        val pocketTotalMass = pocketGasVolumes.values.sum()
        val pocketAvgDensity = densityAverage(pocketGasVolumes)
        val pocketAvgViscosity = viscosityAverage(pocketGasVolumes)
        val pocketPressure = calcPressure(pocketTotalMass, pocketVolume, pocketTemperature, pocketAvgDensity)
        val pocketAvgSpecificHeat = specificHeatAverage(pocketGasVolumes)


        var newCurrentNodeTemperature: Double = currentNodeTemperature
        var newPocketTemperature: Double = pocketTemperature
        val newCurrentNodeMasses = HashMap<GasType, Double>()
        val newPocketMasses = HashMap<GasType, Double>()


        val density = (currentNodeAvgDensity + pocketAvgDensity) / 2.0
        val viscosity = (currentNodeAvgViscosity + pocketAvgViscosity) / 2.0

        var flow = calculateFlow(currentNodePressure, pocketPressure, 0.5, viscosity)

        if (flow < 0) {
            flow = 0.0
        }

        if (flow.isInfinite() || flow.isNaN() || flow == 0.0) return

        for (gas in GasType.entries) {
            val currentMass = currentNodeGasVolumes[gas] ?: 0.0
            val pocketMass = pocketGasVolumes[gas] ?: 0.0

            val deltaMass = Mth.clamp(flow, 0.0, currentMass)
            newCurrentNodeMasses[gas] = max(currentMass - deltaMass, 0.0)
            newPocketMasses[gas] = max(pocketMass + deltaMass, 0.0)
        }

        var deltaThermalEnergy = if (flow > 0) {
            (currentNodeTotalMass * currentNodeAvgSpecificHeat * (currentNodeTemperature - pocketTemperature))
        } else {
            0.0
        }

        val thermalLimit = abs((currentNodeTotalMass*currentNodeAvgSpecificHeat*currentNodeTemperature)-(pocketTotalMass*pocketAvgSpecificHeat*pocketTemperature)) /2.0
        deltaThermalEnergy = Mth.clamp(deltaThermalEnergy, -thermalLimit, thermalLimit)


        if (deltaThermalEnergy.isInfinite() || deltaThermalEnergy.isNaN()) return

        if (flow > 0) {
            if (currentNodeTotalMass > 0) newCurrentNodeTemperature -= (deltaThermalEnergy / (currentNodeTotalMass * currentNodeAvgSpecificHeat))
            if (pocketTotalMass > 0) newPocketTemperature += (deltaThermalEnergy / (pocketTotalMass * pocketAvgSpecificHeat))
        }

        for (gas in GasType.entries) {
            ClockworkMod.getKelvin().modGasMass(blockPos.toJOMLD(), gas, newCurrentNodeMasses[gas]!! - currentNodeGasVolumes[gas]!!)
            serverLevel.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("gas_" + gas.name.lowercase()), pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId, newPocketMasses[gas]!!)
        }
        ClockworkMod.getKelvin().modTemperature(blockPos.toJOMLD(), newCurrentNodeTemperature - currentNodeTemperature)
        serverLevel.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("temperature"), pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId, newPocketTemperature)
    }

    override fun getDuctNodePosition(): DuctNodePos {
        return blockPos.toJOMLD()
    }
}