package org.valkyrienskies.clockwork.content.physicalities.ballast

import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.ContainerHelper
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.WorldlyContainerHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.entity.BarrelBlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.entity.DispenserBlockEntity
import net.minecraft.world.level.block.entity.HopperBlockEntity
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderWandItem
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.ClockworkUtils

abstract class BallastBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) : SmartBlockEntity(type, pos, state) {

    @JvmField
    var inventory: Any? = null
    @JvmField
    var recalculateWeightNextTick = true

    @JvmField
    var oldWeight = 0.0
    @JvmField
    var newWeight = 0.0


    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {

    }

    override fun tick() {
        super.tick()

        if (recalculateWeightNextTick) {
            recalculateWeightNextTick = false

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

        this.oldWeight = compound.getDouble(ClockworkConstants.Nbt.OLD_WEIGHT)
        this.newWeight = compound.getDouble(ClockworkConstants.Nbt.WEIGHT)
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)

        compound.putDouble(ClockworkConstants.Nbt.OLD_WEIGHT, this.oldWeight)
        compound.putDouble(ClockworkConstants.Nbt.WEIGHT, this.newWeight)
    }

    override fun setChanged() {
        recalculateWeightNextTick = true
        super.setChanged()
    }
}