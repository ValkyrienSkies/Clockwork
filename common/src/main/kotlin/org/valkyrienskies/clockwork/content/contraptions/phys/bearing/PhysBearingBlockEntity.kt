package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.ControlledContraptionEntity
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions
import com.simibubi.create.content.contraptions.bearing.BearingBlock
import com.simibubi.create.content.contraptions.bearing.IBearingBlockEntity
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity
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
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.*
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingCreateData
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingUpdateData
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController
import org.valkyrienskies.clockwork.platform.api.ContraptionController
import org.valkyrienskies.clockwork.platform.api.ContraptionController.LockedMode
import org.valkyrienskies.clockwork.util.ClockworkConstants
import org.valkyrienskies.clockwork.util.GlueAssembler.collectGlued
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.apigame.constraints.VSAttachmentConstraint
import org.valkyrienskies.core.apigame.constraints.VSConstraintAndId
import org.valkyrienskies.core.apigame.constraints.VSConstraintId
import org.valkyrienskies.core.apigame.constraints.VSHingeOrientationConstraint
import org.valkyrienskies.core.impl.game.ships.ShipDataCommon
import org.valkyrienskies.core.impl.game.ships.ShipTransformImpl.Companion.create
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toBlockPos
import org.valkyrienskies.mod.common.util.toJOMLD
import java.lang.Math
import kotlin.math.sign

class PhysBearingBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    GeneratingKineticBlockEntity(type, pos, state), IBearingBlockEntity, IDisplayAssemblyExceptions,
    ContraptionController {

    var movementMode: ScrollOptionBehaviour<LockedMode>? = null
    var shouldRefresh = false
    protected var bearingAngle = 0f
    var isRunning = false
        protected set
    var assembleNextTick = false
    protected var clientAngleDiff = 0f
    protected var lastException: AssemblyException? = null
    protected var disassembleWhenPossible = false
    private var prevAngle = 0f
    private var shiptraptionID = NO_SHIPTRAPTION_ID
    private var bearingID: Int? = null
    var coreAngle = 0f
    var wingAngle = 0f
    var previousCoreAngle = 0f
    var opening = false
    var open = false
    var closing = false
    private var openProgress = 0f
    private var openProgressMax = 70f
    private val closeProgress = 0f
    private var inOutCorner = 0f
    private var cornerShrinking = false

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
//        if (!level.isClientSide)
//            disassemble();
        super.remove()
    }

    public override fun write(compound: CompoundTag, clientPacket: Boolean) {
        compound.putBoolean(ClockworkConstants.Nbt.RUNNING, isRunning)
        compound.putFloat(ClockworkConstants.Nbt.ANGLE, bearingAngle)
        if (bearingID != null) {
            compound.putInt(ClockworkConstants.Nbt.BEARING_ID, bearingID!!)
        }
        if (shiptraptionID != NO_SHIPTRAPTION_ID) {
            compound.putLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID, shiptraptionID)
        }
        AssemblyException.write(compound, lastException)
        compound.putBoolean(ClockworkConstants.Nbt.OPEN, open)
        super.write(compound, clientPacket)
    }

    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        if (wasMoved) {
            super.read(compound, clientPacket)
            return
        }
        val angleBefore = bearingAngle
        open = compound.getBoolean(ClockworkConstants.Nbt.OPEN)
        isRunning = compound.getBoolean(ClockworkConstants.Nbt.RUNNING)
        bearingAngle = compound.getFloat(ClockworkConstants.Nbt.ANGLE)
        lastException = AssemblyException.read(compound)
        if (compound.contains(ClockworkConstants.Nbt.BEARING_ID)) {
            bearingID = compound.getInt(ClockworkConstants.Nbt.BEARING_ID)
        }
        if (compound.contains(ClockworkConstants.Nbt.SHIPTRAPTION_ID)) {
            shiptraptionID = compound.getLong(ClockworkConstants.Nbt.SHIPTRAPTION_ID)
        }
        if (isRunning) {
            if (shiptraptionID == NO_SHIPTRAPTION_ID) {
                clientAngleDiff = AngleHelper.getShortestAngleDiff(angleBefore.toDouble(), bearingAngle.toDouble())
                bearingAngle = angleBefore
            }
        } else {
            shiptraptionID = NO_SHIPTRAPTION_ID
        }
        shouldRefresh = true
        super.read(compound, clientPacket)
        if (!clientPacket) return
    }

    override fun getInterpolatedAngle(partialTicks: Float): Float {
        var partialTicks = partialTicks
        if (isVirtual) return Mth.lerp(partialTicks + .5f, prevAngle, bearingAngle)
        if (shiptraptionID == NO_SHIPTRAPTION_ID || !isRunning) partialTicks = 0f
        return Mth.lerp(partialTicks, bearingAngle, bearingAngle + angularSpeed)
    }

    fun getOpeningProgress() : Float {
        return openProgress
    }

    fun getWingRotOffset(): Float {
        return if (open) {
            openProgressMax.toDouble().toFloat()
        } else if (isRunning) {
            Mth.lerp(openProgress.toDouble(), 0.0, openProgressMax.toDouble()).toFloat()
        } else {
            0f
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

    override fun onSpeedChanged(prevSpeed: Float) {
        super.onSpeedChanged(prevSpeed)
        if (shiptraptionID != NO_SHIPTRAPTION_ID && sign(prevSpeed) != sign(getSpeed()) && prevSpeed != 0f) {
//            movedContraption.getContraption()
//                    .stop(level);
        }
        // todo : stop shiptraption
    }

    val angularSpeed: Float
        get() {
            var speed = convertToAngular(if (isWindmill) generatedSpeed else getSpeed())
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

    protected val isWindmill: Boolean
        get() = false

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

        val (shiptraption, previousCenterBP, newCenter, _) = PhysBearingAssembler.assembleToShip(level, selection.map { it.toBlockPos() }, true, 1.0, true)
        val previousCenter = Vector3d(previousCenterBP)

        // AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);
        ClockworkSounds.PHYSICS_INFUSER_LIGHTNING.playOnServer(level, worldPosition)
        shiptraptionID = shiptraption.id

        val otherShipID = shipOn?.id ?: level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]!!.toLong()

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

        val posInOwnerShip = Vector3d(worldPos)
        val bearingPos = Vector3d(worldPos).sub(previousCenter).add(newCenter)

        val posInWorld = shipOn?.transform?.shipToWorld?.transformPosition(posInOwnerShip, Vector3d()) ?: shiptraption.transform.positionInWorld // posInOwnerShip;
        val rotInWorld = shipOn?.transform?.shipToWorldRotation ?: Quaterniond()
        val scaling    = shipOn?.transform?.shipToWorldScaling ?: Vector3d(1.0, 1.0, 1.0)

        (shiptraption as ShipDataCommon).transform = create(posInWorld, shiptraption.inertiaData.centerOfMassInShip, rotInWorld, scaling)

        val hingeOrientation: Quaterniondc = rotationQuaternion.mul(
            Quaterniond(AxisAngle4d(Math.toRadians(90.0), 0.0, 0.0, 1.0)),
            Quaterniond()
        ).normalize()

        val hingeConstraint = VSHingeOrientationConstraint(
            shiptraptionID, otherShipID,
            1e-100,
            hingeOrientation, hingeOrientation,
            1e100
        )

        // TODO: Maybe change this based on ship size?
        val extraDist = 1.0
        val firstAttachment = VSAttachmentConstraint(
            shiptraptionID, otherShipID,
            1e-100,
            bearingPos.fma(-extraDist, axis, Vector3d()),
            posInOwnerShip.fma(-extraDist, axis, Vector3d()),
            1e100,
            0.0
        )

        val secondAttachment = VSAttachmentConstraint(
            shiptraptionID, otherShipID,
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
            worldPos, // literally useless
            axis,
            bearingAngle.toDouble(),
            veryUncoolFix * getSpeed(),
            movementMode!!.get() == LockedMode.LOCKED,
            shiptraptionID,
            firstAttachmentConstraint,
            hingeContraptionConstraint,
            null,
            null,
            secondAttachmentConstraint,
        )
        bearingID = BearingController.getOrCreate(shiptraption)!!.addPhysBearing(data)
        isRunning = true
        bearingAngle = 0f
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
        bearingAngle = 0f
        if (shiptraptionID != NO_SHIPTRAPTION_ID) {
            val ship: ServerShip? =
                (level as ServerLevel).shipObjectWorld.allShips.getById(shiptraptionID)
            if (ship != null) {
//
//                BearingController controller = BearingController.getOrCreate(ship);
//                Direction direction = getBlockState().getValue(BearingBlock.FACING);
//                Vector3dc inWorld = VectorConversionsMCKt.toJOMLD(worldPosition.relative(direction, 1));
//                if (!controller.canDisassemble()) {
//                    disassembleWhenPossible = true;
//                    controller.setAligning(true, shiptraptionID);
//                } else {
//                    shipDisassemble();
//                }

//                controller.removePhysBearing(bearingID);
            }
            AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition)
            // todo finish disassembly
            return
        }
        shiptraptionID = NO_SHIPTRAPTION_ID
        isRunning = false
        updateGeneratedRotation()
        assembleNextTick = false
        sendData()
    }

    private fun shipDisassemble() {
        if (shiptraptionID == NO_SHIPTRAPTION_ID) {
            return
        }
        if (level!!.isClientSide) return
        val ship: ServerShip =
            (level as ServerLevel).shipObjectWorld.allShips.getById(shiptraptionID)
                ?: return
        if (bearingID != null) {
            val controller: BearingController = BearingController.getOrCreate(ship)!!
            val direction = blockState.getValue(BearingBlock.FACING)
            val inWorld: Vector3dc = worldPosition.relative(direction, 1).toJOMLD()
            if (!controller.canDisassemble()) {
                return
            }
        }
    }

    private fun assembleNextTick() {
        if (assembleNextTick) {
            assembleNextTick = false
            if (isRunning) {
//                disassemble();
//                return;
            } else {
                assemble()
            }
        }
    }
    private fun attemptRefresh() {
        if (!shouldRefresh) {return}
        val level = level as ServerLevel
        val ship = level.shipObjectWorld.allShips.getById(shiptraptionID) ?: return
        if (bearingID == null) {return}

        val bearingData = BearingController.getOrCreate(ship)!!.bearingData[bearingID] ?: return

        val (shipId0, _, compliance, localPos0, localPos1, maxForce, fixedDistance) = bearingData.attachConstraint!!
        val (shipId01, _, compliance1, localRot0, localRot1, maxTorque) = bearingData.hingeConstraint!!
        val (shipId02, _, compliance2, localPos02, localPos12, maxForce2, fixedDistance2) = bearingData.secondAttachConstraint!!

        // todo TEMP, REPLACE ONCE TRIODE FIXES
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
            shipId0, shipOnID,
            compliance,
            localPos0, localPos1, maxForce, fixedDistance
        )
        val hingeConstraint = VSHingeOrientationConstraint(
            shipId01, shipOnID,
            compliance1, localRot0, localRot1, maxTorque
        )
        val secondAttachConstraint = VSAttachmentConstraint(
            shipId02, shipOnID,
            compliance2,
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

    private fun attemptToDoSmthIdfk() {
        if (shiptraptionID == NO_SHIPTRAPTION_ID) { return }
        val level = level as ServerLevel
        val ship = level.shipObjectWorld.allShips.getById(shiptraptionID) ?: return
        val bearingController = BearingController.getOrCreate(ship)!!
        val bearingData = bearingController.bearingData[bearingID] ?: return
//                        val hingeOrientationConstraint = BearingController.getOrCreate(ship)!!.bearingData[bearingID]!!.hingeConstraint
//                        val hingeTargetConstraint = BearingController.getOrCreate(ship)!!.bearingData[bearingID]!!.angleConstraint
        //                        if (BearingController.getOrCreate(ship).bearingData.get(bearingID).hingeID == null) {
//                            return;
//                        }
//                        if (movementMode.get() == LockedMode.LOCKED) {
//                            Vector3dc facing = VectorConversionsMCKt.toJOMLD(getBlockState().getValue(BlockStateProperties.FACING).getNormal());
//                            Quaterniond localRot0 = new Quaterniond(hingeConstraint.getLocalRot0());
//                            localRot0 = localRot0.premul(new Quaterniond(new AxisAngle4d(Math.toRadians(angle), facing))).normalize();
//                            angleConstraint = new VSFixedOrientationConstraint(hingeConstraint.getShipId0(), hingeConstraint.getShipId1(), 1e-10, localRot0, hingeConstraint.getLocalRot1(), 1e8);
//                            VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).updateConstraint(BearingController.getOrCreate(ship).bearingData.get(bearingID).hingeID, angleConstraint);
//                        } else if (movementMode.get() == LockedMode.UNLOCKED) {
//                            hingeConstraint = BearingController.getOrCreate(ship).bearingData.get(bearingID).hingeConstraint;
//                            VSGameUtilsKt.getShipObjectWorld((ServerLevel) level).updateConstraint(BearingController.getOrCreate(ship).bearingData.get(bearingID).hingeID, hingeConstraint);
//
//                        }

        //DUMB FIX FOR INV ROTATION
        var dumbFix = 1
        val direction = blockState.getValue(BearingBlock.FACING)
        if (direction == Direction.WEST || direction == Direction.NORTH || direction == Direction.DOWN) {
            dumbFix = -dumbFix
        }

        val data = PhysBearingUpdateData(
            bearingAngle.toDouble(), dumbFix * getSpeed(),
            movementMode!!.get() == LockedMode.LOCKED, null, null
        )
        //println("${bearingAngle.toDouble()} : ${getSpeed()}")
        bearingController.updatePhysBearing(bearingID!!, data)
    }

    override fun tick() {
        super.tick()
        prevAngle = bearingAngle
        if (level!!.isClientSide) clientAngleDiff /= 2f
        if (!level!!.isClientSide) {
            assembleNextTick()
            attemptRefresh()
        }
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
        if (opening && isRunning && openProgress < 1) {
            openProgress += 0.05f
        } else if (openProgress >= 1) {
            opening = false
            open = true
            openProgress = 1f
        }
        if (!isRunning) return
        if (shiptraptionID != NO_SHIPTRAPTION_ID) {
            val angularSpeed = angularSpeed
            val newAngle = bearingAngle + angularSpeed
            bearingAngle = (newAngle % 360)
        }
        if (!level!!.isClientSide) {
            attemptToDoSmthIdfk()
        }
        if (disassembleWhenPossible) {
            shipDisassemble()
        }
        applyRotation()
    }

    val isNearInitialAngle: Boolean
        get() = Math.abs(bearingAngle) < 45 || Math.abs(bearingAngle) > 7 * 45

    override fun lazyTick() {
        super.lazyTick()
        if (shiptraptionID != NO_SHIPTRAPTION_ID && !level!!.isClientSide) sendData()
    }

    private fun applyRotation() {

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
        if (!isWindmill && getSpeed() == 0f) return false
        if (isRunning) return false
        val state = blockState
        if (state.block !is BearingBlock) return false
        val attachedState = level!!.getBlockState(worldPosition.relative(state.getValue(BearingBlock.FACING)))
        if (attachedState.material.isReplaceable) return false
        TooltipHelper.addHint(tooltip, "hint.empty_bearing")
        return true
    }

    override fun setAngle(forcedAngle: Float) {
        bearingAngle = forcedAngle
    }

    override val isShipContraptionController: Boolean
        get() = true
    override val connectedShip: Ship?
        get() = null

    fun getAngle(): Float {
        return bearingAngle
    }

    companion object {
        const val NO_SHIPTRAPTION_ID: Long = -1
    }
}
