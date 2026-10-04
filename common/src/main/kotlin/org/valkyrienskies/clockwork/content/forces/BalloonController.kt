package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.SectionPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.HitResult
import org.joml.Matrix3dc
import org.joml.Matrix4dc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.forces.data.BalloonData
import org.valkyrienskies.clockwork.content.forces.data.BalloonData.PhysBalloonData
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.mod.common.dimensionId
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

/**
 * Partially taken from https://github.com/SergeyFeduk/Create-Propulsion/blob/main/src/main/java/com/deltasf/createpropulsion/balloons/hot_air/BalloonAttachment.java
 * Many thanks to Delta for making his code open source and MIT licensed.
 */
@OptIn(PhysTickOnly::class, VsBeta::class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class BalloonController: ShipPhysicsListener {

    val balloons: ConcurrentHashMap<Int, BalloonData> = ConcurrentHashMap()
    @JsonIgnore
    @Volatile
    internal var forcefulBalloons: List<PhysBalloonData> = emptyList()

    @JsonIgnore private val pendingChanges = HashSet<BlockPos>()
    @JsonIgnore private val sections = HashMap<Long, MutableSet<Int>>()
    @JsonIgnore private var indexDirty = true

    val nextBalloonID: Int
        get() = (balloons.keys.maxOrNull() ?: 0) + 1

    @JsonIgnore
    private val epsilon = 1e-5

    // Reused on the physics thread.
    @JsonIgnore
    private val accumulatedForce = Vector3d()
    @JsonIgnore
    private val accumulatedTorque = Vector3d()
    @JsonIgnore
    private val shipCOMWorld = Vector3d()
    @JsonIgnore
    private val balloonWorldPos = Vector3d()
    @JsonIgnore
    private val leverArmWorld = Vector3d()
    @JsonIgnore
    private val upWorld = Vector3d(0.0, 1.0, 0.0)
    @JsonIgnore
    private val tmpForce = Vector3d()
    @JsonIgnore
    private val shipUpWorld = Vector3d()
    @JsonIgnore
    private val alignAxis = Vector3d()
    @JsonIgnore
    private val alignTorque = Vector3d()
    @JsonIgnore
    private val angMomentumShipSpace = Vector3d()
    @JsonIgnore
    private val dampingTorqueShipSpace = Vector3d()
    @JsonIgnore
    private val dampingTorqueWorldSpace = Vector3d()
    @JsonIgnore
    private val angVelShipSpace = Vector3d()
    @JsonIgnore
    private val horizontalVelocity = Vector3d()

    override fun physTick(
        physShip: PhysShip,
        physLevel: PhysLevel
    ) {
        val active = forcefulBalloons
        if (active.isEmpty()) return
        val shipToWorld: Matrix4dc = physShip.transform.shipToWorld

        accumulatedForce.zero()
        accumulatedTorque.zero()

        for (balloonData in active) {
            calculateForcesForBalloon(shipToWorld, physShip, physLevel, balloonData)
        }

        shipUpWorld.set(0.0, 1.0, 0.0)
        shipToWorld.transformDirection(shipUpWorld)
        shipUpWorld.normalize()

        shipUpWorld.cross(upWorld, alignAxis) // axis direction and magnitude ~ sin(angle)
        val alignMag = alignAxis.length()

        if (alignMag > 1e-6) {
            alignAxis.normalize()
            alignTorque.set(alignAxis).mul(ClockworkConfig.SERVER.balloons.balloonAlignmentKp * alignMag)
            accumulatedTorque.add(alignTorque)
        }

        val physShipImpl = physShip as PhysShipImpl
        val angVel = physShipImpl.angularVelocity

        if (angVel.lengthSquared() > 1e-9) {
            val worldToShip: Matrix4dc = physShip.transform.worldToShip
            worldToShip.transformDirection(angVel, angVelShipSpace)
            val momentOfInertia: Matrix3dc = physShipImpl.momentOfInertia
            momentOfInertia.transform(angVelShipSpace, angMomentumShipSpace)
            dampingTorqueShipSpace.set(angMomentumShipSpace).mul(-ClockworkConfig.SERVER.balloons.balloonAngularDamping)
            dampingTorqueShipSpace.y *= 0.2 // Dampen the dampening to make rotation along Y axis actually possible
            shipToWorld.transformDirection(dampingTorqueShipSpace, dampingTorqueWorldSpace)
            accumulatedTorque.add(dampingTorqueWorldSpace)
        }

        val linearVel: Vector3dc = physShipImpl.velocity

        if (linearVel.lengthSquared() > epsilon * epsilon) {
            var totalBalloonVolume = 0.0
            for (balloonData in active) {
                if (balloonData.hotAir > epsilon) {
                    totalBalloonVolume += balloonData.volume
                }
            }

            if (totalBalloonVolume > epsilon) {
                val approxSurfaceArea = totalBalloonVolume.pow(2.0 / 3.0)

                val verticalVelocity = linearVel.y()
                if (abs(verticalVelocity) > epsilon) {
                    val dragForceY = -verticalVelocity * approxSurfaceArea * ClockworkConfig.SERVER.balloons.balloonVerticalDragCoefficient
                    accumulatedForce.add(0.0, dragForceY, 0.0)
                }

                horizontalVelocity.set(linearVel.x(), 0.0, linearVel.z())
                if (horizontalVelocity.lengthSquared() > epsilon * epsilon) {
                    horizontalVelocity.mul(-approxSurfaceArea * ClockworkConfig.SERVER.balloons.balloonHorizontalDragCoefficient)
                    accumulatedForce.add(horizontalVelocity)
                }
            }
        }

        if (accumulatedForce.lengthSquared() > 1e-9) {
            physShip.applyWorldForce(accumulatedForce, physShip.kinematics.position)
        }
        if (accumulatedTorque.lengthSquared() > 1e-9) {
            physShip.applyWorldTorque(accumulatedTorque)
        }
    }

    private fun calculateForcesForBalloon(
        shipToWorld: Matrix4dc,
        physShip: PhysShip,
        physLevel: PhysLevel,
        balloonData: PhysBalloonData
    ) {
        val shipCOMInShipSpace = physShip.transform.positionInShip
        shipToWorld.transformPosition(shipCOMInShipSpace.x(), shipCOMInShipSpace.y(), shipCOMInShipSpace.z(), shipCOMWorld)
        shipToWorld.transformPosition(balloonData.center.x(), balloonData.center.y(), balloonData.center.z(), balloonWorldPos)

        // hotAir is a mass deficit in kg.
        var forceMagnitude = balloonData.hotAir * gravity(physLevel)
        forceMagnitude = max(0.0, forceMagnitude * ClockworkConfig.SERVER.balloons.balloonForceMult)

        tmpForce.set(upWorld).mul(forceMagnitude)

        accumulatedForce.add(tmpForce)
        leverArmWorld.set(balloonWorldPos).sub(shipCOMWorld)
        leverArmWorld.cross(tmpForce, tmpForce) // tmpForce is reused to hold torque here
        accumulatedTorque.add(tmpForce)
    }

    private fun gravity(physLevel: PhysLevel): Double {
        return physLevel.aerodynamicUtils.getAtmosphereForDimension(physLevel.dimension).third
    }

    private fun indexedAt(pos: BlockPos): Set<Int> {
        if (indexDirty) {
            sections.clear()
            for ((id, balloon) in balloons) {
                if (balloon.shouldRemove) continue
                for (box in balloon.regions) {
                    for (x in ((box.minX() - 1) shr 4)..(box.maxX() shr 4))
                        for (y in ((box.minY() - 1) shr 4)..(box.maxY() shr 4))
                            for (z in ((box.minZ() - 1) shr 4)..(box.maxZ() shr 4)) {
                                sections.getOrPut(SectionPos.asLong(x, y, z)) { HashSet() }.add(id)
                            }
                }
            }
            indexDirty = false
        }
        return sections[SectionPos.asLong(pos.x shr 4, pos.y shr 4, pos.z shr 4)] ?: emptySet()
    }

    fun onBlockChanged(pos: BlockPos) {
        if (indexedAt(pos).any { balloons[it]?.touchesPosition(pos) == true }) pendingChanges.add(pos.immutable())
    }

    internal fun processBlockChanges(level: Level) {
        if (pendingChanges.isEmpty()) return
        val changes = pendingChanges.toList()
        pendingChanges.clear()
        for (pos in changes) {
            if (!level.isLoaded(pos)) {
                pendingChanges.add(pos)
                continue
            }
            val affected = indexedAt(pos).mapNotNull { id -> balloons[id]?.takeUnless { it.shouldRemove }?.let { id to it } }
                .filter { it.second.touchesPosition(pos) }
            val solid = level.getBlockState(pos).isValidBalloonEnclosure(level, pos)
            if (!solid) {
                val connected = affected.filter { (_, balloon) ->
                    balloon.containsPosition(pos) || Direction.values().any { balloon.containsPosition(pos.relative(it)) }
                }
                if (connected.size > 1) {
                    val base = connected.first().second
                    for ((_, other) in connected.drop(1)) {
                        base.mergeWith(other, level)
                        other.shouldRemove = true
                    }
                    val cells = HashSet(base.geometry.cells).apply { add(pos) }
                    base.updateRegionsNoValidation(BalloonGeometry.regions(cells), level)
                    base.shouldReScan = true
                    indexDirty = true
                }
            }
            for ((_, balloon) in affected) {
                if (balloon.shouldRemove) continue
                balloon.markBoundaryDirty(pos)
                if ((solid && balloon.containsPosition(pos)) || (!solid && pos in balloon.geometry.boundary)) {
                    balloon.shouldReScan = true
                }
            }
        }
    }

    fun gameTick(level: ServerLevel, ship: LoadedServerShip) {
        if (level.dimensionId != ship.chunkClaimDimension) return
        removeInvalidBalloons()
        processBlockChanges(level)
        for ((_, balloon) in balloons.entries.toList()) {
            if (balloon.shouldRemove) continue
            if (balloon.validationRetryTicks > 0) {
                balloon.validationRetryTicks--
                continue
            }
            if (!balloon.chunksReady(level)) continue
            if (balloon.shouldReScan) {
                val unloaded = balloon.geometry.cells.firstOrNull { !level.isLoaded(it) }
                if (unloaded != null) {
                    balloon.waitForChunk(unloaded)
                    continue
                }
                val (changed, additional) = balloon.trySplit(level)
                if (changed) indexDirty = true
                additional.forEach {
                    it.shouldReScan = true
                    addBalloon(it)
                }
                if (balloon.shouldRemove) continue
                if (!rescan(balloon, level)) continue
                balloon.shouldReScan = false
            }
            if (balloon.shouldValidate) {
                balloon.shouldRemove = balloon.validate(level) == BalloonData.EnclosureStatus.INVALID
            }
            if (balloon.currentVolume <= 0.0) balloon.shouldRemove = true
        }
        val forces = ArrayList<PhysBalloonData>()
        for ((_, balloon) in balloons) {
            if (balloon.shouldRemove || balloon.shouldValidate || balloon.shouldReScan || balloon.enclosureStatus == BalloonData.EnclosureStatus.UNKNOWN) continue
            if (balloon.tick(level, ship)) {
                if (balloon.isLeaking && balloon.isNearlyAtmospheric(level)) {
                    balloon.shouldRemove = true
                } else {
                    val data = balloon.makeForceData(level, ship)
                    if (data.hotAir.isFinite() && data.hotAir > epsilon && data.volume.isFinite() && data.volume > epsilon) forces.add(data)
                }
            }
        }
        removeInvalidBalloons()
        // Publish one complete physics snapshot.
        forcefulBalloons = forces.toList()
    }

    private fun removeInvalidBalloons() {
        for ((id, balloon) in balloons) if (balloon.shouldRemove && balloons.remove(id, balloon)) indexDirty = true
    }

    private fun rescan(balloon: BalloonData, level: Level): Boolean {
        val seed = balloon.interiorSeed(level) ?: return true
        val start = balloon.getFirstValidExternalPosition(level) ?: return true
        var unavailable = false
        val onUnloaded: (BlockPos) -> Unit = { pos -> unavailable = true; balloon.waitForChunk(pos) }
        val shell = scanShell(start, level, ClockworkConfig.SERVER.balloons.hotAirBalloonMaxScanSurface.toInt(), onUnloaded) ?: return !unavailable
        val filled = tryFillBalloonFromShell(shell, seed, level, onUnloaded)
        if (filled.isEmpty()) return !unavailable
        val geometry = BalloonGeometry(filled)
        // Preserve the envelope while leaking.
        if (!geometry.cells.containsAll(balloon.geometry.cells)) return true
        val candidateIds = HashSet<Int>()
        val visitedSections = HashSet<Long>()
        for (pos in geometry.cells) {
            val section = SectionPos.asLong(pos.x shr 4, pos.y shr 4, pos.z shr 4)
            if (visitedSections.add(section)) candidateIds.addAll(indexedAt(pos))
        }
        val others = candidateIds.mapNotNull { balloons[it] }.filter { other ->
            other !== balloon && !other.shouldRemove && geometry.cells.any { other.containsPosition(it) }
        }
        if (others.any { !geometry.cells.containsAll(it.geometry.cells) }) return true
        for (other in others) {
            balloon.mergeWith(other, level)
            other.shouldRemove = true
            indexDirty = true
        }
        if (geometry.cells != balloon.geometry.cells) {
            balloon.updateRegionsNoValidation(filled, level)
            indexDirty = true
        }
        return true
    }

    fun getExistingBalloon(pos: BlockPos): Int = indexedAt(pos).firstOrNull {
        balloons[it]?.let { balloon -> !balloon.shouldRemove && balloon.containsPosition(pos) } == true
    } ?: -1

    fun getBalloonById(id: Int): BalloonData? = balloons[id]?.takeUnless { it.shouldRemove }

    fun addBalloon(balloonData: BalloonData) {
        balloons[nextBalloonID] = balloonData
        indexDirty = true
    }

    fun tryGetOrCreateBalloon(startPos: BlockPos, level: Level): Int {
        val result = level.clip(
            ClipContext(
                startPos.center.add(0.0, 0.5, 0.0),
                startPos.center.add(0.0, ClockworkConfig.SERVER.balloons.hotAirBalloonMaxRaycastDistance, 0.0),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                null
            )
        )
        if (result.type != HitResult.Type.BLOCK) {
            return -1
        }
        val hitPos = result.blockPos ?: return -1
        if (hitPos.distManhattan(startPos) < 2) {
            return -1
        }

        val shellStart = hitPos // ray hit should be shell
        val existingBalloonID = getExistingBalloon(shellStart.relative(Direction.DOWN))
        if (existingBalloonID != -1) return existingBalloonID

        val shell = scanShell(shellStart, level, ClockworkConfig.SERVER.balloons.hotAirBalloonMaxScanSurface.toInt())
            ?: return -1

        val seed = shellStart.relative(Direction.DOWN)
        val filled = tryFillBalloonFromShell(shell, seed, level)
        if (filled.isEmpty()) {
            return -1
        }
        val newBalloonID = nextBalloonID
        val newBalloon = BalloonData(
            regions = ArrayList(filled),
            gasMasses = hashMapOf(),
            currentEnergy = 0.0,
            currentVolume = 0.0,
            isLeaking = false
        )
        newBalloon.recalculateVolume()
        if (newBalloon.validate(level) != BalloonData.EnclosureStatus.VALID) return -1
        addBalloon(newBalloon)
        return newBalloonID
    }

    fun scanShell(startShell: BlockPos, level: Level, maxShellBlocks: Int, onUnloaded: (BlockPos) -> Unit = {}): ShellInfo? {
        if (maxShellBlocks < 1) return null
        if (!level.isLoaded(startShell)) { onUnloaded(startShell); return null }
        if (!level.getBlockState(startShell).isValidBalloonEnclosure(level, startShell)) return null
        val seen = HashSet<BlockPos>()
        val queue = ArrayDeque<BlockPos>()
        queue.add(startShell)
        seen.add(startShell)
        var minY = startShell.y
        var maxY = startShell.y
        var minX = startShell.x
        var maxX = startShell.x
        var minZ = startShell.z
        var maxZ = startShell.z
        var top = startShell
        while (queue.isNotEmpty()) {
            val pos = queue.removeFirst()
            minX = minOf(minX, pos.x); maxX = maxOf(maxX, pos.x)
            minZ = minOf(minZ, pos.z); maxZ = maxOf(maxZ, pos.z)
            minY = minOf(minY, pos.y)
            if (pos.y > maxY) { maxY = pos.y; top = pos }
            for (dx in -1..1) for (dy in -1..1) for (dz in -1..1) {
                if (dx == 0 && dy == 0 && dz == 0) continue
                val next = pos.offset(dx, dy, dz)
                if (next in seen) continue
                if (!level.isLoaded(next)) { onUnloaded(next); return null }
                if (!level.getBlockState(next).isValidBalloonEnclosure(level, next)) continue
                if (seen.size >= maxShellBlocks) return null
                seen.add(next)
                queue.add(next)
            }
        }
        return ShellInfo(minY, maxY, top, minX, maxX, minZ, maxZ)
    }

    fun tryFillBalloonFromShell(shell: ShellInfo, seed: BlockPos, level: Level, onUnloaded: (BlockPos) -> Unit = {}): List<AABBic> =
        BalloonGeometry.fill(shell, seed, level, ClockworkConfig.SERVER.balloons.hotAirBalloonMaxScanVolume.toInt(), onUnloaded)

    data class ShellInfo(
        val minY: Int,
        val maxY: Int,
        val topShellPos: BlockPos,
        val minX: Int, val maxX: Int,
        val minZ: Int, val maxZ: Int
    )

    companion object {
        fun getOrCreate(ship: LoadedServerShip): BalloonController {
            val existing = ship.getAttachment(BalloonController::class.java)
            if (existing != null) {
                return existing
            }
            val controller = BalloonController()
            ship.setAttachment(controller)
            return controller
        }

        @JvmStatic
        fun BlockState.isValidBalloonEnclosure(level: Level, pos: BlockPos): Boolean {
            return !this.isAir && this.isCollisionShapeFullBlock(level, pos)
        }

        @JvmStatic
        fun BlockState.isValidBalloonEnclosureDirectional(level: Level, pos: BlockPos, direction: Direction): Boolean {
            return !this.isAir && this.isFaceSturdy(level, pos, direction.opposite)
        }
    }
}
