package org.valkyrienskies.clockwork.content.physicalities.ballast

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.util.ClockworkUtils

abstract class BallastBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos, state) {

    @JvmField
    var inventory: Any? = null
    @JvmField
    var recalculateWeightNextTick = false

    @JvmField
    var oldWeight = 0.0
    @JvmField
    var newWeight = 0.0


    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {

    }

    override fun tick() {
        super.tick()

        if (recalculateWeightNextTick) {
            recalculateWeightNextTick = true

            if (this.level is ServerLevel) {
                val serverLevel = this.level as ServerLevel
                updateWeight()
                ClockworkUtils.updateBlockStateWeight(serverLevel, blockPos, oldWeight, newWeight)
            }
        }
    }

    abstract fun updateWeight()

    fun mapValue(oldValue: Int, oldMin: Int, oldMax: Int, newMin: Int, newMax: Int): Int {
        return ((oldValue - oldMin) * (newMax - newMin) / (oldMax - oldMin)) + newMin
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)

        this.oldWeight = compound.getDouble("OldWeight")
        this.newWeight = compound.getDouble("Weight")
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)

        compound.putDouble("OldWeight", this.oldWeight)
        compound.putDouble("Weight", this.newWeight)
    }

}