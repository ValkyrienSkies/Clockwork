package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.createmod.catnip.animation.LerpedFloat
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkAugmentations
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.util.ClockworkUtils.retrieveGasInfoFromPocket
import org.valkyrienskies.clockwork.util.KNodeKineticBlockEntity
import org.valkyrienskies.core.api.world.connectivity.ConnectionStatus
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3i
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.time.Clock
import kotlin.math.abs
import kotlin.math.roundToInt

class GasNozzleBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState): KNodeKineticBlockEntity(type, pos, state) {

    var hasPocket = false
    var pointerSpeed = 0.0

    val pointer: LerpedFloat = LerpedFloat.linear()
        .startWithValue(0.5)
        .chase(0.5, pointerSpeed, LerpedFloat.Chaser.LINEAR)

    var currentIdealOutput: Double = 0.0

    var pocketTemperature: Double = 0.0

    override fun write(tag: CompoundTag, clientPacket: Boolean) {

        tag.putDouble("pointer_target",pointer.chaseTarget.toDouble())
        tag.putDouble("pointer_speed",pointerSpeed)
        tag.putBoolean("has_pocket",hasPocket)
        tag.putDouble("pocket_temperature", pocketTemperature)
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)

        val target = tag.getDouble("pointer_target")
        pointerSpeed = tag.getDouble("pointer_speed")
        hasPocket = tag.getBoolean("has_pocket")
        pocketTemperature = tag.getDouble("pocket_temperature")

        pointer.chase(target, pointerSpeed, LerpedFloat.Chaser.LINEAR)
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun tick() {
        super.tick()

        pointer.tickChaser()
        if (level == null || level!!.isClientSide) return

        val serverLevel = level!! as ServerLevel

        val oldHas = hasPocket
        //println(serverLevel.shipObjectWorld.isIsolatedAir(blockPos.x, blockPos.y+1, blockPos.z, serverLevel.dimensionId))
        hasPocket = serverLevel.shipObjectWorld.isIsolatedAir(blockPos.x, blockPos.y+1, blockPos.z, serverLevel.dimensionId)  == ConnectionStatus.DISCONNECTED


        if (oldHas != hasPocket) sendData()

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
        pointerSpeed = getChaseSpeed()
        pointer.chase(target, pointerSpeed, LerpedFloat.Chaser.LINEAR)
        sendData()
    }

    fun getChaseSpeed(): Double {
        return Mth.clamp(abs(getSpeed().toDouble()) / 16.0 / 40.0, 0.0, 1.0)
    }

    private fun heatPocket() {
        val serverLevel = level as? ServerLevel ?: return

        val pocketRef = blockPos.above()
        val (pocketGasMass, pocketHeatEnergy) = retrieveGasInfoFromPocket(pocketRef.toVector3i(), serverLevel)
        val pocketGasMassTotal = pocketGasMass.values.sum()

        val gasMass = ClockworkMod.getKelvin().getGasMassAt(getDuctNodePosition())
        val gasMassTotal = gasMass.values.sum()
        val heatEnergy = ClockworkMod.getKelvin().getHeatEnergy(getDuctNodePosition())

        val usedUpMass = gasMassTotal * pointer.value
        val usedEnergy = heatEnergy * pointer.value

        pocketTemperature = (pocketHeatEnergy + usedEnergy) / ClockworkMod.getKelvin().mixtureCapacity(pocketGasMass)

        serverLevel.shipObjectWorld.setAirComponentAugmentation(
            ClockworkAugmentations.getComponentAugmentation("heatEnergy"),
            blockPos.x,
            blockPos.y+1,
            blockPos.z,
            serverLevel.dimensionId,
            pocketHeatEnergy + usedEnergy
        )

        gasMass.forEach {
            KelvinMod.getKelvin().removeGas(getDuctNodePosition(), it.key,usedUpMass * it.value / gasMassTotal)
        }
        sendData()
    }

    private fun heatPocketOld() {
//        val serverLevel = level!! as ServerLevel
//
//        val realY = if (level.getShipObjectManagingPos(blockPos) != null) {
//            level.getShipObjectManagingPos(blockPos)!!.transform.shipToWorld.transformPosition(blockPos.toJOMLD()).y + 0.5
//        } else {
//            blockPos.y + 0.5
//        }
//
//        val dimension = serverLevel.dimension().location()
//
//
//        val (pocketGasVolumes, pocketTemperature) = retrieveGasInfoFromPocket(pocketRef.toJOML(), serverLevel)
//        val pocketVolume =
//        val pocketTotalMass = pocketGasVolumes.values.sum()
//        val pocketAvgDensity = densityAverage(pocketGasVolumes)
//        val pocketAvgViscosity = dynamicViscosityAverage(pocketGasVolumes, pocketTemperature)
//        val pocketPressure = serverLevel.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("pressure"), pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId)
//
//        val currentNodeTemperature = ClockworkMod.getKelvin().getTemperatureAt(blockPos.toDuctNodePos(dimension))
//        val currentNodePressure = ClockworkMod.getKelvin().getPressureAt(blockPos.toDuctNodePos(dimension))
//        val currentNodeGasVolumes = ClockworkMod.getKelvin().getGasMassAt(blockPos.toDuctNodePos(dimension))
//        val currentNodeTotalMass = currentNodeGasVolumes.values.sum()
//        val currentNodeAvgViscosity = dynamicViscosityAverage(currentNodeGasVolumes, currentNodeTemperature)
//        val currentNodeAvgSpecificHeat = specificHeatAverage(currentNodeGasVolumes)
//
//        val newNodeMasses = HashMap<GasType, Double>()
//
//        if (currentNodeTotalMass <= 0.0001 || pocketTotalMass <= 0.0001 || pocketTemperature >= currentNodeTemperature) return
//
//        val outsideAirTemp = AerodynamicUtils.getAirTemperatureForY(realY, serverLevel.dimensionId)
//
//        // Gas consumption
//
//        val outputRateMult = pointer.value.toDouble()
//        val consumedGasses = HashMap<GasType, Double>()
//
//        val idealOutputEnergy = 100000.0 //100 kW * closed off valve amount
//
//        val targetTemperature = currentNodeTemperature * outputRateMult
//
//        currentIdealOutput = Mth.lerp(1.0/60.0, currentIdealOutput, idealOutputEnergy)
//
//        var actualOutputEnergy = 0.0
//
//        val temperatureDiff = if (currentNodeTemperature - outsideAirTemp >= 0.001) {
//            currentNodeTemperature - outsideAirTemp
//        } else {
//            -0.001
//        }
//
//        val idealFlowRate = currentIdealOutput / (temperatureDiff * currentNodeAvgSpecificHeat)
//
//        val flowRate = Mth.clamp(idealFlowRate, 0.0, currentNodeTotalMass) / 20.0
//
//        actualOutputEnergy = flowRate * (temperatureDiff * currentNodeAvgSpecificHeat)
//
//        // Heat transfer
//
//        var temperatureChangeInPocket = (actualOutputEnergy / 20.0) / (pocketTotalMass * specificHeatAverage(pocketGasVolumes))
//
//        if (temperatureChangeInPocket.isInfinite() || temperatureChangeInPocket.isNaN() || temperatureChangeInPocket < 0.0) return
//
//        temperatureChangeInPocket = Mth.clamp(temperatureChangeInPocket, -pocketTemperature, currentNodeTemperature - pocketTemperature)
//
//        var newPocketTemperature = max(Mth.clamp(pocketTemperature + temperatureChangeInPocket, 0.0001, currentNodeTemperature), pocketTemperature)
//
//        val adjustment = pid.control(targetTemperature, pocketTemperature)
//
//        newPocketTemperature += adjustment
//
//        var newCurrentNodeTemperature = currentNodeTemperature - (actualOutputEnergy / (currentNodeTotalMass * currentNodeAvgSpecificHeat))
//        if (newCurrentNodeTemperature <= 0.0001 || newCurrentNodeTemperature.isNaN() && newCurrentNodeTemperature.isInfinite()) newCurrentNodeTemperature = 0.0001
//        for (gas in GasTypeRegistry.GAS_TYPES.values) {
//            val currentMass = currentNodeGasVolumes[gas] ?: 0.0
//            val deltaMass = Mth.clamp(flowRate, 0.0, currentMass)
//            newNodeMasses[gas] = max(currentMass - deltaMass, 0.0)
//            consumedGasses[gas] = deltaMass
//        }
//
//        //apply stuff
//
//        for (gas in GasTypeRegistry.GAS_TYPES.values) {
//            ClockworkMod.getKelvin().removeGas(blockPos.toDuctNodePos(dimension), gas, (consumedGasses[gas] ?: 0.0))
//        }
//        ClockworkMod.getKelvin().modTemperature(blockPos.toDuctNodePos(dimension), max(newCurrentNodeTemperature - currentNodeTemperature, -currentNodeTemperature))
//        serverLevel.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("temperature"), pocketRef.x, pocketRef.y, pocketRef.z, serverLevel.dimensionId, newPocketTemperature)
    }

    override fun getDuctNodePosition(): DuctNodePos {
        if (level != null) {
            return blockPos.toDuctNodePos(level!!.dimension().location())
        }
        return blockPos.toDuctNodePos()
    }

    override fun addToGoggleTooltip(tooltip: List<Component>?, isPlayerSneaking: Boolean): Boolean {
        val bool = super.addToGoggleTooltip(tooltip, isPlayerSneaking)
        if (!hasPocket || pocketTemperature.isNaN()) {
            (tooltip as MutableList?)?.add(Component.literal("Missing pocket.").withStyle(ChatFormatting.GRAY).withStyle(ChatFormatting.ITALIC))
        } else {
            (tooltip as MutableList?)?.add(Component.literal("Pocket Temperature: ${pocketTemperature.roundToInt()}K").withStyle(ChatFormatting.RED))
        }
        return bool
    }
}
