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
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.AerodynamicUtils
import org.valkyrienskies.clockwork.util.AerodynamicUtils.calcPressure
import org.valkyrienskies.clockwork.util.AerodynamicUtils.calculateFlow
import org.valkyrienskies.clockwork.util.AerodynamicUtils.densityAverage
import org.valkyrienskies.clockwork.util.AerodynamicUtils.densityFromPressureAverage
import org.valkyrienskies.clockwork.util.AerodynamicUtils.dynamicViscosityAverage
import org.valkyrienskies.clockwork.util.AerodynamicUtils.specificHeatAverage
import org.valkyrienskies.clockwork.util.ClockworkUtils.retrieveGasInfoFromPocket
import org.valkyrienskies.clockwork.util.PIDstance
import org.valkyrienskies.kelvin.impl.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
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
        pointer.chase(target, getChaseSpeed(), LerpedFloat.Chaser.LINEAR)
        ClockworkPackets.sendToNear(level, blockPos, 100, GasNozzlePacket(target, blockPos))
        sendData()
    }

    fun getChaseSpeed(): Double {
        return Mth.clamp(abs(getSpeed().toDouble()) / 16.0 / 40.0, 0.0, 1.0)
    }

    private fun heatPocket() {
        val serverLevel = level!! as ServerLevel

        val realY = if (level.getShipObjectManagingPos(blockPos) != null) {
            level.getShipObjectManagingPos(blockPos)!!.transform.shipToWorld.transformPosition(blockPos.toJOMLD()).y + 0.5
        } else {
            blockPos.y + 0.5
        }

        val dimension = serverLevel.dimension().location()

        val pocketRef = blockPos.above()
        val (pocketGasVolumes, pocketTemperature) = retrieveGasInfoFromPocket(pocketRef.toJOML(), serverLevel)
        val pocketVolume = serverLevel.shipObjectWorld.getAirComponentSize(pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId).toDouble()
        val pocketTotalMass = pocketGasVolumes.values.sum()
        val pocketAvgDensity = densityAverage(pocketGasVolumes)
        val pocketAvgViscosity = dynamicViscosityAverage(pocketGasVolumes, pocketTemperature)
        val pocketPressure = serverLevel.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("pressure"), pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId)

        val currentNodeTemperature = ClockworkMod.getKelvin().getTemperatureAt(blockPos.toDuctNodePos(dimension))
        val currentNodePressure = ClockworkMod.getKelvin().getPressureAt(blockPos.toDuctNodePos(dimension))
        val currentNodeGasVolumes = ClockworkMod.getKelvin().getGasMassAt(blockPos.toDuctNodePos(dimension))
        val currentNodeTotalMass = currentNodeGasVolumes.values.sum()
        val currentNodeAvgViscosity = dynamicViscosityAverage(currentNodeGasVolumes, currentNodeTemperature)
        val currentNodeAvgSpecificHeat = specificHeatAverage(currentNodeGasVolumes)

        val newNodeMasses = HashMap<GasType, Double>()

        if (currentNodeTotalMass <= 0.0001 || pocketTotalMass <= 0.0001 || pocketTemperature >= currentNodeTemperature) return

        val outsideAirTemp = AerodynamicUtils.getAirTemperatureForY(realY, serverLevel.dimensionId)

        // Gas consumption

        val outputRateMult = pointer.value.toDouble()
        val consumedGasses = HashMap<GasType, Double>()

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
        for (gas in GasTypeRegistry.GAS_TYPES.values) {
            val currentMass = currentNodeGasVolumes[gas] ?: 0.0
            val deltaMass = Mth.clamp(flowRate, 0.0, currentMass)
            newNodeMasses[gas] = max(currentMass - deltaMass, 0.0)
            consumedGasses[gas] = deltaMass
        }

        //apply stuff

        for (gas in GasTypeRegistry.GAS_TYPES.values) {
            ClockworkMod.getKelvin().modGasMass(blockPos.toDuctNodePos(dimension), gas, -(consumedGasses[gas] ?: 0.0))
        }
        ClockworkMod.getKelvin().modTemperature(blockPos.toDuctNodePos(dimension), max(newCurrentNodeTemperature - currentNodeTemperature, -currentNodeTemperature))
        serverLevel.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("temperature"), pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId, newPocketTemperature)
    }

    override fun getDuctNodePosition(): DuctNodePos {
        if (level != null) {
            return blockPos.toDuctNodePos(level!!.dimension().location())
        }
        return blockPos.toDuctNodePos()
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