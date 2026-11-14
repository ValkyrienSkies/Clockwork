package org.valkyrienskies.clockwork.content.contraptions.phys.linear

import com.simibubi.create.AllSoundEvents
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.IDisplayAssemblyExceptions
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3i
import org.valkyrienskies.clockwork.util.alignAssembledShip
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.internal.joints.VSD6Joint
import org.valkyrienskies.core.internal.joints.VSFixedJoint
import org.valkyrienskies.core.internal.joints.VSJoint
import org.valkyrienskies.core.internal.joints.VSJointPose
import org.valkyrienskies.core.internal.joints.VSPrismaticJoint
import org.valkyrienskies.core.internal.world.VsiPhysLevel
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.common.assembly.ShipAssembler.AssembleContext
import org.valkyrienskies.mod.common.assembly.ShipAssembler.assembleToShipFull
import org.valkyrienskies.mod.common.assembly.ShipAssembler.findMinAndMax
import org.valkyrienskies.mod.common.assembly.ShipAssembler.moveBlocksFromTo
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.inAssemblyBlacklist
import org.valkyrienskies.mod.common.util.toJOMLD

private const val DRIVE_VELOCITY_GAIN = 24.0
private const val DRIVE_POSITION_GAIN = 600.0
private const val MAX_DRIVE_ACCELERATION = 360.0

abstract class PhysicalLinearBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) :
    KineticBlockEntity(type, pos, state), IDisplayAssemblyExceptions, BlockEntityPhysicsListener, IThinkable {

    override fun providesThinkable(side: Direction) = side.axis != facing.axis
    override fun thinkablePortName(side: Direction): Component? =
        Component.translatable("thinkable.vs_clockwork.port.displacement").takeIf { providesThinkable(side) }

    @Synchronized
    override fun thinkableValue(side: Direction): Double? {
        val world = level as? ServerLevel ?: return null
        val current = slide ?: return null
        if (!providesThinkable(side) || !isAssembled) return null
        val child = world.getLoadedShipManagingPos(current.childAnchor)?.takeIf { it.id == current.childId } ?: return null
        val parent = if (current.parentId == null) null else
            world.getLoadedShipManagingPos(current.parentAnchor)?.takeIf { it.id == current.parentId } ?: return null
        return displacement(parent, child, current.parentAnchor, current.childAnchor, current.axis)
    }

    protected val facing: Direction get() = blockState.getValue(BlockStateProperties.FACING)
    internal abstract val isAssembled: Boolean
    @Volatile
    override lateinit var dimension: DimensionId
    private var assemblyException: AssemblyException? = null
    protected var waitForStop = false
    private var lockedOffset: Double? = null

    private var slide: Slide? = null
    private var jointId = -1
    private var appliedJoint: VSJoint? = null

    protected abstract fun assemble(level: ServerLevel)
    protected abstract fun disassemble(level: ServerLevel)

    fun toggleAssembly() {
        val level = level as? ServerLevel ?: return
        if (isAssembled) disassemble(level) else tryAssemble(level)
    }

    protected fun tryAssemble(level: ServerLevel) {
        val previousException = assemblyException
        assemblyException = try {
            assemble(level)
            null
        } catch (exception: AssemblyException) {
            exception
        }
        if (previousException != null || assemblyException != null) sendData()
    }

    override fun getLastAssemblyException(): AssemblyException? = assemblyException

    @Synchronized
    protected fun drive(
        parent: ServerShip?, child: ServerShip, parentAnchor: BlockPos, childAnchor: BlockPos,
        axis: Direction, min: Int, max: Int, speed: Double, locked: Boolean
    ) {
        val offset = displacement(parent, child, parentAnchor, childAnchor, axis)
        val target = when {
            !locked -> null
            speed != 0.0 -> ((lockedOffset ?: offset) + speed).coerceIn(offset - 0.5, offset + 0.5)
            slide?.speed?.let { it != 0.0 } == true -> offset
            else -> lockedOffset ?: offset
        }?.coerceIn(min.toDouble(), max.toDouble())
        if (lockedOffset != target) {
            lockedOffset = target
            setChanged()
        }
        slide = Slide(parent?.id, child.id, parentAnchor, childAnchor, axis, min, max, target, speed)
    }

    @Synchronized
    private fun removeJoint() {
        slide = null
        if (jointId != -1) (level as? ServerLevel)?.gtpa?.removeJoint(jointId)
        jointId = -1
        appliedJoint = null
    }

    protected fun stopDrive() {
        removeJoint()
        lockedOffset = null
    }

    @Synchronized
    protected fun pauseDrive() {
        slide = slide?.copy(speed = 0.0)
    }

    override fun invalidate() {
        removeJoint()
        super.invalidate()
    }

    @Synchronized
    override fun physTick(physShip: PhysShip?, physLevel: PhysLevel) {
        val slide = slide ?: return
        val child = physLevel.getShipById(slide.childId) ?: return
        val parent = slide.parentId?.let { physLevel.getShipById(it) ?: return }
        val world = physLevel as VsiPhysLevel
        val next = slide.joint(parent?.transform?.scaling?.x() ?: 1.0)
        if (appliedJoint != next || world.getJointById(jointId) == null) {
            if (world.getJointById(jointId) != null) world.removeJoint(jointId)
            jointId = world.addJoint(next)
            appliedJoint = next
        }
        if (jointId != -1 && next is VSPrismaticJoint && slide.speed != 0.0 && !child.isStatic) {
            slide.applyDrive(parent, child)
        }
    }

    protected fun playAssemblySound(level: ServerLevel, pos: BlockPos, assembled: Boolean) {
        val sound = if (assembled) AllSoundEvents.CONTRAPTION_ASSEMBLE else AllSoundEvents.CONTRAPTION_DISASSEMBLE
        sound.playOnServer(level, pos)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        super.write(tag, clientPacket)
        AssemblyException.write(tag, assemblyException)
        tag.putBoolean("WaitForStop", waitForStop)
        lockedOffset?.let { tag.putDouble("LockedOffset", it) }
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        assemblyException = AssemblyException.read(tag)
        waitForStop = tag.getBoolean("WaitForStop")
        lockedOffset = tag.getDouble("LockedOffset").takeIf { tag.contains("LockedOffset") && it.isFinite() }
    }

    private data class Slide(
        val parentId: Long?, val childId: Long, val parentAnchor: BlockPos, val childAnchor: BlockPos,
        val axis: Direction, val min: Int, val max: Int, val target: Double?, val speed: Double
    ) {
        fun joint(scale: Double): VSJoint {
            val direction = axis.normal.toJOMLD()
            val rotation = Quaterniond().rotationTo(Vector3d(1.0, 0.0, 0.0), direction)
            val childPose = VSJointPose(childAnchor.center.toJOML(), rotation)
            val parentPos = parentAnchor.center.toJOML()
            val fixed = if (min == max) min.toDouble() else target.takeIf { speed == 0.0 }
            if (fixed != null) {
                return VSFixedJoint(parentId, VSJointPose(parentPos.fma(fixed, direction), rotation), childId, childPose, compliance = 0.0)
            }
            val midpoint = (min.toDouble() + max) / 2.0
            val halfRange = ((max.toDouble() - min) * scale / 2.0).toFloat()
            return VSPrismaticJoint(
                parentId, VSJointPose(parentPos.fma(midpoint, direction), rotation), childId, childPose,
                linearLimitPair = VSD6Joint.LinearLimitPair(-halfRange, halfRange)
            )
        }

        fun applyDrive(parent: PhysShip?, child: PhysShip) {
            val axisWorld = axis.normal.toJOMLD()
            val scale = parent?.transform?.scaling?.x() ?: 1.0
            parent?.transform?.shipToWorldRotation?.transform(axisWorld)
            val childPoint = child.transform.shipToWorld.transformPosition(childAnchor.center.toJOML())
            val parentPoint = parentAnchor.center.toJOML()
            parent?.transform?.shipToWorld?.transformPosition(parentPoint)
            val position = childPoint.sub(parentPoint, Vector3d()).dot(axisWorld)
            if (target == null && (speed > 0 && position >= max * scale || speed < 0 && position <= min * scale)) return
            parentPoint.fma(position, axisWorld)
            val relativeVelocity = velocityAt(child, childPoint).sub(velocityAt(parent, parentPoint)).dot(axisWorld)
            val mass = if (parent == null || parent.isStatic) child.mass else 1.0 / (1.0 / child.mass + 1.0 / parent.mass)
            val motorVelocity = speed * scale * 20.0
            val correction = if (target != null) (target * scale - position) * DRIVE_POSITION_GAIN else 0.0
            val acceleration = ((motorVelocity - relativeVelocity) * DRIVE_VELOCITY_GAIN + correction)
                .coerceIn(-MAX_DRIVE_ACCELERATION, MAX_DRIVE_ACCELERATION)
            val force = axisWorld.mul(acceleration * mass)
            if (!force.isFinite) return
            child.applyWorldForce(force, childPoint)
            if (parent != null && !parent.isStatic) parent.applyWorldForce(force.negate(Vector3d()), parentPoint)
        }

        private fun velocityAt(ship: PhysShip?, point: Vector3d): Vector3d {
            if (ship == null) return Vector3d()
            return ship.angularVelocity.cross(point.sub(ship.transform.positionInWorld, Vector3d()), Vector3d())
                .add(ship.velocity)
        }
    }
}

internal fun displacement(parent: ServerShip?, child: ServerShip, parentAnchor: BlockPos, childAnchor: BlockPos, axis: Direction): Double {
    val point = child.transform.shipToWorld.transformPosition(childAnchor.center.toJOML())
    parent?.transform?.worldToShip?.transformPosition(point)
    return point.sub(parentAnchor.center.toJOML()).dot(axis.normal.toJOMLD())
}

internal fun AssembleContext.migrate(pos: BlockPos): BlockPos {
    val point = pos.center.toJOML().sub(fromCenter).add(toCenter)
    return BlockPos.containing(point.x, point.y, point.z)
}

internal fun assembleLinearShip(level: ServerLevel, blocks: Set<BlockPos>): AssembleContext {
    val (min, max) = findMinAndMax(blocks)
    val sourceCenter = min.center.add(max.center).scale(0.5).toJOML()
    val parent = level.getShipManagingPos(blocks.first())
    val assembly = assembleToShipFull(level, blocks).copy(fromCenter = sourceCenter)
    alignAssembledShip(assembly.ship, parent, sourceCenter, assembly.toCenter)
    return assembly
}

internal fun shipBlocks(level: ServerLevel, ship: ServerShip): Set<BlockPos> {
    val box = ship.shipAABB ?: return emptySet()
    if (!level.hasChunksAt(box.minX(), box.minZ(), box.maxX() - 1, box.maxZ() - 1)) return emptySet()
    return BlockPos.betweenClosed(
        box.minX(), box.minY(), box.minZ(), box.maxX() - 1, box.maxY() - 1, box.maxZ() - 1
    ).asSequence().filterNot(level::isEmptyBlock).mapTo(mutableSetOf()) { it.immutable() }
}

internal fun canAssembleLinearBlock(level: ServerLevel, pos: BlockPos, state: BlockState): Boolean =
    level.isLoaded(pos) && !state.inAssemblyBlacklist() &&
        (level.getBlockEntity(pos) as? PhysicalLinearBlockEntity)?.isAssembled != true

internal fun moveOffShip(
    level: ServerLevel, blocks: Set<BlockPos>, source: BlockPos, destination: BlockPos, ship: ServerShip
): Boolean {
    if (blocks.isEmpty()) return true
    val parent = level.getLoadedShipManagingPos(destination)
    for (pos in blocks) {
        val target = destination.offset(pos.subtract(source))
        if (level.getBlockState(pos).inAssemblyBlacklist() || !level.isLoaded(target) ||
            level.isOutsideBuildHeight(target) || (parent == null && !level.worldBorder.isWithinBounds(target)) ||
            !level.isEmptyBlock(target)) return false
    }
    val (min, max) = findMinAndMax(blocks)
    val corner = destination.offset(min.subtract(source))
    val center = Vector3i(
        corner.x + (max.x - min.x + 1) / 2,
        corner.y + (max.y - min.y + 1) / 2,
        corner.z + (max.z - min.z + 1) / 2
    )
    return moveBlocksFromTo(level, blocks, ship, parent, min, max, center).wasSuccessful
}
