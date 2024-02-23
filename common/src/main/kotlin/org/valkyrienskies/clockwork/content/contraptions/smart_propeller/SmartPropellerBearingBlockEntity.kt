package org.valkyrienskies.clockwork.content.contraptions.smart_propeller

import com.simibubi.create.AllSoundEvents
import com.simibubi.create.AllTags
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.BearingContraption
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.contraptions.bearing.MechanicalBearingBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.utility.Lang
import com.simibubi.create.foundation.utility.ServerSpeedProvider
import com.simibubi.create.foundation.utility.VecHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.util.Mth
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.MathUtil
import kotlin.math.max
import kotlin.math.sqrt


class SmartPropellerBearingBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    MechanicalBearingBlockEntity(type, pos, state), IBearingBlockEntity {

    private var movementDirection: ScrollOptionBehaviour<RotationDirection>? = null
    private var sailPositions: MutableList<BlockPos> = mutableListOf()
    private var rotationSpeed: Float = 0f
    private var disassemblyTimer: Float = 0f
    private var disassemblySlowdown: Boolean = false
    private var thrustDirection: Vec3 = Vec3.ZERO
    private var disassemblyTimerTotal: Float = 0f
    private var disassemblyTimerScale: Float = 3.5f
    private var prevAngle: Float = 0f
    private var tiltVector: Vec3 = Vec3(0.0, 1.0, 0.0)
    private var targetVector: Vec3 = Vec3(0.0, 1.0, 0.0)

    var tiltQuaternion: Quaternionf = Quaternionf(0f, 0f, 0f, 1f)
    var blockNormal: Vec3? = null

    init {
        tiltQuaternion.normalize()
    }

    override fun addBehavioursDeferred(behaviours: MutableList<BlockEntityBehaviour>?) {
        super.addBehavioursDeferred(behaviours)

        movementMode.setValue(2)
        behaviours?.remove(movementMode)
        movementDirection = ScrollOptionBehaviour(
            RotationDirection::class.java,
            TranslatableComponent("vs_clockwork:rotation_direction"), this, movementModeSlot
        )

        movementDirection!!.requiresWrench()
        movementDirection!!.withCallback { _: Int? -> onDirectionChanged() }
        behaviours!!.add(movementDirection!!)
    }

    private fun onDirectionChanged() {
        if (!running)
            return
        if (!level!!.isClientSide) {
            updateGeneratedRotation()
        }
    }

    fun startDisassemblySlowdown() {
        if (!disassemblySlowdown) {
            disassemblySlowdown = true
            disassemblyTimerTotal = 1 + disassemblyTimerScale * sqrt(sailPositions.size.toDouble()).toFloat()
            disassemblyTimer = disassemblyTimerTotal
        }
    }

    fun setTilt(target: Vec3) {
        val direction: Direction = blockState.getValue(BlockStateProperties.FACING)
        blockNormal = Vec3(direction.stepX.toDouble(), direction.stepY.toDouble(), direction.stepZ.toDouble())

        targetVector = target
        tiltVector = MathUtil.clampIntoCone(target, blockNormal!!, Math.toRadians(12.0))
        if (disassemblySlowdown) {
            tiltVector = VecHelper.lerp(disassemblyTimer / disassemblyTimerTotal, blockNormal, tiltVector)
        }
        tiltQuaternion = MathUtil.getQuaternionFromVectorRotation(blockNormal!!, tiltVector)
        thrustDirection = tiltVector
    }

    override fun tick() {
        super.tick()

        prevAngle = angle
        if (disassemblySlowdown) {
            updateSlowdownSpeed()
        } else {
            updateRotationSpeed()
        }

        if (movedContraption != null && !movedContraption.isAlive) {
            movedContraption = null
        }

        val facing = level!!.getBlockState(worldPosition).getValue(BlockStateProperties.FACING)
        blockNormal = Vec3(facing.stepX.toDouble(), facing.stepY.toDouble(), facing.stepZ.toDouble())

        if (movedContraption == null) {
            return
        }

        if (movedContraption is SuperContraptionEntity) {
            val superContraption = movedContraption as SuperContraptionEntity
            superContraption.tiltQuaternion = tiltQuaternion
            superContraption.superDirection = blockState.getValue(BlockStateProperties.FACING)
        }
    }


    private fun updateRotationSpeed() {
        var nextSpeed = convertToAngular(getSpeed())
        if (getSpeed() == 0f) {
            nextSpeed = 0f
        }
        if (sailPositions.size > 0) {
            val lerpAmount = 0.4f / sqrt(sailPositions.size.toDouble()).toFloat()
            rotationSpeed = Mth.lerp(lerpAmount, rotationSpeed, nextSpeed)
        } else {
            rotationSpeed = nextSpeed
        }
    }

    private fun updateSlowdownSpeed() {
        disassemblyTimer--
        if (disassemblyTimer <= 0.5) {
            if (!level!!.isClientSide) {
                disassemble()
            }
            disassemblySlowdown = false

            running = false
            return
        }
        val currentStoppingPoint = (angle + rotationSpeed * disassemblyTimer * 0.5f)

        val optimalStoppingPoint = 90f * Math.round(currentStoppingPoint / 90f)

        val slowdownFactor = (optimalStoppingPoint - currentStoppingPoint) / disassemblyTimer

        rotationSpeed = (rotationSpeed + 6f * slowdownFactor / disassemblyTimer) * (1f - 1f / disassemblyTimer)
    }

    override fun assemble() {
        if (level!!.getBlockState(worldPosition).block !is BearingBlock) {
            return
        }

        val direction = blockState.getValue(BlockStateProperties.FACING)
        val contraption = BearingContraption(false, direction)
        try {
            if (!contraption.assemble(level, worldPosition)) {
                return
            }

            lastException = null
        } catch (e: AssemblyException) {
            lastException = e
            sendData()
            return
        }


        contraption.removeBlocksFromWorld(level, BlockPos.ZERO)
        val anchor = worldPosition.relative(direction)

        movedContraption = SuperContraptionEntity.create(level, this, contraption)
        movedContraption.setPos(anchor.x.toDouble(), anchor.y.toDouble(), anchor.z.toDouble())
        movedContraption.rotationAxis = direction.axis
        level!!.addFreshEntity(movedContraption)

        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition)

        running = true
        angle = 0f
        sendData()
        updateGeneratedRotation()
        rotationSpeed = 0f
        getSails()
    }

    fun getPartialVelocity(partialTick: Float): Float {
        val currentStoppingPoint = (angle + rotationSpeed * disassemblyTimer * 0.5f)

        val optimalStoppingPoint = 90f * Math.round(currentStoppingPoint / 90f)

        val velocityFactor = (optimalStoppingPoint - currentStoppingPoint) / disassemblyTimer

        val scaledTime = partialTick / disassemblyTimer

        return partialTick * (rotationSpeed + scaledTime * (3f * velocityFactor - rotationSpeed * 0.5f - 2f * velocityFactor * scaledTime))
    }

    override fun getAngularSpeed(): Float {
        var speed = rotationSpeed

        if (disassemblySlowdown) speed = getPartialVelocity(1f)

        if (level!!.isClientSide) {
            speed *= ServerSpeedProvider.get()
            speed += clientAngleDiff / 3f
        }
        return speed
    }

    fun getDirectionScale(): Float {
        var speed = getSpeed()
        if (speed == 0f) {
            return 1f
        }
        val facing = blockState.getValue(BlockStateProperties.FACING)
        speed = convertToDirection(speed, facing)
        if (movementDirection!!.value == 1) {
            speed *= -1f
        }
        return if (speed > 0) 1f else -1f
    }

    fun getDirectedRotationRate(): Double {
        var speed = rotationSpeed
        val facing = blockState.getValue(BlockStateProperties.FACING)
        speed = convertToDirection(speed, facing)
        if (movementDirection!!.value == 1) {
            speed *= -1f
        }
        return speed.toDouble()
    }

    private fun getSails() {
        sailPositions = ArrayList()
        if (movedContraption != null) {
            val blocks = movedContraption!!.contraption.blocks
            for ((key, value) in blocks) {
                if (AllTags.AllBlockTags.WINDMILL_SAILS.matches(value.state)) {
                    sailPositions.add(key)
                }
            }
        }
    }

    override fun disassemble() {
        if (!running && movedContraption == null) {
            return
        }
        angle = 0f
        applyRotation()
        super.disassemble()
    }

    override fun attach(contraption: ControlledContraptionEntity?) {
        super.attach(contraption)
        getSails()
    }

    override fun getBlockPosition(): BlockPos {
        return worldPosition
    }

    override fun getInterpolatedAngle(partialTicks: Float): Float {
        var newPartialTicks = partialTicks
        if (isVirtual) {
            return Mth.lerp(newPartialTicks + .5f, prevAngle, angle)
        }
        if (movedContraption == null || movedContraption.isStalled || !running) {
            newPartialTicks = 0f
        }

        if (disassemblySlowdown) {
            return angle + getPartialVelocity(newPartialTicks)
        }

        return Mth.lerp(newPartialTicks, angle, angle + angularSpeed)
    }

    override fun isWoodenTop(): Boolean {
        return false
    }

    override fun setAngle(forcedAngle: Float) {
        angle = forcedAngle
    }

    override fun write(compound: CompoundTag, clientPacket: Boolean) {
        compound.putFloat(ClockworkConstants.Nbt.ROT_SPEED, rotationSpeed)
        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        rotationSpeed = compound.getFloat(ClockworkConstants.Nbt.ROT_SPEED)
        super.read(compound, clientPacket)
    }

    override fun calculateStressApplied(): Float {
        if (!running) {
            return 0f
        }
        var sails = 0
        if (movedContraption != null) {
            sails = (movedContraption.contraption as BearingContraption).sailBlocks
        }
        sails = max(sails.toDouble(), 2.0).toInt()
        return sails * 2f
    }

    fun setAssembleNextTick(b: Boolean) {
        assembleNextTick = b
    }

    enum class RotationDirection(private val icon: AllIcons) : INamedIconOptions {
        NORMAL(AllIcons.I_REFRESH),
        INVERTED(AllIcons.I_ROTATE_CCW);

        private val translationKey: String = "generic." + ClockworkLang.asId(name)

        override fun getIcon(): AllIcons {
            return icon
        }

        override fun getTranslationKey(): String {
            return translationKey
        }
    }
}