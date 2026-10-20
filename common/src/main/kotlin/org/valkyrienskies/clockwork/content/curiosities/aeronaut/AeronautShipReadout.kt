package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.Contraption
import com.simibubi.create.content.kinetics.fan.EncasedFanBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.chunk.LevelChunk
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.forces.BalloonController
import org.valkyrienskies.clockwork.content.forces.WanderShipControl
import org.valkyrienskies.core.api.ships.LoadedServerShip
import org.valkyrienskies.core.api.ships.ContraptionWingProvider
import org.valkyrienskies.core.api.ships.Wing
import org.valkyrienskies.mod.common.block.WingBlock
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD
import java.util.WeakHashMap
import kotlin.math.PI
import kotlin.math.abs

/** Game-thread cache. Never loads chunks or walks a ship's empty bounding-box volume. */
object AeronautShipReadout {
    data class Snapshot(val mass: Double, val massCenter: Vector3dc,
        val potentialThrust: AeronautForceCenter?, val liveThrust: AeronautForceCenter?,
        val potentialLift: AeronautForceCenter?, val liveLift: AeronautForceCenter?)

    private data class PositionedWing(val position: BlockPos, val wing: Wing)
    private class Cache {
        var dirty = true
        var sampled = Long.MIN_VALUE
        var used = 0L
        var chunkCount = -1
        val chunks = HashMap<ChunkPos, LevelChunk>()
        val sources = ArrayList<BlockEntity>()
        val wings = ArrayList<PositionedWing>()
        var snapshot: Snapshot? = null
    }
    private val levels = WeakHashMap<ServerLevel, MutableMap<Long, Cache>>()
    private val contraptionWings = WeakHashMap<Contraption, List<PositionedWing>>()

    fun invalidate(level: ServerLevel, shipId: Long) { levels[level]?.get(shipId)?.dirty = true }

    @JvmStatic
    fun invalidateContraption(contraption: Contraption?) { if (contraption != null) contraptionWings.remove(contraption) }

    fun clear() {
        levels.clear()
        contraptionWings.clear()
        AeronautForceTelemetry.clear()
    }

    fun sample(level: ServerLevel, ship: LoadedServerShip): Snapshot {
        val caches = levels.getOrPut(level) { HashMap() }
        caches.entries.removeIf { level.gameTime - it.value.used > 200 }
        val cache = caches.getOrPut(ship.id) { Cache() }
        cache.used = level.gameTime
        AeronautForceTelemetry.watch(ship.id)
        if (cache.snapshot != null && level.gameTime - cache.sampled in 0 until AeronautForceReadout.UPDATE_TICKS)
            return cache.snapshot!!

        if (cache.dirty || cache.chunkCount != ship.activeChunksSet.size ||
            cache.chunks.any { (pos, chunk) -> level.chunkSource.getChunkNow(pos.x, pos.z) !== chunk }) rebuild(level, ship, cache)
        val thrust = AeronautForceAccumulator()
        for (source in cache.sources) {
            if (source.isRemoved || !level.hasChunkAt(source.blockPos)) { cache.dirty = true; continue }
            if (source is AeronautForceSource) source.addPotentialForces(level, ship, thrust)
            else if (source is EncasedFanBlockEntity) {
                // Static-air form of EncasedFanController's blade calculation, at the design RPM.
                val lift = 2.0 * PI * Math.toRadians(15.0)
                val force = 0.5 * 1.225 * (AeronautForceReadout.REFERENCE_RPM * 0.5).let { it * it } *
                    0.15 * 0.5 * lift * ClockworkConfig.SERVER.encasedFanForceMul
                thrust.add(source.blockPos.toJOMLD().add(0.5, 0.5, 0.5),
                    source.blockState.getValue(BlockStateProperties.FACING).normal.toJOMLD().mul(force))
            }
        }

        val potentialLift = AeronautForceAccumulator()
        val liveLift = AeronautForceAccumulator()
        for (wing in cache.wings) addWing(level, ship, wing.position.toJOMLD().add(0.5, 0.5, 0.5),
            wing.wing.wingNormal, wing.wing, potentialLift, liveLift)

        // Flaps and other assembled Create contraptions have left the chunk's block palette.
        ship.shipAABB?.let { box ->
            val bounds = AABB(box.minX().toDouble(), box.minY().toDouble(), box.minZ().toDouble(),
                box.maxX() + 1.0, box.maxY() + 1.0, box.maxZ() + 1.0).inflate(16.0)
            for (entity in level.getEntitiesOfClass(AbstractContraptionEntity::class.java, bounds)) {
                if (level.getLoadedShipManagingPos(entity.blockPosition())?.id != ship.id) continue
                val provider = entity as? ContraptionWingProvider ?: continue
                if (provider.wingGroupId < 0) continue
                val contraption = entity.contraption ?: continue
                val wings = contraptionWings.getOrPut(contraption) {
                    contraption.blocks.values.mapNotNull { info ->
                        val block = info.state.block as? WingBlock ?: return@mapNotNull null
                        val wing = block.getWing(level, info.pos, info.state) ?: return@mapNotNull null
                        PositionedWing(info.pos, wing)
                    }
                }
                val transform = provider.computeContraptionWingTransform()
                for (wing in wings) {
                    val local = Vec3.atCenterOf(wing.position)
                    val movement = entity.toGlobalVector(local, 1f).subtract(entity.toGlobalVector(local, 0f, true)).scale(20.0)
                    addWing(level, ship, transform.transformPosition(wing.position.toJOMLD().add(0.5, 0.5, 0.5)),
                        transform.transformDirection(wing.wing.wingNormal, Vector3d()).normalize(),
                        wing.wing, potentialLift, liveLift, Vector3d(movement.x, movement.y, movement.z))
                }
            }
        }

        val up = ship.transform.shipToWorldRotation.transformInverse(Vector3d(0.0, 1.0, 0.0))
        val gravity = level.shipObjectWorld.aerodynamicUtils.getAtmosphereForDimension(level.dimensionId).third
        ship.getAttachment(BalloonController::class.java)?.let { controller ->
            for (balloon in controller.balloons.values) {
                if (balloon.shouldRemove || balloon.shouldValidate) continue
                val data = balloon.makeForceData(level, ship)
                val force = Vector3d(up).mul(data.hotAir * gravity * ClockworkConfig.SERVER.balloons.balloonForceMult)
                potentialLift.add(data.center, force)
            }
            for (data in controller.forcefulBalloons) liveLift.add(data.center,
                Vector3d(up).mul(data.hotAir * gravity * ClockworkConfig.SERVER.balloons.balloonForceMult))
        }
        ship.getAttachment(WanderShipControl::class.java)?.let { controller ->
            // Match the controller's mean application point, including unequal block strengths.
            val blocks = controller.wanderBlocks
            if (blocks.isNotEmpty()) {
                val center = Vector3d()
                blocks.keys.forEach { center.add(it.x() + 0.5, it.y() + 0.5, it.z() + 0.5) }
                center.div(blocks.size.toDouble())
                val force = Vector3d(up).mul(blocks.values.sum() * gravity * ClockworkConfig.SERVER.wanderOreForceMultiplier)
                potentialLift.add(center, force)
                liveLift.add(center, force)
            }
        }
        cache.sampled = level.gameTime
        return Snapshot(ship.inertiaData.mass, Vector3d(ship.transform.positionInShip), thrust.center(),
            AeronautForceTelemetry.thrust(ship.id), potentialLift.center(), liveLift.center()).also { cache.snapshot = it }
    }

    private fun rebuild(level: ServerLevel, ship: LoadedServerShip, cache: Cache) {
        cache.sources.clear()
        cache.wings.clear()
        cache.chunks.clear()
        var missingChunks = false
        ship.activeChunksSet.forEach { x, z ->
            val chunk = level.chunkSource.getChunkNow(x, z)
            if (chunk == null) missingChunks = true
            else {
                cache.chunks[chunk.pos] = chunk
                cache.sources.addAll(chunk.blockEntities.values.filter { it is AeronautForceSource || it is EncasedFanBlockEntity })
                for ((index, section) in chunk.sections.withIndex()) {
                    if (section.hasOnlyAir() || !section.maybeHas { it.block is WingBlock }) continue
                    val baseY = level.getSectionYFromSectionIndex(index) shl 4
                    for (dx in 0..15) for (dy in 0..15) for (dz in 0..15) {
                        val state = section.getBlockState(dx, dy, dz)
                        val block = state.block as? WingBlock ?: continue
                        val pos = BlockPos((x shl 4) + dx, baseY + dy, (z shl 4) + dz)
                        val wing = block.getWing(level, pos, state) ?: continue
                        cache.wings.add(PositionedWing(pos, wing))
                    }
                }
            }
        }
        cache.chunkCount = ship.activeChunksSet.size
        cache.dirty = missingChunks
    }

    private fun addWing(level: ServerLevel, ship: LoadedServerShip, position: Vector3dc, normal: Vector3dc,
        wing: Wing, potential: AeronautForceAccumulator, live: AeronautForceAccumulator, movement: Vector3dc = Vector3d()) {
        if (!position.isFinite || !normal.isFinite || normal.lengthSquared() < 1e-12) return
        val worldPosition = ship.transform.shipToWorld.transformPosition(position, Vector3d())
        val worldNormal = ship.transform.shipToWorldRotation.transform(normal, Vector3d()).normalize()
        val atmosphere = level.shipObjectWorld.aerodynamicUtils
        val density = atmosphere.getAirDensityForY(worldPosition.y, level.dimensionId)
        val velocity = ship.angularVelocity.cross(worldPosition.sub(ship.transform.positionInWorld, Vector3d()), Vector3d()).add(ship.velocity)
            .add(ship.transform.shipToWorld.transformDirection(movement, Vector3d()))
        val force = AeronautForceReadout.wingLift(worldNormal, velocity, density, wing.wingLiftPower, wing.wingCamberAttackAngleBias)
        live.add(position, ship.transform.shipToWorldRotation.transformInverse(force, Vector3d()))
        val reference = AeronautForceReadout.wingLift(normal, AeronautForceReadout.referenceFlow(normal),
            1.225, wing.wingLiftPower, Math.toRadians(10.0))
        potential.add(position, reference)
    }
}
