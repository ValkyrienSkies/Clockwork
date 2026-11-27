package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.createmod.catnip.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity

class GasBacktankBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state) {

    val gaugeNeedle: LerpedFloat = LerpedFloat.linear().startWithValue(0.0)
    var clientPressure: Double = 0.0
        private set
    private var needleInitialized = false

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun tick() {
        super.tick()
        val level = level ?: return
        if (level.isClientSide) {
            val pressure = GasBacktankRenderer.getClientPressure(level, getDuctNodePosition())
            if (pressure != null) {
                clientPressure = pressure
                val target = GasBacktankGauge.fillFromPressure(pressure).toDouble()
                if (!needleInitialized) {
                    gaugeNeedle.setValue(target)
                    needleInitialized = true
                } else {
                    gaugeNeedle.chase(target, 0.25, LerpedFloat.Chaser.EXP)
                    gaugeNeedle.tickChaser()
                }
            } else if (needleInitialized) {
                clientPressure = 0.0
                gaugeNeedle.chase(0.0, 0.25, LerpedFloat.Chaser.EXP)
                gaugeNeedle.tickChaser()
            }
        }
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
    }

}
