package org.valkyrienskies.clockwork.content.physicalities.extendon

import dev.architectury.platform.Platform
import net.createmod.ponder.api.level.PonderLevel
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.api.thinkable.ThinkableReading
import org.valkyrienskies.clockwork.content.forces.contraption.BearingDriveSupport
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.util.gtpa
import org.valkyrienskies.clockwork.util.updateJoint
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.internal.joints.*
import org.valkyrienskies.core.internal.joints.VSD6Joint.D6Axis
import org.valkyrienskies.core.internal.joints.VSD6Joint.D6Motion
import org.valkyrienskies.core.internal.joints.VSD6Joint.D6Drive
import org.valkyrienskies.kelvin.api.*
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.isBlockInShipyard
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.EnumMap
import org.valkyrienskies.kelvin.api.DuctNetwork.Companion.idealGasConstant
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.dimensionId
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

import org.valkyrienskies.clockwork.util.universal_joint.BlockAnchorLinks
import org.valkyrienskies.clockwork.util.universal_joint.GasLinkBlockEntity
import org.valkyrienskies.clockwork.util.matchesByAnchors

class ExtendonBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : GasLinkBlockEntity(type, pos, state), IThinkable {
    override val linkKind = BlockAnchorLinks.Kind.EXTENDON
    override val maxCreationDistance: Double get() = MAX_LENGTH

    override fun providesThinkable(side: Direction) = side.axis != blockState.getValue(BlockStateProperties.FACING).axis

    override fun thinkablePortName(side: Direction): Component? =
        Component.translatable("thinkable.vs_clockwork.port.readings").takeIf { providesThinkable(side) }

    override fun thinkableReadings(side: Direction) = if (providesThinkable(side)) READINGS else emptyList()

    override fun thinkableValue(side: Direction) = thinkableValue(side, "length")

    override fun thinkableValue(side: Direction, reading: String): Double? {
        val world = level as? ServerLevel ?: return null
        if (!providesThinkable(side)) return null
        val other = connectedEnd(world) ?: return null
        val point = anchorPoint(world, blockPos) ?: return null
        val otherPoint = anchorPoint(world, other.blockPos) ?: return null
        val length = point.distance(otherPoint).takeIf(Double::isFinite) ?: return null
        return when (reading) {
            "", "length" -> length
            "target_length" -> targetLength(ClockworkMod.getKelvin(world), other, world.dimensionId)
            "length_error" -> targetLength(ClockworkMod.getKelvin(world), other, world.dimensionId) - length
            "extension_speed" -> if (length < 1e-4) null else {
                val relative = velocityAt(world.getShipManagingPos(other.blockPos), otherPoint)
                    .sub(velocityAt(world.getShipManagingPos(blockPos), point))
                relative.dot(otherPoint.sub(point).div(length))
            }
            else -> null
        }?.takeIf(Double::isFinite)
    }

    private fun connectedEnd(world: Level): ExtendonBlockEntity? {
        val id = linkId ?: return null
        val otherPos = connectedPos?.takeIf(world::hasChunkAt) ?: return null
        return (world.getBlockEntity(otherPos) as? ExtendonBlockEntity)
            ?.takeIf { !it.isRemoved && it.linkId == id && it.connectedPos == blockPos }
    }

    private fun anchorPoint(world: Level, pos: BlockPos): Vector3d? {
        val center = pos.toJOMLD().add(0.5, 0.5, 0.5)
        val ship = world.getLoadedShipManagingPos(pos)
        return if (ship != null) ship.transform.shipToWorld.transformPosition(center)
            else center.takeIf { world is PonderLevel || !world.isBlockInShipyard(pos) }
    }

    internal fun buildJointBlueprints(other: ExtendonBlockEntity): Pair<VSDistanceJoint, VSD6Joint>? {
        val level = level as? ServerLevel ?: return null

        val pos0 = blockPos.toJOMLD().add(0.5, 0.5, 0.5)
        val pos1 = other.blockPos.toJOMLD().add(0.5, 0.5, 0.5)
        val ship0 = level.getShipManagingPos(blockPos)
        val ship1 = level.getShipManagingPos(other.blockPos)
        val shipId0 = ship0?.id
        val shipId1 = ship1?.id
        if (shipId0 == shipId1) return null
        val world0 = ship0?.transform?.shipToWorld?.transformPosition(pos0, Vector3d()) ?: Vector3d(pos0)
        val world1 = ship1?.transform?.shipToWorld?.transformPosition(pos1, Vector3d()) ?: Vector3d(pos1)
        val separation = world0.distance(world1)
        if (!separation.isFinite() || separation < 1e-4) return null
        val frame = Quaterniond().rotationTo(Vector3d(1.0, 0.0, 0.0), world1.sub(world0, Vector3d()).normalize())
        val quater0 = Quaterniond(ship0?.transform?.shipToWorldRotation ?: Quaterniond()).invert().mul(frame)
        val quater1 = Quaterniond(ship1?.transform?.shipToWorldRotation ?: Quaterniond()).invert().mul(frame)
        val mass = effectiveMass(ship0, ship1)

        val distanceJoint = VSDistanceJoint(
            pose0 = VSJointPose(pos0, quater0),
            pose1 = VSJointPose(pos1, quater1),
            shipId0 = shipId0,
            shipId1 = shipId1,
            minDistance = separation.toFloat(),
            maxDistance = separation.toFloat(),
            stiffness = (mass * STIFFNESS).toFloat(),
            damping = (mass * DAMPING).toFloat()
        )

        val limit = VSD6Joint.LimitCone(PI.toFloat() / 4f, PI.toFloat() / 4f,
            stiffness = (mass * 20.0).toFloat(), damping = (mass * 8.0).toFloat())
        val motions = EnumMap<D6Axis, D6Motion>(D6Axis::class.java)
        motions[D6Axis.X] = D6Motion.FREE
        motions[D6Axis.Y] = D6Motion.FREE
        motions[D6Axis.Z] = D6Motion.FREE
        val drives = twistDrive(mass)
        motions[D6Axis.TWIST] = if (drives == null) D6Motion.LOCKED else D6Motion.FREE
        motions[D6Axis.SWING1] = D6Motion.LIMITED
        motions[D6Axis.SWING2] = D6Motion.LIMITED

        val sphericalJoint = VSD6Joint(
            pose0 = VSJointPose(pos0, quater0),
            pose1 = VSJointPose(pos1, quater1),
            shipId0 = shipId0,
            shipId1 = shipId1,
            swingLimit = limit,
            motions = motions,
            drives = drives,
            drivePosition = drives?.let { VSD6Joint.DrivePosition(VSJointPose(Vector3d(), Quaterniond())) }
        )

        return distanceJoint to sphericalJoint
    }

    override fun tick() {
        super.tick()
        val world = level as? ServerLevel ?: return
        if (!main || connectedBe == null || distanceJoint == null || sphericalJoint == null || distanceJointId == null || sphericalJointId == null) return
        if (world.gtpa.getJointById(distanceJointId!!)?.matchesByAnchors(distanceJoint!!) != true ||
            world.gtpa.getJointById(sphericalJointId!!)?.matchesByAnchors(sphericalJoint!!) != true) return
        val kelvin = ClockworkMod.getKelvin(level)
        val serverLevel = level as ServerLevel

        val current = distanceJoint!!
        val other = connectedBe as? ExtendonBlockEntity ?: return
        val firstIsThis = current.pose0.pos.distanceSquared(blockPos.toJOMLD().add(.5, .5, .5)) < 1e-6
        val first = if (firstIsThis) this else other
        val second = if (firstIsThis) other else this
        val ship0 = serverLevel.getShipManagingPos(first.blockPos)
        val ship1 = serverLevel.getShipManagingPos(second.blockPos)
        if ((ship0?.id ?: -1L) != (current.shipId0 ?: -1L) || (ship1?.id ?: -1L) != (current.shipId1 ?: -1L)) return
        val point0 = ship0?.transform?.shipToWorld?.transformPosition(current.pose0.pos, Vector3d()) ?: Vector3d(current.pose0.pos)
        val point1 = ship1?.transform?.shipToWorld?.transformPosition(current.pose1.pos, Vector3d()) ?: Vector3d(current.pose1.pos)
        val chord = point1.sub(point0, Vector3d())
        val separation = chord.length()
        if (!separation.isFinite() || separation < 1e-4) return
        val target = targetLength(kelvin, other, serverLevel.dimensionId)
        if (!target.isFinite()) return
        val previous = current.minDistance?.toDouble()?.takeIf { it.isFinite() } ?: separation
        val length = previous + (target - previous).coerceIn(-MAX_SPEED / 20.0, MAX_SPEED / 20.0)
        val speed = velocityAt(ship1, point1).sub(velocityAt(ship0, point0)).dot(chord.div(separation))
        if (!speed.isFinite()) return
        val load = abs(separation - length) * STIFFNESS + abs(speed) * DAMPING
        val mass = effectiveMass(ship0, ship1)
        updateStress((load / MAX_ACCELERATION).toFloat())
        other.updateStress(stress)
        val strength = mass * (MAX_ACCELERATION / max(MAX_ACCELERATION, load))
        val next = current.copy(shipId0 = current.shipId0?.takeIf { it >= 0 },
            shipId1 = current.shipId1?.takeIf { it >= 0 }, minDistance = length.toFloat(), maxDistance = length.toFloat(),
            stiffness = (strength * STIFFNESS).toFloat(), damping = (strength * DAMPING).toFloat())
        next.shouldBeSerialized = false
        if (next != current) {
            serverLevel.gtpa.updateJoint(distanceJointId!!, next)
            distanceJoint = next
            other.distanceJoint = next
        }
        val angular = sphericalJoint!!
        val nextAngular = angular.copy(shipId0 = angular.shipId0?.takeIf { it >= 0 },
            shipId1 = angular.shipId1?.takeIf { it >= 0 }, drives = twistDrive(mass), swingLimit = angular.swingLimit?.copy(
            stiffness = (mass * 20.0).toFloat(), damping = (mass * 8.0).toFloat()))
        nextAngular.shouldBeSerialized = false
        if (nextAngular != angular) {
            serverLevel.gtpa.updateJoint(sphericalJointId!!, nextAngular)
            sphericalJoint = nextAngular
            other.sphericalJoint = nextAngular
        }
    }

    override fun heatableGoggleTooltip(tooltip: MutableList<Component>, isPlayerSneaking: Boolean): Boolean {
        val result = super.heatableGoggleTooltip(tooltip, isPlayerSneaking)
        val world = level ?: return result
        val other = connectedEnd(world) ?: return result
        val point = anchorPoint(world, blockPos) ?: return result
        val otherPoint = anchorPoint(world, other.blockPos) ?: return result
        val length = point.distance(otherPoint).takeIf(Double::isFinite) ?: return result
        val kelvin = if (world is PonderLevel || world is ServerLevel) ClockworkMod.getKelvin(world)
            else if (Minecraft.getInstance().isLocalServer && Platform.isFabric()) ClockworkMod.getKelvin()
            else ClockworkModClient.getKelvin()
        tooltip.add(lengthTooltip("measured_length", length))
        val target = targetLength(kelvin, other, world.dimensionId)
        if (target.isFinite()) tooltip.add(lengthTooltip("target_length", target))
        return true
    }

    private fun lengthTooltip(key: String, length: Double): Component = Component.translatable("vs_clockwork.extendon.$key",
        Component.literal("${(length * 10).roundToInt() / 10.0} m").withStyle(ChatFormatting.YELLOW))

    private fun targetLength(kelvin: DuctNetwork<*>, other: ExtendonBlockEntity, dimension: DimensionId): Double =
        (gasToDistance(kelvin, getDuctNodePosition(), dimension) +
            gasToDistance(kelvin, other.getDuctNodePosition(), dimension)).toDouble().coerceIn(MIN_LENGTH, MAX_LENGTH)

    companion object {
        private val READINGS = listOf("length", "target_length", "length_error", "extension_speed").map {
            ThinkableReading(it, Component.translatable("thinkable.vs_clockwork.reading.extendon.$it"))
        }
        private const val MIN_LENGTH = 1.5
        private const val MAX_LENGTH = 1000.0
        private const val MAX_SPEED = 4.0
        private const val STIFFNESS = 100.0
        private const val DAMPING = 20.0
        private const val MAX_ACCELERATION = 120.0

        private fun twistDrive(mass: Double): EnumMap<D6Drive, VSD6Joint.D6JointDrive>? {
            if (!BearingDriveSupport.supportsAngularDrives()) return null
            return EnumMap<D6Drive, VSD6Joint.D6JointDrive>(D6Drive::class.java).apply {
                put(D6Drive.TWIST, VSD6Joint.D6JointDrive((mass * 20.0).toFloat(),
                    (mass * 8.0).toFloat(), (mass * 60.0).toFloat()))
            }
        }

        private fun effectiveMass(a: ServerShip?, b: ServerShip?): Double {
            val inverseA = a?.takeUnless { it.isStatic }?.inertiaData?.mass?.takeIf { it.isFinite() && it > 0 }?.let { 1.0 / it } ?: 0.0
            val inverseB = b?.takeUnless { it.isStatic }?.inertiaData?.mass?.takeIf { it.isFinite() && it > 0 }?.let { 1.0 / it } ?: 0.0
            return if (inverseA + inverseB > 0.0) (1.0 / (inverseA + inverseB)).coerceAtMost(1e9) else 1.0
        }

        private fun velocityAt(ship: ServerShip?, point: Vector3d): Vector3d {
            if (ship == null || ship.isStatic) return Vector3d()
            return ship.angularVelocity.cross(point.sub(ship.transform.positionInWorld, Vector3d()), Vector3d()).add(ship.velocity)
        }


        // Calculates volume of cylinder via Ideal Gas Law, and then calculates said cylinder's height
        // Doesn't account for the elastic force of the hose, because doing so would require solving a cubic polynomial
        fun gasToDistance(network: DuctNetwork<*>, pos: DuctNodePos, dimensionId: DimensionId): Float {
            var moles = 0.0
            for ((gas, mass) in network.getGasMassAt(pos)) moles +=  gas.massToMoles(mass)

            val worldPos = Vector3d(pos.x, pos.y, pos.z)
            ValkyrienSkiesMod.currentServer?.allLevels?.firstOrNull { it.dimensionId == dimensionId }?.let { level ->
                level.getShipManagingPos(BlockPos.containing(pos.x, pos.y, pos.z))?.transform?.shipToWorld?.transformPosition(worldPos)
            }
            val pressure = vsApi.getServerShipWorld(ValkyrienSkiesMod.currentServer)?.aerodynamicUtils?.getAirPressureForY(worldPos.y, dimensionId) ?: 101325.0
            val temperature = network.getTemperatureAt(pos)

            if (!temperature.isFinite() || !moles.isFinite() || temperature <= 0.0 || moles <= 0.0) return 0f
            if (!pressure.isFinite() || pressure <= 0.0) return MAX_LENGTH.toFloat()
            val volume = temperature*idealGasConstant*moles/pressure
            val height = 4 * volume / PI


            return height.coerceIn(0.0, MAX_LENGTH).toFloat()
        }

        fun getQuaterniond(direction: Direction): Quaterniond {
            return when (direction) {
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

                else -> {
                    // UP or null
                    Quaterniond()
                }
            }
        }
    }

}
