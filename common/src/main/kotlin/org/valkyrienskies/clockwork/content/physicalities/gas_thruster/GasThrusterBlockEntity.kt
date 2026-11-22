package org.valkyrienskies.clockwork.content.physicalities.gas_thruster

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautForceTelemetry
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautForceSource
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautForceAccumulator
import net.minecraft.server.level.ServerLevel
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.clockwork.ClockworkSoundScapes
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.util.AerodynamicUtils
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.internal.world.VsiServerShipWorld
import org.valkyrienskies.clockwork.util.kelvin.KelvinParticleHelper
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.GasPhysics
import org.valkyrienskies.kelvin.util.GasPhysics.mixtureCapacity
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.*

@OptIn(VsBeta::class)
class GasThrusterBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state), BlockEntityPhysicsListener, AeronautForceSource {

    companion object {
        private const val TICK_SECONDS = 0.05 // 20 game ticks per second
        private const val DISCHARGE_COEFFICIENT = 0.8
    }

    @Volatile
    override lateinit var dimension: DimensionId

    @Volatile
    var thrust = 0.0
    var velocity = 0.0

    val gasMassFlow = HashMap<GasType, Double>()
    val massPerParticle = 0.005

    override fun write(tag: CompoundTag, clientPacket: Boolean) {

        val compound = CompoundTag()
        for ((gas, mass) in gasMassFlow) {
            compound.putDouble(gas.resourceLocation.toString(), mass)
        }
        tag.put("GasMassFlow", compound)
        tag.putDouble("FlowRate",velocity)
        tag.putDouble("Thrust",thrust)
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {

        super.read(tag, clientPacket)
        val compound = tag.get("GasMassFlow") as? CompoundTag ?: return
        for (key in compound.allKeys) {
            val gasType = GasTypeRegistry.getGasType(ResourceLocation(key)) ?: continue
            gasMassFlow[gasType] = compound.getDouble(key)
        }

        velocity = tag.getDouble("FlowRate")
        thrust = tag.getDouble("Thrust")

    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun getDuctNodePosition(): DuctNodePos {
        if (level != null && !level!!.isClientSide()) {
            return blockPos.toDuctNodePos(level!!.dimension().location())
        }
        return blockPos.toDuctNodePos()
    }

    fun clientTick() {
        if (gasMassFlow.isEmpty() || velocity == 0.0) return

        // Handle audio
        val pitch = 60f / cbrt(velocity).toFloat()
        val scape = ClockworkSoundScapes.AmbienceGroup.THRUSTER
        ClockworkSoundScapes.play(scape, this.worldPosition, pitch)


        // Handle particles
        val direction = blockState.getValue(BlockStateProperties.FACING)
        val outward = direction.opposite
        val speed = cbrt(abs(velocity)) / 50
        val center = blockPos.toJOMLD().add(0.5, 0.5, 0.5)
        val ductNodePos = blockPos.toDuctNodePos(level!!.dimension().location())

        for ((gas, mass) in gasMassFlow) {
            val particleCount = mass / massPerParticle
            KelvinParticleHelper.spawnJetForGas(
                level as ClientLevel,
                ductNodePos,
                gas,
                center,
                outward,
                speed,
                particleCount
            )
        }
    }

    fun clearMassFlow() {
        if (gasMassFlow.isEmpty() && velocity == 0.0 && thrust == 0.0) return
        gasMassFlow.clear()
        velocity = 0.0
        thrust = 0.0
        sendData()
    }

    @OptIn(GameTickOnly::class)
    override fun tick() {
        super.tick()
        val currentLevel = level ?: return clearMassFlow()
        if (currentLevel.isClientSide) return clientTick()

        val ductNodePos = getDuctNodePosition()
        val kelvin = ClockworkMod.getKelvin(currentLevel)
        if (kelvin.getNodeAt(ductNodePos) == null) return clearMassFlow()

        val gasMasses = kelvin.getGasMassAt(ductNodePos).filterValues { it.isFinite() && it > 0.0 }
        val availableMass = gasMasses.values.sum()
        if (availableMass <= 0.0 || !availableMass.isFinite()) return clearMassFlow()

        val ambientPressure = (currentLevel.shipObjectWorld as? VsiServerShipWorld)
            ?.aerodynamicUtils
            ?.getAirPressureForY(blockPos.y.toDouble(), currentLevel.dimensionId)
            ?: return clearMassFlow()
        val chamberPressure = kelvin.getPressureAt(ductNodePos)
        val chamberTemperature = kelvin.getTemperatureAt(ductNodePos)

        if (!ambientPressure.isFinite() || !chamberPressure.isFinite() || !chamberTemperature.isFinite() ||
            ambientPressure < 0.0 || chamberPressure <= ambientPressure || chamberTemperature <= 0.0
        ) return clearMassFlow()

        val gamma = GasPhysics.gammaMix(gasMasses)
        val specificGasConstant = GasPhysics.mixtureR(gasMasses)
        if (!gamma.isFinite() || !specificGasConstant.isFinite() ||
            gamma <= 1.0 || specificGasConstant <= 0.0
        ) return clearMassFlow()

        val nozzleArea = ClockworkConstants.Misc.DUCT_AREA
        if (!nozzleArea.isFinite() || nozzleArea <= 0.0) return clearMassFlow()

        val criticalRatio = (2.0 / (gamma + 1.0)).pow(gamma / (gamma - 1.0))
        val ambientRatio = (ambientPressure / chamberPressure).coerceIn(0.0, 1.0)
        val choked = ambientRatio <= criticalRatio
        val exitPressure = if (choked) chamberPressure * criticalRatio else ambientPressure
        val exitRatio = (exitPressure / chamberPressure).coerceIn(0.0, 1.0)

        val theoreticalMassFlow = if (choked) {
            GasPhysics.mdotChoked(
                gasMasses, chamberPressure, chamberTemperature,
                sqrt(nozzleArea / PI), DISCHARGE_COEFFICIENT
            )
        } else {
            val expansion = (2.0 * gamma / (specificGasConstant * chamberTemperature * (gamma - 1.0))) *
                (ambientRatio.pow(2.0 / gamma) - ambientRatio.pow((gamma + 1.0) / gamma))
            DISCHARGE_COEFFICIENT * nozzleArea * chamberPressure * sqrt(expansion.coerceAtLeast(0.0))
        }
        if (!theoreticalMassFlow.isFinite() || theoreticalMassFlow <= 0.0) return clearMassFlow()

        val massThisTick = min(theoreticalMassFlow * TICK_SECONDS, availableMass)
        if (massThisTick <= 0.0) return clearMassFlow()

        val energyPerMass = (2.0 * gamma / (gamma - 1.0)) * specificGasConstant * chamberTemperature *
            (1.0 - exitRatio.pow((gamma - 1.0) / gamma))
        val exhaustVelocity = sqrt(energyPerMass.coerceAtLeast(0.0))
        if (!exhaustVelocity.isFinite()) return clearMassFlow()

        gasMassFlow.clear()
        var expelledMass = 0.0
        for ((gas, available) in gasMasses) {
            val massToRemove = min(available, massThisTick * (available / availableMass))
            if (massToRemove <= 0.0) continue
            if (kelvin.removeGas(ductNodePos, gas, massToRemove)) {
                gasMassFlow[gas] = massToRemove
                expelledMass += massToRemove
            }
        }
        if (expelledMass <= 0.0) return clearMassFlow()

        val actualMassFlow = expelledMass / 0.05

        val flowFraction = (actualMassFlow / theoreticalMassFlow).coerceIn(0.0, 1.0)
        val pressureThrust = (exitPressure - ambientPressure) * nozzleArea * flowFraction
        val rawThrust = actualMassFlow * exhaustVelocity + pressureThrust

        velocity = exhaustVelocity
        thrust = (rawThrust * ClockworkConfig.SERVER.gasThruster.gasThrusterForceMul)
            .takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        sendData()
    }

    @OptIn(PhysTickOnly::class)
    override fun physTick(physShip: PhysShip?, physLevel: PhysLevel) {
        physShip?: return
        if (blockState.block !is GasThrusterBlock) return
        val force = blockState.getValue(BlockStateProperties.FACING).normal.toJOMLD().mul(thrust)
        physShip.applyModelForce(force, blockPos.toJOMLD().add(0.5,0.5,0.5))
        val source = "gas:${blockPos.asLong()}"
        val readout = AeronautForceTelemetry.begin(physShip.id, source)
        readout?.add(blockPos.toJOMLD().add(0.5, 0.5, 0.5), force)
        AeronautForceTelemetry.publish(physShip.id, source, readout)
    }

    override fun addPotentialForces(level: ServerLevel, ship: LoadedServerShip, thrust: AeronautForceAccumulator) {
        // Gas jets have no nameplate thrust: available output depends on their gas supply.
        thrust.add(blockPos.toJOMLD().add(0.5, 0.5, 0.5),
            blockState.getValue(BlockStateProperties.FACING).normal.toJOMLD().mul(this.thrust))
    }



}
