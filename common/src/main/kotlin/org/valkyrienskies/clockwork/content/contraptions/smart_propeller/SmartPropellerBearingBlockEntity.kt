package org.valkyrienskies.clockwork.content.contraptions.smart_propeller

import com.simibubi.create.AllSoundEvents
import com.simibubi.create.AllTags
import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.advancement.AllAdvancements
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.ServerSpeedProvider
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.contraptions.propeller.contraption.PropellerContraption
import org.valkyrienskies.clockwork.content.forces.SmartPropellerController
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.abs

class SmartPropellerBearingBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KineticBlockEntity(type, pos, state), IBearingBlockEntity {

    private var sailPositions: MutableList<BlockPos> = ArrayList()
    private var sails: Int = 0

    private var rotSpeed: Float = 0f
    private var running: Boolean = false
    private var assembleNextTick: Boolean = false
    private var isInverted: Boolean = false
    private var physPropId: Int? = null

    private var clientAngleDiff: Float = 0f

    private var angle: Float = 0f
    private var prevAngle: Float = 0f

    private var lastException: AssemblyException? = null
    private var movedContraption: ControlledContraptionEntity? = null

    override fun tick() {
        super.tick()

        prevAngle = angle
        if (level!!.isClientSide) clientAngleDiff /= 2f

        if (!level!!.isClientSide && assembleNextTick) {
            assembleNextTick = false
            if (running) {
                val canDisassemble =isNearInitialAngle()

                if (speed == 0f && (canDisassemble || movedContraption == null || movedContraption!!.contraption.blocks.isEmpty())) {
                    if (movedContraption != null) movedContraption!!.contraption.stop(level)
                    disassemble()
                    return
                }
            } else {
                if (speed == 0f) return
                assemble()
            }
        }

        if (!running) return

        if (!(movedContraption != null && movedContraption!!.isStalled)) {
            val angularSpeed = getAngularSpeed()

            val newAngle = angle + angularSpeed
            angle = (newAngle % 360)
        }

        applyRotation()
    }

    private fun assemble() {
        if (level!!.getBlockState(worldPosition).block !is SmartPropellerBearingBlock) return

        val direction = blockState.getValue(BearingBlock.FACING)
        val contraption = SmartPropellerContraption(direction)
        try {
            if (!contraption.assemble(level, worldPosition)) return

            lastException = null
        } catch (e: AssemblyException) {
            lastException = e
            sendData()
            return
        }

        val anchor = worldPosition.relative(direction)
        contraption.removeBlocksFromWorld(level, BlockPos.ZERO)
        movedContraption = ControlledContraptionEntity.create(level, this, contraption)
        movedContraption!!.setPos(anchor.x.toDouble(), anchor.y.toDouble(), anchor.z.toDouble())
        movedContraption!!.rotationAxis = direction.axis
        movedContraption?.let { level!!.addFreshEntity(it) }

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition)

        if (contraption.containsBlockBreakers()) award(AllAdvancements.CONTRAPTION_ACTORS)

        running = true
        angle = 0f

        if (!level!!.isClientSide) {
            val ship = (level as ServerLevel).getShipObjectManagingPos(
                blockPos
            )
            if (ship != null) {

                getSails()
                val sailVectors = sailPositions.stream().map { v: BlockPos -> v.toJOML() }.toList()
                val bearingAxis: Vector3dc = blockState.getValue(BlockStateProperties.FACING).normal.toJOMLD()
                val bearingPos: Vector3dc = blockPos.toJOMLD()

                val data = SmartPropData(
                    bearingPos,
                    bearingAxis,
                    angle.toDouble(),
                    getAngularSpeed().toDouble(),
                    sailVectors,
                    isInverted,
                    overStressed
                )

                physPropId = SmartPropellerController.getOrCreate(ship)!!.addPropeller(data)
            }
        }
        sendData()
    }

    private fun getSails() {
        sailPositions = ArrayList()
        if (movedContraption != null) {
            val Blocks = movedContraption!!.contraption.blocks
            for ((key, value) in Blocks) {
                if (AllTags.AllBlockTags.WINDMILL_SAILS.matches(value.state)) {
                    sailPositions.add(key)
                }
            }
        }
    }

    private fun disassemble() {

    }

    private fun applyRotation() {

    }

    fun isNearInitialAngle(): Boolean {
        return abs(angle.toDouble()) < 22.5 || abs(angle.toDouble()) > 360 - 22.5
    }

    override fun lazyTick() {
        super.lazyTick()
        if (movedContraption != null && !level!!.isClientSide) sendData()
    }

    override fun attach(contraption: ControlledContraptionEntity) {
        val blockState = blockState
        if (contraption.contraption !is PropellerContraption) return
        if (!blockState.hasProperty(BearingBlock.FACING)) return
        movedContraption = contraption
        setChanged()
        val anchor = worldPosition.relative(blockState.getValue(BearingBlock.FACING))
        movedContraption!!.setPos(anchor.x.toDouble(), anchor.y.toDouble(), anchor.z.toDouble())
        if (!level!!.isClientSide) {
            running = true
            sendData()
        }
    }

    override fun getInterpolatedAngle(partialTicks: Float): Float {
        var partialTicksOut = partialTicks

        if (isVirtual) return Mth.lerp(partialTicksOut + .5f, prevAngle, angle)
        if (movedContraption == null || movedContraption!!.isStalled || !running) partialTicksOut = 0f
        val angularSpeed: Float = getAngularSpeed()
        return Mth.lerp(partialTicksOut, angle, angle + angularSpeed)
    }

    override fun isWoodenTop(): Boolean {
        return false
    }

    override fun setAngle(forcedAngle: Float) {
        angle = forcedAngle
    }

    private fun getAngularSpeed(): Float {
        var speed = convertToAngular(getSpeed())
        if (getSpeed() == 0f) speed = 0f
        if (level!!.isClientSide) {
            speed *= ServerSpeedProvider.get()
            speed += clientAngleDiff / 3f
        }
        return speed
    }

    public override fun write(compound: CompoundTag, clientPacket: Boolean) {
        compound.putFloat(ClockworkConstants.Nbt.ROT_SPEED, rotSpeed)
        compound.putBoolean(ClockworkConstants.Nbt.RUNNING, running)
        compound.putFloat(ClockworkConstants.Nbt.ANGLE, angle)
        compound.putBoolean(ClockworkConstants.Nbt.INVERTED, isInverted)
        if (physPropId != null) {
            compound.putInt(ClockworkConstants.Nbt.ID, physPropId!!)
        }
        AssemblyException.write(compound, lastException)
        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        val angleBefore = angle
        rotSpeed = compound.getFloat(ClockworkConstants.Nbt.ROT_SPEED)
        running = compound.getBoolean(ClockworkConstants.Nbt.RUNNING)
        angle = compound.getFloat(ClockworkConstants.Nbt.ANGLE)
        isInverted = compound.getBoolean(ClockworkConstants.Nbt.INVERTED)
        lastException = AssemblyException.read(compound)

        if (compound.contains(ClockworkConstants.Nbt.ID)) {
            physPropId = compound.getInt(ClockworkConstants.Nbt.ID)
        }
        super.read(compound, clientPacket)
        if (!clientPacket) return
        if (running) {
            clientAngleDiff = AngleHelper.getShortestAngleDiff(angleBefore.toDouble(), angle.toDouble())
            angle = angleBefore
        } else {
            movedContraption = null
        }
    }

    override fun isAttachedTo(contraption: AbstractContraptionEntity): Boolean {
        return if (contraption.contraption !is SmartPropellerContraption) false else movedContraption === contraption
    }

    override fun onStall() {
        if (!level!!.isClientSide) sendData()
    }

    override fun isValid(): Boolean {
        return !isRemoved
    }

    override fun getBlockPosition(): BlockPos {
        return worldPosition
    }
}