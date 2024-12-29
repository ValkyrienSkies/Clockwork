package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity
import com.simibubi.create.content.kinetics.transmission.sequencer.SequencerInstructions
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour
import com.simibubi.create.foundation.item.TooltipHelper
import com.simibubi.create.foundation.utility.AngleHelper
import com.simibubi.create.foundation.utility.ServerSpeedProvider
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.*
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.platform.api.ContraptionController
import org.valkyrienskies.clockwork.platform.api.ContraptionController.LockedMode
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.ClockworkConstants.Nbt.ORIGINAL_DIRECTION
import org.valkyrienskies.clockwork.util.ClockworkUtils.getVector3d
import org.valkyrienskies.clockwork.util.GlueAssembler.collectGlued
import org.valkyrienskies.clockwork.util.minus
import org.valkyrienskies.clockwork.util.plus
import org.valkyrienskies.clockwork.util.times
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.core.apigame.joints.*
import org.valkyrienskies.core.impl.bodies.properties.BodyTransformVelocityImpl
import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3d
import org.valkyrienskies.mod.common.*
import org.valkyrienskies.mod.common.util.SplittingDisablerAttachment
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.mod.common.world.clipIncludeShips
import org.valkyrienskies.mod.util.putVector3d
import java.lang.Math
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.sign

class PhysBearingBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    GeneratingKineticBlockEntity(type, pos, state), IBearingBlockEntity, IDisplayAssemblyExceptions,
    ContraptionController {

    var assembleNextTick = false
    var movementMode: ScrollOptionBehaviour<LockedMode>? = null
    var isRunning = false
        private set
    var shiptraptionID = NO_SHIPTRAPTION_ID
        private set
    var targetAngle = 0f
        private set
    var disassembleWhenPossible = false
        private set
    var manualTargetAngleChange = false

    private var lastException: AssemblyException? = null
    private var open = false
    private var originalDirection: Direction? = null
    private var shouldRefresh = false
    private var clientAngleDiff = 0f
    private var prevAngle = 0f
    private var coreAngle = 0f
    private var previousCoreAngle = 0f

    private var opening = false
    private var openProgress = 0f
    private var openProgressMax = 70f
    private var inOutCorner = 0f
    private var cornerShrinking = false

    private var ticks = 0
    private var lastStateChanged = 0
    private var cooldown = 20

    private var sequencedAngleLimit = -1.0f
    private var sequencedAngleProgress = 0f

    private var joint : VSJointAndId? = null
    //pos of bearing in subship coordinates
    private var bearingPos: Vector3d = Vector3d()
    private var aligning = false
    private var bearingAxis: Vector3d = Vector3d()

    private var lastSpeed = 0f
    private var lastMode = LockedMode.UNLOCKED

    init {
        setLazyTickRate(3)
    }

    override fun isWoodenTop(): Boolean {
        return false
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        movementMode = ScrollOptionBehaviour(
            LockedMode::class.java, TextComponent("Locked or Unlocked"),
            this, movementModeSlot
        )
        movementMode!!.withCallback{movementModeChanged(it)}
        movementMode!!.requiresWrench()
        behaviours.add(movementMode!!)
    }

    private fun updateDrive(
        driveVelocity: VSRevoluteJoint.VSRevoluteDriveVelocity? = null
    ) {
        val level = level as ServerLevel
        joint = VSJointAndId(joint!!.jointId, VSRevoluteJoint(
            joint!!.joint.shipId0, joint!!.joint.pose0,
            joint!!.joint.shipId1, joint!!.joint.pose1,
            maxForceTorque = VSJointMaxForceTorque(1.0E38F, 1.0E38F),
            driveFreeSpin = movementMode!!.get() == LockedMode.UNLOCKED,
            driveVelocity = driveVelocity
        ))

        level.shipObjectWorld.updateConstraint(joint!!.jointId, joint!!.joint)
    }

    private fun movementModeChanged(value: Int) {
        if (level == null || level!!.isClientSide) {return}
        sendData()
        if (joint == null) {return}
        updateDrive((joint!!.joint as VSRevoluteJoint).driveVelocity)
    }

    override fun remove() {
        if (!level!!.isClientSide) { destroy() }
        super.remove()
    }

    public override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)

        tag.putBoolean(ClockworkConstants.Nbt.RUNNING, isRunning)
        tag.putFloat(ClockworkConstants.Nbt.ANGLE, targetAngle)
        if (shiptraptionID != NO_SHIPTRAPTION_ID) {
            tag.putLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID, shiptraptionID)
        }
        if (originalDirection != null) {
            tag.putInt(ORIGINAL_DIRECTION, originalDirection!!.ordinal)
        }
        AssemblyException.write(tag, lastException)
        tag.putBoolean(ClockworkConstants.Nbt.OPEN, open)
        tag.putBoolean(ClockworkConstants.Nbt.MANUAL_TARGET_ANGLE_CHANGE, manualTargetAngleChange)
        tag.putFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_LIMIT, sequencedAngleLimit)
        tag.putFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_PROGRESS, sequencedAngleProgress)

        tag.putVector3d("bearingPos", bearingPos)
        tag.putVector3d("bearingAxis", bearingAxis)
        tag.putBoolean("aligning", aligning)

        val mapper = VSJacksonUtil.dtoMapper
        joint?.let{tag.putByteArray("constraint", mapper.writeValueAsBytes(it))}

        if (shiptraptionID == NO_SHIPTRAPTION_ID) return

        tag.putLong(ClockworkConstants.Nbt.OLD_POS, worldPosition.asLong())
        //to make it more general
        tag.putVector3d(ClockworkConstants.Nbt.OLD_SHIPTRAPTION_CENTER, bearingPos)
        tag.putVector3d(ClockworkConstants.Nbt.NEW_SHIPTRAPTION_CENTER, bearingPos)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        if (wasMoved) {
            super.read(tag, clientPacket)
            return
        }
        val angleBefore = targetAngle
        open = tag.getBoolean(ClockworkConstants.Nbt.OPEN)
        isRunning = tag.getBoolean(ClockworkConstants.Nbt.RUNNING)
        targetAngle = tag.getFloat(ClockworkConstants.Nbt.ANGLE)
        lastException = AssemblyException.read(tag)
        if (tag.contains(ClockworkConstants.Nbt.SHIPTRAPTION_ID)) {
            shiptraptionID = tag.getLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID)
        }
        if (tag.contains(ORIGINAL_DIRECTION)) {
            originalDirection = Direction.entries[tag.getInt(ORIGINAL_DIRECTION)]
        }
        if (isRunning) {
            if (shiptraptionID == NO_SHIPTRAPTION_ID) {
                clientAngleDiff = AngleHelper.getShortestAngleDiff(angleBefore.toDouble(), targetAngle.toDouble())
                targetAngle = angleBefore
            }
        } else {
            shiptraptionID = NO_SHIPTRAPTION_ID
        }
        manualTargetAngleChange = tag.getBoolean(ClockworkConstants.Nbt.MANUAL_TARGET_ANGLE_CHANGE)
        sequencedAngleLimit = tag.getFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_LIMIT)
        sequencedAngleProgress = tag.getFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_PROGRESS)

        bearingPos = tag.getVector3d("bearingPos")!!
        bearingAxis = tag.getVector3d("bearingAxis")!!
        aligning = tag.getBoolean("aligning")

        val mapper = VSJacksonUtil.dtoMapper
        joint = if (tag.contains("constraint")) {mapper.readValue(tag.getByteArray("constraint"), VSJointAndId::class.java)} else {null}

        super.read(tag, clientPacket)

        if (clientPacket) {return}

        shouldRefresh = true
        val oldPos = BlockPos.of(tag.getLong(ClockworkConstants.Nbt.OLD_POS)).toJOMLD()
        if (oldPos == worldPosition || level == null || level!!.isClientSide) {return}

        val level = level as ServerLevel
        val subship = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return
        val mainId = level.getShipManagingPos(worldPosition)?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!

        val newPos = worldPosition.toJOMLD()

        val oldSPos = tag.getVector3d(ClockworkConstants.Nbt.OLD_SHIPTRAPTION_CENTER)!!
        val newSPos = tag.getVector3d(ClockworkConstants.Nbt.NEW_SHIPTRAPTION_CENTER)!!

        var attachConstraint = joint?.joint as VSRevoluteJoint?
        attachConstraint = attachConstraint?.let{it.copy(subship.id, pose0 = VSJointPose(it.pose0.pos - oldSPos + newSPos, it.pose0.rot), mainId, pose1 = VSJointPose(it.pose1.pos - oldPos + newPos, it.pose1.rot))}
        joint = joint?.let{VSJointAndId(it.jointId, attachConstraint!!)}
    }

    override fun getInterpolatedAngle(partialTicks: Float): Float {
        var partialTicks = partialTicks
        if (isVirtual) return Mth.lerp(partialTicks + .5f, prevAngle, targetAngle)
        if (shiptraptionID == NO_SHIPTRAPTION_ID || !isRunning) partialTicks = 0f
        return Mth.lerp(partialTicks, targetAngle, targetAngle + angularSpeed)
    }

    fun getWingRotOffset(): Float {
        return if (isRunning && open) {
            openProgressMax.toDouble().toFloat()
        } else if (isRunning) {
            Mth.lerp(openProgress.toDouble(), 0.0, openProgressMax.toDouble()).toFloat()
        } else if (!isRunning && open) {
            Mth.lerp(openProgress.toDouble(), 1.0, openProgressMax.toDouble()).toFloat()
        } else {
            0.0f
        }
    }

    fun getInterpolatedCoreAngle(partialTicks: Float): Float {
        previousCoreAngle = coreAngle
        coreAngle++
        if (coreAngle == 360f) {
            coreAngle = 0f
        }
        return if (isVirtual) Mth.lerp(partialTicks + .5f, previousCoreAngle, coreAngle) else Mth.lerp(
            partialTicks,
            coreAngle,
            coreAngle + 4f
        )
    }

    val angularSpeed: Float
        get() {
            var speed = convertToAngular(getSpeed())
            if (getSpeed() == 0f) speed = 0f
            if (level!!.isClientSide) {
                speed *= ServerSpeedProvider.get()
                speed += clientAngleDiff / 3f
            }
            return speed
        }

    override fun getLastAssemblyException(): AssemblyException? {
        return lastException
    }

    override fun getBlockPosition(): BlockPos {
        return worldPosition
    }

    private fun assemble() {
        if (level!!.getBlockState(worldPosition).block !is BearingBlock) return
        val level = level as ServerLevel

        originalDirection = blockState.getValue(BearingBlock.FACING)
        val direction = originalDirection!!
        val attachPoint = worldPosition.relative(direction)

        // bearing data
        val worldPos: Vector3dc = worldPosition.toJOMLD().add(0.5, 0.5, 0.5)
        val axis = direction.normal.toJOMLD()
        val shipOn = level.getShipObjectManagingPos(worldPosition)

        val startPos = worldPos + axis * 0.5
        val endPos = worldPos + axis * 1.5

        val otherPos = level.clipIncludeShips(
            ClipContext(
                (shipOn?.transform?.shipToWorld?.transformPosition(startPos) ?: startPos).toMinecraft(),
                (shipOn?.transform?.shipToWorld?.transformPosition(endPos) ?: endPos).toMinecraft(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
                ), false, shipOn?.id)

        val otherShip = level.getShipObjectManagingPos(otherPos.blockPos)
        val posInOwnerShip = Vector3d(worldPos)

        val (bearingPos, shiptraption: ServerShip) = if (otherShip == null) {
            val selection: DenseBlockPosSet?
            try {
                selection = collectGlued(level, attachPoint)
                lastException = null
            } catch (e: AssemblyException) {
                lastException = e
                sendData()
                return
            }
            if (selection == null) return

            val (shiptraption, previousCenterBP, newCenter, _) = PhysBearingAssembler.assembleToShip(level, selection, true, 1.0, true)
            val previousCenter = Vector3d(previousCenterBP)

            shiptraptionID = shiptraption.id

            Pair(Vector3d(worldPos).sub(previousCenter).add(newCenter), shiptraption)
        } else {
            shiptraptionID = otherShip.id
            Pair(otherPos.blockPos.toVector3d() + 0.5 - direction.normal.toJOMLD(), otherShip)
        }


        // AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);
        ClockworkSounds.PHYSICS_INFUSER_LIGHTNING.playOnServer(level, worldPosition)

        val shipOnID = shipOn?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!.toLong()

        var veryUncoolFix = 1
        val rotationQuaternion: Quaterniond = when (direction) {
            Direction.DOWN -> {
                Quaterniond(AxisAngle4d(Math.PI, Vector3d(1.0, 0.0, 0.0)))
            }

            Direction.NORTH -> {
                Quaterniond(AxisAngle4d(Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                    Quaterniond(
                        AxisAngle4d(
                            Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)
                        )
                    )
                ).normalize()
            }

            Direction.EAST -> {
                Quaterniond(AxisAngle4d(0.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                    Quaterniond(
                        AxisAngle4d(
                            Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)
                        )
                    )
                ).normalize()
            }

            Direction.SOUTH -> {
                Quaterniond(AxisAngle4d(Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0))).normalize()
            }

            Direction.WEST -> {
                veryUncoolFix = -veryUncoolFix
                Quaterniond(AxisAngle4d(1.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                    Quaterniond(
                        AxisAngle4d(
                            Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)
                        )
                    )
                ).normalize()
            }

            else -> {
                // UP or null
                Quaterniond()
            }
        }

        val posInWorld = shipOn?.transform?.shipToWorld?.transformPosition(
            posInOwnerShip - bearingPos + shiptraption.inertiaData.centerOfMass + 0.5, Vector3d()
        ) ?: (worldPos - bearingPos + shiptraption.inertiaData.centerOfMass + 0.5)
        val rotInWorld = shipOn?.transform?.shipToWorldRotation ?: Quaterniond()
        val scaling    = shipOn?.transform?.shipToWorldScaling ?: Vector3d(1.0, 1.0, 1.0)

        shiptraption.unsafeSetTransform(BodyTransformVelocityImpl(posInWorld, shiptraption.inertiaData.centerOfMass, rotInWorld, scaling, shiptraption.velocity, shiptraption.angularVelocity))

        val hingeOrientation: Quaterniondc = rotationQuaternion.mul(
            Quaterniond(AxisAngle4d(Math.toRadians(90.0), 0.0, 0.0, 1.0)),
            Quaterniond()
        ).normalize()

        val extraDist = 1.0
        val realSpeed = if (getSpeed().absoluteValue > 0.0f) getRealisticAngularSpeed() else 0.0f
        val newDriveVelocity = if (realSpeed != 0.0f) VSRevoluteJoint.VSRevoluteDriveVelocity(getRealisticAngularSpeed(), true) else null
        val joint = VSRevoluteJoint(
            shiptraptionID, VSJointPose(bearingPos.fma(-extraDist, axis, Vector3d()), hingeOrientation),
            shipOnID, VSJointPose(posInOwnerShip.fma(-extraDist, axis, Vector3d()), hingeOrientation),
            maxForceTorque = VSJointMaxForceTorque(1.0E10F, 1.0E10F), driveFreeSpin = this.movementMode!!.get() == LockedMode.UNLOCKED,
            driveVelocity = newDriveVelocity
        )
        val firstAttachmentId: VSJointId = level.shipObjectWorld.createNewConstraint(joint) ?: return

        this.joint = VSJointAndId(firstAttachmentId, joint)
        this.bearingPos = bearingPos
        bearingAxis = axis

        isRunning = true
        targetAngle = 0f
        lastStateChanged = ticks
        sendData()
        updateGeneratedRotation()
    }

    override fun destroy() {
        val level = level ?: return
        if (level.isClientSide || level !is ServerLevel) return

        joint?.let { level.shipObjectWorld.removeConstraint(it.jointId) }
    }

    fun disassemble() {
        if (!isRunning && shiptraptionID == NO_SHIPTRAPTION_ID) return
        if (ticks - lastStateChanged <= cooldown) return
        targetAngle = 0f
        if (shiptraptionID == NO_SHIPTRAPTION_ID) return
        val level = level as ServerLevel
        val ship = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return resetState()

        if (!canDisassemble(bearingAxis, ship, level.getShipObjectManagingPos(worldPosition))) {
            disassembleWhenPossible = !disassembleWhenPossible;
            aligning = !aligning
        } else {
            shipDisassemble()
        }
        AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition)
    }

    private fun shipDisassemble() {
        if (shiptraptionID == NO_SHIPTRAPTION_ID || level!!.isClientSide) { return }
        val level = level as ServerLevel
        val subShip = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return
        val mainShip = level.getShipObjectManagingPos(worldPosition)

        if (!canDisassemble(bearingAxis, subShip, mainShip)) { return }
        val direction = originalDirection ?: blockState.getValue(BearingBlock.FACING)
        val inMain = worldPosition.relative(direction, 1)
        val inSubship = bearingPos.add(bearingAxis, Vector3d())

        //todo this is stupid
        val aabb = subShip.shipAABB!!
        val blocks = DenseBlockPosSet()
        for (x in aabb.minX() - 1 until  aabb.maxX() + 1) {
            for (z in aabb.minZ() - 1 until  aabb.maxZ() + 1) {
                for (y in aabb.minY() - 1 until  aabb.maxY() + 1) {
                    blocks.add(x, y, z)
                }
            }
        }

        val subCouldSplit = subShip.getAttachment<SplittingDisablerAttachment>()?.let { if (it.canSplit()) { it.disableSplitting(); true } else {false} } ?: false
        val mainCouldSplit = mainShip?.getAttachment<SplittingDisablerAttachment>()?.let { if (it.canSplit()) { it.disableSplitting(); true } else {false} } ?: false

        val hasMoved = PhysBearingAssembler.moveBlocksFromTo(level, blocks, true, BlockPos(inSubship.toMinecraft()), inMain)

        if (subCouldSplit) { subShip.getAttachment<SplittingDisablerAttachment>()?.enableSplitting() }
        if (mainCouldSplit) { mainShip?.getAttachment<SplittingDisablerAttachment>()?.enableSplitting() }

        if (!hasMoved) {
            aligning = false
            assembleNextTick = false
            disassembleWhenPossible = false
            return
        }

        lastStateChanged = ticks
        resetState()
    }

    private fun resetState() {
        shiptraptionID = NO_SHIPTRAPTION_ID
        isRunning = false
        updateGeneratedRotation()
        assembleNextTick = false
        disassembleWhenPossible = false
        sequencedAngleLimit = -1.0f
        sequencedAngleProgress = 0f
        targetAngle = 0.0f
        sendData()
    }

    private fun tryAssembleNextTick() {
        if (!assembleNextTick) {return}
        if (ticks - lastStateChanged <= cooldown) {return}
        assembleNextTick = false
        if (!isRunning) {
            assemble()
        }
    }

    //should be public cuz reasons
    fun tryRefresh() {
        if (!isRunning || !shouldRefresh || joint == null) {return}
        val level = level as ServerLevel

        val (shipId00, pose0, _, pose1, maxForceTorque) = joint!!.joint as VSRevoluteJoint

        val shipOn = level.getShipObjectManagingPos(worldPosition)
        val shipOnID = shipOn?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
        // The ship was deleted, delete this bearing
        if (shipOn == null && level.isBlockInShipyard(worldPosition)) {
            isRunning = false
            assembleNextTick = false
            shouldRefresh = false
            return
        }

        val attachConstraint = VSRevoluteJoint(shipId00, pose0, shipOnID, pose1, maxForceTorque)

        level.shipObjectWorld.createNewConstraint(attachConstraint)?.let {
            joint = VSJointAndId(it, attachConstraint)
            shouldRefresh = false
        }
    }

    private fun tryUpdateData() {
        if (shiptraptionID == NO_SHIPTRAPTION_ID) {return}
        if (!(lastSpeed != getSpeed() || lastMode != movementMode?.get())) {return}

        lastSpeed = getSpeed()
        lastMode = movementMode!!.get()

        val realSpeed = if (abs(getSpeed()) > 0.0f) getRealisticAngularSpeed() else 0.0f
        val newDriveVelocity = if (realSpeed != 0.0f) VSRevoluteJoint.VSRevoluteDriveVelocity(getRealisticAngularSpeed(), true) else null
        updateDrive(newDriveVelocity)
    }

    private fun tickAnimationLogic() {
        if (inOutCorner < 1 && !cornerShrinking) {
            inOutCorner += 0.0075f
        } else if (inOutCorner >= 1) {
            cornerShrinking = true
        }
        if (inOutCorner > 0 && cornerShrinking) {
            inOutCorner -= 0.0075f
        } else if (inOutCorner <= 0) {
            cornerShrinking = false
        }


        if (isRunning && !open && !opening) {
            opening = true
        }
        if (opening && isRunning && openProgress < 1.0f) {
            openProgress += 0.05f
        } else if (openProgress >= 1.0f) {
            opening = false
            open = true
            openProgress = 1f
        }

        if (open && !isRunning && openProgress > 0.0f) {
            openProgress -= 0.05f
        } else if (openProgress <= 0.0f) {
            open = false
            openProgress = 0.0f
        }
    }

    fun getActualAngularSpeed(): Float {
        val dir = originalDirection!!
        return convertToAngular(getSpeed()) * if (dir == Direction.WEST || dir == Direction.NORTH || dir == Direction.DOWN) 1 else -1
    }

    fun getRealisticAngularSpeed(): Float {
        val dir = originalDirection!!
        return getSpeed() * 2f * PI.toFloat() / 60f * if (dir == Direction.WEST || dir == Direction.NORTH || dir == Direction.DOWN) 1 else -1
    }

    override fun tick() {
        super.tick()
        prevAngle = targetAngle
        ticks++
        if (level!!.isClientSide) clientAngleDiff /= 2f
        if (!level!!.isClientSide) {
            tryAssembleNextTick()
            tryRefresh()
        }
        tickAnimationLogic()
        if (!isRunning) return
        if (shiptraptionID != NO_SHIPTRAPTION_ID) {
            val angularSpeed = getActualAngularSpeed()
            var diff = 0.0f

            if (sequencedAngleLimit >= 0.0f) {
                val sequencedAngleLimit = sequencedAngleLimit * angularSpeed.sign

                sequencedAngleProgress += angularSpeed

                if (angularSpeed > 0 && sequencedAngleProgress > sequencedAngleLimit
                 || angularSpeed < 0 && sequencedAngleProgress < sequencedAngleLimit) {
                    diff = sequencedAngleProgress - sequencedAngleLimit
                    sequencedAngleProgress = sequencedAngleLimit
                }
            }
            val newAngle = targetAngle + angularSpeed - diff
            targetAngle = (newAngle % 360)
        }
        if (!level!!.isClientSide) {
            tryUpdateData()
        }
        if (disassembleWhenPossible) {
            shipDisassemble()
        }
    }

    override fun onSpeedChanged(previousSpeed: Float) {
        sequencedAngleLimit = -1.0f
        sequencedAngleProgress = 0.0f

        if (sequenceContext != null && sequenceContext.instruction == SequencerInstructions.TURN_ANGLE) {
            sequencedAngleLimit = sequenceContext.getEffectiveValue(theoreticalSpeed.toDouble()).toFloat()
        }

        if (level != null && !level!!.isClientSide && joint != null) {
            lastSpeed = getSpeed()
            val realSpeed = if (abs(getSpeed()) > 0.0f) getRealisticAngularSpeed() else 0.0f
            val newDriveVelocity = if (realSpeed != 0.0f) VSRevoluteJoint.VSRevoluteDriveVelocity(getRealisticAngularSpeed(), true) else null
            updateDrive(newDriveVelocity)
        }
        super.onSpeedChanged(previousSpeed)
    }

    override fun lazyTick() {
        super.lazyTick()
        if (shiptraptionID != NO_SHIPTRAPTION_ID && !level!!.isClientSide) sendData()
    }

    override fun attach(contraption: ControlledContraptionEntity) {}

    override fun onStall() {
        if (!level!!.isClientSide) sendData()
    }

    override fun isValid(): Boolean {
        return !isRemoved
    }

    override fun isAttachedTo(contraption: AbstractContraptionEntity): Boolean {
        return false
    }

    override fun addToTooltip(tooltip: List<Component>, isPlayerSneaking: Boolean): Boolean {
        if (super.addToTooltip(tooltip, isPlayerSneaking)) return true
        if (isPlayerSneaking) return false
        if (getSpeed() == 0f) return false
        if (isRunning) return false
        val state = blockState
        if (state.block !is BearingBlock) return false
        val attachedState = level!!.getBlockState(worldPosition.relative(state.getValue(BearingBlock.FACING)))
        if (attachedState.material.isReplaceable) return false
        TooltipHelper.addHint(tooltip, "hint.empty_bearing")
        return true
    }

    override fun setAngle(forcedAngle: Float) {
        targetAngle = forcedAngle
    }

    fun getActualAngle(): Double? {
        val level = level as ServerLevel
        val shiptraption = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return null
        val mainShip = level.getShipManagingPos(worldPosition)
        return getAngle(bearingAxis, shiptraption.transform, mainShip?.transform)
    }

    companion object {
        const val NO_SHIPTRAPTION_ID: Long = -1

        //tolerance is in degrees
        @JvmStatic
        fun canDisassemble(bearingAxis: Vector3d, mainShip: ServerShip, otherShip: ServerShip?, tolerance: Int=5): Boolean {
            if (abs(Math.toDegrees(getAngle(bearingAxis, mainShip.transform, otherShip?.transform))) > tolerance) return false
            return true
        }

        @JvmStatic
        fun getAngle(bearingAxis: Vector3d, physShipTransform: ShipTransform, otherPhysShipTransform: ShipTransform?): Double {
            val bearingAxisInGlobal = Vector3d(bearingAxis)
            otherPhysShipTransform?.shipToWorldRotation?.transform(bearingAxisInGlobal)

            // Only apply torque on the bearing axis
            // Proportional
            val perpendicularAxis: Vector3dc = if (abs(bearingAxis.x()) == 1.0) {
                Vector3d(0.0, 1.0, 0.0)
            } else if (abs(bearingAxis.y()) == 1.0) {
                Vector3d(1.0, 0.0, 0.0)
            } else if (abs(bearingAxis.z()) == 1.0) {
                Vector3d(0.0, 1.0, 0.0)
            } else {
                throw RuntimeException("how the fuck did you mess this up g")
            }
            var perpAfterRot: Vector3dc = perpendicularAxis.rotate(physShipTransform.shipToWorldRotation, Vector3d())
            perpAfterRot = otherPhysShipTransform?.shipToWorldRotation?.transformInverse(perpAfterRot, Vector3d()) ?: perpAfterRot

            val perpAfterRotInPlane: Vector3dc = perpAfterRot.sub(bearingAxis.mul(bearingAxis.dot(perpAfterRot), Vector3d()), Vector3d())
            val angleBTShipInRadians = perpAfterRotInPlane.angle(perpendicularAxis)
            val crossOfYourMother: Vector3dc = perpAfterRotInPlane.cross(perpendicularAxis, Vector3d())
            val angleWRespectToBearingAxis: Double = if (crossOfYourMother.lengthSquared() > 1e-12) {
                angleBTShipInRadians * sign(crossOfYourMother.dot(bearingAxis)) * -1
                // bro what do you expect me to do :sus:
            } else {
                0.0
            }
            return angleWRespectToBearingAxis
        }
    }
}
