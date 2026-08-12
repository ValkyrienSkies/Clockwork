package org.valkyrienskies.clockwork.content.contraptions.phys.bearing

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.data.PhysBearingUpdateData
import org.valkyrienskies.clockwork.platform.api.ContraptionController.LockedMode
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.updateJoint
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.properties.ShipId
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.core.internal.joints.VSJoint
import org.valkyrienskies.core.internal.joints.VSJointId
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.joints.VSRevoluteJoint
import org.valkyrienskies.mod.api.getShipManagingBlock
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.api.toMinecraft
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.util.getVector3d
import org.valkyrienskies.mod.util.putVector3d

object BearingPlan {
    val level: Level = TODO()
    val worldPosition: BlockPos = TODO()
    val facing: Direction = TODO()
    val mode: Any = TODO()

    var jointId: VSJointId = -1
    var partnerPos : BlockPos? = null

    var queuedJointToAdd: VSJoint? = null

    val partnerShipId: ShipId
        get() = level.getShipManagingBlock(partnerPos)?.id ?: -1

    val joint: VSJoint?
        get() = (level as ServerLevel).gtpa.getJointById(jointId)

    fun write(tag: CompoundTag) {
        tag.putInt("jointId", jointId)
        partnerPos?.let { pos -> tag.putVector3d("partnerPos", pos.center.toJOML()) }
        tag.putLong("partnerShipId", partnerShipId)
    }

    fun read(tag: CompoundTag) {
        jointId = tag.getInt("jointId")
        if (tag.contains("partnerPosx")) {
            partnerPos = BlockPos.containing(
                tag.getVector3d("partnerPos")!!.toMinecraft()
            )
        }

        updateJoint()
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
            var partnerPos = tag.getVector3d("partnerPos")!!

            val partnerId = tag.getInt("partnerShipId").toLong()
            val centerMigrate = centerPositions[partnerId] ?: return null
            partnerPos = partnerPos.sub(centerMigrate.first).add(centerMigrate.second)
            tag.putVector3d("partnerPos", partnerPos)
            tag.putInt("jointId", -1)
            return tag
        }

        return null
    }

    fun assemble() {
        // Prevent making joints twice
        if (queuedJointToAdd != null) return
        if (jointId != -1) return

        val targetPosition = worldPosition.relative(facing)
        val blocks = findConnectedBlocks(targetPosition)
        val newShip = makeShip(blocks)

        partnerPos = BlockPos.containing(newShip.transform.worldToShip.transformPosition(targetPosition.center.toJOML()).toMinecraft())

        updateJoint()
    }

    fun updateJoint() {
        val updatedJoint = buildJoint(0.0f)

        if (jointId != -1 && updatedJoint != null) {
            (level as ServerLevel).gtpa.updateJoint(jointId, updatedJoint)
        } else {
            queuedJointToAdd = updatedJoint
        }

    }

    fun tick() {

    }

    fun physTick() {
        if (queuedJointToAdd != null) {
            jointId = TODO()
        }
    }

    fun buildJoint(targetAngle: Float): VSJoint? {
        partnerPos ?: return null

        val thisShipId = level.getShipManagingPos(worldPosition)?.id

        val pose0 = VSJointPose(worldPosition.center.toJOML(), getHingeRotation(facing))
        val pose1 = VSJointPose(partnerPos!!.center.toJOML(), getHingeRotation(facing.opposite))

        var joint: VSJoint?
        if (mode == LockedMode.FOLLOW_ANGLE) {
            joint = VSFixedJoint(thisShipId, pose0, partnerShipId, pose1, compliance = 1e-100)
            joint!!.serialized()
            PhysBearingUpdateData(
                Math.toRadians(targetAngle.toDouble()),
                0f,
                true
            )
        } else {
            joint = VSRevoluteJoint(thisShipId, pose0, partnerShipId, pose1, compliance = 1e-100, driveFreeSpin = true)
            joint!!.serialized()
            PhysBearingUpdateData(
                Math.toRadians(targetAngle.toDouble()),
                getRealisticAngularSpeed(),
                false
            )
        }
        return joint
    }

    fun getHingeRotation(localDirection: Direction): Quaterniond {
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

    fun getRealisticAngularSpeed(): Float { TODO() }
    fun makeShip(blocks: Any): ServerShip { TODO() }
    fun findConnectedBlocks(root: BlockPos) {}
}