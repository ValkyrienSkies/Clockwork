package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.createmod.catnip.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import kotlin.math.abs

class GasBacktankBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {

    var gaugeFraction = 0f
        private set

    val needle: LerpedFloat = LerpedFloat.linear().startWithValue(0.0)
    private var needleInitialized = false
    private var gaugeUpdateTimer = 0

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun tick() {
        super.tick()
        val level = level ?: return
        if (level.isClientSide) {
            needle.tickChaser()
            return
        }
        if (++gaugeUpdateTimer < GAUGE_UPDATE_INTERVAL) return
        gaugeUpdateTimer = 0

        val fraction = computeGaugeFraction()
        val changed = abs(fraction - gaugeFraction) >= GAUGE_SYNC_THRESHOLD ||
            (fraction != gaugeFraction && (fraction == 0f || fraction == 1f))
        if (changed) {
            gaugeFraction = fraction
            sendData()
        }
    }

    private fun computeGaugeFraction(): Float {
        val maxPressure = (blockState.block as? IHaveDuctStats)?.getMaximumPressure() ?: GasBacktankBlock.MAX_PRESSURE
        val pressure = ClockworkMod.getKelvin(level).getPressureAt(getDuctNodePosition())
        return toGaugeFraction(pressure, maxPressure)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        tag.putFloat(GAUGE_TAG, gaugeFraction)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        gaugeFraction = tag.getFloat(GAUGE_TAG).coerceIn(0f, 1f)
        if (!clientPacket) return
        if (needleInitialized) {
            needle.chase(gaugeFraction.toDouble(), NEEDLE_CHASE_SPEED, LerpedFloat.Chaser.EXP)
        } else {
            needle.startWithValue(gaugeFraction.toDouble())
            needleInitialized = true
        }
    }

    companion object {
        private const val GAUGE_TAG = "Gauge"
        private const val GAUGE_UPDATE_INTERVAL = 5
        private const val GAUGE_SYNC_THRESHOLD = 1f / 180f
        private const val NEEDLE_CHASE_SPEED = 0.2

        fun toGaugeFraction(pressure: Double, maxPressure: Double): Float {
            if (!pressure.isFinite() || !maxPressure.isFinite() || maxPressure <= 0.0) return 0f
            return (pressure / maxPressure).coerceIn(0.0, 1.0).toFloat()
        }
    }
}
