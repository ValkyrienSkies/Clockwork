package org.valkyrienskies.clockwork.content.physicalities.gas_thruster

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkSoundScapes
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.forces.GasThrusterController
import org.valkyrienskies.clockwork.content.generic.IForceApplierBE
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.data.GasThrusterCreateData
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.data.GasThrusterData
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.data.GasThrusterUpdateData
import org.valkyrienskies.clockwork.util.AerodynamicUtils
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.*
import kotlin.random.Random

class GasThrusterBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos, state), IHeatableBlockEntity, IForceApplierBE<GasThrusterUpdateData, GasThrusterData, GasThrusterCreateData, GasThrusterController> {

    override var physID: Int = -1
    var newForce = 0.0
    var velocity = 0.0

    val gasMassFlow = HashMap<GasType, Double>()
    val massPerParticle = 5

    override fun write(tag: CompoundTag, clientPacket: Boolean) {

        val compound = CompoundTag()
        for ((gas, mass) in gasMassFlow) {
            compound.putDouble(gas.resourceLocation.toString(), mass)
        }
        tag.put("GasMassFlow", compound)
        tag.putDouble("FlowRate",velocity)
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

    }

    override fun newCreateData(): GasThrusterCreateData {
        return GasThrusterCreateData(worldPosition.toJOML())
    }

    override fun newUpdateData(): GasThrusterUpdateData {
        return GasThrusterUpdateData(blockState.getValue(BlockStateProperties.FACING).normal.toJOMLD().mul(newForce))
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
        sendData()
    }

    override fun tick() {
        super.tick()


        if (level!!.isClientSide) return clientTick()

        val ductnodepos = getDuctNodePosition()
        val kelvin = ClockworkMod.getKelvin()
        val node = kelvin.getNodeAt(ductnodepos) ?: return clearMassFlow()
        val gasMasses = kelvin.getGasMassAt(ductnodepos)

        if (gasMasses.values.sum() == 0.0) return clearMassFlow()

        val airPressure = AerodynamicUtils.getAirPressureForY(blockPos.y.toDouble(), level.dimensionId!!)
        val gasPressure = kelvin.getPressureAt(ductnodepos)
        val temp = kelvin.getTemperatureAt(ductnodepos)
        val avgSpecificHeat = AerodynamicUtils.specificHeatAverage(kelvin.getGasMassAt(ductnodepos))


        if (gasPressure<airPressure) return clearMassFlow()

        velocity = 0.0

        for (edge in node.nodeEdges) {
            velocity += edge.currentFlowRate
        }

        val maxFlowRate = (AerodynamicUtils.DUCT_AREA * gasPressure / sqrt(temp)) * sqrt(avgSpecificHeat/AerodynamicUtils.UNIVERSAL_GAS_CONSTANT) * ((avgSpecificHeat+1)/2).pow(-(avgSpecificHeat+1)/(2*(avgSpecificHeat-1)))
        val flowRate = min(maxFlowRate, velocity)

        for (gas in gasMasses) {
            velocity += flowRate/(gas.key.density*AerodynamicUtils.DUCT_AREA)

            val gasMassLoss = max(flowRate*0.05, gas.value)

            gasMassFlow[gas.key] = gasMassLoss
            kelvin.modGasMass(ductnodepos, gas.key, -gasMassLoss)
        }
        sendData()

        val prevForce = newForce
        val thrust = flowRate * velocity + (gasPressure-airPressure)
        val force = blockState.getValue(DirectionalBlock.FACING).normal.toJOMLD().mul(thrust)
        newForce = force.length()
        val serverLevel = level as? ServerLevel ?: return
        val ship = serverLevel.getShipObjectManagingPos(blockPos) ?: return
        val controller = GasThrusterController.getOrCreate(ship) ?: return
        tickData(controller, newForce != prevForce)

    }


    override fun remove() {
        super.remove()
        if (level!!.isClientSide) return
        val serverLevel = level as ServerLevel? ?: return
        removeApplier(GasThrusterController::class.java, serverLevel, worldPosition)
    }

    override fun destroy() {
        super.destroy()
        if (level!!.isClientSide) return
        val serverLevel = level as ServerLevel? ?: return
        removeApplier(GasThrusterController::class.java, serverLevel, worldPosition)
    }


}