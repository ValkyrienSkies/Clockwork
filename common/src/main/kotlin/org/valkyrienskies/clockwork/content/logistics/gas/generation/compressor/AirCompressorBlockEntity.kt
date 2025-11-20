package org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor

import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkConfig;
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.AerodynamicUtils
import org.valkyrienskies.clockwork.util.KNodeKineticBlockEntity
import org.valkyrienskies.core.impl.shadow.Do
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.api.getShipManagingBlock
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class AirCompressorBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeKineticBlockEntity(typeIn, pos, state) {
    var isActivated: Boolean = false
        private set

    var clientParticles: Boolean = false
    var clientSize: Float = 0.0f

    private val airGas = GasTypeRegistry.getGasType("kelvin", "air")
    private val heliumGas = GasTypeRegistry.getGasType("kelvin", "helium")

    fun getAirDensity(): Double {

        val ship = level.getShipManagingBlock(blockPos)
        val position = ship?.transform?.toWorld?.transformPosition(blockPos.toJOMLD()) ?: blockPos.toJOMLD()

        return AerodynamicUtils.getAirDensityForY(position.y, level!!.dimensionId)
    }

    fun getAirTemperature(): Double {
        val ship = level.getShipManagingBlock(blockPos)
        val position = ship?.transform?.toWorld?.transformPosition(blockPos.toJOMLD()) ?: blockPos.toJOMLD()

        return AerodynamicUtils.getAirTemperatureForY(position.y, level!!.dimensionId)
    }

    override fun tick() {
        super.tick()

        if (airGas == null || heliumGas == null) return ClockworkMod.LOGGER.error("Could not get GasType `kelvin:air` or `kelvin:helium`. Is Gas Registry broken?")

        if (level!!.isClientSide) return

        val kelvin = ClockworkMod.getKelvin()
        kelvin.getNodeAt(getDuctNodePosition()) ?: return

        val pressure = kelvin.getPressureAt(getDuctNodePosition())
        val speed = abs(getSpeed())

        if (speed>0 && pressure< ClockworkConfig.SERVER.airCompressorMaxPressure) {
            if (!isActivated) {
                isActivated = true
                sendData()
            }


            val heliumShare = max(0.0, (ClockworkConfig.SERVER.airCompressorHeliumAirDensity - getAirDensity())) / ClockworkConfig.SERVER.airCompressorHeliumAirDensity
            val deltaVolume = ClockworkConfig.SERVER.airCompressorSpeed*speed

            kelvin.modGasMassOfTemperature(getDuctNodePosition(),airGas, (1-heliumShare)*deltaVolume, getAirTemperature())
            kelvin.modGasMassOfTemperature(getDuctNodePosition(),heliumGas, heliumShare*deltaVolume, getAirTemperature())
        } else if (isActivated) {
            isActivated = false;
            sendData()
        }
    }

    override fun remove() {
        super.remove()
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        isActivated = tag.getBoolean("isActivated")
        super.read(tag, clientPacket)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.putBoolean("isActivated",isActivated)
        super.write(tag, clientPacket)
    }

}