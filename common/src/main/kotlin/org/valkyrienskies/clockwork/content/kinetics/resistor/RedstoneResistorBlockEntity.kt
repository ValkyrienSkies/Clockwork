package org.valkyrienskies.clockwork.content.kinetics.resistor

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation
import com.simibubi.create.content.contraptions.DirectionalExtenderScrollOptionSlot
import com.simibubi.create.content.kinetics.RotationPropagator
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlock
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.utility.CreateLang
import dev.engine_room.flywheel.lib.transform.TransformStack
import net.createmod.catnip.data.Iterate
import net.createmod.catnip.lang.Lang
import net.createmod.catnip.math.AngleHelper
import net.createmod.catnip.math.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.minecraft.world.ticks.TickPriority
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity.RotationDirection
import org.valkyrienskies.clockwork.util.ClockworkConstants
import kotlin.math.abs

open class RedstoneResistorBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) :
    SplitShaftBlockEntity(type, pos, state), IHaveGoggleInformation {
    var state = 0
    var lastChange = 0
    lateinit var resistDirection: ScrollOptionBehaviour<ResistDirection>
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
                val i = if (resistDirection.get() == ResistDirection.MAX_TO_NONE) {
                    abs(state - 15) / 15f
                } else {
                    abs(state) / 15f
                }
                return i
            }
        }
        return 1f
    }

    //add behavior for resistor direction
    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour?>) {
        super.addBehaviours(behaviours)
        resistDirection = ScrollOptionBehaviour(
            ResistDirection::class.java,
            ClockworkLang.translateDirect("contraptions.resistor.resist_direction"), this,
            ResistorValueBox()
        )
        behaviours.add(resistDirection)
    }

    //resistor direction enum
    enum class ResistDirection(private val icon: AllIcons) : INamedIconOptions {
        MAX_TO_NONE(AllIcons.I_REFRESH),
        NONE_TO_MAX(AllIcons.I_ROTATE_CCW); //don't know what else to put, "0 RPM to 256 RPM" is pretty long and not even accurate most the time.

        private val translationKey: String = "resistor.resist_direction." + Lang.asId(name)

        override fun getIcon(): AllIcons {
            return icon
        }

        override fun getTranslationKey(): String {
            return translationKey
        }
    }

    //value box class for scroll behavior
    class ResistorValueBox : ValueBoxTransform.Sided() {
        override fun getSouthLocation(): Vec3? {
            return VecHelper.voxelSpace(8.0, 8.0, 16.0)
        }
        override fun rotate(level: LevelAccessor?, pos: BlockPos?, state: BlockState, ms: PoseStack?) {
            super.rotate(level, pos, state, ms)
            val axis = state.getValue(AbstractEncasedShaftBlock.AXIS)

            if (axis === Direction.Axis.Y) return
            if (side != Direction.UP) return //default rotation so ignore/return

            val horizontalAngle = when (axis) {
                Direction.Axis.X -> 90f
                Direction.Axis.Z -> 0f
                else -> return
            }

            TransformStack.of(ms)
                .rotateZDegrees(-horizontalAngle + 180f)
        }

        override fun isSideActive(state: BlockState, direction: Direction): Boolean {
            val axis = state.getValue(AbstractEncasedShaftBlock.AXIS)
            return direction.getAxis() !== axis
        }

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
