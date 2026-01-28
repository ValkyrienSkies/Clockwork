package org.valkyrienskies.clockwork.content.curiosities.altmeter

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3d
import org.valkyrienskies.mod.common.getShipManagingPos
import kotlin.math.absoluteValue

class AltMeterBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos, state: BlockState) :
    SmartBlockEntity(typeIn, pos, state) {
    internal var triggerHeight: Int = 0
    internal var triggerSensitivity: Int = 1
    internal var triggerDirection: AltMeterDirection = AltMeterDirection.UP;
    internal var signalStrength = 0
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    override fun tick() {
        super.tick()
        if (level!!.isClientSide) return

        val posInWorld = blockPos.toVector3d().add(.5, .5, .5)
        level.getShipManagingPos(blockPos)?.transform?.shipToWorld?.transformPosition(posInWorld)
        val distance = posInWorld.y - triggerHeight

        signalStrength = when (triggerDirection) {
            AltMeterDirection.BOTH -> (triggerSensitivity - distance.absoluteValue.toInt())
            AltMeterDirection.DOWN -> (triggerSensitivity - distance.toInt())
            AltMeterDirection.UP -> (triggerSensitivity + distance.toInt())
        }.coerceIn(0..triggerSensitivity)
            .toRange(0..triggerSensitivity, 0..15);

        val currentPower = blockState.getValue(AltMeterBlock.POWER)

        if (currentPower != signalStrength) {
            level!!.setBlock(blockPos, blockState
                .setValue(AltMeterBlock.POWER, signalStrength), 3)
        }
    }

    public override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)
        compound.putInt(ClockworkConstants.Nbt.TRIGGER_HEIGHT,triggerHeight)
        compound.putInt(ClockworkConstants.Nbt.TRIGGER_SENSITIVITY, triggerSensitivity)
        compound.putString(ClockworkConstants.Nbt.TRIGGER_DIRECTION, triggerDirection.name)
        compound.putInt("Signal Strength", signalStrength)
    }

    public override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        triggerHeight = compound.getInt(ClockworkConstants.Nbt.TRIGGER_HEIGHT)
        triggerSensitivity = compound.getInt(ClockworkConstants.Nbt.TRIGGER_SENSITIVITY)

        // v will be "" ONLY when pasting from a schematic
        var v = compound.getString(ClockworkConstants.Nbt.TRIGGER_DIRECTION)
        if (v == "") {
            v = AltMeterDirection.UP.name
        }

        triggerDirection = enumValueOf<AltMeterDirection>(v)
        signalStrength = compound.getInt("Signal Strength")
    }

    enum class AltMeterDirection {
        /**
         * Signal getting stronger bottom to top, reaching maximum on the set altitude and above
         */
        UP,
        /**
         * Signal getting stronger top to bottom, reaching maximum on the set altitude and below
         */
        DOWN,
        /**
         * Signal getting stronger from both ways, reaching maximum on the exact altitude
         */
        BOTH;

    }

    fun Int.toRange(source: IntRange, target: IntRange): Int {
        return (toFloat() / (source.last - source.first) * (target.last - target.first)).toInt()
    }
}
