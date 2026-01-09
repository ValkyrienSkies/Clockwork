package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.createmod.catnip.animation.LerpedFloat
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkAugmentations
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.forces.BalloonController
import org.valkyrienskies.clockwork.content.forces.data.BalloonData
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.util.ClockworkUtils.retrieveGasInfoFromPocket
import org.valkyrienskies.clockwork.util.KNodeKineticBlockEntity
import org.valkyrienskies.clockwork.util.KelvinParticleHelper
import org.valkyrienskies.clockwork.util.gui.ClockworkTooltipHelper
import org.valkyrienskies.clockwork.util.gui.DuctTextUtil
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3i
import org.valkyrienskies.mod.api.isBlockInShipyard
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

class GasNozzleBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState): KNodeKineticBlockEntity(type, pos, state) {

    var hasPocket = false
    var pointerSpeed = 0.0
    var scanCooldown = 0

    val pointer: LerpedFloat = LerpedFloat.linear()
        .startWithValue(0.5)
        .chase(0.5, pointerSpeed, LerpedFloat.Chaser.LINEAR)

    var currentIdealOutput: Double = 0.0

    var pocketTemperature: Double = 0.0
    var balloonVolume: Double = 0.0

    var balloon: BalloonData? = null

    var shouldFetchNextTick = false

    @Environment(EnvType.CLIENT)
    var soundInstance: GasNozzleSoundInstance? = null

    override fun write(tag: CompoundTag, clientPacket: Boolean) {

        tag.putDouble("pointer_target",pointer.chaseTarget.toDouble())
        tag.putDouble("pointer_speed",pointerSpeed)
        tag.putBoolean("has_pocket",hasPocket)
        tag.putDouble("pocket_temperature", pocketTemperature)
        tag.putDouble("balloon_volume", balloonVolume)
        tag.putInt("leaks", currentIdealOutput.toInt())
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)

        val target = tag.getDouble("pointer_target")
        pointerSpeed = tag.getDouble("pointer_speed")
        hasPocket = tag.getBoolean("has_pocket")
        pocketTemperature = tag.getDouble("pocket_temperature")
        balloonVolume = tag.getDouble("balloon_volume")
        currentIdealOutput = tag.getInt("leaks").toDouble()

        pointer.chase(target, pointerSpeed, LerpedFloat.Chaser.LINEAR)
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    fun randomPos(deviation: Double, random: RandomSource): Double {
        return (0.5-deviation/2.0)+random.nextDouble()*deviation
    }

    override fun invalidate() {
        if (level != null && level!!.isClientSide) clientInvalidate()
        super.invalidate()
    }

    fun clientInvalidate() {
        soundInstance?.stopNow()
        soundInstance = null
    }

    override fun tick() {
        super.tick()

        pointer.tickChaser()

        if (level != null && level!!.isClientSide && hasPocket) {
            val state = level!!.getBlockState(blockPos)
            if (state.block !is GasNozzleBlock) return
            val facing = Direction.UP
            val random = level!!.random
            val network = ClockworkModClient.getKelvin()
            val gasses = network.getGasMassAt(getDuctNodePosition())
            val pressure = network.getPressureAt(getDuctNodePosition())
            val MASS_PER_EXHAUST = 0.001

            for (i in 1..min(floor((gasses.values.sum()*pointer.value)/MASS_PER_EXHAUST).toInt(), 60)) {
                KelvinParticleHelper.spawnParticleWithRatio(level as ClientLevel, getDuctNodePosition(),
                    blockPos.toJOMLD().add(randomPos(0.3, random), randomPos(0.3, random), randomPos(0.3, random)),
                    facing.normal.toJOMLD().mul(Mth.clamp(0.0025 * pressure.pow(0.4), 0.1,5.0 )))
            }

            if (soundInstance == null) {
                soundInstance = GasNozzleSoundInstance(this, random)
                Minecraft.getInstance().soundManager.play(soundInstance)
            }
        }

        if (level == null || level!!.isClientSide) return

        val serverLevel = level!! as ServerLevel

        val oldHas = hasPocket
        //println(serverLevel.shipObjectWorld.isIsolatedAir(blockPos.x, blockPos.y+1, blockPos.z, serverLevel.dimensionId))
        //hasPocket = serverLevel.shipObjectWorld.isIsolatedAir(blockPos.x, blockPos.y+1, blockPos.z, serverLevel.dimensionId)  == ConnectionStatus.DISCONNECTED

        if (shouldFetchNextTick && scanCooldown <= 0) {
            fetchBloon()
            shouldFetchNextTick = false
        }
        if (scanCooldown > 0) {
            scanCooldown--
        }

        if (oldHas != hasPocket) {
            if (hasPocket) {
                val shipOn = serverLevel.getLoadedShipManagingPos(blockPos)
                val upInWorld = if (shipOn != null) {
                    shipOn.transform.rotation.transform(Direction.UP.step())
                } else {
                    Direction.UP.step()
                }
                serverLevel.playSound(
                    null,
                    blockPos,
                    ClockworkSounds.GAS_NOZZLE_START.mainEvent!!,
                    net.minecraft.sounds.SoundSource.BLOCKS,
                    1.0f,
                    0.5f + 0.5f * serverLevel.random.nextFloat()
                )
                // send initial particle burst
                serverLevel.sendParticles(
                    LeakParticleData(
                        upInWorld,
                        2f,
                    ),
                    blockPos.x + 0.5,
                    blockPos.y + 1.0,
                    blockPos.z + 0.5,
                    5,
                    0.3,
                    0.3,
                    0.3,
                    1.0
                )
            }
            sendData()
        }

        if (hasPocket) {
            if (balloon?.shouldRemove == true || balloon == null) {
                shouldFetchNextTick = true
                hasPocket = false
                sendData()
                return
            }
            //flowIntoPocket()
//            if (serverLevel.shipObjectWorld.getAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("airupdated"), blockPos.x, blockPos.y +1, blockPos.z, serverLevel.dimensionId) < 1.0) {
//                serverLevel.shipObjectWorld.setAirComponentAugmentation(ClockworkAugmentations.getComponentAugmentation("airupdated"), blockPos.x, blockPos.y +1, blockPos.z, serverLevel.dimensionId, 0.0)
//            }
            //heatPocket()
            heatBalloon()
            if (this.balloon != null) {
                var pocketGasMass: HashMap<GasType, Double> = HashMap()
                for ((key, value) in balloon!!.gasMasses) {
                    val gasType = GasTypeRegistry.getGasType(ResourceLocation(key)) ?: continue
                    pocketGasMass[gasType] = value
                }
                val pocketHeatEnergy = balloon!!.currentEnergy
                val pocketCapacity = ClockworkMod.getKelvin().mixtureCapacity(pocketGasMass)
                val currentPocketTemperature = (pocketHeatEnergy) / pocketCapacity
                pocketTemperature = currentPocketTemperature
                balloonVolume = balloon!!.currentVolume
                currentIdealOutput = balloon!!.missingExternalPositions.toDouble() // this is cursed but i made it without reloading the game so variable reuse lesgo
                sendData()
            }

        }

//        if (balloon != null) {
//            var pocketGasMass: HashMap<GasType, Double> = HashMap()
//            for ((key, value) in balloon!!.gasMasses) {
//                val gasType = GasTypeRegistry.getGasType(ResourceLocation(key)) ?: continue
//                pocketGasMass[gasType] = value
//            }
//            val pocketHeatEnergy = balloon!!.currentEnergy
//            val pocketCapacity = ClockworkMod.getKelvin().mixtureCapacity(pocketGasMass)
//            pocketTemperature = (pocketHeatEnergy) / pocketCapacity
//            balloonVolume = balloon!!.currentVolume
//            currentIdealOutput = balloon!!.missingExternalPositions.toDouble()
//            sendData()
//        }

    }

    fun fetchBloon() {
        val serverLevel = level as? ServerLevel ?: return
        val ship = serverLevel.getLoadedShipManagingPos(blockPos) ?: return
        val controller = BalloonController.getOrCreate(ship)
        val balloonId = controller.tryGetOrCreateBalloon(blockPos.above(), serverLevel)
        this.balloon = controller.getBalloonById(balloonId)
        this.hasPocket = this.balloon != null
        this.balloonVolume = this.balloon?.currentVolume ?: 0.0
        this.scanCooldown = 60
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

    private fun heatBalloon() {
        val balloon = this.balloon ?: return
        if (this.pointer.value <= 0) return

        var pocketGasMass: HashMap<GasType, Double> = HashMap()
        for ((key, value) in balloon.gasMasses) {
            val gasType = GasTypeRegistry.getGasType(ResourceLocation(key)) ?: continue
            pocketGasMass[gasType] = value
        }
        val pocketHeatEnergy = balloon.currentEnergy

        val gasMass = ClockworkMod.getKelvin().getGasMassAt(getDuctNodePosition())
        val gasMassTotal = gasMass.values.sum()
        val heatEnergy = ClockworkMod.getKelvin().getHeatEnergy(getDuctNodePosition())
        val pocketCapacity = ClockworkMod.getKelvin().mixtureCapacity(pocketGasMass)
        val currentPocketTemperature = (pocketHeatEnergy) / pocketCapacity
        val targetTemperature = ClockworkMod.getKelvin().getTemperatureAt(getDuctNodePosition()) * pointer.value.toDouble()
        if (currentPocketTemperature >= targetTemperature) return
        val maxEnergyAddedThisTick = (heatEnergy / 2.0)
        val energyToAdd = min(pocketCapacity * min(targetTemperature - currentPocketTemperature, 100.0), maxEnergyAddedThisTick)

        val usedUpMass = gasMassTotal * pointer.value
        val usedEnergy = min(heatEnergy, energyToAdd) * pointer.value

//        pocketTemperature = (pocketHeatEnergy + usedEnergy) / pocketCapacity
//        balloonVolume = balloon.currentVolume
//        currentIdealOutput = balloon.missingExternalPositions.toDouble() // this is cursed but i made it without reloading the game so variable reuse lesgo

        balloon.currentEnergy = pocketHeatEnergy + usedEnergy

        gasMass.forEach {
            KelvinMod.getKelvin().removeGas(getDuctNodePosition(), it.key,usedUpMass * it.value / gasMassTotal)
        }

        sendData()
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
        ClockworkLang.translate("gui.gas_nozzle.info.title").forGoggles((tooltip as MutableList))

        if (!level.isBlockInShipyard(blockPos.x, blockPos.y, blockPos.z)) {
            ClockworkTooltipHelper.addHint(tooltip, "gui.gas_nozzle.info.no_ship", ChatFormatting.GOLD)
        }
        if (!hasPocket || pocketTemperature.isNaN()) {
            ClockworkTooltipHelper.addHint(
                tooltip, "gui.gas_nozzle.info.no_pocket", ChatFormatting.GOLD, 0, 0,
                Minecraft.getInstance().options.keyUse.translatedKeyMessage
            )
        } else {
            ClockworkLang.translate(
                "gui.gas_nozzle.info.volume",
                DuctTextUtil.translateVolume(ClockworkLang.builder(), balloonVolume, true)
            ).style(ChatFormatting.GREEN).forGoggles(tooltip)
            ClockworkLang.translate(
                "gui.gas_nozzle.info.temperature",
                DuctTextUtil.translateTemperature(ClockworkLang.builder(), pocketTemperature, true)
            ).style(ChatFormatting.GOLD).forGoggles(tooltip)

            if (isPlayerSneaking) {
                val targetTemperature = ClockworkModClient.getKelvin().getTemperatureAt(getDuctNodePosition()) * pointer.value.toDouble()
                ClockworkLang.translate(
                    "gui.gas_nozzle.info.target_temperature",
                    DuctTextUtil.translateTemperature(ClockworkLang.builder(), targetTemperature, true)
                ).style(ChatFormatting.YELLOW).style(ChatFormatting.ITALIC).forGoggles(tooltip)
            }
            if (currentIdealOutput.toInt() != 0) {
                tooltip.add(CommonComponents.EMPTY)
                ClockworkTooltipHelper.addHint(tooltip, "gui.gas_nozzle.info.leak", ChatFormatting.RED, -2)
                ClockworkLang
                    .translate("gui.gas_nozzle.info.leak.count", currentIdealOutput.roundToInt())
                    .style(ChatFormatting.RED)
                    .forGoggles(tooltip)
            }
        }
        tooltip.add(CommonComponents.EMPTY)

        return super.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }
}
