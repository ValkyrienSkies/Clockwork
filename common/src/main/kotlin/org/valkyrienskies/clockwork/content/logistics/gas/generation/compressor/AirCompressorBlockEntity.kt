package org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor

import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkConfig;
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.KNodeKineticBlockEntity
import org.valkyrienskies.clockwork.util.gui.ClockworkTooltipHelper
import org.valkyrienskies.clockwork.util.gui.DuctTextUtil
import org.valkyrienskies.core.api.util.AerodynamicUtils
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.api.shipWorld
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.vsCore
import kotlin.math.abs
import kotlin.math.max

class AirCompressorBlockEntity(typeIn: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeKineticBlockEntity(typeIn, pos, state) {
    enum class CompressorStatus {
        ACTIVE,
        INACTIVE,
        EXHAUSTED,
        OBSTRUCTED
    }
    var status: CompressorStatus = CompressorStatus.INACTIVE
        set(value) { if (field != value) { field = value; sendData() } }

    var clientParticles: Boolean = false
    var clientSize: Float = 0.0f

    private val airGas = GasTypeRegistry.getGasType("kelvin", "air")
    private val heliumGas = GasTypeRegistry.getGasType("vs_clockwork", "aether")

    val active get() = status == CompressorStatus.ACTIVE
    val exhausted get() = status == CompressorStatus.EXHAUSTED
    val obstructed get() = status == CompressorStatus.OBSTRUCTED

    fun getAirDensity(): Double {
        val position = level.toWorldCoordinates(blockPos)
        return vsCore.dummyShipWorldServer.aerodynamicUtils.getAirDensityForY(position.y, level!!.dimensionId)
    }

    fun getAirTemperature(): Double {
        val position = level.toWorldCoordinates(blockPos)
        return vsCore.dummyShipWorldServer.aerodynamicUtils.getAirTemperatureForY(position.y, level!!.dimensionId)
    }

    override fun tick() {
        super.tick()

        if (airGas == null || heliumGas == null) return ClockworkMod.LOGGER.error("Could not get GasType `kelvin:air` or `vs_clockwork:aether`. Is Gas Registry broken?")

        if (level!!.isClientSide) return

        val kelvin = ClockworkMod.getKelvin()
        kelvin.getNodeAt(getDuctNodePosition()) ?: return

        val speed = abs(getSpeed())
        if (level!!.getBlockState(blockPos.above()).isAir) {
            if (speed > 0) {
                // Should this depend on actual air pressure outside? If we are in vacuum,
                // no way this compressor thing will still pump 10 ground atmospheres.
                // Maybe something airCompressorMaxPressureFactor instead?
                if (kelvin.getPressureAt(getDuctNodePosition()) < ClockworkConfig.SERVER.airCompressorMaxPressure) {
                    status = CompressorStatus.ACTIVE
                    // Future: proper API for defining atmospheric composition?
                    val heliumShare = max(
                        0.0,
                        1 - getAirDensity() / ClockworkConfig.SERVER.airCompressorHeliumAirDensity
                    )
                    val deltaVolume = ClockworkConfig.SERVER.airCompressorSpeed * speed
                    kelvin.addGasAtTemperature(
                        getDuctNodePosition(),airGas, (1 - heliumShare) * deltaVolume, getAirTemperature()
                    )
                    kelvin.addGasAtTemperature(
                        getDuctNodePosition(),heliumGas, heliumShare * deltaVolume, getAirTemperature()
                    )
                } else status = CompressorStatus.EXHAUSTED
            } else status = CompressorStatus.INACTIVE
        } else status = CompressorStatus.OBSTRUCTED
    }

    override fun addToGoggleTooltip(tooltip: List<Component>?, isPlayerSneaking: Boolean): Boolean {
        if (status != CompressorStatus.INACTIVE) {
            ClockworkLang.translate("gui.air_compressor.info.title").forGoggles((tooltip as MutableList))
            when (status) {
                CompressorStatus.OBSTRUCTED -> {
                    ClockworkTooltipHelper.addHint(tooltip, "gui.air_compressor.info.obstructed")
                }
                CompressorStatus.EXHAUSTED -> {
                    ClockworkTooltipHelper.addHint(tooltip, "gui.air_compressor.info.exhausted")
                }
                CompressorStatus.ACTIVE -> {
                    ClockworkLang
                        .translate("gui.air_compressor.info.active.title").style(ChatFormatting.GREEN)
                        .forGoggles(tooltip)
                    ClockworkLang
                        .translate(
                            "gui.air_compressor.info.active",
                            DuctTextUtil.translateTemperature(
                                ClockworkLang.builder(), getAirTemperature(), true
                            ).style(ChatFormatting.GOLD)
                        ).style(ChatFormatting.GRAY)
                        .forGoggles(tooltip)
                }
                else -> {}
            }
            tooltip.add(CommonComponents.EMPTY)
        }
        return super.addToGoggleTooltip(tooltip, isPlayerSneaking)
    }

    override fun remove() {
        super.remove()
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        status = CompressorStatus.valueOf(tag.getString("status"))
        super.read(tag, clientPacket)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.putString("status", status.toString())
        super.write(tag, clientPacket)
    }

}
