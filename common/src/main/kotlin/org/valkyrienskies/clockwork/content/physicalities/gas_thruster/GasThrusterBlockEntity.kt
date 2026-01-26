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
import org.valkyrienskies.clockwork.ClockworkSoundScapes
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.KNodeBlockEntity
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.util.AerodynamicUtils
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.internal.world.VsiServerShipWorld
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.vsCore
import kotlin.math.*
import kotlin.random.Random

@OptIn(VsBeta::class)
class GasThrusterBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state), BlockEntityPhysicsListener {

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
        val ductNetwork = KelvinMod.KelvinClient
        for ((gas,mass) in gasMassFlow) {
            val particleCount = mass/massPerParticle
            val direction = blockState.getValue(BlockStateProperties.FACING)
            val speed = direction.normal.toJOMLD().mul(-cbrt(abs(velocity))/50)


            fun random() = Random.nextDouble(-0.35,0.35)
            val position = blockPos.toJOMLD().add(0.5, 0.5, 0.5)


            for (count in 1..particleCount.toInt()) {
                ductNetwork.createGasParticle(level as ClientLevel, gas, blockPos.toDuctNodePos(level!!.dimension().location()),
                    position.x+random(), position.y+random(), position.z+random(), speed.x, speed.y, speed.z)
            }
        }
    }

    fun clearMassFlow() {
        if (gasMassFlow.isEmpty()) return
        gasMassFlow.clear()
        velocity = 0.0
        thrust = 0.0
        sendData()
    }

    @OptIn(GameTickOnly::class)
    override fun tick() {
        super.tick()


        if (level!!.isClientSide) return clientTick()

        val ductnodepos = getDuctNodePosition()
        val kelvin = ClockworkMod.getKelvin()
        val node = kelvin.getNodeAt(ductnodepos) ?: return clearMassFlow()
        val gasMasses = kelvin.getGasMassAt(ductnodepos)

        if (gasMasses.values.sum() == 0.0) return clearMassFlow()

        val airPressure = (level?.shipObjectWorld as? VsiServerShipWorld)?.aerodynamicUtils?.getAirPressureForY(blockPos.y.toDouble(), level!!.dimensionId) ?: return clearMassFlow()
        val gasPressure = kelvin.getPressureAt(ductnodepos)
        val temp = kelvin.getTemperatureAt(ductnodepos)
        val avgSpecificHeat = kelvin.mixtureCapacity(kelvin.getGasMassAt(ductnodepos))


        if (gasPressure < airPressure) return clearMassFlow()

        velocity = 0.0

        for (edge in node.nodeEdges) {
            velocity += edge.currentFlowRate
        }

        val maxFlowRate = (ClockworkConstants.Misc.DUCT_AREA * gasPressure / sqrt(temp)) * sqrt(avgSpecificHeat/ AerodynamicUtils.UNIVERSAL_GAS_CONSTANT) * ((avgSpecificHeat+1)/2).pow(-(avgSpecificHeat+1)/(2*(avgSpecificHeat-1)))
        val flowRate = min(maxFlowRate, velocity)

        for (gas in gasMasses) {
            velocity += flowRate/(gas.key.density*ClockworkConstants.Misc.DUCT_AREA)

            val gasMassLoss = max(flowRate*0.05, gas.value)

            gasMassFlow[gas.key] = gasMassLoss
            kelvin.removeGas(ductnodepos, gas.key, gasMassLoss)
        }

        thrust = (flowRate * velocity + (gasPressure-airPressure)) * ClockworkConfig.SERVER.gasThrusterForceMul
        sendData()
    }

    @OptIn(PhysTickOnly::class)
    override fun physTick(physShip: PhysShip?, physLevel: PhysLevel) {
        physShip?: return
        if (blockState.block !is GasThrusterBlock) return
        val force = blockState.getValue(BlockStateProperties.FACING).normal.toJOMLD().mul(thrust)
        physShip.applyModelForce(force, blockPos.toJOMLD().add(0.5,0.5,0.5))
    }



}
