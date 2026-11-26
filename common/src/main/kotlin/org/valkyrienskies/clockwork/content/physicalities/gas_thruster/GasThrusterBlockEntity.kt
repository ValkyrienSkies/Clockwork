package org.valkyrienskies.clockwork.content.physicalities.gas_thruster

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.util.toMinecraft
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautForceTelemetry
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautForceSource
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautForceAccumulator
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrHit
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrPassage
import org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr.GatemakyrTarget
import org.valkyrienskies.clockwork.content.kinetics.combobulator.IThinkable
import org.valkyrienskies.clockwork.content.kinetics.combobulator.ThinkableConnections
import org.valkyrienskies.clockwork.content.physicalities.steering.DrivingControlLinks
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.clockwork.util.sound.GasThrusterSound
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.internal.world.VsiServerShipWorld
import org.valkyrienskies.clockwork.util.kelvin.KelvinParticleHelper
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.api.BlockEntityPhysicsListener
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.toWorldCoordinates
import org.valkyrienskies.mod.common.world.clipIncludeShips
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.*

@OptIn(VsBeta::class)
class GasThrusterBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) :
    KNodeBlockEntity(type, pos, state), BlockEntityPhysicsListener, AeronautForceSource, IThinkable {

    companion object {
        internal const val NOZZLE_PIVOT = 6.0 / 16.0
        internal const val NOZZLE_LENGTH = 1.0 - NOZZLE_PIVOT
        private const val MAX_GIMBAL_PITCH = 30.0
        private const val MIN_VISIBLE_GAS_KG_PER_TICK = 0.000001
        private val AIR_ID = ResourceLocation("kelvin", "air")
        private const val EPS = 0.00001
        private const val CLEARANCE = 0.018

        private val spawningExhaust = ThreadLocal<Boolean>()
        @JvmStatic fun isSpawningExhaust() = spawningExhaust.get() == true

        internal fun spawnExhaust(action: () -> Unit) {
            spawningExhaust.set(true)
            try { action() } finally { spawningExhaust.remove() }
        }

        @JvmStatic fun sweepExhaust(level: ClientLevel, origin: Vec3, delta: Vec3): ExhaustMotion? {
            val path = castExhaust(level, origin, delta)
            val last = path.lastOrNull() ?: return null
            if (last.endNormal == null && path.size == 1) return null
            val stopped = last.endNormal != null
            return ExhaustMotion(last.end.add(last.endNormal?.scale(CLEARANCE) ?: Vec3.ZERO),
                if (stopped) Vec3.ZERO else last.direction.scale(delta.length()), stopped, path.any { it.gate != null })
        }

        internal fun castExhaust(world: ClientLevel, start: Vec3, delta: Vec3): List<ExhaustSegment> {
            val segments = ArrayList<ExhaustSegment>(2)
            var remaining = delta.length()
            var origin = start
            var direction = delta.normalize()
            var startNormal: Vec3? = null
            var startRadius = Double.POSITIVE_INFINITY
            var hops = 0
            repeat(4) {
                if (!remaining.isFinite() || remaining <= EPS) return segments
                val end = origin.add(direction.scale(remaining))
                val hit = world.clipIncludeShips(ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null), true)
                val path = (hit as? GatemakyrHit)?.gatePath
                if (path != null) for ((i, crossing) in path.crossings.withIndex()) {
                    val (entry, exit) = crossing
                    val gate = path.gates[i]
                    val passage = gate.passage
                    val length = origin.distanceTo(entry).coerceAtMost(remaining)
                    val front = passage.entrance.normal.normalize()
                    val normal = if (direction.dot(front) < 0) front else front.scale(-1.0)
                    segments.add(ExhaustSegment(origin, direction, length, startNormal, normal, passage, startRadius,
                        apertureRadius(entry, passage.entrance, gate.source)))
                    if (++hops > 4 || !world.hasChunkAt(BlockPos.containing(exit))) return segments
                    startRadius = apertureRadius(exit, passage.exit, gate.destination)
                    origin = exit
                    direction = passage.direction(direction).normalize()
                    startNormal = passage.direction(normal).scale(-1.0)
                    remaining = (remaining - length).coerceAtLeast(0.0)
                }
                if (hit.type == HitResult.Type.MISS) {
                    val length = if (path == null) remaining else
                        hit.location.subtract(origin).dot(direction).coerceIn(0.0, remaining)
                    segments.add(ExhaustSegment(origin, direction, length, startNormal, null, startRadius = startRadius))
                    return segments
                }
                val face = hit.direction.normal.toJOMLD()
                world.getLoadedShipManagingPos(hit.blockPos)?.renderTransform?.shipToWorld?.transformDirection(face)
                val normal = face.normalize().toMinecraft()
                val length = hit.location.subtract(origin).dot(direction).coerceIn(0.0, remaining)
                segments.add(ExhaustSegment(origin, direction, length, startNormal, normal, startRadius = startRadius))
                val incidence = normal.dot(direction)
                val tangent = direction.subtract(normal.scale(incidence))
                if (length < EPS || incidence >= -EPS || tangent.lengthSqr() < EPS * EPS) return segments
                remaining -= length + CLEARANCE
                origin = hit.location.add(normal.scale(CLEARANCE))
                direction = tangent.normalize()
                startNormal = normal
                startRadius = Double.POSITIVE_INFINITY
            }
            return segments
        }

        private fun apertureRadius(point: Vec3, frame: GatemakyrTarget.Frame, target: GatemakyrTarget): Double {
            val offset = point.subtract(frame.position)
            fun clearance(axis: Vec3, cells: Int) = axis.length() * cells / 2 - abs(offset.dot(axis.normalize()))
            return minOf(clearance(frame.u, target.width), clearance(frame.v, target.height)).coerceAtLeast(0.0)
        }
    }

    internal data class ExhaustSegment(val origin: Vec3, val direction: Vec3, val length: Double,
                                       val startNormal: Vec3?, val endNormal: Vec3?, val gate: GatemakyrPassage? = null,
                                       val startRadius: Double = Double.POSITIVE_INFINITY, val endRadius: Double = Double.POSITIVE_INFINITY) {
        val end get() = origin.add(direction.scale(length))
    }

    class ExhaustMotion(val position: Vec3, val velocity: Vec3, val stopped: Boolean, val crossedGate: Boolean)

    @Volatile
    override lateinit var dimension: DimensionId

    @Volatile
    var thrust = 0.0
    @Volatile
    private var gimbalPitch = 0.0
    private val facing get() = blockState.getValue(BlockStateProperties.FACING)
    private val gimbalSide get() = if (facing.axis.isHorizontal) Direction.UP else Direction.NORTH

    var velocity = 0.0
    private var chamberHeat = 0.0
    internal val normalizedTemperature get() = ((chamberHeat - 550.0) / 900.0).coerceIn(0.0, 1.0)
    internal val stellarSteam get() = ((chamberHeat - 1025.0) / 75.0).coerceIn(0.0, 1.0).toFloat() *
        (steamFraction - exhaustFraction).coerceAtLeast(0f)
    internal val violetExhaust get() = ((chamberHeat - 1480.0) / 140.0).coerceIn(0.0, 1.0).toFloat() *
        (exhaustFraction - 8f * steamFraction).coerceAtLeast(0f)
    private var nonAirFlow = 0.0
    var expansion = 0f
        private set
    var plumeHeat = 0f
        private set
    var steamFraction = 0f
        private set
    var vaporStrength = 0f
        private set
    var exhaustFraction = 0f
        private set
    var previousHeatSpread = 0f
        private set
    var heatSpread = 0f
        private set
    var previousLipGlow = 0f
        private set
    var lipGlow = 0f
        private set
    val plumeLength get() = (0.45f + 2.8f * (velocity / 800.0).coerceIn(0.0, 1.4).toFloat()) *
        (0.45f + 0.85f * max(plumeHeat, vaporStrength)) * (1f - 0.35f * steamFraction)

    val gasMassFlow = HashMap<GasType, Double>()
    val massPerParticle = 0.005

    override fun write(tag: CompoundTag, clientPacket: Boolean) {

        val compound = CompoundTag()
        for ((gas, mass) in gasMassFlow) {
            compound.putDouble(gas.resourceLocation.toString(), mass)
        }
        tag.put("GasMassFlow", compound)
        tag.putDouble("FlowRate",velocity)
        tag.putDouble("Thrust",thrust)
        tag.putDouble("PlumeTemperature", chamberHeat)
        tag.putFloat("PlumeExpansion", expansion)
        if (clientPacket) tag.putDouble("GimbalPitch", gimbalPitch)
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {

        super.read(tag, clientPacket)
        gasMassFlow.clear()
        val compound = tag.getCompound("GasMassFlow")
        for (key in compound.allKeys) {
            val gasType = GasTypeRegistry.getGasType(ResourceLocation(key)) ?: continue
            gasMassFlow[gasType] = compound.getDouble(key)
        }

        velocity = tag.getDouble("FlowRate")
        thrust = tag.getDouble("Thrust")
        chamberHeat = tag.getDouble("PlumeTemperature")
        expansion = tag.getFloat("PlumeExpansion")
        gimbalPitch = if (clientPacket) tag.getDouble("GimbalPitch") else 0.0
        nonAirFlow = gasMassFlow.entries.sumOf { (gas, mass) -> if (gas.resourceLocation == AIR_ID) 0.0 else mass }
        val total = nonAirFlow.takeIf { it >= MIN_VISIBLE_GAS_KG_PER_TICK } ?: 0.0
        steamFraction = if (total > 0) ((gasMassFlow[ClockworkGasses.STEAM] ?: 0.0) / total).toFloat() else 0f
        exhaustFraction = if (total > 0) ((gasMassFlow[ClockworkGasses.EXHAUST] ?: 0.0) / total).toFloat() else 0f
        vaporStrength = ((chamberHeat - 350.0) / 450.0).coerceIn(0.0, 1.0).toFloat() * steamFraction
        plumeHeat = (normalizedTemperature *
            sqrt((4.0 * exhaustFraction).coerceIn(0.0, 1.0))).toFloat()

    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {}

    override fun acceptsThinkable(side: Direction) = side == gimbalSide

    override fun thinkablePortName(side: Direction): Component? =
        if (acceptsThinkable(side)) Component.translatable("thinkable.vs_clockwork.port.gimbal_pitch") else null

    private fun requestedGimbalPitch(level: ServerLevel): Double {
        val wheelThrottle = DrivingControlLinks.get(level).throttle(level, blockPos)
        val source = ThinkableConnections.sourceAt(level, blockPos, gimbalSide, cableOnly = wheelThrottle != null)
        if (source == null) return -(wheelThrottle ?: 0f) * MAX_GIMBAL_PITCH
        return source.device?.thinkableValue(source.side, "")?.takeIf { it.isFinite() }
            ?.coerceIn(-MAX_GIMBAL_PITCH, MAX_GIMBAL_PITCH) ?: 0.0
    }

    private fun thrustDirection() = facing.normal.toJOMLD().let { direction ->
        val angle = Math.toRadians(gimbalPitch)
        direction.mul(cos(angle)).fma(-sin(angle), gimbalSide.normal.toJOMLD())
    }

    internal fun exhaustDirection() = thrustDirection().negate()

    internal var plumeSegments: List<ExhaustSegment> = emptyList()
        private set

    internal var gateExits: List<Vec3> = emptyList()
        private set
    private var plumeBounds = AABB(pos)
    override fun getRenderBoundingBox(): AABB = plumeBounds

    fun clientTick() {
        val level = level ?: return
        previousLipGlow = lipGlow
        val showAir = nonAirFlow < MIN_VISIBLE_GAS_KG_PER_TICK
        val flow = if (showAir) gasMassFlow.values.sum() else nonAirFlow
        val metalTarget = (normalizedTemperature *
            (1.0 - exp(-flow / 0.0015))).toFloat()
        lipGlow += (metalTarget - lipGlow) * if (metalTarget > lipGlow) 0.11f else 0.018f
        previousHeatSpread = heatSpread
        heatSpread += (lipGlow - heatSpread) * if (lipGlow > heatSpread) 0.026f else 0.045f
        plumeSegments = emptyList()
        gateExits = emptyList()
        plumeBounds = AABB(blockPos)
        if (gasMassFlow.isEmpty() || velocity == 0.0) return

        if (level is ClientLevel) GasThrusterSound.start(this)

        // Handle particles
        val outward = facing.opposite
        val exhaustDirection = exhaustDirection()
        val speed = (abs(velocity) / 950.0).coerceAtMost(1.15)
        val mouth = blockPos.toJOMLD().add(0.5, 0.5, 0.5).fma(NOZZLE_PIVOT, outward.normal.toJOMLD())
            .fma(NOZZLE_LENGTH, exhaustDirection)
        val ductNodePos = getDuctNodePosition()

        val source = (level as? ClientLevel)?.getLoadedShipManagingPos(blockPos)?.renderTransform
        fun worldPoint(p: Vector3d) = (source?.shipToWorld?.transformPosition(p) ?: p).toMinecraft()
        fun localPoint(p: Vec3) = Vector3d(p.x, p.y, p.z).also { source?.worldToShip?.transformPosition(it) }
        fun localNormal(n: Vec3?) = n?.let { Vector3d(it.x, it.y, it.z) }?.also { source?.worldToShip?.transformDirection(it) }?.normalize()?.toMinecraft()
        val start = Vector3d(mouth).fma(-0.40, exhaustDirection)
        val worldStart = worldPoint(Vector3d(start))
        val range = plumeLength * 1.16 + 0.58
        val delta = worldPoint(Vector3d(start).fma(range, exhaustDirection)).subtract(worldStart)
        val segments = if (level is ClientLevel) castExhaust(level, worldStart, delta)
            else listOf(ExhaustSegment(worldStart, delta.normalize(), delta.length(), null, null))
        gateExits = segments.zipWithNext().mapNotNull { (entry, exit) -> exit.origin.takeIf { entry.gate != null } }
        plumeSegments = segments.map { segment ->
            val origin = localPoint(segment.origin)
            val end = localPoint(segment.end)
            plumeBounds = plumeBounds.minmax(AABB(origin.toMinecraft(), end.toMinecraft()).inflate(0.5))
            val axis = end.sub(origin)
            val length = axis.length()
            val direction = if (length > 0.0) axis.div(length) else exhaustDirection
            ExhaustSegment(origin.sub(blockPos.toJOMLD()).toMinecraft(), direction.toMinecraft(), length,
                localNormal(segment.startNormal), localNormal(segment.endNormal), segment.gate,
                segment.startRadius * range / delta.length(), segment.endRadius * range / delta.length())
        }
        val jetStrength = max(plumeHeat, vaporStrength)
        for ((gas, mass) in gasMassFlow) {
            if (!showAir && gas.resourceLocation == AIR_ID) continue
            val particleCount = mass / massPerParticle
            KelvinParticleHelper.spawnJetForGas(
                level,
                ductNodePos,
                gas,
                mouth,
                outward,
                speed,
                particleCount * (1.0 - 0.65 * plumeHeat * exhaustFraction),
                radius = 0.24 - 0.16 * jetStrength,
                outwardOffset = min(1f, jetStrength * 8f) * plumeLength * 0.55,
                jetDirection = exhaustDirection
            ) { pos, vel ->
                if (level !is ClientLevel) true else {
                    val sample = worldPoint(pos)
                    val motion = sweepExhaust(level, worldStart, sample.subtract(worldStart))
                    if (motion == null) {
                        pos.set(sample.x, sample.y, sample.z)
                        source?.shipToWorld?.transformDirection(vel)
                    } else {
                        pos.set(motion.position.x, motion.position.y, motion.position.z)
                        vel.set(motion.velocity.x, motion.velocity.y, motion.velocity.z)
                        if (!motion.stopped) vel.normalize(speed)
                    }
                    motion?.stopped != true
                }
            }
        }
    }

    private fun clearMassFlow() {
        gasMassFlow.clear()
        velocity = 0.0
        chamberHeat = 0.0
        expansion = 0f
        thrust = 0.0
    }

    @OptIn(GameTickOnly::class)
    override fun tick() {
        super.tick()
        val currentLevel = level ?: return clearMassFlow()
        if (currentLevel.isClientSide) return clientTick()
        if (currentLevel !is ServerLevel) return

        val nextPitch = requestedGimbalPitch(currentLevel)
        val pitchChanged = gimbalPitch != nextPitch
        gimbalPitch = nextPitch
        val hadExhaust = gasMassFlow.isNotEmpty() || velocity != 0.0 || thrust != 0.0
        tickExhaust(currentLevel)
        if (pitchChanged || hadExhaust || gasMassFlow.isNotEmpty()) sendData()
    }

    @OptIn(GameTickOnly::class)
    private fun tickExhaust(currentLevel: ServerLevel) {
        val ductNodePos = getDuctNodePosition()
        val kelvin = ClockworkMod.getKelvin(currentLevel)
        val node = kelvin.getNodeAt(ductNodePos) as? GasThrusterDuctNode ?: return clearMassFlow()

        val worldPosition = currentLevel.toWorldCoordinates(blockPos.toJOMLD().add(0.5, 0.5, 0.5))
        val exhaustNeighbor = blockPos.relative(facing.opposite)
        val worldExhaustCenter = currentLevel.toWorldCoordinates(Vec3.atCenterOf(exhaustNeighbor))
        if (!worldPosition.isFinite || !worldExhaustCenter.x.isFinite() ||
            !worldExhaustCenter.y.isFinite() || !worldExhaustCenter.z.isFinite()) {
            node.environmentValid = false
            return clearMassFlow()
        }
        val aero = (currentLevel.shipObjectWorld as? VsiServerShipWorld)?.aerodynamicUtils
        val ambientPressure = aero?.getAirPressureForY(worldPosition.y, currentLevel.dimensionId)
        val ambientTemperature = aero?.getAirTemperatureForY(worldPosition.y, currentLevel.dimensionId)

        if (ambientPressure == null || !ambientPressure.isFinite() || ambientPressure < 0.0) {
            node.environmentValid = false
            return clearMassFlow()
        }

        val worldExhaustPos = BlockPos.containing(worldExhaustCenter)
        val localState = if (currentLevel.hasChunkAt(exhaustNeighbor)) currentLevel.getBlockState(exhaustNeighbor) else null
        val worldState = if (currentLevel.hasChunkAt(worldExhaustPos)) currentLevel.getBlockState(worldExhaustPos) else null
        val nozzleOpenToAir = localState != null && worldState != null &&
            localState.fluidState.isEmpty && localState.getCollisionShape(currentLevel, exhaustNeighbor).isEmpty &&
            worldState.fluidState.isEmpty && worldState.getCollisionShape(currentLevel, worldExhaustPos).isEmpty

        node.ambientPressure = ambientPressure
        node.ambientTemperature = ambientTemperature?.takeIf { it.isFinite() && it > 0.0 } ?: 293.15
        node.ambientAirGas = GasTypeRegistry.getGasType(AIR_ID)
        node.nozzleOpenToAir = nozzleOpenToAir
        node.environmentValid = true
        if (nozzleOpenToAir && ambientPressure > 0.0) {
            GasThrusterDuctNode.diffuseOpenTailpipe(kelvin, ductNodePos, ambientPressure, 0.05)
        }

        val summary = node.drainTickSummary()
        if (summary.totalExpelledMass <= 0.0 || summary.averageRawThrust <= 0.0) return clearMassFlow()

        gasMassFlow.clear()
        gasMassFlow.putAll(summary.expelledGasMasses)
        velocity = summary.averageVelocity
        chamberHeat = summary.averageTemperature
        expansion = summary.averageExpansion
        thrust = (summary.averageRawThrust * ClockworkConfig.SERVER.gasThruster.gasThrusterForceMul)
            .takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    }

    @OptIn(PhysTickOnly::class)
    override fun physTick(physShip: PhysShip?, physLevel: PhysLevel) {
        physShip?: return
        if (blockState.block !is GasThrusterBlock) return
        val force = thrustDirection().mul(thrust)
        physShip.applyModelForce(force, blockPos.toJOMLD().add(0.5,0.5,0.5))
        val source = "gas:${blockPos.asLong()}"
        val readout = AeronautForceTelemetry.begin(physShip.id, source)
        readout?.add(blockPos.toJOMLD().add(0.5, 0.5, 0.5), force)
        AeronautForceTelemetry.publish(physShip.id, source, readout)
    }

    override fun addPotentialForces(level: ServerLevel, ship: LoadedServerShip, thrust: AeronautForceAccumulator) {
        // Gas jets have no nameplate thrust: available output depends on their gas supply.
        thrust.add(blockPos.toJOMLD().add(0.5, 0.5, 0.5),
            thrustDirection().mul(this.thrust))
    }

}
