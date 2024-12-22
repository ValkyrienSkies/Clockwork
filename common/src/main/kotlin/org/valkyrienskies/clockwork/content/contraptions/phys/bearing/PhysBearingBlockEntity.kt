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
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingCreateData
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingData
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingUpdateData
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController
import org.valkyrienskies.clockwork.platform.api.ContraptionController
import org.valkyrienskies.clockwork.platform.api.ContraptionController.LockedMode
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.ClockworkConstants.Nbt.ORIGINAL_DIRECTION
import org.valkyrienskies.clockwork.util.ClockworkUtils.getVector3d
import org.valkyrienskies.clockwork.util.GlueAssembler.collectGlued
import org.valkyrienskies.clockwork.util.minus
import org.valkyrienskies.clockwork.util.plus
import org.valkyrienskies.clockwork.util.times
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.api.ships.getAttachment
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.core.apigame.constraints.VSConstraintAndId
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.core.apigame.constraints.VSHingeOrientationConstraint
import org.valkyrienskies.core.impl.game.ships.ShipDataCommon
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl.Companion.create
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

    private var bearingID: Int? = null
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
        movementMode!!.requiresWrench()
        behaviours.add(movementMode!!)
    }

    override fun remove() {
        if (!level!!.isClientSide) { destroy() }
        super.remove()
    }

    public override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)

        tag.putBoolean(ClockworkConstants.Nbt.RUNNING, isRunning)
        tag.putFloat(ClockworkConstants.Nbt.ANGLE, targetAngle)
        if (bearingID != null) {
            tag.putInt(ClockworkConstants.Nbt.BEARING_ID, bearingID!!)
        }
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


        if (shiptraptionID == NO_SHIPTRAPTION_ID) return
        val subship = (level as ServerLevel).shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return
        val controller = BearingController.getOrCreate(subship)!!
        val data = controller.bearingData[bearingID] ?: return

        val mapper = VSJacksonUtil.dtoMapper

        tag.putByteArray(ClockworkConstants.Nbt.DATA, mapper.writeValueAsBytes(data))
        tag.putLong(ClockworkConstants.Nbt.OLD_POS, worldPosition.asLong())
        //to make it more general
        tag.putVector3d(ClockworkConstants.Nbt.OLD_SHIPTRAPTION_CENTER, data.bearingPosition!!)
        tag.putVector3d(ClockworkConstants.Nbt.NEW_SHIPTRAPTION_CENTER, data.bearingPosition!!)
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
        if (tag.contains(ClockworkConstants.Nbt.BEARING_ID)) {
            bearingID = tag.getInt(ClockworkConstants.Nbt.BEARING_ID)
        }
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
        shouldRefresh = true
        super.read(tag, clientPacket)

        if (clientPacket) {return}

        val oldPos = BlockPos.of(tag.getLong(ClockworkConstants.Nbt.OLD_POS)).toJOMLD()
        if (oldPos == worldPosition || level == null || level!!.isClientSide) {return}

        val level = level as ServerLevel
        val subship = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return
        val mainId = level.getShipManagingPos(worldPosition)?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!

        val mapper = VSJacksonUtil.dtoMapper

        val data = mapper.readValue(tag.getByteArray(ClockworkConstants.Nbt.DATA), PhysBearingData::class.java)
        val newPos = worldPosition.toJOMLD()

        val oldSPos = tag.getVector3d(ClockworkConstants.Nbt.OLD_SHIPTRAPTION_CENTER)!!
        val newSPos = tag.getVector3d(ClockworkConstants.Nbt.NEW_SHIPTRAPTION_CENTER)!!

        data.attachConstraint       = data.attachConstraint      ?.let{it.copy(subship.id, mainId, localPos0 = it.localPos0 - oldSPos + newSPos, localPos1 = it.localPos1 - oldPos + newPos)}
        data.secondAttachConstraint = data.secondAttachConstraint?.let{it.copy(subship.id, mainId, localPos0 = it.localPos0 - oldSPos + newSPos, localPos1 = it.localPos1 - oldPos + newPos)}
        data.hingeConstraint        = data.hingeConstraint?.copy(subship.id, mainId)

        val controller = BearingController.getOrCreate(subship)!!
        bearingID = controller.addPhysBearing(
            PhysBearingCreateData(
                data.bearingPosition!!, data.bearingAxis!!, data.bearingAngle, data.bearingRPM, data.locked, data.shiptraptionID,
                VSConstraintAndId(-1, data.attachConstraint!!),
                VSConstraintAndId(-1, data.hingeConstraint!!),
                null, null,
                VSConstraintAndId(-1, data.secondAttachConstraint!!)
            )
        )
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

        val direction = blockState.getValue(BearingBlock.FACING)
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

        val otherShip = level.getShipManagingPos(otherPos.blockPos)
        val posInOwnerShip = Vector3d(worldPos)

        val (bearingPos, shiptraption) = if (otherShip == null) {
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
            posInOwnerShip - bearingPos + shiptraption.inertiaData.centerOfMassInShip + 0.5, Vector3d()
        ) ?: (worldPos - bearingPos + shiptraption.inertiaData.centerOfMassInShip + 0.5)
        val rotInWorld = shipOn?.transform?.shipToWorldRotation ?: Quaterniond()
        val scaling    = shipOn?.transform?.shipToWorldScaling ?: Vector3d(1.0, 1.0, 1.0)

        (shiptraption as ShipDataCommon).transform = create(posInWorld, shiptraption.inertiaData.centerOfMassInShip, rotInWorld, scaling)

        val hingeOrientation: Quaterniondc = rotationQuaternion.mul(
            Quaterniond(AxisAngle4d(Math.toRadians(90.0), 0.0, 0.0, 1.0)),
            Quaterniond()
        ).normalize()

        val hingeConstraint = VSHingeOrientationConstraint(
            shiptraptionID, shipOnID,
            1e-100,
            hingeOrientation, hingeOrientation,
            1e100
        )

        // TODO: Maybe change this based on ship size?
        val extraDist = 1.0
        val firstAttachment = VSAttachmentConstraint(
            shiptraptionID, shipOnID,
            1e-100,
            bearingPos.fma(-extraDist, axis, Vector3d()),
            posInOwnerShip.fma(-extraDist, axis, Vector3d()),
            1e100,
            0.0
        )

        val secondAttachment = VSAttachmentConstraint(
            shiptraptionID, shipOnID,
            1e-100,
            bearingPos.fma(extraDist, axis, Vector3d()),
            posInOwnerShip.fma(extraDist, axis, Vector3d()),
            1e100,
            0.0
        )

        // Add position damping to make the hinge more stable
        // VSPosDampingConstraint posDampingConstraint = new VSPosDampingConstraint(shiptraptionID, otherShipID, 1e-10, posInBearingContraption, posInOwnerShip, 1e10, 1e-2);

        // Add rotation damping to make the hinge more stable
        // VSRotDampingConstraint perpendicularRotDampingConstraint = new VSRotDampingConstraint(shiptraptionID, otherShipID, 1e-10, hingeOrientation, hingeOrientation, 1e10, 1e-2, VSRotDampingAxes.ALL_AXES);
        val firstAttachmentId: VSConstraintId = level.shipObjectWorld.createNewConstraint(firstAttachment) ?: return
        val hingeID: VSConstraintId = level.shipObjectWorld.createNewConstraint(hingeConstraint) ?: return
        val secondAttachmentID: VSConstraintId = level.shipObjectWorld.createNewConstraint(secondAttachment) ?: return
        // Integer posDamperID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).createNewConstraint(posDampingConstraint);
        // Integer rotDamperID = VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).createNewConstraint(perpendicularRotDampingConstraint);
        val firstAttachmentConstraint = VSConstraintAndId(firstAttachmentId, firstAttachment)
        val hingeContraptionConstraint = VSConstraintAndId(hingeID, hingeConstraint)
        val secondAttachmentConstraint = VSConstraintAndId(secondAttachmentID, secondAttachment)
        // VSConstraintAndId posDampingContraptionConstraint = new VSConstraintAndId(posDamperID, posDampingConstraint);
        // VSConstraintAndId rotDampingContraptionConstraint = new VSConstraintAndId(rotDamperID, perpendicularRotDampingConstraint);
        val data = PhysBearingCreateData(
            bearingPos,
            axis,
            targetAngle.toDouble(),
            veryUncoolFix * getSpeed(),
            movementMode!!.get() == LockedMode.LOCKED,
            shiptraptionID,
            firstAttachmentConstraint,
            hingeContraptionConstraint,
            null,
            null,
            secondAttachmentConstraint,
        )
        bearingID = BearingController.getOrCreate(if(otherShip == null) shiptraption else level.shipObjectWorld.loadedShips.getById(shiptraptionID)!!)!!.addPhysBearing(data)
        isRunning = true
        targetAngle = 0f
        lastStateChanged = ticks
        originalDirection = direction
        sendData()
        updateGeneratedRotation()
    }

    override fun destroy() {
        val level = level
        if (level == null || bearingID == null) return
        if (level.isClientSide || level !is ServerLevel) return

        val ship = level.shipObjectWorld.allShips.getById(shiptraptionID) ?: return
        val controller: BearingController = BearingController.getOrCreate(ship)!!

        level.shipObjectWorld.removeConstraint(controller.bearingData[bearingID]?.attachID ?: return)
        level.shipObjectWorld.removeConstraint(controller.bearingData[bearingID]?.hingeID ?: return)
        level.shipObjectWorld.removeConstraint(controller.bearingData[bearingID]?.secondAttachId ?: return)
        controller.removePhysBearing(bearingID!!)
    }

    fun disassemble() {
        if (!isRunning && shiptraptionID == NO_SHIPTRAPTION_ID) return
        if (ticks - lastStateChanged <= cooldown) return
        targetAngle = 0f
        if (shiptraptionID == NO_SHIPTRAPTION_ID) return
        val level = level as ServerLevel
        val ship = level.shipObjectWorld.allShips.getById(shiptraptionID) ?: return resetState()

        val controller = BearingController.getOrCreate(ship)!!
        if (!controller.canDisassemble(bearingID, ship, level.getShipObjectManagingPos(worldPosition))) {
            disassembleWhenPossible = !disassembleWhenPossible;
            controller.bearingData[bearingID]?.let { it.aligning = !it.aligning }
        } else {
            shipDisassemble()
        }
        AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition)
    }

    private fun shipDisassemble() {
        if (shiptraptionID == NO_SHIPTRAPTION_ID || level!!.isClientSide || bearingID == null) { return }
        val level = level as ServerLevel
        val subShip = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return
        val mainShip = level.getShipObjectManagingPos(worldPosition)

        val controller: BearingController = BearingController.getOrCreate(subShip)!!
        if (!controller.canDisassemble(bearingID, subShip, mainShip)) { return }
        val curData = controller.bearingData[bearingID] ?: return
        val direction = originalDirection ?: blockState.getValue(BearingBlock.FACING)
        val inMain = worldPosition.relative(direction, 1)
        val inSubship = curData.bearingPosition!!.add(curData.bearingAxis!!, Vector3d())

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
            curData.aligning = false
            assembleNextTick = false
            disassembleWhenPossible = false
            return
        }

        controller.removePhysBearing(bearingID!!)
        lastStateChanged = ticks
        resetState()
    }

    private fun resetState() {
        bearingID = null
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

    private fun tryRefresh() {
        if (!shouldRefresh) {return}
        val level = level as ServerLevel
        val ship = level.shipObjectWorld.allShips.getById(shiptraptionID) ?: return
        if (bearingID == null) {return}

        val bearingData = BearingController.getOrCreate(ship)!!.bearingData[bearingID] ?: return

        val (shipId00, _, compliance0, localPos01, localPos11, maxForce1, fixedDistance1) = bearingData.attachConstraint!!
        val (shipId01, _, compliance1, localPos02, localPos12, maxForce2, fixedDistance2) = bearingData.secondAttachConstraint!!
        val (shipId02, _, compliance2, localRot0, localRot1, maxTorque) = bearingData.hingeConstraint!!

        val shipOn = level.getShipObjectManagingPos(worldPosition)
        val shipOnID = shipOn?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!
        // The ship was deleted, delete this bearing
        if (shipOn == null && level.isBlockInShipyard(worldPosition)) {
            isRunning = false
            assembleNextTick = false
            shouldRefresh = false
            return
        }

        val attachConstraint = VSAttachmentConstraint(
            shipId00, shipOnID,
            compliance0,
            localPos01, localPos11, maxForce1, fixedDistance1
        )
        val hingeConstraint = VSHingeOrientationConstraint(
            shipId02, shipOnID,
            compliance2,
            localRot0, localRot1, maxTorque
        )
        val secondAttachConstraint = VSAttachmentConstraint(
            shipId01, shipOnID,
            compliance1,
            localPos02, localPos12, maxForce2, fixedDistance2
        )

        var createdFirstAttachment = false
        var createdSecondAttachment = false
        var createdHinge = false

        level.shipObjectWorld.createNewConstraint(attachConstraint)?.let {
            BearingController.getOrCreate(ship)!!.bearingData[bearingID]!!.attachConstraint = attachConstraint
            BearingController.getOrCreate(ship)!!.bearingData[bearingID]!!.attachID = it
            createdFirstAttachment = true
        }

        level.shipObjectWorld.createNewConstraint(secondAttachConstraint)?.let {
            BearingController.getOrCreate(ship)!!.bearingData[bearingID]!!.secondAttachConstraint = secondAttachConstraint
            BearingController.getOrCreate(ship)!!.bearingData[bearingID]!!.secondAttachId = it
            createdSecondAttachment = true
        }

        level.shipObjectWorld.createNewConstraint(hingeConstraint)?.let {
            BearingController.getOrCreate(ship)!!.bearingData[bearingID]!!.hingeConstraint = hingeConstraint
            BearingController.getOrCreate(ship)!!.bearingData[bearingID]!!.attachID = it
            createdHinge = true
        }

        if (createdHinge && createdFirstAttachment && createdSecondAttachment) {
            shouldRefresh = false
        }
    }

    private fun tryUpdateData() {
        if (shiptraptionID == NO_SHIPTRAPTION_ID) { return }
        val level = level as ServerLevel
        val ship = level.shipObjectWorld.allShips.getById(shiptraptionID) ?: return
        val bearingController = BearingController.getOrCreate(ship)!!
        val bearingData = bearingController.bearingData[bearingID] ?: return

        //DUMB FIX FOR INV ROTATION
        val dir = originalDirection!!
        val dumbFix = if (dir == Direction.WEST || dir == Direction.NORTH || dir == Direction.DOWN) -1 else 1

        val data = PhysBearingUpdateData(
            targetAngle.toDouble(), dumbFix * getSpeed(),
            movementMode!!.get() == LockedMode.LOCKED
        )
        bearingController.updatePhysBearing(bearingID!!, data)
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
        return convertToAngular(getSpeed()) * if (dir == Direction.WEST || dir == Direction.NORTH || dir == Direction.DOWN) -1 else 1
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
        if (shiptraptionID != NO_SHIPTRAPTION_ID && !manualTargetAngleChange) {
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
        super.onSpeedChanged(previousSpeed)

        sequencedAngleLimit = -1.0f
        sequencedAngleProgress = 0.0f

        if (sequenceContext != null && sequenceContext.instruction == SequencerInstructions.TURN_ANGLE) {
            sequencedAngleLimit = sequenceContext.getEffectiveValue(theoreticalSpeed.toDouble()).toFloat()
        }
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

    //TODO literally not used anywhere??????
    override val isShipContraptionController: Boolean
        get() = true
    override val connectedShip: Ship?
        get() = null

    fun getActualAngle(): Double? {
        val ship = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return null
        val data = BearingController.getOrCreate(ship as ServerShip)!!.bearingData[bearingID] ?: return null
        return data.actualAngle
    }

    companion object {
        const val NO_SHIPTRAPTION_ID: Long = -1
    }
}
