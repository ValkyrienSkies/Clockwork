package org.valkyrienskies.clockwork.content.contraptions.flap

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.Iterate
import com.simibubi.create.foundation.utility.ServerSpeedProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.content.contraptions.flap.contraption.FlapContraption
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerContraption

class FlapBearingBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    KineticBlockEntity(type, pos, state), IBearingBlockEntity {
    var redstoneSideOne = false
    var redstoneSideTwo = false
    protected var angle = 0f
    protected var clientAngleDiff = 0f
    var isRunning = false
        protected set
    var assembleNextTick = false
    protected var lastException: AssemblyException? = null
    protected var flap: ControlledContraptionEntity? = null
    private var prevForcedAngle = 0f
    private var redstoneLevel = 0
    private var redstonePos: BlockPos? = null
    override fun addBehaviours(behaviours: List<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
    }

    fun isFlap(): Boolean {
        return true
    }

    private fun getPower(worldIn: Level, pos: BlockPos): Int {
        var power = 0
        for (direction in Iterate.directions) {
            power = Math.max(worldIn.getSignal(pos.relative(direction), direction), power)
            if (power != 0) {
                redstonePos = pos.relative(direction)
                break
            }
        }
        if (power == 0) {
            redstonePos = null
        }
        return power
    }

    override fun tick() {
        super.tick()
        if (flap != null) {
            flap!!.tick()
        }
        if (level!!.isClientSide) {
            prevForcedAngle = angle
            clientAngleDiff /= 2f
        }
        redstoneLevel = getPower(level!!, worldPosition)
        if (redstonePos != null) {
            if (blockState.getValue<Direction>(BlockStateProperties.FACING) == Direction.UP || blockState.getValue<Direction>(
                    BlockStateProperties.FACING
                ) == Direction.DOWN
            ) {
                redstoneSideOne = redstonePos == worldPosition.relative(Direction.EAST, 1)
                redstoneSideTwo = redstonePos == worldPosition.relative(Direction.WEST, 1)
            } else if (blockState.getValue<Direction>(BlockStateProperties.FACING) == Direction.NORTH || blockState.getValue<Direction>(
                    BlockStateProperties.FACING
                ) == Direction.SOUTH
            ) {
                redstoneSideOne = redstonePos == worldPosition.relative(Direction.EAST, 1)
                redstoneSideTwo = redstonePos == worldPosition.relative(Direction.WEST, 1)
            } else if (blockState.getValue<Direction>(BlockStateProperties.FACING) == Direction.EAST || blockState.getValue<Direction>(
                    BlockStateProperties.FACING
                ) == Direction.WEST
            ) {
                redstoneSideOne = redstonePos == worldPosition.relative(Direction.NORTH, 1)
                redstoneSideTwo = redstonePos == worldPosition.relative(Direction.SOUTH, 1)
            }
        }
        if (!level!!.isClientSide && assembleNextTick) {
            assembleNextTick = false
            if (isRunning) {
                val canDisassemble = true
                if (speed == 0f && (canDisassemble || flap == null || flap!!.contraption
                        .blocks
                        .isEmpty())
                ) {
                    if (flap != null) flap!!.contraption
                        .stop(level)
                    disassemble()
                }
                return
            } else assemble()
            return
        }
        if (!(flap != null && flap!!.isStalled)) {
            val testSpeed = angularSpeed / 2f
            val newAngle = angle + flapSpeed
            angle = newAngle % 360
        }
        if (!isRunning) return
        applyRotations()
    }

    public override fun write(compound: CompoundTag, clientPacket: Boolean) {
        compound.putBoolean("Running", isRunning)
        compound.putFloat("Angle", angle)
        AssemblyException.write(compound, lastException)
        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        val angleBefore = angle
        isRunning = compound.getBoolean("Running")
        angle = compound.getFloat("Angle")
        lastException = AssemblyException.read(compound)
        super.read(compound, clientPacket)
        if (!clientPacket) return
        if (isRunning) {
            clientAngleDiff = AngleHelper.getShortestAngleDiff(angleBefore.toDouble(), angle.toDouble())
            angle = angleBefore
        } else {
            flap = null
        }
    }

    fun assemble() {
        if (level!!.getBlockState(worldPosition)
                .block !is FlapBearingBlock
        ) return
        val direction = blockState.getValue<Direction>(BlockStateProperties.FACING)
        val contraption: FlapContraption?
        try {
            contraption = FlapContraption.assembleFlap(level!!, worldPosition, direction)
            lastException = null
        } catch (e: AssemblyException) {
            lastException = e
            sendData()
            return
        }
        if (contraption == null) return
        if (contraption.getBlocks()
                .isEmpty()
        ) return
        val anchor = worldPosition.relative(direction)
        contraption.removeBlocksFromWorld(level, BlockPos.ZERO)
        flap = ControlledContraptionEntity.create(level, this, contraption)
        flap!!.setPos(anchor.x.toDouble(), anchor.y.toDouble(), anchor.z.toDouble())
        flap!!.setRotationAxis(direction.axis)
        level!!.addFreshEntity(flap)

        //Run
        isRunning = true
        angle = 0f
        sendData()
    }

    override fun remove() {
        if (!level!!.isClientSide) disassemble()
        super.remove()
    }

    fun disassemble() {
        if (!isRunning && flap == null) return
        angle = 0f
        applyRotations()
        if (flap != null) {
            flap!!.disassemble()
        }
        flap = null
        isRunning = false
        sendData()
    }

    protected fun applyRotations() {
        val blockState = blockState
        var axis: Direction.Axis? = Direction.Axis.X
        if (blockState.hasProperty<Direction>(BlockStateProperties.FACING)) axis =
            blockState.getValue<Direction>(BlockStateProperties.FACING)
                .axis
        if (flap != null) {
            flap!!.setAngle(angle)
            flap!!.rotationAxis = axis
        }
    }

    override fun attach(contraption: ControlledContraptionEntity) {
        val blockState = blockState
        if (contraption.contraption !is FlapContraption) return
        if (!blockState.hasProperty(BearingBlock.FACING)) return
        flap = contraption
        setChanged()
        val anchor = worldPosition.relative(blockState.getValue(BearingBlock.FACING))
        flap!!.setPos(anchor.x.toDouble(), anchor.y.toDouble(), anchor.z.toDouble())
        if (!level!!.isClientSide) {
            isRunning = true
            sendData()
        }
    }

    override fun lazyTick() {
        super.lazyTick()
        if (flap != null && !level!!.isClientSide) sendData()
    }

    val flapSpeed: Float
        get() {
            var speed = angularSpeed / 2f
            if (speed != 0f) {
                val flapTarget = getFlapTarget(redstoneSideOne, redstoneSideTwo)
                val shortestAngleDiff = AngleHelper.getShortestAngleDiff(angle.toDouble(), flapTarget.toDouble())
                speed = if (shortestAngleDiff < 0) {
                    Math.max(speed, shortestAngleDiff)
                } else {
                    Math.min(-speed, shortestAngleDiff)
                }
            }
            return speed + clientAngleDiff / 3f
        }

    protected fun getFlapTarget(negativeActivated: Boolean, positiveActivated: Boolean): Float {
        if (negativeActivated && !positiveActivated) {
            return -22.5f * (redstoneLevel.toFloat() / 15)
        }
        if (positiveActivated && !negativeActivated) {
            return 22.5f * (redstoneLevel.toFloat() / 15)
        }
        return if (negativeActivated && positiveActivated) {
            0f
        } else 0f
    }

    override fun setAngle(forcedAngle: Float) {
        angle = forcedAngle
    }

    val angularSpeed: Float
        get() {
            var speed = -Math.abs(getSpeed() * 3 / 10f)
            if (level!!.isClientSide) speed *= ServerSpeedProvider.get()
            return speed
        }

    override fun getInterpolatedAngle(partialTicks: Float): Float {
        var partialTicks = partialTicks
        if (isVirtual) return Mth.lerp(partialTicks, prevForcedAngle, angle)
        if (flap == null || flap!!.isStalled) partialTicks = 0f
        return Mth.lerp(partialTicks, angle, angle + flapSpeed)
    }

    override fun isWoodenTop(): Boolean {
        return false
    }

    override fun isAttachedTo(contraption: AbstractContraptionEntity): Boolean {
        return if (contraption.contraption !is FlapContraption) false else flap === contraption
    }

    override fun onStall() {
        if (!level!!.isClientSide) sendData()
    }

    override fun onSpeedChanged(prevSpeed: Float) {
        super.onSpeedChanged(prevSpeed)
        assembleNextTick = true
    }

    override fun isValid(): Boolean {
        return !isRemoved
    }

    override fun getBlockPosition(): BlockPos {
        return worldPosition
    }
}