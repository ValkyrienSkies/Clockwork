package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntity
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.gimbal.GimbalBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalGantryBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalLinearBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.grippad.GripPadBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.grippad.GripPadController
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.content.physicalities.extendon.ExtendonBlockEntity
import org.valkyrienskies.clockwork.platform.api.ContraptionController.LockedMode
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.properties.ShipTransform
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.abs
import kotlin.math.atan2

object AeronautAssemblyReadout {
    internal const val MAX_MARKERS = 96
    internal const val MAX_OUTLINE_POINTS = 128
    internal const val MAX_CONTACT_FACES = 24
    private const val MAX_FACE_CHECKS = 128

    data class Anchor(val shipId: Long?, val pos: BlockPos)
    data class Foot(val anchor: Anchor, val load: Double)
    data class ContactFace(val shipId: Long?, val center: Vector3dc, val load: Double)

    data class OutlinePose(val x: Double, val z: Double, val yaw: Double) {
        internal fun toLocal(point: Vector3dc) = Vector3d(point).sub(x, 0.0, z).rotateY(-yaw)
        internal fun toWorld(point: Vector3dc) = Vector3d(point).rotateY(yaw).add(x, 0.0, z)

        companion object {
            internal fun of(transform: ShipTransform): OutlinePose {
                val matrix = transform.shipToWorld
                val yaw = if (abs(matrix.m20()) + abs(matrix.m22()) > 1e-8) atan2(matrix.m20(), matrix.m22())
                    else atan2(-matrix.m02(), matrix.m00())
                val position = transform.positionInWorld
                return OutlinePose(position.x(), position.z(), yaw)
            }
        }
    }

    enum class JointKind(val blockId: String, val angular: Boolean) {
        EXTENDON("extendon", false), HINGE("hinge", true), GIMBAL("gimbal_bearing", true),
        BEARING("phys_bearing", true), PISTON("phys_piston", false), GANTRY("phys_gantry", false)
    }

    data class Joint(val anchor: Anchor, val kind: JointKind, val measured: Double, val target: Double? = null,
                     val lower: Double? = null, val upper: Double? = null, val error: Double? = null)
    data class Detail(val bodyCount: Int = 1, val feet: List<Foot> = emptyList(),
                      val outline: List<Vector3dc> = emptyList(), val joints: List<Joint> = emptyList(),
                      val contactFaces: List<ContactFace> = emptyList(), val outlinePose: OutlinePose? = null) {
        internal fun localOutline(): List<Vector3dc> = outlinePose?.let { pose -> outline.map(pose::toLocal) } ?: outline
    }
    internal data class Sample(val mass: Double, val center: Vector3dc, val detail: Detail)

    internal fun sample(level: ServerLevel, root: LoadedServerShip): Sample {
        val world = level.shipObjectWorld
        val adapter = level.gtpa
        val ground = world.dimensionToGroundBodyIdImmutable[level.dimensionId]
        val visited = linkedSetOf(root.id)
        val loaded = ArrayList<LoadedServerShip>()
        val worldAnchors = linkedSetOf<BlockPos>()
        val queue = ArrayDeque<Long>()
        queue.add(root.id)
        val moment = Vector3d()
        val origin = root.transform.positionInWorld
        var mass = 0.0
        var bodyCount = 0
        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            val ship = world.loadedShips.getById(id)?.takeIf { it.chunkClaimDimension == level.dimensionId }
            val body = if (ship == null) world.allBodies.getById(id)?.takeIf { it.dimension == level.dimensionId } else null
            val weight = ship?.inertiaData?.mass ?: body?.inertiaData?.mass ?: continue
            val center = ship?.transform?.positionInWorld ?: body!!.kinematics.position
            if (weight.isFinite() && weight > 0 && center.isFinite) {
                moment.fma(weight, center.sub(origin, Vector3d()))
                mass += weight
                bodyCount++
            }
            if (ship != null) loaded.add(ship)
            for (jointId in adapter.getJointsFromShip(id)) {
                val joint = adapter.getJointById(jointId) ?: continue
                val (other, pose) = when (id) {
                    joint.shipId0 -> joint.shipId1 to joint.pose1
                    joint.shipId1 -> joint.shipId0 to joint.pose0
                    else -> continue
                }
                if (other == null || other == -1L || other == ground) {
                    val point = pose.pos
                    if (point.isFinite) worldAnchors.add(BlockPos.containing(point.x(), point.y(), point.z()))
                } else if (visited.add(other)) queue.add(other)
            }
        }

        val feet = ArrayList<Foot>()
        val joints = ArrayList<Joint>()
        val contactFaces = ArrayList<ContactFace>()
        val supportPoints = ArrayList<Vector3dc>()
        var supportHeight = 0.0
        var upwardLoad = 0.0
        val padContacts = HashMap<Pair<BlockPos, Long>, Direction>()
        val inspected = HashSet<Long>()
        fun inspect(be: BlockEntity, shipId: Long?) {
            if (be.isRemoved || !inspected.add(be.blockPos.asLong())) return
            val anchor = Anchor(shipId, be.blockPos.immutable())
            if (be is GripPadBlockEntity && shipId != null) {
                val sample = GripPadController.feedback(GripPadController.Key(level.dimensionId, be.blockPos)) ?: return
                val load = sample.normalLoad?.takeIf { it.isFinite() && it > 0.0 } ?: return
                val contact = sample.support?.takeIf { it.source == shipId && it.target !in visited } ?: return
                feet.add(Foot(anchor, load))
                padContacts[be.blockPos to contact.target] = be.blockState.getValue(DirectionalBlock.FACING)
                val points = contact.points.filter { it.isFinite }
                if (contact.upward > 0.05 && points.isNotEmpty()) {
                    val weight = load * contact.upward
                    supportHeight += weight * points.map { it.y() }.average()
                    upwardLoad += weight
                    supportPoints.addAll(points)
                }
            } else joint(be, anchor)?.let(joints::add)
        }

        for (ship in loaded) ship.activeChunksSet.forEach { x, z ->
            level.chunkSource.getChunkNow(x, z)?.blockEntities?.values?.forEach { inspect(it, ship.id) }
        }
        for (pos in worldAnchors) {
            if (level.hasChunkAt(pos)) level.getBlockEntity(pos)?.let { inspect(it, null) }
        }

        AeronautContactTelemetry.watch(level.dimensionId, visited)
        val samples = AeronautContactTelemetry.read(level.dimensionId)
        val loadedById = loaded.associateBy { it.id }
        val sources = visited.filter(samples::containsKey)
        for (source in sources) {
            val contacts = samples.getValue(source)
            val sourceShip = loadedById[source]
            val modelToWorld = sourceShip?.transform?.shipToWorld ?: world.allBodies.getById(source)?.kinematics?.toWorld ?: continue
            val ordinary = ArrayList<AeronautContactTelemetry.Cell>()
            for (contact in contacts) {
                if (contact.target in visited) continue
                for (cell in contact.cells) {
                    val face = if (sourceShip != null) occupiedFace(level, cell) else cell.face
                    if (padContacts[face.block to contact.target] == face.direction) continue
                    ordinary.add(if (face == cell.face) cell else cell.copy(face = face))
                    val center = modelToWorld.transformPosition(cell.center, Vector3d())
                    supportHeight += cell.upwardLoad * center.y
                    upwardLoad += cell.upwardLoad
                    for (point in cell.points) supportPoints.add(modelToWorld.transformPosition(point, Vector3d()))
                }
            }
            val faces = AeronautSupportGeometry.connectedFaces(ordinary, MAX_FACE_CHECKS / sources.size) { face ->
                sourceShip != null && hasFullFace(level, face)
            }
            for ((center, load) in faces) {
                val position = if (sourceShip != null) center else modelToWorld.transformPosition(center, Vector3d())
                contactFaces.add(ContactFace(sourceShip?.id, position, load))
            }
        }

        val offset = moment.takeIf { mass.isFinite() && mass > 0 && it.isFinite }
        val center = if (offset != null) root.transform.worldToShip.transformPosition(offset.div(mass).add(origin))
            else Vector3d(root.transform.positionInShip)
        val hull = if (upwardLoad > 0) AeronautSupportGeometry.outline(supportPoints, supportHeight / upwardLoad) else emptyList()
        val outline = if (hull.size <= MAX_OUTLINE_POINTS) hull else List(MAX_OUTLINE_POINTS) { hull[it * hull.size / MAX_OUTLINE_POINTS] }
        val outlinePose = if (outline.isNotEmpty()) OutlinePose.of(root.transform) else null
        val detail = Detail(bodyCount.coerceAtLeast(1), feet.sortedByDescending { it.load }.take(MAX_MARKERS), outline,
            joints.take(MAX_MARKERS), contactFaces.sortedByDescending { it.load }.take(MAX_CONTACT_FACES), outlinePose)
        return Sample(if (offset != null) mass else root.inertiaData.mass, center, detail)
    }

    private fun occupiedFace(level: ServerLevel, cell: AeronautContactTelemetry.Cell): AeronautContactTelemetry.Face {
        val face = cell.face
        val pos = face.block
        if (blockState(level, pos)?.isAir == false) return face
        val center = cell.center
        val xs = faceOffsets(center.x() - pos.x, face.direction.axis == Direction.Axis.X)
        val ys = faceOffsets(center.y() - pos.y, face.direction.axis == Direction.Axis.Y)
        val zs = faceOffsets(center.z() - pos.z, face.direction.axis == Direction.Axis.Z)
        for (x in xs) for (y in ys) for (z in zs) {
            if (x == 0 && y == 0 && z == 0) continue
            val candidate = pos.offset(x, y, z)
            if (blockState(level, candidate)?.isAir == false) return face.copy(block = candidate)
        }
        return face
    }

    private fun faceOffsets(offset: Double, fixed: Boolean): IntArray = when {
        fixed -> intArrayOf(0)
        offset < 0.03 -> intArrayOf(0, -1)
        offset > 0.97 -> intArrayOf(0, 1)
        else -> intArrayOf(0)
    }

    private fun hasFullFace(level: ServerLevel, face: AeronautContactTelemetry.Face): Boolean {
        val normal = face.direction
        val side = normal.axis.choose(face.block.x, face.block.y, face.block.z)
        val boundary = (side + if (normal.axisDirection == Direction.AxisDirection.POSITIVE) 1 else 0) * 8
        return boundary == face.plane && blockState(level, face.block)?.isFaceSturdy(level, face.block, normal) == true
    }

    private fun blockState(level: ServerLevel, pos: BlockPos) =
        level.chunkSource.getChunkNow(pos.x shr 4, pos.z shr 4)?.getBlockState(pos)

    private fun joint(be: BlockEntity, anchor: Anchor): Joint? {
        val device = be as? IThinkable ?: return null
        val kind = when (be) {
            is ExtendonBlockEntity -> JointKind.EXTENDON
            is HingeBlockEntity -> JointKind.HINGE
            is GimbalBearingBlockEntity -> JointKind.GIMBAL
            is PhysBearingBlockEntity -> JointKind.BEARING
            is PhysicalGantryBlockEntity -> JointKind.GANTRY
            is PhysicalLinearBlockEntity -> JointKind.PISTON
            else -> return null
        }
        val side = Direction.entries.firstOrNull(device::providesThinkable) ?: return null
        val actual = device.thinkableValue(side)?.takeIf(Double::isFinite) ?: return null
        fun value(reading: String) = device.thinkableValue(side, reading)?.takeIf(Double::isFinite)
        return when (be) {
            is ExtendonBlockEntity -> Joint(anchor, kind, actual, value("target_length"))
            is HingeBlockEntity -> Joint(anchor, kind, actual, lower = value("lower_limit"), upper = value("upper_limit"))
            is GimbalBearingBlockEntity -> Joint(anchor, kind, actual, value("target_tilt"), error = value("tilt_error"))
            is PhysBearingBlockEntity -> {
                val target = if (be.aligning) 0.0 else when (be.movementMode?.get()) {
                    LockedMode.FOLLOW_ANGLE, LockedMode.LOCKED -> Math.IEEEremainder(-be.targetAngle.toDouble(), 360.0)
                    else -> null
                }
                Joint(anchor, kind, actual, target?.takeIf(Double::isFinite))
            }
            is PhysicalLinearBlockEntity -> Joint(anchor, kind, actual, be.targetDisplacement())
            else -> null
        }
    }
}
