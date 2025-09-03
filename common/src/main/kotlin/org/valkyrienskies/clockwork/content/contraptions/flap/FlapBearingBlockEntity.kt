package org.valkyrienskies.clockwork.content.contraptions.flap

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.ClockworkBearingBlockEntity
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.utility.ServerSpeedProvider
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Axis
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.contraptions.flap.contraption.FlapContraption

open class FlapBearingBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState, val maxSize: Long = 16) :
    KineticBlockEntity(type, pos, state), IBearingBlockEntity, IDisplayAssemblyExceptions {

    var isRunning = false
        private set
    var assembleNextTick = false

    var movedContraption: ControlledContraptionEntity? = null
    var lastException: AssemblyException? = null

    private var bearingAngle = LerpedFloat.linear()
    private val chaser = LerpedFloat.Chaser.LINEAR

    private var lastPower: Int = 0
    private var currentPower: Int = 0


    val angularSpeed: Double
        get() {
            var speed = Math.abs(getSpeed() * 3 / 10f)
            if (level!!.isClientSide) speed *= ServerSpeedProvider.get()
            return speed.toDouble()
        }

    override fun tick() {
        super.tick()

        bearingAngle.tickChaser()
        if (isRunning) applyRotations()

        //println("${level?.isClientSide} ${ movedContraption?.getAngle(0f) }")
        if (level?.isClientSide != false) return

        movedContraption?.tick()
        if (assembleNextTick) {
            assembleNextTick = false
            assemble()
        }

        lastPower = currentPower
        currentPower = getPower()

        bearingAngle.chase(currentPower * 22.5 / 15, angularSpeed, chaser)



        if (lastPower != currentPower) sendData()
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)

        bearingAngle.setValue(tag.getFloat("BearingAngle").toDouble())
        bearingAngle.chase(tag.getFloat("TargetAngle").toDouble(), tag.getDouble("AngularSpeed"), chaser)
        isRunning = tag.getBoolean("IsRunning")

        lastException = AssemblyException.read(tag)

    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        tag.putFloat("BearingAngle",bearingAngle.value)
        tag.putFloat("TargetAngle",bearingAngle.chaseTarget)
        tag.putBoolean("IsRunning",isRunning)
        tag.putDouble("AngularSpeed",angularSpeed)

        AssemblyException.write(tag,lastAssemblyException)
    }

    private fun getPowerDirection(): Direction {
        if (!blockState.hasProperty(DirectionalAxisKineticBlock.FACING) || !blockState.hasProperty(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)) {
            ClockworkMod.LOGGER.error("Flap bearing block lacks FACING or AXIS_ALONG_FIRST_COORDINATE. Did the blockstate build incorrectly?")
            return Direction.UP // return UP, which is more or less redstone-less
        }

        val facing = blockState.getValue(DirectionalAxisKineticBlock.FACING)
        val axisAlong = blockState.getValue(DirectionalAxisKineticBlock.AXIS_ALONG_FIRST_COORDINATE)

        return when  {
            facing.axis == Axis.Z -> Direction.WEST
            axisAlong == false -> Direction.WEST

            else -> Direction.NORTH
        }
    }

    open protected fun getPower(): Int {
        var power = 0

        val powerDirection = getPowerDirection()

        power += level!!.getSignal(blockPos.relative(powerDirection), powerDirection)
        power -= level!!.getSignal(blockPos.relative(powerDirection.opposite), powerDirection.opposite)

        return power
    }

    fun assemble() {
        if (level!!.getBlockState(worldPosition).block !is FlapBearingBlock) return
        val direction = blockState.getValue(BlockStateProperties.FACING)
        val contraption: FlapContraption?
        try {
            contraption = FlapContraption.assembleFlap(level!!, worldPosition, direction)
            lastException = null
        } catch (e: AssemblyException) {
            lastException = e
            return sendData()
        }

        if (contraption == null) return
        if (contraption.blocks.isEmpty()) return
        if (contraption.blocks.size > maxSize && maxSize != -1L) {
            lastException = AssemblyException.structureTooLarge()
            return sendData()
        }
        val anchor = worldPosition.relative(direction)
        contraption.removeBlocksFromWorld(level, BlockPos.ZERO)
        movedContraption = ControlledContraptionEntity.create(level, this, contraption)
        movedContraption!!.setPos(anchor.x.toDouble(), anchor.y.toDouble(), anchor.z.toDouble())
        movedContraption!!.rotationAxis = direction.axis
        level!!.addFreshEntity(movedContraption!!)

        // Run
        isRunning = true
        bearingAngle.setValue(0.0)
        sendData()
    }

    fun disassemble() {
        if (!isRunning && movedContraption == null) return

        bearingAngle.setValue(0.0)
        applyRotations()

        if (movedContraption != null) movedContraption!!.disassemble()

        movedContraption = null
        isRunning = false
        sendData()
    }

    protected fun applyRotations() {
        if (movedContraption == null) return

        var axis: Axis = Axis.X
        if (blockState.hasProperty(BlockStateProperties.FACING)) axis = blockState.getValue(BlockStateProperties.FACING).axis

        movedContraption!!.setAngle(bearingAngle.value)
        movedContraption!!.rotationAxis = axis

    }

    override fun remove() {
        if (!level!!.isClientSide) disassemble()
        super.remove()
    }

    override fun lazyTick() {
        super.lazyTick()
        if (movedContraption != null && level?.isClientSide == false) sendData()
    }


    override fun attach(contraption: ControlledContraptionEntity) {
        if (contraption.contraption !is FlapContraption) return
        if (!blockState.hasProperty(BearingBlock.FACING)) return
        movedContraption = contraption
        setChanged()
        val anchor = worldPosition.relative(blockState.getValue(BearingBlock.FACING))
        movedContraption!!.setPos(anchor.x.toDouble(), anchor.y.toDouble(), anchor.z.toDouble())
        if (!level!!.isClientSide) {
            isRunning = true
            sendData()
        }
    }



    override fun getInterpolatedAngle(partialTicks: Float): Float {
        return bearingAngle.getValue(partialTicks)
    }

    override fun onSpeedChanged(prevSpeed: Float) {
        super.onSpeedChanged(prevSpeed)
        assembleNextTick = true
    }

    override fun setAngle(forcedAngle: Float) {
        bearingAngle.setValue(forcedAngle.toDouble())
        bearingAngle.updateChaseTarget(forcedAngle)
    }

    override fun onStall() {
        if (level?.isClientSide != false) sendData()
    }

    override fun isValid(): Boolean {
        return !isRemoved
    }

    override fun getBlockPosition(): BlockPos {
        return worldPosition
    }

    override fun isWoodenTop(): Boolean {
        return false
    }

    override fun isAttachedTo(contraption: AbstractContraptionEntity?): Boolean {
        return movedContraption == contraption
    }

    override fun getLastAssemblyException(): AssemblyException? {
        return lastException
    }
}
