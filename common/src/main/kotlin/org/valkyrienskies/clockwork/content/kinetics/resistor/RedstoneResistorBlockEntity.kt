package org.valkyrienskies.clockwork.content.kinetics.resistor

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation
import com.simibubi.create.content.kinetics.RotationPropagator
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity
import com.simibubi.create.foundation.utility.Iterate
import com.simibubi.create.foundation.utility.Lang
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.ticks.TickPriority

class RedstoneResistorBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    SplitShaftBlockEntity(type, pos, state), IHaveGoggleInformation {
    var state = 0
    var lastChange = 0
    override fun tick() {
        super.tick()
        lastChange = state
        state = getPower(level!!, worldPosition)
        if (state != lastChange) {
            detachKinetics()
        }
    }

    override fun detachKinetics() {
        RotationPropagator.handleRemoved(level, worldPosition, this)

        // Re-attach next tick
        level!!.scheduleTick(worldPosition, blockState.block, 0, TickPriority.EXTREMELY_HIGH)
    }

    public override fun write(compound: CompoundTag, clientPacket: Boolean) {
        compound.putInt("RedstoneLevel", state)
        compound.putInt("ChangeTimer", lastChange)
        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        state = compound.getInt("RedstoneLevel")
        lastChange = compound.getInt("ChangeTimer")
        super.read(compound, clientPacket)
    }

    private fun getPower(worldIn: Level, pos: BlockPos): Int {
        var power = 0
        for (direction in Iterate.directions) power =
            Math.max(worldIn.getSignal(pos.relative(direction), direction), power)
        for (direction in Iterate.directions) power =
            Math.max(worldIn.getSignal(pos.relative(direction), Direction.UP), power)
        return power
    }

    override fun getRotationSpeedModifier(face: Direction): Float {
        if (hasSource()) {
            if (face != sourceFacing) {
                return Math.abs(state - 15) / 15f
            }
        }
        return 1f
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        tooltip.add(
            componentSpacing.plainCopy().append(
                Lang.translateDirect(
                    "tooltip.analogStrength",
                    state
                )
            )
        )
        return true
    }
}