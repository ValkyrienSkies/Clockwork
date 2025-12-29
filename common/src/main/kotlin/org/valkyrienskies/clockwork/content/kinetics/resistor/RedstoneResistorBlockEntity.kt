package org.valkyrienskies.clockwork.content.kinetics.resistor

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
import com.simibubi.create.content.kinetics.RotationPropagator
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity
import com.simibubi.create.foundation.utility.CreateLang
import net.createmod.catnip.data.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.ticks.TickPriority
import org.valkyrienskies.clockwork.util.ClockworkConstants
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.truncate

open class RedstoneResistorBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) :
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
        compound.putInt(ClockworkConstants.Nbt.REDSTONE_LEVEL, state)
        compound.putInt(ClockworkConstants.Nbt.CHANGE_TIMER, lastChange)
        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        state = compound.getInt(ClockworkConstants.Nbt.REDSTONE_LEVEL)
        lastChange = compound.getInt(ClockworkConstants.Nbt.CHANGE_TIMER)
        super.read(compound, clientPacket)
    }

    private fun getPower(worldIn: Level, pos: BlockPos): Int {
        var power = 0
        for (direction in Iterate.directions) power =
            worldIn.getSignal(pos.relative(direction), direction).coerceAtLeast(power)
        for (direction in Iterate.directions) power =
            worldIn.getSignal(pos.relative(direction), Direction.UP).coerceAtLeast(power)
        return power
    }

    override fun getRotationSpeedModifier(face: Direction): Float {
        if (hasSource()) {
            if (face != sourceFacing) {
                var i = abs(state - 15) / 15.0;
                // We only keep 1 decimal place of precision on the mult, because anymore and
                // create is stupid and fucks everything up with bad float math
                i = round(i*10.0) / 10.0
                return i.toFloat()
            }
        }
        return 1f
    }

    override fun addToGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        tooltip.add(
            CreateLang.translateDirect(
                "tooltip.analogStrength",
                state
            )
        )
        return true
    }
}
