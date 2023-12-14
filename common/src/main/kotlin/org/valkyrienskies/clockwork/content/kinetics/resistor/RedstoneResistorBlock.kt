package org.valkyrienskies.clockwork.content.kinetics.resistor

import com.simibubi.create.content.kinetics.RotationPropagator
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.RandomSource
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.ticks.TickPriority
import org.valkyrienskies.clockwork.ClockworkBlockEntities

open class RedstoneResistorBlock(properties: Properties) : AbstractEncasedShaftBlock(properties), IBE<RedstoneResistorBlockEntity> {


    fun detachKinetics(worldIn: Level, pos: BlockPos?, reAttachNextTick: Boolean) {
        val te = worldIn.getBlockEntity(pos)
        if (te == null || te !is KineticBlockEntity) return
        RotationPropagator.handleRemoved(worldIn, pos, te as KineticBlockEntity?)

        // Re-attach next tick
        if (reAttachNextTick) worldIn.scheduleTick(pos, this, 0, TickPriority.EXTREMELY_HIGH)
    }

    override fun tick(state: BlockState, level: ServerLevel, pos: BlockPos, random: RandomSource) {
        val te = level.getBlockEntity(pos)
        if (te == null || te !is KineticBlockEntity) return
        RotationPropagator.handleAdded(level, pos, te)
    }

    override fun getBlockEntityClass(): Class<RedstoneResistorBlockEntity> {
        return RedstoneResistorBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out RedstoneResistorBlockEntity> {
        return ClockworkBlockEntities.REDSTONE_RESISTOR.get()
    }
}
