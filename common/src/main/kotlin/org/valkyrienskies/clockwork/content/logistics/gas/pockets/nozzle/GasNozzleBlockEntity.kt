package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkAugmentations
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor.AirCompressorPacket
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.AerodynamicUtils
import org.valkyrienskies.clockwork.util.AerodynamicUtils.calcPressure
import org.valkyrienskies.clockwork.util.AerodynamicUtils.calculateFlow
import org.valkyrienskies.clockwork.util.AerodynamicUtils.densityAverage
import org.valkyrienskies.clockwork.util.AerodynamicUtils.densityFromPressureAverage
import org.valkyrienskies.clockwork.util.AerodynamicUtils.dynamicViscosityAverage
import org.valkyrienskies.clockwork.util.AerodynamicUtils.specificHeatAverage
import org.valkyrienskies.clockwork.util.ClockworkUtils.retrieveGasInfoFromPocket
import org.valkyrienskies.clockwork.util.PIDstance
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class GasNozzleBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?): KineticBlockEntity(type, pos, state), IHeatableBlockEntity {

    var hasPocket = false
    val pointer: LerpedFloat = LerpedFloat.linear()
        .startWithValue(0.5)
        .chase(0.5, 0.0, LerpedFloat.Chaser.LINEAR)

    var currentIdealOutput: Double = 0.0

    val pid = PIDstance()

    var clientPocketTemperature: Double = 0.0

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun tick() {
        super.tick()

        pointer.tickChaser()
        if (level == null || level!!.isClientSide) return

        val serverLevel = level!! as ServerLevel

        hasPocket = try {
            serverLevel.shipObjectWorld.getAirComponentSize(blockPos.x, blockPos.y+1, blockPos.z, serverLevel.dimensionId) > 0
        } catch (e: IllegalArgumentException) {
            pid.resetIntegral()
            false
        }

        if (hasPocket) {
            //flowIntoPocket()
            if (serverLevel.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("airupdated"), blockPos.x, blockPos.y +1, blockPos.z, serverLevel.dimensionId) < 1.0) {
                serverLevel.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("airupdated"), blockPos.x, blockPos.y +1, blockPos.z, serverLevel.dimensionId, 0.0)
            }
            heatPocket()
        }

    }

    override fun onSpeedChanged(previousSpeed: Float) {
        super.onSpeedChanged(previousSpeed)
        val speed = getSpeed()
        val target = (if (speed > 0) 1 else 0).toDouble()
        pointer.chase(target, getChaseSpeed().toDouble(), LerpedFloat.Chaser.LINEAR)
        ClockworkPackets.sendToNear(level, blockPos, 100, GasNozzlePacket(target, blockPos))
        sendData()
    }

    fun getChaseSpeed(): Float {
        return Mth.clamp(abs(getSpeed()) / 16f / 20f / 10f, 0f, 1f)
    }

    private fun heatPocket() {
        val serverLevel = level!! as ServerLevel

        val realY = if (level.getShipObjectManagingPos(blockPos) != null) {
            level.getShipObjectManagingPos(blockPos)!!.transform.shipToWorld.transformPosition(blockPos.toJOMLD()).y + 0.5
        } else {
            blockPos.y + 0.5
        }

        val pocketRef = blockPos.above()
        val (pocketGasVolumes, pocketTemperature) = retrieveGasInfoFromPocket(pocketRef.toJOML(), serverLevel)
        val pocketVolume = serverLevel.shipObjectWorld.getAirComponentSize(pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId).toDouble()
        val pocketTotalMass = pocketGasVolumes.values.sum()
        val pocketAvgDensity = densityAverage(pocketGasVolumes)
        val pocketAvgViscosity = dynamicViscosityAverage(pocketGasVolumes, pocketTemperature)
        val pocketPressure = serverLevel.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("pressure"), pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId)

        val currentNodeTemperature = ClockworkMod.getKelvin().getTemperatureAt(blockPos.toJOMLD())
        val currentNodePressure = ClockworkMod.getKelvin().getPressureAt(blockPos.toJOMLD())
        val currentNodeGasVolumes = ClockworkMod.getKelvin().getGasMassAt(blockPos.toJOMLD())
        val currentNodeTotalMass = currentNodeGasVolumes.values.sum()
        val currentNodeAvgViscosity = dynamicViscosityAverage(currentNodeGasVolumes, currentNodeTemperature)
        val currentNodeAvgSpecificHeat = specificHeatAverage(currentNodeGasVolumes)

        val newNodeMasses = EnumMap<GasType, Double>(GasType::class.java)

        if (currentNodeTotalMass <= 0.0001 || pocketTotalMass <= 0.0001 || pocketTemperature >= currentNodeTemperature) return

        val outsideAirTemp = AerodynamicUtils.getAirTemperatureForY(realY, 563.0)

        // Gas consumption

        val outputRateMult = pointer.value.toDouble()
        val consumedGasses = EnumMap<GasType, Double>(GasType::class.java)

        val idealOutputEnergy = 100000.0 //100 kW * closed off valve amount

        val targetTemperature = currentNodeTemperature * outputRateMult

        currentIdealOutput = Mth.lerp(1.0/60.0, currentIdealOutput, idealOutputEnergy)

        var actualOutputEnergy = 0.0

        val temperatureDiff = if (currentNodeTemperature - outsideAirTemp >= 0.001) {
            currentNodeTemperature - outsideAirTemp
        } else {
            -0.001
        }

        val idealFlowRate = currentIdealOutput / (temperatureDiff * currentNodeAvgSpecificHeat)

        val flowRate = Mth.clamp(idealFlowRate, 0.0, currentNodeTotalMass) / 20.0

        actualOutputEnergy = flowRate * (temperatureDiff * currentNodeAvgSpecificHeat)

        // Heat transfer

        var temperatureChangeInPocket = (actualOutputEnergy / 20.0) / (pocketTotalMass * specificHeatAverage(pocketGasVolumes))

        if (temperatureChangeInPocket.isInfinite() || temperatureChangeInPocket.isNaN() || temperatureChangeInPocket < 0.0) return

        temperatureChangeInPocket = Mth.clamp(temperatureChangeInPocket, -pocketTemperature, currentNodeTemperature - pocketTemperature)

        var newPocketTemperature = max(Mth.clamp(pocketTemperature + temperatureChangeInPocket, 0.0001, currentNodeTemperature), pocketTemperature)

        val adjustment = pid.control(targetTemperature, pocketTemperature)

        newPocketTemperature += adjustment

        var newCurrentNodeTemperature = currentNodeTemperature - (actualOutputEnergy / (currentNodeTotalMass * currentNodeAvgSpecificHeat))
        if (newCurrentNodeTemperature <= 0.0001 || newCurrentNodeTemperature.isNaN() && newCurrentNodeTemperature.isInfinite()) newCurrentNodeTemperature = 0.0001
        for (gas in GasType.entries) {
            val currentMass = currentNodeGasVolumes[gas] ?: 0.0
            val deltaMass = Mth.clamp(flowRate, 0.0, currentMass)
            newNodeMasses[gas] = max(currentMass - deltaMass, 0.0)
            consumedGasses[gas] = deltaMass
        }

        //apply stuff

        for (gas in GasType.entries) {
            ClockworkMod.getKelvin().modGasMass(blockPos.toJOMLD(), gas, -(consumedGasses[gas] ?: 0.0))
        }
        ClockworkMod.getKelvin().modTemperature(blockPos.toJOMLD(), max(newCurrentNodeTemperature - currentNodeTemperature, -currentNodeTemperature))
        serverLevel.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("temperature"), pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId, newPocketTemperature)
    }

    private fun flowIntoPocket() {
        val serverLevel = level!! as ServerLevel

        val currentNodeTemperature = ClockworkMod.getKelvin().getTemperatureAt(blockPos.toJOMLD())
        val currentNodePressure = ClockworkMod.getKelvin().getPressureAt(blockPos.toJOMLD())
        val currentNodeGasVolumes = ClockworkMod.getKelvin().getGasMassAt(blockPos.toJOMLD())
        val currentNodeTotalMass = currentNodeGasVolumes.values.sum()
        val currentNodeAvgViscosity = dynamicViscosityAverage(currentNodeGasVolumes, currentNodeTemperature)
        val currentNodeAvgSpecificHeat = specificHeatAverage(currentNodeGasVolumes)


        val pocketRef = blockPos.above()
        val (pocketGasVolumes, pocketTemperature) = retrieveGasInfoFromPocket(pocketRef.toJOML(), serverLevel)
        val pocketVolume = serverLevel.shipObjectWorld.getAirComponentSize(pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId).toDouble()
        val pocketTotalMass = pocketGasVolumes.values.sum()
        val pocketAvgDensity = densityAverage(pocketGasVolumes)
        val pocketAvgViscosity = dynamicViscosityAverage(pocketGasVolumes, pocketTemperature)
        val pocketPressure = serverLevel.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("pressure"), pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId)

        var newCurrentNodeTemperature: Double = currentNodeTemperature
        var newPocketTemperature: Double = pocketTemperature
        val newCurrentNodeMasses: EnumMap<GasType, Double> = EnumMap(GasType::class.java)
        val newPocketMasses: EnumMap<GasType, Double> = EnumMap(GasType::class.java)

        val realDensityNode = densityFromPressureAverage(currentNodeGasVolumes, currentNodeTemperature, currentNodePressure)
        val realDensityPocket = densityFromPressureAverage(pocketGasVolumes, pocketTemperature, pocketPressure)
        val viscosity = (currentNodeAvgViscosity + pocketAvgViscosity) / 2.0

        var flow = calculateFlow(
            currentNodePressure,
            pocketPressure,
            0.5 - (pointer.value/2.0),
            0.5,
            realDensityNode,
            realDensityPocket,
            viscosity
        )

        if (flow < 0) {
            flow = 0.0
        }

        if (flow.isInfinite() || flow.isNaN()) flow = 0.0

        var totalDeltaMass: Double = 0.0
        val transferredGasses = EnumMap<GasType, Double>(GasType::class.java)
        for (gas in GasType.entries) {
            val currentMass = currentNodeGasVolumes[gas] ?: 0.0
            val pocketMass = pocketGasVolumes[gas] ?: 0.0

            val deltaMass = Mth.clamp(flow, 0.0, currentMass)
            newCurrentNodeMasses[gas] = max(currentMass - deltaMass, 0.0)
            newPocketMasses[gas] = max(pocketMass + deltaMass, 0.0)
            totalDeltaMass += deltaMass
            transferredGasses[gas] = deltaMass
        }

        val transferSpecificHeat = specificHeatAverage(transferredGasses)

        var deltaThermalEnergy = if (flow > 0) {
            (totalDeltaMass * transferSpecificHeat * (currentNodeTemperature - pocketTemperature))
        } else {
            0.0
        }

        val thermalLimit = if (flow > 0) {
            currentNodeTotalMass * currentNodeAvgSpecificHeat * currentNodeTemperature
        } else {
            0.0
        }
        deltaThermalEnergy = Mth.clamp(deltaThermalEnergy, -thermalLimit, thermalLimit)

        if (deltaThermalEnergy.isInfinite() || deltaThermalEnergy.isNaN()) deltaThermalEnergy = 0.0


        //if (nodeA.currentTemperature > 300.0 || nodeB.currentTemperature > 300.0) KELVINLOGGER.logger.warn("High Temp! DeltaThermalEnergy: $deltaThermalEnergy, flowHeat: $flowHeatCapacity, ThermalLimit: $thermalLimit, totalGasMassA: $newTotalGasMassesA, totalGasMassB: $newTotalGasMassesB")
        if (newCurrentNodeMasses.values.sum() >= 0.0001 && newPocketMasses.values.sum() >= 0.0001) {
            newCurrentNodeTemperature -= (deltaThermalEnergy) / (newCurrentNodeMasses.values.sum() * specificHeatAverage(newCurrentNodeMasses))
            newPocketTemperature += (deltaThermalEnergy) / (newPocketMasses.values.sum() * specificHeatAverage(newPocketMasses))
        }

        if (newPocketTemperature <= 0.0001) newPocketTemperature = 0.0001
        if (newCurrentNodeTemperature <= 0.0001) newCurrentNodeTemperature = 0.0001

        newPocketTemperature = min(Mth.clamp(newPocketTemperature, 0.0001, currentNodeTemperature), 5772.0)

        //var newPocketPressure = calcPressure(newPocketMasses.values.sum(), pocketVolume, newPocketTemperature, densityAverage(newPocketMasses))

        for (gas in GasType.entries) {
            ClockworkMod.getKelvin().modGasMass(blockPos.toJOMLD(), gas, -(transferredGasses[gas]?: 0.0))
            serverLevel.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("gas_" + gas.name.lowercase()), pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId, newPocketMasses[gas]!!)
        }
        ClockworkMod.getKelvin().modTemperature(blockPos.toJOMLD(), newCurrentNodeTemperature - currentNodeTemperature)
        serverLevel.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("temperature"), pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId, newPocketTemperature)
        //serverLevel.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("pressure"), pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId, newPocketPressure)
    }

    override fun getDuctNodePosition(): DuctNodePos {
        return blockPos.toJOMLD()
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        super<IHeatableBlockEntity>.addToGoggleTooltip(tooltip, isPlayerSneaking)
        if (!hasPocket) {
            tooltip.add(TextComponent("Missing pocket.").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC))
            return false
        } else {
            tooltip.add(TextComponent("Pocket Temperature: $clientPocketTemperature").withStyle(ChatFormatting.RED))
        }
        return super<KineticBlockEntity>.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }
}