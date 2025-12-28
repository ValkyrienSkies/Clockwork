package org.valkyrienskies.clockwork.content.forces

import com.fasterxml.jackson.annotation.JsonAutoDetect
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.HitResult
import org.joml.Vector3d
import org.joml.Vector3dc
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
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max

@OptIn(PhysTickOnly::class, VsBeta::class)
@JsonAutoDetect
class BalloonController: ShipPhysicsListener {

    val balloons: ConcurrentHashMap<Int, BalloonData> = ConcurrentHashMap()
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

        val buoyantForce = physBalloon.volume * (atmoDensity - physBalloon.internalDensity) * gravity * ClockworkConfig.SERVER.balloonForceMult
        if (buoyantForce.isInfinite() || buoyantForce.isNaN()) {
            return 0.0
        }
        return max(buoyantForce, 0.0)
    }

    fun gameTick(
        level: ServerLevel,
        ship: LoadedServerShip
    ) {
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
            if (balloon.shouldReScan) {
                val newRegions = tryFillBalloon(
                    BlockPos(
                        balloon.regions[0].minX().toInt(),
                        balloon.regions[0].minY().toInt(),
                        balloon.regions[0].minZ().toInt()
                    ),
                    level
                )
                if (newRegions.isNotEmpty()) {
                    balloon.updateRegionsNoValidation(newRegions, level)
                    balloon.shouldReScan = false
                } else {
                    // Balloon is no longer valid
                    balloon.shouldReScan = false
                    balloon.shouldRemove = true
                }
            }
            if (balloon.isLeaking && !balloon.canLeakGassesOnly()) {
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
        val hitPos = result.blockPos!!
        if (hitPos.distManhattan(startPos) < 2) {
            return -1
        }
        val existingBalloonID = getExistingBalloon(hitPos.relative(Direction.DOWN))
        if (existingBalloonID != -1) {
            return existingBalloonID
        }

        //crawl to top of bloon so that we can be sure to start floodfill from roof level
        val currentPos = crawlToHighest(hitPos.relative(Direction.DOWN), level)

        val filled = tryFillBalloon(currentPos, level)
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
        return newBalloonID
    }

    fun crawlToHighest(pos: BlockPos, level: Level): BlockPos {
        var currentPos = pos
        val visited = HashSet<BlockPos>()
        val queue = ArrayDeque<BlockPos>()
        queue.add(currentPos)
        while (queue.isNotEmpty()) {
            val p = queue.removeFirst()
            if (visited.contains(p)) {
                continue
            }
            visited.add(p)
            val blockState = level.getBlockState(p)
            if (blockState.isValidBalloonEnclosure(level, p)) {
                continue
            }
            currentPos = p
            for (dir in Direction.values()) {
                if (dir == Direction.DOWN) {
                    continue
                }
                val relativePos = p.relative(dir)
                if (level.getBlockState(relativePos).isValidBalloonEnclosure(level, relativePos)) {
                    continue
                }
                if (!visited.contains(relativePos)) {
                    queue.add(relativePos)
                }
            }
        }
        queue.sortByDescending { p -> p.y }
        return queue.first()
    }

    /**
     * Attempts to use floodfill to determine a balloon's inside area starting from [pos], where pos is a blockpos at roof level.
     */
    fun tryFillBalloon(pos: BlockPos, level: Level): List<AABBic> {
        val maxScan = ClockworkConfig.SERVER.hotAirBalloonMaxScanVolume

        val toFill = ArrayList<AABBic>()
        val visited = HashSet<BlockPos>()
        val queue = ArrayDeque<BlockPos>()
        queue.add(pos)
        while (queue.isNotEmpty() && visited.size < maxScan) {
            val currentPos = queue.removeFirst()
            if (visited.contains(currentPos)) {
                continue
            }
            visited.add(currentPos)
            val blockState = level.getBlockState(currentPos)
            if (blockState.isValidBalloonEnclosure(level, currentPos)) {
                continue
            }
            val aabb = currentPos.toAABBic()
            toFill.add(aabb)
            for (dir in Direction.values()) {
                if (dir == Direction.UP) {
                    continue
                }
                val neighborPos = currentPos.relative(dir)
                if (!visited.contains(neighborPos)) {
                    queue.add(neighborPos)
                }
            }
        }
        return mergeAdjacentFast(toFill)
    }

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
            return !this.isAir && !this.getCollisionShape(level, pos).isEmpty
        }
    }
}
