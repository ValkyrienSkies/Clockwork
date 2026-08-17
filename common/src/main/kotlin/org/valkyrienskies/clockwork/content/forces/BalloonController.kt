package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
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
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandItem.Companion.toAABBic
import org.valkyrienskies.clockwork.content.forces.data.BalloonData
import org.valkyrienskies.clockwork.content.forces.data.BalloonData.PhysBalloonData
import org.valkyrienskies.clockwork.util.AABBHelper.mergeAdjacentFast
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.mod.common.dimensionId
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.pow

/**
 * Inspired by https://github.com/SergeyFeduk/Create-Propulsion/blob/main/src/main/java/com/deltasf/createpropulsion/balloons/hot_air/BalloonAttachment.java
 */
@OptIn(PhysTickOnly::class, VsBeta::class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class BalloonController: ShipPhysicsListener {

    val balloons: ConcurrentHashMap<Int, BalloonData> = ConcurrentHashMap()
    @JsonIgnore
    val forcefulBalloons: ConcurrentHashMap<Int, PhysBalloonData> = ConcurrentHashMap()

    val nextBalloonID: Int
        get() = (balloons.keys.maxOrNull() ?: 0) + 1

    @JsonIgnore
    private val epsilon = 1e-5

    @JsonIgnore
    private val balloonAngularDamping = 1.2

    @JsonIgnore
    private val balloonAlignmentKp = 10.0

    @JsonIgnore
    private val balloonVerticalDragCoefficient = 100.0

    @JsonIgnore
    private val balloonHorizontalDragCoefficient = 80.0

    // Scratch vectors, reused every tick to avoid allocations on the physics thread
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
        val shipToWorld: Matrix4dc = physShip.transform.shipToWorld

        accumulatedForce.zero()
        accumulatedTorque.zero()

        for ((_, balloonData) in forcefulBalloons) {
            val fullness = balloonData.hotAir / balloonData.volume
            if (fullness <= epsilon) continue
            calculateForcesForBalloon(shipToWorld, physShip, physLevel, balloonData, fullness)
        }

        // Angular dampening keeps balloon-borne ships flying upright
        shipUpWorld.set(0.0, 1.0, 0.0)
        shipToWorld.transformDirection(shipUpWorld)
        shipUpWorld.normalize()

        shipUpWorld.cross(upWorld, alignAxis) // axis direction and magnitude ~ sin(angle)
        val alignMag = alignAxis.length()

        // P torque dampening
        if (alignMag > 1e-6) {
            alignAxis.normalize()
            alignTorque.set(alignAxis).mul(balloonAlignmentKp * alignMag)
            accumulatedTorque.add(alignTorque)
        }

        // D torque dampening
        val physShipImpl = physShip as PhysShipImpl
        val angVel = physShipImpl.angularVelocity

        if (angVel.lengthSquared() > 1e-9) {
            val worldToShip: Matrix4dc = physShip.transform.worldToShip
            worldToShip.transformDirection(angVel, angVelShipSpace)
            val momentOfInertia: Matrix3dc = physShipImpl.momentOfInertia
            momentOfInertia.transform(angVelShipSpace, angMomentumShipSpace)
            dampingTorqueShipSpace.set(angMomentumShipSpace).mul(-balloonAngularDamping)
            dampingTorqueShipSpace.y *= 0.2 // Dampen the dampening to make rotation along Y axis actually possible
            shipToWorld.transformDirection(dampingTorqueShipSpace, dampingTorqueWorldSpace)
            accumulatedTorque.add(dampingTorqueWorldSpace)
        }

        // Vertical/horizontal linear drag based on surface area of all balloons
        val linearVel: Vector3dc = physShipImpl.velocity

        if (linearVel.lengthSquared() > epsilon * epsilon) {
            var totalBalloonVolume = 0.0
            for ((_, balloonData) in forcefulBalloons) {
                if (balloonData.hotAir > epsilon) {
                    totalBalloonVolume += balloonData.volume
                }
            }

            if (totalBalloonVolume > epsilon) {
                val approxSurfaceArea = totalBalloonVolume.pow(2.0 / 3.0)

                // Vertical drag
                val verticalVelocity = linearVel.y()
                if (abs(verticalVelocity) > epsilon) {
                    val dragForceY = -verticalVelocity * approxSurfaceArea * balloonVerticalDragCoefficient
                    accumulatedForce.add(0.0, dragForceY, 0.0)
                }

                // Horizontal drag
                horizontalVelocity.set(linearVel.x(), 0.0, linearVel.z())
                if (horizontalVelocity.lengthSquared() > epsilon * epsilon) {
                    horizontalVelocity.mul(-approxSurfaceArea * balloonHorizontalDragCoefficient)
                    accumulatedForce.add(horizontalVelocity)
                }
            }
        }

        // Apply aggregated force and torque
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
        balloonData: PhysBalloonData,
        fullness: Double
    ) {
        val shipCOMInShipSpace = physShip.transform.positionInShip
        shipToWorld.transformPosition(shipCOMInShipSpace.x(), shipCOMInShipSpace.y(), shipCOMInShipSpace.z(), shipCOMWorld)
        shipToWorld.transformPosition(balloonData.center.x(), balloonData.center.y(), balloonData.center.z(), balloonWorldPos)

        // Calculate force magnitude
        val externalDensity = calculateVariableExternalAirDensity(physLevel, balloonWorldPos.y(), physLevel.dimension)
        var forceMagnitude = balloonData.volume * externalDensity * gravity(physLevel) * fullness
        forceMagnitude = max(0.0, forceMagnitude * ClockworkConfig.SERVER.balloonForceMult)

        // Calculate force vector
        tmpForce.set(upWorld).mul(forceMagnitude)

        // Aggregate force and torque
        accumulatedForce.add(tmpForce)
        leverArmWorld.set(balloonWorldPos).sub(shipCOMWorld)
        leverArmWorld.cross(tmpForce, tmpForce) // tmpForce is reused to hold torque here
        accumulatedTorque.add(tmpForce)
    }

    private fun calculateVariableExternalAirDensity(physLevel: PhysLevel, y: Double, id: DimensionId): Double {
        return physLevel.aerodynamicUtils.getAirDensityForY(y, id )
    }

    private fun gravity(physLevel: PhysLevel): Double {
        return physLevel.aerodynamicUtils.getAtmosphereForDimension(physLevel.dimension).third
    }

    fun gameTick(
        level: ServerLevel,
        ship: LoadedServerShip
    ) {
        if (level.dimensionId != ship.chunkClaimDimension) return
        // Clean up balloons that should be removed
        val toRemove = mutableListOf<Int>()
        for ((id, balloon) in balloons) {
            if (balloon.shouldRemove) {
                toRemove.add(id)
            }
        }
        for (id in toRemove) {
            balloons.remove(id)
        }
        for ((id, balloon) in balloons) {
            if (balloon.shouldValidate) {
                val status = balloon.validate(level)
                balloon.shouldValidate = false
//                if (status == BalloonData.EnclosureStatus.INVALID) {
//                    balloon.shouldReScan = true
//                }
            }
            if (balloon.shouldReScan) {
                val validScanStart = balloon.getFirstValidExternalPosition(level)
                if (validScanStart == null) {
                    // Balloon is no longer valid
                    balloon.shouldReScan = false
                    balloon.shouldRemove = true
                    continue
                }
                val shell = scanShell(
                    validScanStart,
                    level,
                    ClockworkConfig.SERVER.hotAirBalloonMaxScanSurface.toInt()
                )
                if (shell == null) {
                    // Balloon is no longer valid
                    balloon.shouldReScan = false
                    balloon.shouldRemove = true
                    continue
                }
                val seed = findInteriorSeedFromTop(shell.topShellPos, level)
                if (seed == null) {
                    // Balloon is no longer valid
                    balloon.shouldReScan = false
                    balloon.shouldRemove = true
                    continue
                }
                val newRegions = tryFillBalloonFromShell(
                    shell,
                    seed,
                    level
                )
                if (newRegions.isNotEmpty()) {
                    balloon.updateRegionsNoValidation(newRegions, level)
                    balloon.shouldReScan = false
                } else {
                    // Balloon is no longer valid
                    balloon.shouldReScan = false
                    if (balloon.isNearlyAtmospheric(level)) {
                        balloon.shouldRemove = true
                    } else {
                        balloon.shouldRemove = balloon.validate(level) == BalloonData.EnclosureStatus.INVALID
                    }
                }
            }
            if (balloon.isLeaking && balloon.isNearlyAtmospheric(level)) {
                balloon.shouldRemove = true
            }
            if (balloon.regions.isEmpty || balloon.currentVolume <= 0.0) {
                balloon.shouldRemove = true
            }
        }
        val tickableBloons = balloons.filter { !it.value.shouldRemove }
        val shouldApplyForces = ArrayList<Int>()
        for ((id, balloon) in tickableBloons) {
            val result = balloon.tick(level, ship)
            if (result) {
                shouldApplyForces.add(id)
            }
        }

        forcefulBalloons.clear()
        for (id in shouldApplyForces) {
            val balloon = balloons[id] ?: continue
            forcefulBalloons[id] = balloon.makeForceData(level, ship)
        }
    }

    fun getExistingBalloon(pos: BlockPos): Int {
        for ((id, balloon) in balloons) {
            if (balloon.containsPosition(pos)) {
                return id
            }
        }
        return -1
    }

    fun getBalloonById(id: Int): BalloonData? {
        return balloons[id]
    }

    fun addBalloon(balloonData: BalloonData) {
        balloons[nextBalloonID] = balloonData
    }

    fun tryGetOrCreateBalloon(startPos: BlockPos, level: Level): Int {
        val result = level.clip(
            ClipContext(
                startPos.center.add(0.0, 0.5, 0.0),
                startPos.center.add(0.0, ClockworkConfig.SERVER.hotAirBalloonMaxRaycastDistance, 0.0),
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

        val shell = scanShell(shellStart, level, ClockworkConfig.SERVER.hotAirBalloonMaxScanSurface.toInt())
            ?: return -1

        //Finding valid position inside the balloon
        val seed = shellStart.relative(Direction.DOWN)//findInteriorSeedFromTop(shell.topShellPos, level) ?: return -1
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
        balloons[newBalloonID] = newBalloon
        newBalloon.recalculateVolume()
        return newBalloonID
    }

    fun scanShell(startShell: BlockPos, level: Level, maxShellBlocks: Int): ShellInfo? {
        if (!level.getBlockState(startShell).isValidBalloonEnclosure(level, startShell)) return null

        val visited = HashSet<Long>(4096)
        val q = ArrayDeque<Long>()
        q.add(startShell.asLong())

        var minY = startShell.y
        var maxY = startShell.y
        var topPos = startShell

        var minX = startShell.x
        var maxX = startShell.x
        var minZ = startShell.z
        var maxZ = startShell.z

        while (q.isNotEmpty() && visited.size < maxShellBlocks) {
            val curL = q.removeFirst()
            if (!visited.add(curL)) continue
            val cur = BlockPos.of(curL)

            val y = cur.y
            if (y < minY) minY = y
            if (y > maxY) { maxY = y; topPos = cur }

            val x = cur.x
            val z = cur.z
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (z < minZ) minZ = z
            if (z > maxZ) maxZ = z

            //also scan diagonal edges
            for (dir in Direction.values()) {
                val n = cur.relative(dir)
                for (diag in Direction.values()) {
                    if (dir.axis == diag.axis) {
                        continue
                    }
                    val nd = n.relative(diag)
                    for (corner in Direction.values()) {
                        if (corner.axis == diag.axis || corner.axis == dir.axis) {
                            continue
                        }
                        val nc = nd.relative(corner)
                        if (!level.getBlockState(nc).isValidBalloonEnclosure(level, nc)) continue
                        val nlc = nc.asLong()
                        if (!visited.contains(nlc)) {
                            q.add(nlc)
                        }
                    }
                    if (!level.getBlockState(nd).isValidBalloonEnclosure(level, nd)) continue
                    val ndl = nd.asLong()
                    if (!visited.contains(ndl)) q.add(ndl)
                }
                if (!level.getBlockState(n).isValidBalloonEnclosure(level, n)) continue
                val nl = n.asLong()
                if (!visited.contains(nl)) q.add(nl)
            }
            // If we hit the cap, treat as failure (prevents scanning half a world if something is weird)
            if (visited.size >= maxShellBlocks) return null
        }



        return ShellInfo(minY, maxY, topPos, minX, maxX, minZ, maxZ)
    }

    fun findInteriorSeedFromTop(shellTop: BlockPos, level: Level, maxStepsDown: Int = 64): BlockPos? {
        var p = shellTop.below()
        var steps = 0
        while (steps++ < maxStepsDown) {
            if (!level.getBlockState(p).isValidBalloonEnclosure(level, p)) return p
            p = p.below()
        }
        return null
    }

    fun tryFillBalloonFromShell(shell: ShellInfo, seed: BlockPos, level: Level): List<AABBic> {
        val maxScan = ClockworkConfig.SERVER.hotAirBalloonMaxScanVolume

        val minYInterior = shell.minY + 1

        // Slightly expand bounds so you can still touch the inside adjacent to the shell.
        val minX = shell.minX - 1
        val maxX = shell.maxX + 1
        val minZ = shell.minZ - 1
        val maxZ = shell.maxZ + 1

        val visited = HashSet<Long>(maxScan.toInt() * 2)
        val q = ArrayDeque<Long>()
        q.add(seed.asLong())

        val toFill = ArrayList<AABBic>(minOf(maxScan.toInt(), 4096))

        while (q.isNotEmpty() && visited.size < maxScan.toInt()) {
            val curL = q.removeFirst()
            if (!visited.add(curL)) continue
            val cur = BlockPos.of(curL)

            // Bounds + open-bottom cut
            if (cur.y <= minYInterior) continue
            val state = level.getBlockState(cur)
            if (state.isValidBalloonEnclosure(level, cur)) continue
            if (cur.x !in minX..maxX || cur.z !in minZ..maxZ) {
//                if (level is ServerLevel) {
//                    val player = (level as ServerLevel).getNearestPlayer(seed.x.toDouble(), seed.y.toDouble(), seed.z.toDouble(), 256.0, false)
//                    //todo lang
//                    level.sendParticles(
//                        ParticleTypes.LARGE_SMOKE,
//                        seed.x + 0.5,
//                        seed.y + 0.5,
//                        seed.z + 0.5,
//                        20,
//                        0.3,
//                        0.3,
//                        0.3,
//                        0.0
//                    )
//                    player?.displayClientMessage(Component.literal("Invalid position at ${cur}"), true)
//                    level.sendParticles(
//                        ParticleTypes.LARGE_SMOKE,
//                        cur.x + 0.5,
//                        cur.y + 0.5,
//                        cur.z + 0.5,
//                        20,
//                        0.3,
//                        0.3,
//                        0.3,
//                        0.0
//                    )
//                }
                return emptyList()
            }

            toFill.add(cur.toAABBic())

            for (dir in Direction.values()) {
                val n = cur.relative(dir)
                if (level.getBlockState(n).isValidBalloonEnclosure(level, n)) continue
                val nl = n.asLong()
                if (!visited.contains(nl)) q.add(nl)
            }
        }

        return mergeAdjacentFast(toFill)
    }


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
            return !this.isAir && !this.isFaceSturdy(level, pos, direction.opposite)
        }
    }
}
