package org.valkyrienskies.clockwork.content.contraptions.phys.speed_gauge

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.Components
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.ChatFormatting
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlock
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.mod.common.getShipManagingPos
import kotlin.math.max
import kotlin.math.min

class SpeedGaugeBlockEntity(typeIn: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?): SmartBlockEntity(typeIn, pos, state), IHaveGoggleInformation {

    var current = 0.0
    var target = 0.0
    var speed = 0.0

    private val maxSpeed = 100.0
    private val changeSpeed = 0.15

    internal var triggerSpeed: Double = 0.0
    internal var moreThan: Boolean = false

    override fun addToGoggleTooltip(tooltip: MutableList<Component?>, isPlayerSneaking: Boolean): Boolean {
        tooltip.add(Components.empty())
        tooltip.add(Components.literal("Speed:")
            .withStyle(ChatFormatting.GRAY))
        tooltip.add(Components.literal(getShipSpeed().toInt().toString()+" m/s")
            .withStyle(ChatFormatting.GOLD))


        return true
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun tick() {
        super.tick()

        //Smooth dial movement
        speed = getShipSpeed()
        target = speed/maxSpeed
        current +=  max(min(target-current,changeSpeed),-changeSpeed)

        var shouldBePowered = false
        val isCurrentlyPowered = blockState.getValue(SpeedGaugeBlock.POWERED)

        if ((moreThan && getShipSpeed()>triggerSpeed) || (!moreThan && getShipSpeed()<triggerSpeed) ) {
            shouldBePowered = true
        }

        if (shouldBePowered != isCurrentlyPowered) {
            if (shouldBePowered) {
                // Same flags as a redstone torch update
                level!!.setBlock(blockPos, blockState.setValue(AltMeterBlock.POWERED, true), 3)
            } else {
                level!!.setBlock(blockPos, blockState.setValue(AltMeterBlock.POWERED, false), 3)
            }
        }

    }



    fun getShipSpeed(): Double {
        var ship = level.getShipManagingPos(blockPos)
        if (ship != null) {
            return ship.velocity.length()
        }
        return 0.0
    }



    public override fun write(compound: CompoundTag, clientPacket: Boolean) {
        super.write(compound, clientPacket)
        val triggerSpeedCopy = triggerSpeed
        val moreThanCopy = moreThan
        compound.putDouble(ClockworkConstants.Nbt.TRIGGER_SPEED, triggerSpeedCopy)
        compound.putBoolean(ClockworkConstants.Nbt.MORE_THAN, moreThanCopy)
    }

    public override fun read(compound: CompoundTag, clientPacket: Boolean) {
        super.read(compound, clientPacket)
        triggerSpeed = compound.getDouble(ClockworkConstants.Nbt.TRIGGER_SPEED)
        moreThan = compound.getBoolean(ClockworkConstants.Nbt.MORE_THAN)
    }

}