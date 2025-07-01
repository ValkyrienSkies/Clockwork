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
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.*
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingData
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingUpdateData
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController.Companion.getAngle
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
import org.valkyrienskies.core.apigame.joints.*
import org.valkyrienskies.core.impl.bodies.properties.BodyTransformFactory

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
import kotlin.math.*

class PhysBearingBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    GeneratingKineticBlockEntity(type, pos, state), IBearingBlockEntity, IDisplayAssemblyExceptions,
    ContraptionController {

    var assembleNextTick = false
    var movementMode: ScrollOptionBehaviour<LockedMode>? = null
    var stopTargetAngleChange = false
    var isRunning = false
        private set
    var shiptraptionID = NO_SHIPTRAPTION_ID
        private set
    var targetAngle = 0f
        private set
    var disassembleWhenPossible = false
        private set

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
    private var bearingID: Int = -1

    private var lastSpeed = 0f
    private var lastMode = LockedMode.UNLOCKED

    private var controllerCreationData: PhysBearingData? = null
    private var controllerUpdateData: PhysBearingUpdateData? = null
    private var loadingFn: ((ServerLevel) -> Unit)? = null

    init {
        setLazyTickRate(3)
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        movementMode = ScrollOptionBehaviour(
            LockedMode::class.java, Component.translatableWithFallback("vs_clockwork:locked_mode", "Locked or Unlocked"),
            this, movementModeSlot
        )
        movementMode!!.withCallback{movementModeChanged(it)}
        movementMode!!.requiresWrench()
        behaviours.add(movementMode!!)
    }

    private fun updateDrive(driveVelocity: VSRevoluteJoint.VSRevoluteDriveVelocity? = null) {
        val driveVelocity = if (
        // unlocked needs driveVelocity to be null with no speed to spin freely for some reason
               movementMode?.get() != LockedMode.LOCKED && (driveVelocity?.velocity == 0.0f || driveVelocity == null)
            || movementMode?.get() == LockedMode.FOLLOW_ANGLE
            || aligning
            ) {
            null
        } else driveVelocity ?: VSRevoluteJoint.VSRevoluteDriveVelocity(0f)

        joint = VSJointAndId(joint!!.jointId, VSRevoluteJoint(
            joint!!.joint.shipId0, joint!!.joint.pose0,
            joint!!.joint.shipId1, joint!!.joint.pose1,
            driveFreeSpin = movementMode!!.get() != LockedMode.LOCKED,
            driveVelocity = driveVelocity,
        ))

        controllerUpdateData = PhysBearingUpdateData(
            Math.toRadians(targetAngle.toDouble()),
            getRealisticAngularSpeed(),
            movementMode!!.get() == LockedMode.FOLLOW_ANGLE || aligning
        )

        (level as ServerLevel).shipObjectWorld.updateConstraint(joint!!.jointId, joint!!.joint)
    }

    private fun movementModeChanged(value: Int) {
        if (level == null || level!!.isClientSide) {return}
        sendData()
    }

    override fun remove() {
        if (!level!!.isClientSide) { destroy() }
        super.remove()
    }

    public override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)

        tag.putBoolean(ClockworkConstants.Nbt.RUNNING, isRunning)
        tag.putFloat(ClockworkConstants.Nbt.ANGLE, targetAngle)
        tag.putBoolean("stopTargetAngleChange", stopTargetAngleChange)
        if (shiptraptionID != NO_SHIPTRAPTION_ID) {
            tag.putLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID, shiptraptionID)
        }
        if (originalDirection != null) {
            tag.putInt(ORIGINAL_DIRECTION, originalDirection!!.ordinal)
        }
        AssemblyException.write(tag, lastException)
        tag.putBoolean(ClockworkConstants.Nbt.OPEN, open)
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

    private fun loadTheRest(tag: CompoundTag, level: ServerLevel) {
        shouldRefresh = true
        var joint = this.joint?.joint as? VSRevoluteJoint ?: return
        val mainId = level.getShipManagingPos(worldPosition)?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!

        //TODO is this fine or dumb?
        val makeData = { joint: VSRevoluteJoint? -> PhysBearingData(
            bearingAxis.get(Vector3d()),
            Math.toRadians(targetAngle.toDouble()),
            getRealisticAngularSpeed(),
            movementMode!!.get() == LockedMode.FOLLOW_ANGLE,
            aligning,
            if (level.shipObjectWorld.dimensionToGroundBodyIdImmutable.values.contains(mainId)) {-1L} else {mainId},
            joint?.pose1?.pos?.get(Vector3d()) ?: Vector3d(),
            joint?.pose0?.pos?.get(Vector3d()) ?: Vector3d()
        ) }

        controllerCreationData = makeData(joint)

        val oldPos = BlockPos.of(tag.getLong(ClockworkConstants.Nbt.OLD_POS)).toJOMLD()
        if (oldPos == worldPosition) {return}

        val subship = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return

        val newPos = worldPosition.toJOMLD()

        val oldSPos = tag.getVector3d(ClockworkConstants.Nbt.OLD_SHIPTRAPTION_CENTER)!!
        val newSPos = tag.getVector3d(ClockworkConstants.Nbt.NEW_SHIPTRAPTION_CENTER)!!

        joint = joint.let{it.copy(subship.id, pose0 = VSJointPose(it.pose0.pos - oldSPos + newSPos, it.pose0.rot), mainId, pose1 = VSJointPose(it.pose1.pos - oldPos + newPos, it.pose1.rot))}
        this.joint = this.joint?.let{VSJointAndId(it.jointId, joint)}

        controllerCreationData = makeData(joint)
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
        stopTargetAngleChange = tag.getBoolean("stopTargetAngleChange")
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
        sequencedAngleLimit = tag.getFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_LIMIT)
        sequencedAngleProgress = tag.getFloat(ClockworkConstants.Nbt.SEQUENCED_ANGLE_PROGRESS)

        bearingPos = tag.getVector3d("bearingPos")!!
        bearingAxis = tag.getVector3d("bearingAxis")!!
        aligning = tag.getBoolean("aligning")

        val mapper = VSJacksonUtil.dtoMapper
        this.joint = if (tag.contains("constraint")) {mapper.readValue(tag.getByteArray("constraint"), VSJointAndId::class.java)} else {null}

        super.read(tag, clientPacket)
        if (clientPacket) {return}

        // may not have level on read so i have to do this
        loadingFn = { level -> loadTheRest(tag, level) }

        val level = level as? ServerLevel ?: return
        loadingFn!!(level)
        loadingFn = null
    }

    override fun getInterpolatedAngle(partialTicks: Float): Float {
        var partialTicks = partialTicks
        if (isVirtual) return Mth.lerp(partialTicks + .5f, prevAngle, targetAngle)
        if (shiptraptionID == NO_SHIPTRAPTION_ID || !isRunning) partialTicks = 0f
        return Mth.lerp(partialTicks, targetAngle, targetAngle + angularSpeed)
    }

    fun getWingRotOffset(): Float = when {
         isRunning && open -> openProgressMax.toDouble().toFloat()
         isRunning         -> Mth.lerp(openProgress.toDouble(), 0.0, openProgressMax.toDouble()).toFloat()
        !isRunning && open -> Mth.lerp(openProgress.toDouble(), 1.0, openProgressMax.toDouble()).toFloat()
        else -> 0.0f
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

    private fun getHingeRotation(localDirection: Direction): Quaterniond {
        val rotationQuaternion: Quaterniond = when (localDirection) {
            Direction.UP -> {
                Quaterniond()
            }
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
                Quaterniond(AxisAngle4d(1.5 * Math.PI, Vector3d(0.0, 1.0, 0.0))).mul(
                    Quaterniond(
                        AxisAngle4d(
                            Math.PI / 2.0, Vector3d(1.0, 0.0, 0.0)
                        )
                    )
                ).normalize()
            }
        }

        val hingeOrientation: Quaterniond = rotationQuaternion.mul(
            Quaterniond(AxisAngle4d(Math.toRadians(90.0), 0.0, 0.0, 1.0)),
            Quaterniond()
        ).normalize()

        return hingeOrientation
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

        val (bearingPos, shiptraption, otherDirection) = if (otherShip == null) {
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
            Triple(Vector3d(worldPos).sub(previousCenter).add(newCenter), shiptraption, direction)
        } else {
            shiptraptionID = otherShip.id
            Triple(otherPos.blockPos.toVector3d() + 0.5 - direction.normal.toJOMLD(), otherShip, otherPos.direction)
        }


        // AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);
        ClockworkSounds.PHYSICS_INFUSER_LIGHTNING.playOnServer(level, worldPosition)

        val shipOnID = shipOn?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!.toLong()

        val posInWorld = shipOn?.transform?.shipToWorld?.transformPosition(
            posInOwnerShip - bearingPos + shiptraption.inertiaData.centerOfMass + 0.5, Vector3d()
        ) ?: (worldPos - bearingPos + shiptraption.inertiaData.centerOfMass + 0.5)
        val rotInWorld = shipOn?.transform?.shipToWorldRotation ?: Quaterniond()
        val scaling    = shipOn?.transform?.shipToWorldScaling ?: Vector3d(1.0, 1.0, 1.0)

        shiptraption.unsafeSetTransform(BodyTransformFactory.create(
            posInWorld, rotInWorld, scaling, shiptraption.transform.positionInModel
        ))

        val ship1rot = getHingeRotation(direction)
        val ship2rot = getHingeRotation(direction)

        val extraDist = 1.0
        val realSpeed = if (getSpeed().absoluteValue > 0.0f) getRealisticAngularSpeed() else 0.0f
        val newDriveVelocity = if (realSpeed != 0.0f) VSRevoluteJoint.VSRevoluteDriveVelocity(getRealisticAngularSpeed(), true) else null
        val angle = if (movementMode!!.get() == LockedMode.FOLLOW_ANGLE) { Math.toRadians(targetAngle.toDouble()).toFloat().let { VSD6Joint.AngularLimitPair(it, it.nextUp()) } } else {null}
        val joint = VSRevoluteJoint(
            shiptraptionID, VSJointPose(bearingPos.fma(-extraDist, axis, Vector3d()), ship1rot),
            shipOnID, VSJointPose(posInOwnerShip.fma(-extraDist, axis, Vector3d()), ship2rot),
            driveFreeSpin = this.movementMode!!.get() == LockedMode.UNLOCKED,
            driveVelocity = newDriveVelocity,
            angularLimitPair = angle
        )
        val firstAttachmentId: VSJointId = level.shipObjectWorld.createNewConstraint(joint) ?: return

        this.joint = VSJointAndId(firstAttachmentId, joint)
        this.bearingPos = bearingPos
        bearingAxis = axis

        isRunning = true
        targetAngle = 0f
        lastStateChanged = ticks

        controllerCreationData = PhysBearingData(
            bearingAxis.get(Vector3d()),
            Math.toRadians(targetAngle.toDouble()),
            getRealisticAngularSpeed(),
            movementMode!!.get() == LockedMode.FOLLOW_ANGLE,
            aligning,
            if (level.shipObjectWorld.dimensionToGroundBodyIdImmutable.values.contains(shipOnID)) {-1L} else {shipOnID},
            joint.pose1.pos.get(Vector3d()),
            joint.pose0.pos.get(Vector3d())
        )

        sendData()
        updateGeneratedRotation()
    }

    override fun destroy() {
        val level = level ?: return
        if (level.isClientSide || level !is ServerLevel) return

        val ship = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return
        BearingController.getOrCreate(ship)!!.removePhysBearing(bearingID)

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
            disassembleWhenPossible = !disassembleWhenPossible
            aligning = !aligning
            BearingController.getOrCreate(ship)!!.bearingData[bearingID]?.let { it.aligning = this.aligning }
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
        } } }

        val subCouldSplit = subShip.getAttachment<SplittingDisablerAttachment>()?.let { if (it.canSplit()) { it.disableSplitting(); true } else {false} } ?: false
        val mainCouldSplit = mainShip?.getAttachment<SplittingDisablerAttachment>()?.let { if (it.canSplit()) { it.disableSplitting(); true } else {false} } ?: false

        val hasMoved = PhysBearingAssembler.moveBlocksFromTo(level, blocks, true, BlockPos.containing(inSubship.toMinecraft()), inMain)

        if (subCouldSplit) { subShip.getAttachment<SplittingDisablerAttachment>()?.enableSplitting() }
        if (mainCouldSplit) { mainShip?.getAttachment<SplittingDisablerAttachment>()?.enableSplitting() }

        if (!hasMoved) {
            aligning = false
            assembleNextTick = false
            disassembleWhenPossible = false
            return
        }
        BearingController.getOrCreate(subShip)!!.removePhysBearing(bearingID)

        aligning = false
        targetAngle = 0f

        lastStateChanged = ticks
        resetState()
    }

    private fun resetState() {
        bearingID = -1
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
        if (!isRunning) {assemble()}
    }

    //should be public cuz reasons
    fun tryRefresh() {
        if (!isRunning || !shouldRefresh || joint == null) {return}
        val level = level as ServerLevel
        if (joint!!.joint !is VSRevoluteJoint) return

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

        val joint = VSRevoluteJoint(shipId00, pose0, shipOnID, pose1, maxForceTorque)

        level.shipObjectWorld.createNewConstraint(joint)?.let {
            this.joint = VSJointAndId(it, joint)
            shouldRefresh = false
        }
    }

    private fun tryUpdateData() {
        if (shiptraptionID == NO_SHIPTRAPTION_ID) {return}
        if (   (lastSpeed == getSpeed() && lastMode == movementMode?.get())
            && (movementMode!!.get() != LockedMode.FOLLOW_ANGLE && !aligning)
        ) {return}

        if (lastMode != movementMode?.get() && movementMode?.get() == LockedMode.FOLLOW_ANGLE) {
            val shipOn = level!!.getShipObjectManagingPos(blockPos)?.transform
            val shiptraption = level!!.shipObjectWorld.allShips.getById(shiptraptionID)!!.transform

            targetAngle = Math.toDegrees(getAngle(bearingAxis, shiptraption, shipOn)).toFloat()
        }

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
            loadingFn?.also {
                it(level as ServerLevel)
                loadingFn = null
            }

            val subShip = (level as ServerLevel).shipObjectWorld.loadedShips.getById(shiptraptionID)
            controllerCreationData?.also {
                bearingID = BearingController
                    .getOrCreate(subShip ?: return@also)!!
                    .addPhysBearing(it)
                controllerCreationData = null
            }
            controllerUpdateData?.also {
                BearingController
                    .getOrCreate(subShip ?: return@also)!!
                    .updatePhysBearing(bearingID, it)
                controllerUpdateData = null
            }
            tryAssembleNextTick()
            tryRefresh()
        }
        tickAnimationLogic()
        if (!isRunning) return
        if (shiptraptionID == NO_SHIPTRAPTION_ID) {
            targetAngle = 0f
        } else if (!stopTargetAngleChange) {
            val angularSpeed = -getActualAngularSpeed()
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
            targetAngle = when {
                newAngle >= 360f -> 0f
                newAngle < 0f -> 360f
                else -> newAngle
            }
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
            val newDriveVelocity = if (realSpeed != 0.0f) VSRevoluteJoint.VSRevoluteDriveVelocity(getRealisticAngularSpeed(), true) else VSRevoluteJoint.VSRevoluteDriveVelocity(0f, true)
            updateDrive(newDriveVelocity)
        }
        super.onSpeedChanged(previousSpeed)
    }

    override fun lazyTick() {
        super.lazyTick()
        if (shiptraptionID != NO_SHIPTRAPTION_ID && !level!!.isClientSide) sendData()
    }

    override fun addToTooltip(tooltip: List<Component>, isPlayerSneaking: Boolean): Boolean {
        if (super.addToTooltip(tooltip, isPlayerSneaking)) return true
        if (isPlayerSneaking) return false
        if (getSpeed() == 0f) return false
        if (isRunning) return false
        if (blockState.block !is BearingBlock) return false
        val attachedState = level!!.getBlockState(worldPosition.relative(blockState.getValue(BearingBlock.FACING)))
        if (attachedState.canBeReplaced()) return false
        TooltipHelper.addHint(tooltip, "hint.empty_bearing")
        return true
    }

    fun getActualAngle(): Double? {
        val level = level as ServerLevel
        val shiptraption = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return null
        val mainShip = level.getShipManagingPos(worldPosition)
        return getAngle(bearingAxis, shiptraption.transform, mainShip?.transform)
    }

    override fun attach(contraption: ControlledContraptionEntity) {}
    override fun onStall() { if (!level!!.isClientSide) sendData() }
    override fun isValid(): Boolean = !isRemoved
    override fun isAttachedTo(contraption: AbstractContraptionEntity): Boolean = false
    override fun setAngle(forcedAngle: Float) { targetAngle = forcedAngle }
    override fun getLastAssemblyException(): AssemblyException? = lastException
    override fun getBlockPosition(): BlockPos = worldPosition
    override fun isWoodenTop(): Boolean = false

    companion object {
        const val NO_SHIPTRAPTION_ID: Long = -1

        //tolerance is in degrees
        @JvmStatic
        fun canDisassemble(bearingAxis: Vector3d, mainShip: ServerShip, otherShip: ServerShip?, tolerance: Int=5): Boolean {
            if (abs(Math.toDegrees(getAngle(bearingAxis, mainShip.transform, otherShip?.transform))) > tolerance) return false
            return true
        }
    }
}
