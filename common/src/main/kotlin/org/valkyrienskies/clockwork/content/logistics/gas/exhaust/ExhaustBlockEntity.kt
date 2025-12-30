package org.valkyrienskies.clockwork.content.logistics.gas.exhaust

import com.simibubi.create.content.kinetics.fan.AirCurrent
import com.simibubi.create.content.kinetics.fan.IAirCurrentSource
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.infrastructure.config.AllConfigs
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.mixinduck.MixinAirCurrentDuck
import org.valkyrienskies.clockwork.util.KNodeBlockEntity
import org.valkyrienskies.clockwork.util.KelvinParticleHelper
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.pow

class ExhaustBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state),
    IAirCurrentSource {

    val MASS_PER_EXHAUST = 0.0005
    // Airflow speed parameters cannot be easily made configurable because air current code runs both on server and client
    // so values need to somehow be synced.
    val MAX_AIRFLOW_SPEED = 256F // like a maxed out encased fan with default max rpm cap
    val PRESSURE_TO_SPEED = 256F / 2500 // outflow pressure in exhausts is very low compared to thrusters, may need tweaking later

    @JvmField
    var airCurrent: AirCurrent? = null

    var airCurrentUpdateCooldown: Int = 0
    var entitySearchCooldown: Int = 0
    var updateAirFlow: Boolean = false

    init {
        airCurrent = AirCurrent(this)
        updateAirFlow = true
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    fun randomPos(deviation: Double, random: RandomSource): Double {
        return (0.5-deviation/2.0)+random.nextDouble()*deviation
    }

    override fun tick() {
        super.tick()

        // Kelvin behavior
        val network = if (level?.isClientSide != true) {
            ClockworkMod.getKelvin()
        } else {
            ClockworkModClient.getKelvin()
        }
        val gasses = network.getGasMassAt(getDuctNodePosition())
        val pressure = network.getPressureAt(getDuctNodePosition())
        if (gasses.isEmpty()) return super.tick()

        if (level!!.isClientSide) {

            val state = level!!.getBlockState(blockPos)
            if (state.block !is ExhaustBlock) return
            val facing = state.getValue(BlockStateProperties.FACING)
            val random = level!!.random

            for (i in 1..floor(gasses.values.sum()/MASS_PER_EXHAUST).toInt()) {
                KelvinParticleHelper.spawnParticleWithRatio(level as ClientLevel, getDuctNodePosition(),
                    blockPos.toJOMLD().add(randomPos(0.3, random), randomPos(0.3, random), randomPos(0.3, random)),
                    facing.normal.toJOMLD().mul(Mth.clamp(0.0025 * pressure.pow(0.4), 0.1,5.0 )))
            }
            //println("PARTICLE PARTER ${floor(gasses.values.sum()/MASS_PER_EXHAUST).toInt()}")
        } else {
            for ((gas, value) in gasses) {
                network.removeGas(getDuctNodePosition(), gas, value)
            }
        }

        updateAirCurrent()
    }

    fun updateAirCurrent() {
        if (airCurrent == null) return

        val network = KelvinMod.getKelvinByPlatform() ?: return

        if (AllConfigs.server() != null && airCurrentUpdateCooldown-- <= 0) {
            airCurrentUpdateCooldown = AllConfigs.server().kinetics.fanBlockCheckRate.get();
            updateAirFlow = true;
        }

        if (updateAirFlow) {
            updateAirFlow = false;

            val temp = network.getTemperatureAt(getDuctNodePosition())
            val duck = (airCurrent as? MixinAirCurrentDuck)
            duck?.disableParticles(true)
            duck?.setOwnProcessingType(duck?.getProcessingTypeFor(temp))

            airCurrent?.rebuild();
            sendData();
        }

        if (speed == 0.0F)
            return;

        if (entitySearchCooldown-- <= 0) {
            entitySearchCooldown = 5;
            airCurrent!!.findEntities();
        }

        airCurrent!!.tick();
    }

    override fun getSpeed(): Float {
        val network = KelvinMod.getKelvinByPlatform() ?: return 0F
        val pressure = network.getPressureAt(getDuctNodePosition())
        return min(
            (pressure * PRESSURE_TO_SPEED).toFloat(),
            MAX_AIRFLOW_SPEED
        )
    }

    // Copied from Create.
    override fun getAirCurrent(): AirCurrent? {
        return airCurrent
    }

    override fun getAirCurrentWorld(): Level? {
        return level
    }

    override fun getAirCurrentPos(): BlockPos? {
        return worldPosition
    }

    override fun getAirflowOriginSide(): Direction? {
        return blockState.getValue(BlockStateProperties.FACING)
    }

    override fun getAirFlowDirection(): Direction? {
        return (if (speed == 0.0F) null else return blockState.getValue(BlockStateProperties.FACING))
    }

    override fun isSourceRemoved(): Boolean {
        return remove
    }
}
