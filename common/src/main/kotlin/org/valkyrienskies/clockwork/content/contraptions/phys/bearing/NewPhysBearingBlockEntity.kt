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
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkMod.MOD_ID
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController
import org.valkyrienskies.clockwork.platform.api.ContraptionController
import org.valkyrienskies.clockwork.platform.api.ContraptionController.LockedMode
import org.valkyrienskies.clockwork.util.GlueAssembler.collectGlued
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.updateJoint
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.core.internal.joints.VSJoint
import org.valkyrienskies.core.internal.joints.VSJointId
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import org.valkyrienskies.core.util.datastructures.DenseBlockPosSet
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener
import org.valkyrienskies.mod.api.getShipManagingBlock
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.common.assembly.ShipAssembler.assembleToShip
import org.valkyrienskies.mod.common.assembly.VSAssemblyEvents
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.util.SplittingDisablerAttachment
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.world.clipIncludeShips
import org.valkyrienskies.mod.util.getVector3d
import org.valkyrienskies.mod.util.putVector3d
import kotlin.math.abs

class NewPhysBearingBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    GeneratingKineticBlockEntity(type, pos, state), IBearingBlockEntity, IDisplayAssemblyExceptions,
    ContraptionController, BlockEntityPhysicsListener {

    var movementMode: ScrollOptionBehaviour<LockedMode>? = null

    @Volatile
    var jointId: VSJointId = -1
        private set

    var partnerPos: BlockPos? = null
        private set

    @Volatile
    private var queuedJointToAdd: VSJoint? = null
    private var lastException: AssemblyException? = null
    private var targetAngle = 0f

    @Volatile override lateinit var dimension: DimensionId

    private val facing: Direction
        get() = blockState.getValue(BearingBlock.FACING)

    private val partnerShipId: ShipId
        get() = level.getShipManagingBlock(partnerPos)?.id ?: -1

    private val joint: VSJoint?
        get() = (level as? ServerLevel)?.gtpa?.getJointById(jointId)

    private fun movementModeChanged(value: Int) {
        if (level == null || level!!.isClientSide) { return }
        updateJoint()
        sendData()
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        super.addBehaviours(behaviours)
        movementMode = ScrollOptionBehaviour(
            LockedMode::class.java, Component.translatable("$MOD_ID.phys_bearing.rotation_mode"),
            this, movementModeSlot
        )
        movementMode!!.withCallback { movementModeChanged(it) }
        movementMode!!.requiresWrench()
        behaviours.add(movementMode!!)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        tag.putInt("jointId", jointId)
        partnerPos?.let { pos -> tag.putVector3d("partnerPos", pos.center.toJOML()) }
        tag.putLong("partnerShipId", partnerShipId)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        jointId = tag.getInt("jointId")
        if (tag.contains("partnerPosx")) {
            val vec = tag.getVector3d("partnerPos")!!
            partnerPos = BlockPos.containing(Vec3(vec.x, vec.y, vec.z))
        }
        if (clientPacket) { return }
        val level = level as? ServerLevel ?: return
        updateJoint(level)
    }

    fun onPaste(
        level: ServerLevel,
        pos: BlockPos,
        state: BlockState,
        oldShipIdToNewId: Map<Long, Long>,
        centerPositions: Map<Long, Pair<Vector3dc, Vector3dc>>,
        tag: CompoundTag?
    ): CompoundTag? {
        tag ?: return null

        if (tag.contains("partnerPosx")) {
            var newPartnerPos = tag.getVector3d("partnerPos")!!

            val partnerId = tag.getInt("partnerShipId").toLong()
            val centerMigrate = centerPositions[partnerId] ?: return null
            newPartnerPos = newPartnerPos.sub(centerMigrate.first).add(centerMigrate.second)
            tag.putVector3d("partnerPos", newPartnerPos)
            tag.putInt("jointId", -1)
            return tag
        }

        return null
    }

    fun assemble() {
        // Prevent making joints twice
        if (queuedJointToAdd != null) return
        if (jointId != -1) return
        val level = level as? ServerLevel ?: return
        if (level.getBlockState(worldPosition).block !is BearingBlock) return

        val direction = facing
        val targetPosition = worldPosition.relative(direction)

        val worldPos: Vector3dc = worldPosition.center.toJOML()
        val axis: Vector3d = direction.normal.toJOMLD()
        val shipOn = level.getLoadedShipManagingPos(worldPosition)

        val startPos = Vector3d(worldPos).fma(0.5, axis)
        val endPos = Vector3d(worldPos).fma(1.5, axis)

        fun Vector3d.toVec3() = Vec3(this.x, this.y, this.z)

        val otherHit = level.clipIncludeShips(
            ClipContext(
                (shipOn?.transform?.shipToWorld?.transformPosition(startPos) ?: startPos).toVec3(),
                (shipOn?.transform?.shipToWorld?.transformPosition(endPos) ?: endPos).toVec3(),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
            ), false, shipOn?.id
        )

        val otherShip = level.getLoadedShipManagingPos(otherHit.blockPos)

        val newPartnerPos: BlockPos
        if (otherShip == null) {
            val selection: DenseBlockPosSet
            try {
                selection = collectGlued(level, targetPosition) ?: return
                selection.remove(worldPosition.x, worldPosition.y, worldPosition.z)
                lastException = null
            } catch (e: AssemblyException) {
                lastException = e
                sendData()
                return
            }

            var centerPositions: Pair<Vector3d, Vector3d> = Pair(Vector3d(), Vector3d())
            val event = VSAssemblyEvents.onPasteBeforeBlocksAreLoaded.on {
                centerPositions = it.centerPosition.first.get(Vector3d()) to it.centerPosition.second.get(Vector3d())
            }
            assembleToShip(
                level,
                selection.toSet().map { it.toMinecraft() }.toSet(),
                1.0
            )
            event.unregister()

            val migratedPos = Vector3d(targetPosition.center.toJOML()).sub(centerPositions.first).add(centerPositions.second)
            newPartnerPos = BlockPos.containing(migratedPos.x, migratedPos.y, migratedPos.z)
        } else {
            newPartnerPos = otherHit.blockPos
        }

        partnerPos = newPartnerPos

        ClockworkSounds.PHYSICS_INFUSER_LIGHTNING.playOnServer(level, worldPosition)

        updateJoint(level)
        sendData()
    }


    override fun remove() {
        if (!(level?.isClientSide ?: true)) {
            removeJoint(level as ServerLevel)
        }
        super.remove()
    }

    fun disassemble() {
        val level = level as? ServerLevel ?: return
        val partnerPos = partnerPos ?: return
        val subShip = level.getLoadedShipManagingPos(partnerPos) ?: run {
            removeJoint(level)
            return
        }
        val mainShip = level.getLoadedShipManagingPos(worldPosition)

        val axis = facing.normal.toJOMLD()
        if (abs(Math.toDegrees(BearingController.getAngle(axis, subShip.transform, mainShip?.transform))) > DISASSEMBLE_ANGLE_TOLERANCE_DEGREES) {
            return
        }

        val inMain = worldPosition.relative(facing, 1)
        val inSubship = partnerPos

        val aabb = subShip.shipAABB ?: run {
            removeJoint(level)
            return
        }
        val blocks = DenseBlockPosSet()
        for (x in aabb.minX() - 1 until aabb.maxX() + 1) {
        for (z in aabb.minZ() - 1 until aabb.maxZ() + 1) {
        for (y in aabb.minY() - 1 until aabb.maxY() + 1) {
            blocks.add(x, y, z)
        } } }

        val subCouldSplit = subShip.getAttachment<SplittingDisablerAttachment>()?.let {
            if (it.canSplit()) { it.disableSplitting(); true } else { false }
        } ?: false
        val mainCouldSplit = mainShip?.getAttachment<SplittingDisablerAttachment>()?.let {
            if (it.canSplit()) { it.disableSplitting(); true } else { false }
        } ?: false

        val hasMoved = PhysBearingAssembler.moveBlocksFromTo(level, blocks, true, inSubship, inMain, subShip, mainShip)

        if (subCouldSplit) { subShip.getAttachment<SplittingDisablerAttachment>()?.enableSplitting() }
        if (mainCouldSplit) { mainShip?.getAttachment<SplittingDisablerAttachment>()?.enableSplitting() }

        if (!hasMoved) return

        removeJoint(level)
        AllSoundEvents.CONTRAPTION_DISASSEMBLE.playOnServer(level, worldPosition)
        sendData()
    }

    private fun removeJoint(level: ServerLevel) {
        if (jointId != -1) {
            level.gtpa.removeJoint(jointId)
        }
        jointId = -1
        queuedJointToAdd = null
        partnerPos = null
    }

    fun updateJoint(level: ServerLevel? = this.level as? ServerLevel) {
        level ?: return
        val updatedJoint = buildJoint(targetAngle)

        if (jointId != -1 && updatedJoint != null) {
            level.gtpa.updateJoint(jointId, updatedJoint)
        } else {
            queuedJointToAdd = updatedJoint
        }
    }

    @PhysTickOnly
    override fun physTick(physShip: PhysShip?, physLevel: PhysLevel) {
        if (isRemoved) return
        queuedJointToAdd ?: return

        physLevel as VsiPhysLevel
        // If either ship isn't loaded, skip this tick
        if (
                queuedJointToAdd!!.shipId0 != null && physLevel.getShipById(queuedJointToAdd!!.shipId0!!) == null ||
                queuedJointToAdd!!.shipId1 != null && physLevel.getShipById(queuedJointToAdd!!.shipId1!!) == null
            ) return

        val id = physLevel.addJoint(queuedJointToAdd!!)
        if (id == -1) {
            return
        }
        jointId = id
        queuedJointToAdd = null
    }

    private fun buildJoint(targetAngle: Float): VSJoint? {
        val partnerPos = partnerPos ?: return null

        val thisShipId = level.getShipManagingPos(worldPosition)?.id

        val pose0 = VSJointPose(worldPosition.center.toJOML(), getHingeRotation(facing))
        val pose1 = VSJointPose(partnerPos.relative(facing.opposite).center.toJOML(), getHingeRotation(facing))

        val joint = if (movementMode?.get() == LockedMode.FOLLOW_ANGLE) {
            VSFixedJoint(thisShipId, pose0, partnerShipId, pose1, compliance = 1e-100)
        } else {
            VSRevoluteJoint(thisShipId, pose0, partnerShipId, pose1, compliance = 1e-100, driveFreeSpin = true)
        }
        joint.serialized()
        return joint
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

        return rotationQuaternion.mul(
            Quaterniond(AxisAngle4d(Math.toRadians(90.0), 0.0, 0.0, 1.0)),
            Quaterniond()
        ).normalize()
    }

    override fun getInterpolatedAngle(partialTicks: Float): Float = targetAngle
    override fun setAngle(forcedAngle: Float) {
        if (!forcedAngle.isFinite()) return
        targetAngle = forcedAngle
    }
    override fun getLastAssemblyException(): AssemblyException? = lastException
    override fun getBlockPosition(): BlockPos = worldPosition
    override fun isWoodenTop(): Boolean = false
    override fun attach(contraption: ControlledContraptionEntity) {}
    override fun onStall() { if (!level!!.isClientSide) sendData() }
    override fun isValid(): Boolean = !isRemoved
    override fun isAttachedTo(contraption: AbstractContraptionEntity): Boolean = false

    companion object {
        private const val DISASSEMBLE_ANGLE_TOLERANCE_DEGREES = 5
    }
}
