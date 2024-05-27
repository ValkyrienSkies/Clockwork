package org.valkyrienskies.clockwork.content.contraptions.propeller

import com.simibubi.create.AllSoundEvents
import com.simibubi.create.AllTags
import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.INamedIconOptions
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.Lang
import com.simibubi.create.foundation.utility.ServerSpeedProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.contraptions.propeller.contraption.PropellerContraption
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropCreateData
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropUpdateData
import org.valkyrienskies.clockwork.content.forces.PropellerController
import org.valkyrienskies.clockwork.integration.cc.ComputerAttachmentHandler
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.EaseHelper
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.math.sin

class PropellerBearingBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    KineticBlockEntity(type, pos, state), IBearingBlockEntity {

    val computerHandler = ComputerAttachmentHandler()

    var sailPositions: MutableList<BlockPos> = ArrayList()
    protected var airCurrentUpdateCooldown = 0
    protected var entitySearchCooldown = 0
    var speedChanged = false
    var rotspeed = 0f
    var oldRotspeed = 0f
    var assembleCooldown = 0
    var assembleNextTick = false
    var sails = 0
    var moddingSpeed = 0
    var slowingDown = false
        set(value) {
            field = value
            if (value && computerHandler != null)
                computerHandler.sendEvent("slowing_down", arrayListOf<Any>())
        }
    var disassembling = 0f
    var countDown = 200
    var spinup = 0f
    var spinningUp = false
        set(value) {
            field = value
            if (value && computerHandler != null)
                computerHandler.sendEvent("spinning_up", arrayListOf<Any>())
        }
    protected var realAngle = 0f
    var running = false
        set(value) {
            field = value
            if (value && computerHandler != null)
                computerHandler.sendEvent("running", arrayListOf<Any>())
        }
    var wasOverStressed = false
    protected var clientAngleDiff = 0f
    protected var lastException: AssemblyException? = null
        set(value) {
            field = value
            if (value != null && computerHandler != null)
                computerHandler.sendEvent("assembly_exception", value.localizedMessage)
        }
    protected var movedContraption: ControlledContraptionEntity? = null
    private var prevAngle = 0f
    private val prevSpeed = 0f
    protected var movementDirection: ScrollOptionBehaviour<RotationDirection>? = null
        set(value) {
            field = value
            if (value != null && computerHandler != null)
                computerHandler.sendEvent("direction_changed", value.get()?.name)
        }
    var isInverted = false
        private set
    private var physPropId: Int? = null

    private val pistonsA: Float = 0.0f
    private val pistonsB: Float = 0.09375f
    private val pistonsC: Float = 0.1875f
    private val pistonsD: Float = 0.28125f


    fun getCornerHorizontalOffset(partialTicks: Float, te: PropellerBearingBlockEntity, ordinal: Int): Float {
        if (!this.running) {
            return 0f
        }
        return when (ordinal) {
            1 -> {
                3f / 16f + sin(EaseHelper.easeInOutSine(this.pistonsA).toDouble()).toFloat() / 16f
            }

            2 -> {
                3f / 16f + sin(EaseHelper.easeInOutSine(this.pistonsB).toDouble()).toFloat() / 16f
            }

            3 -> {
                3f / 16f + sin(EaseHelper.easeInOutSine(this.pistonsC).toDouble()).toFloat() / 16f
            }

            else -> {
                3f / 16f + sin(EaseHelper.easeInOutSine(this.pistonsD).toDouble()).toFloat() / 16f
            }
        }
    }

    override fun getInterpolatedAngle(partialTicks: Float): Float {
        var partialTicks = partialTicks
        if (isVirtual) return Mth.lerp(partialTicks + .5f, prevAngle, realAngle)
        if (movedContraption == null || movedContraption!!.isStalled || !running) partialTicks = 0f
        if(overStressed) return 0f
        return Mth.lerp(partialTicks, realAngle, realAngle + angularSpeed)
    }

    val angularSpeed: Float
        get() {
            var angspeed = convertToAngular(rotspeed) * 1.25f //* 1.25 to make it a bit easier to fly //TODO config?
            if (rotspeed == 0f) {
                angspeed = 0f
            }
            if (level!!.isClientSide) {
                angspeed *= ServerSpeedProvider.get()
                angspeed += clientAngleDiff / 3f
            }
            return angspeed
        }

    fun handleOverStressed(): Boolean {
        if (overStressed) {
            wasOverStressed = true
        }

        if (wasOverStressed) {
            countDown--
            if (countDown <= 0) {
                wasOverStressed =  false
                countDown = 20 * 3
            }
        }

        if (wasOverStressed) {
            computerHandler.sendEvent("overstressed", countDown)
            return true
        }

        return false
    }

    override fun tick() {
        super.tick()
        val server = !level!!.isClientSide || isVirtual
        if (rotSpeedChanged()) {
            sendData()
        }
        oldRotspeed = rotspeed
        if (spinningUp) {
            modSpinupSpeed()
        } else if (slowingDown) {
            modSlowdownSpeed()
        } else {
            modSpeed()
        }
        if (overStressed) {
            //TODO ? stressShutdown()
        }

        if (rotspeed < 0) {
            setBlockDirection(PropellerBearingBlock.Direction.PULL)
        } else {
            setBlockDirection(PropellerBearingBlock.Direction.PUSH)
        }
        if (speedChanged) {
            onSpeedChanged(prevSpeed)
            onRotspeedChanged()
            speedChanged = false
        }

        if (assembleCooldown > 0) {
            assembleCooldown--
        }

        prevAngle = realAngle

        if (level!!.isClientSide) clientAngleDiff /= 2f
        if (!level!!.isClientSide && assembleNextTick) {
            assembleCooldown = 20 * 2
            assembleNextTick = false
            if (!running) {
                assemble()
            }
        }

        if (handleOverStressed()) {
            return
        }

        if (!running) return
        if (!(movedContraption != null && movedContraption!!.isStalled) && !overStressed) {
            val angularSpeed = angularSpeed
            val newAngle = realAngle + angularSpeed
            realAngle = (newAngle % 360)
        }
        applyRotation()
        if (physPropId != null) {
            if (server) {
                val ship = (level as ServerLevel).getShipObjectManagingPos(
                    blockPos
                )
                if (ship != null) {
                    var dumbFix = 1
                    val direction = blockState.getValue(BearingBlock.FACING)
                    if (direction == Direction.WEST || direction == Direction.NORTH || direction == Direction.DOWN) {
                        dumbFix = -dumbFix
                    }

                    val data = PropUpdateData(dumbFix * angularSpeed.toDouble(), realAngle.toDouble(), isInverted, overStressed)
                    PropellerController.getOrCreate(ship)!!.updatePropeller(physPropId!!, data)
                }
            }
        }
    }


    private fun modSpeed() {
        if (movedContraption == null) {
            return
        }
        if (rotspeed == speed) {
            return
        }

        val diff = speed - rotspeed
        rotspeed += Mth.clamp(diff / 10, -32f, 32f)
    }

    private fun modSlowdownSpeed() {
        disassembling--
        if (disassembling <= 0) {
            if (!level!!.isClientSide) {
                disassemble()
            }
            slowingDown = false
            disassembling = 0f
            return
        }
        val stoppingPoint = realAngle + rotspeed * disassembling * 0.5f
        val optimalStoppingPoint = 90f * Math.round(stoppingPoint / 90f)
        val Q = (optimalStoppingPoint - stoppingPoint) / disassembling
        rotspeed = (rotspeed + 6f * Q / disassembling) * (1f - 1f / disassembling)
    }

    private fun modSpinupSpeed() {
        if (level!!.isClientSide) {
            return
        }
        spinup--
        if (abs(rotspeed) >= abs(speed)) {
            spinningUp = false
            if (abs(rotspeed) > abs(speed)) {
                rotspeed = speed
            }
            return
        }

        val startingPoint = realAngle + speed * spinup * 0.5f
        val Q = startingPoint / spinup
        rotspeed = (rotspeed + 6f * Q / spinup) * (1f - 1f / spinup)
    }

    override fun calculateStressApplied(): Float {
        if (running && movedContraption != null) {
            if (sails == 0) {
                getSails()
            }
            sails = sailPositions.size
            return sails * 2f
        }
        return 0.0f
    }

    protected fun applyRotation() {
        if (movedContraption == null) return
        movedContraption!!.setAngle(realAngle)
        val blockState = blockState
        if (blockState.hasProperty(BlockStateProperties.FACING)) movedContraption!!.rotationAxis =
            blockState.getValue(BlockStateProperties.FACING)
                .axis
    }

    public override fun write(compound: CompoundTag, clientPacket: Boolean) {
        compound.putFloat(ClockworkConstants.Nbt.ROT_SPEED, rotspeed)
        compound.putBoolean(ClockworkConstants.Nbt.RUNNING, running)
        compound.putFloat(ClockworkConstants.Nbt.ANGLE, realAngle)
        compound.putBoolean(ClockworkConstants.Nbt.INVERTED, isInverted)
        if (physPropId != null) {
            compound.putInt(ClockworkConstants.Nbt.ID, physPropId!!)
        }
        AssemblyException.write(compound, lastException)
        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        val angleBefore = realAngle
        rotspeed = compound.getFloat(ClockworkConstants.Nbt.ROT_SPEED)
        running = compound.getBoolean(ClockworkConstants.Nbt.RUNNING)
        realAngle = compound.getFloat(ClockworkConstants.Nbt.ANGLE)
        isInverted = compound.getBoolean(ClockworkConstants.Nbt.INVERTED)
        lastException = AssemblyException.read(compound)
        if (compound.contains(ClockworkConstants.Nbt.ID)) {
            physPropId = compound.getInt(ClockworkConstants.Nbt.ID)
        }
        super.read(compound, clientPacket)
        if (!clientPacket) return
        if (running) {
            clientAngleDiff = AngleHelper.getShortestAngleDiff(angleBefore.toDouble(), realAngle.toDouble())
            realAngle = angleBefore
        } else {
            movedContraption = null
        }
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

    override fun isWoodenTop(): Boolean {
        return false
    }

    val directonFromBlock: PropellerBearingBlock.Direction
        get() = PropellerBearingBlock.getDirectionof(blockState)

    protected fun setBlockDirection(direction: PropellerBearingBlock.Direction) {
        val inBlockState: PropellerBearingBlock.Direction = directonFromBlock
        if (inBlockState === direction) return
        level!!.setBlockAndUpdate(worldPosition, blockState.setValue(PropellerBearingBlock.DIRECTION, direction))
        notifyUpdate()
    }

    fun getSails() {
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

    override fun setAngle(forcedAngle: Float) {
        realAngle = forcedAngle
    }

    override fun remove() {
        if (!level!!.isClientSide) {
            disassemble()
        }
        super.remove()
    }

    private fun assemble() {
        if (level!!.getBlockState(worldPosition)
                .block !is PropellerBearingBlock
        ) return
        val direction = blockState.getValue<Direction>(BlockStateProperties.FACING)
        val contraption: PropellerContraption
        try {
            contraption = PropellerContraption.assembleProp(level!!, worldPosition, direction)!!
            lastException = null
        } catch (e: AssemblyException) {
            lastException = e
            sendData()
            return
        }
        speedChanged = rotSpeedChanged()
        if (contraption.blocks.isEmpty()) return
        val anchor = worldPosition.relative(direction)
        contraption.removeBlocksFromWorld(level, BlockPos.ZERO)
        movedContraption = ControlledContraptionEntity.create(level, this, contraption)
        movedContraption!!.setPos(anchor.x.toDouble(), anchor.y.toDouble(), anchor.z.toDouble())
        movedContraption!!.rotationAxis = direction.axis
        movedContraption?.let { level!!.addFreshEntity(it) }
        AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition)
        running = true
        realAngle = 0f
        rotspeed = 0f
        spinningUp = true
        spinup = abs(speed).toInt().toFloat()
        getSails()
        val sailVecs = sailPositions.stream().map { v: BlockPos -> v.toJOML() }
            .toList()
        val axis: Vector3dc = blockState.getValue(BlockStateProperties.FACING).normal.toJOMLD()
        val vecPos: Vector3dc = blockPos.toJOMLD()
        val data = PropCreateData(
            vecPos, axis,
            realAngle.toDouble(),
            angularSpeed.toDouble(),
            sailVecs,
            isInverted,
            overStressed
        )
        if (!level!!.isClientSide) {
            val ship = (level as ServerLevel).getShipObjectManagingPos(
                blockPos
            )
            if (ship != null) {
                physPropId = PropellerController.getOrCreate(ship)!!.addPropeller(data)
            }
        }
        sendData()
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        movementDirection = ScrollOptionBehaviour(
            RotationDirection::class.java,
            Lang.translateDirect("contraptions.Propeller.movement_direction"), this,
            movementModeSlot
        )
        movementDirection!!.requiresWrench()
        movementDirection!!.withCallback { onOrientationChanged() }
        behaviours.add(movementDirection!!)
    }

    private fun onOrientationChanged() {
        isInverted = !isInverted
    }

    override fun lazyTick() {
        super.lazyTick()
        if (running && movedContraption != null) {
            sendData()
        }
    }

    fun shutDown() {
        if (assembleCooldown > 0) {
            return
        }
        slowingDown = true
        disassembling = abs(rotspeed)
        spinningUp = false
        spinup = 0f
    }

    private fun disassemble() {
        if (!running && movedContraption == null) return

        rotspeed = 0f
        realAngle = 0f
        slowingDown = false
        disassembling = 0f
        if (movedContraption != null) {
            movedContraption!!.disassemble()
            AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition)
        }
        movedContraption = null
        running = false
        if (physPropId != null) {
            if (!level!!.isClientSide) {
                val ship = (level as ServerLevel).getShipObjectManagingPos(
                    blockPos
                )
                if (ship != null) {
                    PropellerController.getOrCreate(ship)!!.removePropeller(physPropId!!)
                }
            }
        }
        sendData()
    }

    private fun rotSpeedChanged(): Boolean {
        return rotspeed != oldRotspeed
    }

    private fun onRotspeedChanged() {}

    override fun onSpeedChanged(prevSpeed: Float) {
        super.onSpeedChanged(prevSpeed)
        detachKinetics()
    }

    override fun isAttachedTo(contraption: AbstractContraptionEntity): Boolean {
        return if (contraption.contraption !is PropellerContraption) false else movedContraption === contraption
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

    enum class RotationDirection(private val icon: AllIcons) : INamedIconOptions {
        NORMAL(AllIcons.I_REFRESH),
        INVERTED(AllIcons.I_ROTATE_CCW);

        private val translationKey: String = "generic." + Lang.asId(name)

        override fun getIcon(): AllIcons {
            return icon
        }

        override fun getTranslationKey(): String {
            return translationKey
        }
    }
}
