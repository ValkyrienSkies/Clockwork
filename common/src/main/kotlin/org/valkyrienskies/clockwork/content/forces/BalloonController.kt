package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Vec3i
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.ai.targeting.TargetingConditions
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.HitResult
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3f
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandItem.Companion.toAABBic
import org.valkyrienskies.clockwork.content.forces.data.BalloonData
import org.valkyrienskies.clockwork.content.forces.data.BalloonData.PhysBalloonData
import org.valkyrienskies.clockwork.util.AABBHelper.mergeAdjacentFast
import org.valkyrienskies.clockwork.util.AerodynamicUtils.dimensionMap
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.attachment.getAttachment
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ShipPhysicsListener
import org.valkyrienskies.core.api.util.PhysTickOnly
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.util.pollUntilEmpty
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

@OptIn(PhysTickOnly::class, VsBeta::class)
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class BalloonController: ShipPhysicsListener {

    val balloons: ConcurrentHashMap<Int, BalloonData> = ConcurrentHashMap()
    @JsonIgnore
    val forcefulBalloons: ConcurrentHashMap<Int, PhysBalloonData> = ConcurrentHashMap()

    val nextBalloonID: Int
        get() = (balloons.keys.maxOrNull() ?: 0) + 1

    override fun physTick(
        physShip: PhysShip,
        physLevel: PhysLevel
    ) {
        for ((_, balloonData) in forcefulBalloons) {
            val buoyancyForce = calculateBuoyancyForce(physShip, physLevel, balloonData)
            if (buoyancyForce > 0.0) {
                val forceVector: Vector3dc = Vector3d(0.0, buoyancyForce, 0.0)
                physShip.applyWorldForceToModelPos(forceVector, balloonData.center)
            }
        }
    }

    fun calculateBuoyancyForce(physShip: PhysShip, physLevel: PhysLevel, physBalloon: PhysBalloonData): Double {
        val (_, _, gravity) = physLevel.aerodynamicUtils.getAtmosphereForDimension(physLevel.dimension)
        val yHeight = physShip.transform.shipToWorld.transformPosition(physBalloon.center, Vector3d()).y()
        val atmoDensity = physLevel.aerodynamicUtils.getAirDensityForY(yHeight, physLevel.dimension)

        val buoyantForce = physBalloon.volume * (atmoDensity - physBalloon.internalDensity) * gravity * ClockworkConfig.SERVER.balloonForceMult * 100.0
        if (buoyantForce.isInfinite() || buoyantForce.isNaN()) {
            return 0.0
        }
        return max(buoyantForce, 0.0)
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
        val tickableBloons = balloons.filter { !it.value.shouldRemove && !it.value.shouldReScan }
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
            forcefulBalloons[id] = balloon.makeForceData()
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
        val seed = findInteriorSeedFromTop(shell.topShellPos, level) ?: return -1
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
