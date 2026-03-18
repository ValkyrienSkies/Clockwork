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
import com.simibubi.create.foundation.utility.ServerSpeedProvider
import net.createmod.catnip.math.AngleHelper
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.*
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
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
import org.valkyrienskies.clockwork.util.VS2AssemblyBridge
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.updateJoint
import org.valkyrienskies.clockwork.util.minus
import org.valkyrienskies.clockwork.util.plus
import org.valkyrienskies.clockwork.util.times
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.internal.joints.*
import org.valkyrienskies.core.impl.bodies.properties.BodyTransformFactory

import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3d
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.common.*
import org.valkyrienskies.mod.common.assembly.ICopyableBlock
import org.valkyrienskies.mod.common.util.SplittingDisablerAttachment
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.world.clipIncludeShips
import org.valkyrienskies.mod.util.putVector3d
import java.lang.Math
import kotlin.math.*

//TODO move
fun getHingeRotation(localDir: Vector3dc, right: Vector3dc = Vector3d(1.0, 0.0, 0.0)): Quaterniond {
    if ((localDir - right).length() < 1e-5) { return Quaterniond() }

    val v1l = right.length()
    val v2l = localDir.length()

    val a = right.cross(localDir, Vector3d())

    val k = sqrt(v1l * v1l * v2l * v2l)
    val kCosTheta = right.dot(localDir)

    if (abs(kCosTheta / k + 1.0) < 1e-5) {
        val ort = right.let { it.orthogonalize(it, Vector3d()) }
        return Quaterniond(ort.x, ort.y, ort.z, 0.0).normalize()
    }

    return Quaterniond(a.x, a.y, a.z, k + kCosTheta).normalize()
}

class PhysBearingBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    GeneratingKineticBlockEntity(type, pos, state), IBearingBlockEntity, IDisplayAssemblyExceptions,
    ContraptionController, BlockEntityPhysicsListener {

    var assembleNextTick = false
    var movementMode: ScrollOptionBehaviour<LockedMode>? = null
    var isRunning = false
        private set
    var shiptraptionID = NO_SHIPTRAPTION_ID
        private set
    var targetAngle = 0f
        get() = field
        private set(idk) {field = idk}
    var disassembleWhenPossible = false
        private set
    @Volatile var joint : VSJoint? = null
        private set
    @Volatile var jointID : Int = -1
        private set

    private var lastException: AssemblyException? = null
    private var open = false
    private var originalDirection: Direction? = null
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
    private var assemblyInProgress = false
    private var disassemblyInProgress = false

    init {
        setLazyTickRate(3)
    }

    private fun movementModeChanged(value: Int) {
        if (level == null || level!!.isClientSide) {return}
        sendData()
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        movementMode = ScrollOptionBehaviour(
            LockedMode::class.java, Component.translatable("$MOD_ID.phys_bearing.rotation_mode"),
            this, movementModeSlot
        )
        movementMode!!.withCallback{movementModeChanged(it)}
        movementMode!!.requiresWrench()
        behaviours.add(movementMode!!)
    }

    private fun updateDrive(driveVelocity: VSRevoluteJoint.VSRevoluteDriveVelocity? = null) {
        if (movementMode!!.get() == LockedMode.FOLLOW_ANGLE || aligning) {
            joint = VSFixedJoint(joint!!.shipId0, joint!!.pose0, joint!!.shipId1, joint!!.pose1, compliance = 1e-100)
            controllerUpdateData = PhysBearingUpdateData(
                Math.toRadians(targetAngle.toDouble()),
                0f,
                false
            )
        } else {
            joint = VSRevoluteJoint(joint!!.shipId0, joint!!.pose0, joint!!.shipId1, joint!!.pose1, compliance = 1e-100, driveFreeSpin = true)
            controllerUpdateData = PhysBearingUpdateData(
                Math.toRadians(targetAngle.toDouble()),
                getRealisticAngularSpeed(),
                false
            )
            (level as ServerLevel).gtpa.updateJoint(jointID, joint!!)
        }
    }

    @Volatile override lateinit var dimension: DimensionId
    @Volatile private var sDir1: Vector3dc? = null
    @Volatile private var sDir2: Vector3dc? = null
    @Volatile private var pTick = 0
    @Volatile private var lastAngle = targetAngle
    @Volatile private var curAngle = targetAngle
    override fun physTick(physShip: PhysShip?, physLevel: PhysLevel) {
        if (isRemoved || !isRunning) return
        if (jointID == -1) return
        val joint = joint as? VSFixedJoint ?: return

        if (curAngle != targetAngle) {
            pTick = 0
            lastAngle = curAngle
            curAngle = targetAngle
        }

        var angle = Math.toRadians(lastAngle + (targetAngle - lastAngle) * ((pTick+1) / 3.0))
        if (aligning) { angle = 0.0 }

        physLevel as VsiPhysLevel
        if (sDir1 == null || sDir2 == null) {
            sDir1 = bearingAxis
            sDir2 = bearingAxis
        }

        //AxisAngle4d clamps angle, so when going from 359 to 0 degrees quat jumps from -0.999 w to 0.999 w or smth like that
        // which causes krunch to incorrectly interpolate, so i just extend angle range to [0, 720) and manually do this shit
        val s = sin(angle * 0.5)
        val fRot2 = Quaterniond(
            sDir1!!.x() * s,
            sDir1!!.y() * s,
            sDir1!!.z() * s,
            org.joml.Math.cosFromSin(s, angle * 0.5)
        ).mul(getHingeRotation(sDir1!!))
        val fRot1 = getHingeRotation(sDir2!!)

        this.joint = VSFixedJoint(
            joint.shipId0, VSJointPose(joint.pose0.pos, fRot1),
            joint.shipId1, VSJointPose(joint.pose1.pos, fRot2),
            compliance = 1e-100
        )

        physLevel.updateJoint(jointID, this.joint!!)

        pTick = max(pTick++, 2)
    }

    public override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)

        tag.putBoolean(ClockworkConstants.Nbt.RUNNING, isRunning)
        tag.putFloat(ClockworkConstants.Nbt.ANGLE, targetAngle)
        tag.putBoolean("assemblyInProgress", assemblyInProgress)
        tag.putBoolean("disassemblyInProgress", disassemblyInProgress)
        tag.putBoolean("assembleNextTick", assembleNextTick)
        tag.putBoolean("disassembleWhenPossible", disassembleWhenPossible)
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
        when (joint) {
            is VSRevoluteJoint -> tag.putByteArray("joint", mapper.writeValueAsBytes(joint!!))
            is VSFixedJoint -> tag.putByteArray("fjoint", mapper.writeValueAsBytes(joint!!))
        }

        tag.putInt("jointID", jointID)

        if (shiptraptionID == NO_SHIPTRAPTION_ID) return

        tag.putLong(ClockworkConstants.Nbt.OLD_POS, worldPosition.asLong())
        //to make it more general
        tag.putVector3d(ClockworkConstants.Nbt.OLD_SHIPTRAPTION_CENTER, bearingPos)
        tag.putVector3d(ClockworkConstants.Nbt.NEW_SHIPTRAPTION_CENTER, bearingPos)
    }

    private fun loadTheRest(tag: CompoundTag, level: ServerLevel) {
        var joint = this.joint ?: return
        val mainId = level.getShipManagingPos(worldPosition)?.id

        val oldBPos = BlockPos.of(tag.getLong(ClockworkConstants.Nbt.OLD_POS))
        val oldPos = oldBPos.toJOMLD()

        val newPos = worldPosition.toJOMLD()

        val oldSPos = tag.getVector3d(ClockworkConstants.Nbt.OLD_SHIPTRAPTION_CENTER) ?: return
        val newSPos = tag.getVector3d(ClockworkConstants.Nbt.NEW_SHIPTRAPTION_CENTER) ?: return

        bearingPos = bearingPos.sub(oldSPos).add(newSPos)

        this.joint = when(joint) {
            is VSRevoluteJoint -> joint.copy(
                shiptraptionID, pose0 = VSJointPose(joint.pose0.pos - oldSPos + newSPos, joint.pose0.rot),
                mainId,         pose1 = VSJointPose(joint.pose1.pos - oldPos  + newPos,  joint.pose1.rot))
            is VSFixedJoint -> joint.copy(
                shiptraptionID, pose0 = VSJointPose(joint.pose0.pos - oldSPos + newSPos, joint.pose0.rot),
                mainId,         pose1 = VSJointPose(joint.pose1.pos - oldPos  + newPos,  joint.pose1.rot))
            else -> throw AssertionError()
        }

        controllerCreationData = PhysBearingData(
            bearingAxis.get(Vector3d()),
            Math.toRadians(targetAngle.toDouble()),
            getRealisticAngularSpeed(),
            movementMode!!.get() == LockedMode.FOLLOW_ANGLE,
            aligning,
            mainId ?: -1L,
            this.joint?.pose1?.pos?.get(Vector3d()) ?: Vector3d(),
            this.joint?.pose0?.pos?.get(Vector3d()) ?: Vector3d()
        )

        tryMakeJoint()
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        val angleBefore = targetAngle
        open = tag.getBoolean(ClockworkConstants.Nbt.OPEN)
        isRunning = tag.getBoolean(ClockworkConstants.Nbt.RUNNING)
        targetAngle = tag.getFloat(ClockworkConstants.Nbt.ANGLE)
        assemblyInProgress = tag.getBoolean("assemblyInProgress")
        disassemblyInProgress = tag.getBoolean("disassemblyInProgress")
        assembleNextTick = tag.getBoolean("assembleNextTick")
        disassembleWhenPossible = tag.getBoolean("disassembleWhenPossible")
        lastAngle = targetAngle
        curAngle = targetAngle
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

        if (tag.contains("constraint")) {
            val temp = mapper.readValue(tag.getByteArray("constraint"), VSJointAndId::class.java)
            joint = temp.joint as VSRevoluteJoint
            jointID = temp.jointId
        } else if (tag.contains("joint")) {
            joint = mapper.readValue(tag.getByteArray("joint"), VSRevoluteJoint::class.java)
            jointID = tag.getInt("jointID")
        } else if (tag.contains("fjoint")) {
            joint = mapper.readValue(tag.getByteArray("fjoint"), VSFixedJoint::class.java)
            jointID = tag.getInt("jointID")
        }

        super.read(tag, clientPacket)
        if (clientPacket) {return}

        if (assemblyInProgress && !isRunning) {
            assemblyInProgress = false
            assembleNextTick = true
        }
        if (disassemblyInProgress && shiptraptionID != NO_SHIPTRAPTION_ID) {
            disassemblyInProgress = false
            disassembleWhenPossible = true
        }

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

    fun tryMakeJoint() {
        val joint = joint ?: return

        ClockworkMod.physTickOnce(level.dimensionId!!) { level, _, tryNextTick ->
            level as VsiPhysLevel
            val existing = level.getJointById(jointID)
            if (existing != null && existing == joint) {
                isRunning = true
                return@physTickOnce
            }
            if (
                joint.shipId0 != null && level.getShipById(joint.shipId0!!) == null ||
                joint.shipId1 != null && level.getShipById(joint.shipId1!!) == null
            ) {
                tryNextTick()
                return@physTickOnce
            }
            val id = level.addJoint(joint)
            if (id == -1) {
                tryNextTick()
                return@physTickOnce
            }
            this.jointID = id

            isRunning = true
            lastStateChanged = ticks
        }
    }

    private fun createCurrentBearingData(mainShipId: Long): PhysBearingData? {
        val currentJoint = joint ?: return null
        return PhysBearingData(
            bearingAxis.get(Vector3d()),
            Math.toRadians(targetAngle.toDouble()),
            getRealisticAngularSpeed(),
            movementMode!!.get() == LockedMode.FOLLOW_ANGLE,
            aligning,
            mainShipId,
            currentJoint.pose1.pos.get(Vector3d()),
            currentJoint.pose0.pos.get(Vector3d())
        )
    }

    private fun suspendBearingForTransfer(level: ServerLevel, subShip: LoadedServerShip) {
        if (bearingID != -1) {
            BearingController.getOrCreate(subShip)?.removePhysBearing(bearingID)
            bearingID = -1
        }
        controllerCreationData = null
        controllerUpdateData = null

        if (joint != null && jointID != -1) {
            level.gtpa.removeJoint(jointID)
            jointID = -1
        }

        isRunning = false
    }

    private fun restoreBearingAfterFailedTransfer(level: ServerLevel, subShip: LoadedServerShip, mainShip: ServerShip?) {
        controllerCreationData = createCurrentBearingData(mainShip?.id ?: -1L)
        tryMakeJoint()
        sendData()
    }

    private fun assemble() {
        if (assemblyInProgress) return
        if (level!!.getBlockState(worldPosition).block !is BearingBlock) return
        val level = level as ServerLevel

        originalDirection = blockState.getValue(BearingBlock.FACING)
        val direction = originalDirection!!
        val attachPoint = worldPosition.relative(direction)

        // bearing data
        val worldPos: Vector3dc = worldPosition.center.toJOML()
        val axis = direction.normal.toJOMLD()
        val shipOn = level.getShipObjectManagingPos(worldPosition)

        val startPos = worldPos + axis * 0.5
        val endPos = worldPos + axis * 1.5

        fun Vector3d.toVec3() = this.let { net.minecraft.world.phys.Vec3(it.x, it.y, it.z) }

        val otherPos = level.clipIncludeShips(
            ClipContext(
                (shipOn?.transform?.shipToWorld?.transformPosition(startPos) ?: startPos).toVec3(),
                (shipOn?.transform?.shipToWorld?.transformPosition(endPos) ?: endPos).toVec3(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
                ), false, shipOn?.id)

        val otherShip = level.getShipObjectManagingPos(otherPos.blockPos)
        val posInOwnerShip = Vector3d(worldPos)

        if (otherShip == null) {
            val selection: DenseBlockPosSet?
            try {
                selection = collectGlued(level, attachPoint)
                selection?.remove(this.blockPos.x, this.blockPos.y, this.blockPos.z)
                lastException = null
            } catch (e: AssemblyException) {
                lastException = e
                sendData()
                return
            }
            if (selection == null) return

            assemblyInProgress = true
            sendData()
            val server = level.server
            VS2AssemblyBridge.queueAssembleToShipFull(
                level,
                selection.toSet().map { it.toMinecraft() }.toSet(),
                1.0
            ).thenAccept { assembleContext ->
                server.execute {
                    val serverLevel = this.level as? ServerLevel ?: return@execute
                    if (isRemoved) {
                        assemblyInProgress = false
                        return@execute
                    }
                    val queuedBearingPos = Vector3d(worldPosition.center.toJOML())
                        .sub(assembleContext.fromCenter)
                        .add(assembleContext.toCenter)
                    finishAssembly(
                        serverLevel,
                        direction,
                        queuedBearingPos,
                        serverLevel.getShipObjectManagingPos(worldPosition),
                        assembleContext.ship
                    )
                }
            }.exceptionally { error: Throwable ->
                server.execute {
                    assemblyInProgress = false
                    lastException = AssemblyException(
                        Component.literal(error.cause?.message ?: error.message ?: "Failed to assemble bearing ship")
                    )
                    sendData()
                }
                null
            }
            return
        }

        finishAssembly(
            level,
            direction,
            otherPos.blockPos.toVector3d() + 0.5 - direction.normal.toJOMLD(),
            shipOn,
            otherShip
        )
    }

    private fun finishAssembly(
        level: ServerLevel,
        direction: Direction,
        bearingPos: Vector3dc,
        shipOn: ServerShip?,
        shiptraption: ServerShip
    ) {
        val worldPos: Vector3dc = worldPosition.center.toJOML()
        val axis = direction.normal.toJOMLD()
        val posInOwnerShip = Vector3d(worldPos)
        val shipOnID = shipOn?.id
        shiptraptionID = shiptraption.id

        // AllSoundEvents.CONTRAPTION_ASSEMBLE.playOnServer(level, worldPosition);
        ClockworkSounds.PHYSICS_INFUSER_LIGHTNING.playOnServer(level, worldPosition)

        val posInWorld = shipOn?.transform?.shipToWorld?.transformPosition(
            posInOwnerShip - bearingPos + shiptraption.inertiaData.centerOfMass , Vector3d()
        ) ?: (worldPos - bearingPos + shiptraption.inertiaData.centerOfMass)
        val rotInWorld = shipOn?.transform?.shipToWorldRotation ?: Quaterniond()
        val scaling    = shipOn?.transform?.shipToWorldScaling ?: Vector3d(1.0, 1.0, 1.0)

        shiptraption.unsafeSetTransform(BodyTransformFactory.create(
            posInWorld, rotInWorld, scaling, shiptraption.transform.positionInModel
        ))

        val ship1rot = getHingeRotation(direction)
        val ship2rot = getHingeRotation(direction)

        val extraDist = 1.0
//        val realSpeed = if (getSpeed().absoluteValue > 0.0f) getRealisticAngularSpeed() else 0.0f
//        val newDriveVelocity = if (realSpeed != 0.0f) VSRevoluteJoint.VSRevoluteDriveVelocity(getRealisticAngularSpeed(), true) else null
        joint = VSRevoluteJoint(
            shiptraptionID, VSJointPose(bearingPos.fma(-extraDist, axis, Vector3d()), ship1rot),
            shipOnID, VSJointPose(posInOwnerShip.fma(-extraDist, axis, Vector3d()), ship2rot),
            compliance = 1e-100,
            driveFreeSpin = true//movementMode!!.get() != LockedMode.LOCKED,
//            driveVelocity = newDriveVelocity,
        )

        this.bearingAxis = axis
        this.bearingPos = bearingPos.get(Vector3d())
        this.assemblyInProgress = false
        this.lastException = null

        controllerCreationData = PhysBearingData(
            bearingAxis.get(Vector3d()),
            Math.toRadians(targetAngle.toDouble()),
            getRealisticAngularSpeed(),
            movementMode!!.get() == LockedMode.FOLLOW_ANGLE,
            aligning,
            shipOnID ?: -1L,
            joint!!.pose1.pos.get(Vector3d()),
            joint!!.pose0.pos.get(Vector3d())
        )

        tryMakeJoint()

        sendData()
        updateGeneratedRotation()
    }

    override fun destroy() {
        val level = level ?: return
        if (level.isClientSide || level !is ServerLevel) return

        val ship = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return
        BearingController.getOrCreate(ship)!!.removePhysBearing(bearingID)

        joint?.let { level.gtpa.removeJoint(jointID) }
    }

    fun disassemble() {
        if (disassemblyInProgress) return
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
        if (disassemblyInProgress) return
        if (shiptraptionID == NO_SHIPTRAPTION_ID || level!!.isClientSide) { return }
        val level = level as ServerLevel
        val subShip = level.shipObjectWorld.loadedShips.getById(shiptraptionID) ?: return
        val mainShip = level.getShipObjectManagingPos(worldPosition)

        if (!canDisassemble(bearingAxis, subShip, mainShip)) { return }
        val direction = originalDirection ?: blockState.getValue(BearingBlock.FACING)
        val inMain = worldPosition.relative(direction, 1)
        val inSubship = bearingPos.add(bearingAxis, Vector3d()).let { BlockPos.containing(it.x, it.y, it.z) }
        val blocks = PhysBearingAssembler.collectLoadedShipBlocks(level, subShip) ?: return
        if (blocks.isEmpty()) return

        val subCouldSplit = subShip.getAttachment<SplittingDisablerAttachment>()?.let { if (it.canSplit()) { it.disableSplitting(); true } else {false} } ?: false
        val mainCouldSplit = mainShip?.getAttachment<SplittingDisablerAttachment>()?.let { if (it.canSplit()) { it.disableSplitting(); true } else {false} } ?: false

        suspendBearingForTransfer(level, subShip)
        disassemblyInProgress = true
        sendData()
        PhysBearingAssembler.queueMoveBlocksFromTo(level, blocks, true, inSubship, inMain, subShip, mainShip)
            .whenComplete { hasMoved, error ->
                level.server.execute {
                    if (subCouldSplit) { subShip.getAttachment<SplittingDisablerAttachment>()?.enableSplitting() }
                    if (mainCouldSplit) { mainShip?.getAttachment<SplittingDisablerAttachment>()?.enableSplitting() }

                    disassemblyInProgress = false
                    if (error != null || !hasMoved) {
                        aligning = false
                        assembleNextTick = false
                        disassembleWhenPossible = false
                        restoreBearingAfterFailedTransfer(level, subShip, mainShip)
                        return@execute
                    }

                    lastStateChanged = ticks
                    resetState()
                }
            }
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
        targetAngle = 0f
        aligning = false
        assemblyInProgress = false
        disassemblyInProgress = false
        jointID = -1
        joint = null
        controllerCreationData = null
        controllerUpdateData = null

        sDir1 = null
        sDir2 = null
        pTick = 0
        lastAngle = 0f
        curAngle = 0f

        sendData()
    }

    private fun tryAssembleNextTick() {
        if (!assembleNextTick) {return}
        if (ticks - lastStateChanged <= cooldown) {return}
        assembleNextTick = false
        if (!isRunning) {assemble()}
    }

    private fun tryUpdateData() {
        if (shiptraptionID == NO_SHIPTRAPTION_ID) {return}
        if (   (lastSpeed == getSpeed() && lastMode == movementMode?.get())
            && (movementMode!!.get() != LockedMode.FOLLOW_ANGLE && !aligning)
        ) {return}

        if (lastMode != movementMode?.get() && movementMode?.get() == LockedMode.FOLLOW_ANGLE) {
            val shipOn = level!!.getShipObjectManagingPos(blockPos)?.transform
            val shiptraption = level!!.shipObjectWorld.allShips.getById(shiptraptionID)?.transform ?: return

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
            if (disassembleWhenPossible) { shipDisassemble() }
        }
        tickAnimationLogic()
        if (!isRunning) return
        if (shiptraptionID == NO_SHIPTRAPTION_ID) {
            targetAngle = 0f
        } else if (joint != null && jointID != -1) {
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
            //this is stupid
            lastAngle = when {
                newAngle >= 360f * 2 -> lastAngle - 360f * 2
                newAngle < 0f -> lastAngle + 360f * 2
                else -> lastAngle
            }
            curAngle = when {
                newAngle >= 360f * 2 -> curAngle - 360f * 2
                newAngle < 0f -> curAngle + 360f * 2
                else -> curAngle
            }
            targetAngle = when {
                newAngle >= 360f * 2 -> newAngle - 360f * 2
                newAngle < 0f -> newAngle + 360f * 2
                else -> newAngle
            }
        }
        //needs to be after targetAngle change
        if (!level!!.isClientSide) { tryUpdateData() }
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
