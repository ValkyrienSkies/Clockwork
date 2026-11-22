package org.valkyrienskies.clockwork.content.curiosities.harpoon

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandServer
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandStatePacket
import org.valkyrienskies.core.api.particles.*
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld

/** Owns transient projectiles on the game thread. VS queues contact events from its physics thread. */
@OptIn(GameTickOnly::class)
object HarpoonProjectiles {
    private data class Shot(val level: ServerLevel, val launcher: HarpoonBlockEntity,
                            val source: WandAnchor, val born: Long, var position: Vec3)
    private val shots = linkedMapOf<ParticleId, Shot>()
    private const val PARTICLE_TAG = 0x434C4B4852504EL // "CLKHRPN", reserved for transient harpoons.

    val maxRange: Double
        get() = ClockworkConfig.SERVER.harpoonMaxRange.takeIf { it.isFinite() }?.coerceIn(1.0, 128.0) ?: 128.0

    private val radius: Double
        get() = ClockworkConfig.SERVER.harpoonRadius.takeIf { it.isFinite() }?.coerceIn(0.01, 1.0) ?: 0.125

    private val lifetimeTicks: Int
        get() = ClockworkConfig.SERVER.harpoonLifetimeTicks.coerceIn(1, 12000)

    /** VS velocities are metres/second. Include rotation at the muzzle, not just centre-of-mass motion. */
    fun launchVelocity(direction: Vector3dc, charge: Double, linear: Vector3dc = Vector3d(),
                       angular: Vector3dc = Vector3d(), lever: Vector3dc = Vector3d()): Vector3d {
        val baseSpeed = ClockworkConfig.SERVER.harpoonBaseLaunchSpeed.takeIf { it.isFinite() }?.coerceIn(0.0, 1000.0) ?: 10.0
        val chargeSpeed = ClockworkConfig.SERVER.harpoonChargeLaunchSpeed.takeIf { it.isFinite() }?.coerceIn(0.0, 1000.0) ?: 50.0
        return Vector3d(direction).normalize().mul(baseSpeed + chargeSpeed * charge.coerceIn(0.0, 1.0))
            .add(angular.cross(lever, Vector3d())).add(linear)
    }

    fun hasProjectile(level: ServerLevel, pos: BlockPos) =
        shots.values.any { it.level === level && it.launcher.blockPos == pos }

    fun fire(level: ServerLevel, launcher: HarpoonBlockEntity, charge: Double): Boolean {
        if (!charge.isFinite() || shots.size >= 256 || hasProjectile(level, launcher.blockPos) || launcher.ropeId != null) return false
        val facing = launcher.blockState.getValue(BlockStateProperties.FACING)
        val source = WandAnchor.at(level, launcher.blockPos, facing)
        val origin = source.world(level) ?: return false
        val ship = level.getShipManagingPos(launcher.blockPos)
        val direction = Vector3d(facing.stepX.toDouble(), facing.stepY.toDouble(), facing.stepZ.toDouble())
        ship?.transform?.shipToWorldRotation?.transform(direction)
        val projectileRadius = radius
        // Clear both the collision sphere and the full visible harpoon (13 pixels long).
        // Start from the transformed muzzle so scaled ships retain the same world-space clearance.
        val clearance = maxOf(13.0 / 16.0, projectileRadius + 0.25)
        val position = Vector3d(origin.x, origin.y, origin.z).add(Vector3d(direction).mul(clearance))
        val velocity = launchVelocity(direction, charge,
            ship?.velocity ?: Vector3d(), ship?.angularVelocity ?: Vector3d(),
            ship?.let { Vector3d(position).sub(it.transform.positionInWorld) } ?: Vector3d())
        val particle = level.shipObjectWorld.createParticle(VsParticleCreateData(
            dimensionId = level.dimensionId, position = position, velocity = velocity,
            radius = projectileRadius, mass = 2.0, userTag = PARTICLE_TAG,
            material = ParticleMaterial(restitution = 0.0), eventSettings = ParticleEventSettings(rigidContacts = true)))
        if (particle.status.lifecycle == ParticleLifecycle.FAILED) {
            level.shipObjectWorld.deleteParticle(particle.id)
            return false
        }
        val shot = Shot(level, launcher, source, level.gameTime, Vec3(position.x, position.y, position.z))
        shots[particle.id] = shot
        launcher.projectileLaunched(velocity.length())
        syncFlight(particle.id, shot)
        level.playSound(null, origin.x, origin.y, origin.z, SoundEvents.CROSSBOW_SHOOT, SoundSource.BLOCKS, 1f, 0.7f)
        return true
    }

    fun cancelProjectile(level: ServerLevel, launcher: HarpoonBlockEntity, snap: Boolean = false) {
        shots.filterValues { it.level === level && it.launcher === launcher }.keys.toList().forEach { remove(it, snap) }
    }

    fun detach(level: ServerLevel, launcher: HarpoonBlockEntity) {
        cancelProjectile(level, launcher, snap = true)
        val data = WandLinks.get(level)
        // Check ownership: a copied block's NBT must never remove the original launcher's rope.
        launcher.ropeId?.let { id -> data.links[id]?.takeIf { it.a.pos == launcher.blockPos }?.let { link ->
            data.remove(level, link)
            WanderwandServer.breakEffect(level, link)
            WanderwandServer.syncLinks(level)
        } }
        if (launcher.ropeId != null) {
            launcher.ropeId = null
            launcher.setChanged()
            launcher.sendData()
        }
    }

    fun tick(level: ServerLevel) {
        // Core checkpoints particles. Remove any transient harpoons restored without a live owner.
        if (level.gameTime % 20L == 0L) {
            level.shipObjectWorld.allParticles.getInDimension(level.dimensionId)
                .filter { it.userTag == PARTICLE_TAG && it.id !in shots }
                .forEach { level.shipObjectWorld.deleteParticle(it.id) }
        }
        if (shots.isEmpty()) return
        // This queue belongs to the whole ship world, not one dimension. Route every event to its owner.
        val batch = level.shipObjectWorld.drainParticleEvents()
        // Missing contacts cannot safely be reconstructed from a later particle position.
        if (batch.requiresResynchronization) {
            ClockworkMod.LOGGER.warn("Harpoon particle contact queue overflowed; cancelling in-flight harpoons")
            shots.keys.toList().forEach { remove(it) }
            return
        }
        for (event in batch.events) {
            if (event.kind != ParticleEventKind.CONTACT_START) continue
            val shot = shots[event.particle] ?: continue
            if (event.other !is ParticleContactOther.Body) continue
            // A rejected contact must not consume the projectile before a valid impact arrives.
            if (attach(shot, event)) remove(event.particle)
        }
        for ((id, shot) in shots.toMap()) {
            if (shot.level !== level) continue
            val particle = level.shipObjectWorld.getParticleById(id)
            val origin = shot.source.world(level)
            if (!valid(shot) || particle == null || origin == null ||
                particle.status.lifecycle in listOf(ParticleLifecycle.FAILED, ParticleLifecycle.REMOVED, ParticleLifecycle.SUSPENDED)) {
                remove(id)
                continue
            }
            val p = particle.position
            shot.position = Vec3(p.x(), p.y(), p.z())
            if (level.gameTime - shot.born > lifetimeTicks ||
                p.distance(Vector3d(origin.x, origin.y, origin.z)) > maxRange) {
                remove(id, snap = true)
                continue
            }
            shot.launcher.updateProjectileSpeed(particle.velocity.length())
            syncFlight(id, shot)
        }
    }

    private fun valid(shot: Shot): Boolean = !shot.launcher.isRemoved &&
        shot.level.hasChunkAt(shot.launcher.blockPos) &&
        shot.level.getBlockEntity(shot.launcher.blockPos) === shot.launcher &&
        shot.level.hasNeighborSignal(shot.launcher.blockPos)

    private fun attach(shot: Shot, event: ParticleEvent): Boolean {
        if (!valid(shot)) return false
        val level = shot.level
        val other = event.other as ParticleContactOther.Body
        val world = level.shipObjectWorld
        val ground = world.dimensionToGroundBodyIdImmutable[level.dimensionId]
        val targetShip = world.loadedShips.firstOrNull { it.bodyId == other.id }
        if (other.id != ground && targetShip == null) return false
        val point = Vector3d(event.position)
        val normal = Vector3d(event.normalTowardParticle)
        if (targetShip != null) {
            targetShip.worldToShip.transformPosition(point)
            targetShip.transform.shipToWorldRotation.invert(org.joml.Quaterniond()).transform(normal)
        }
        // The contact is on the surface. Step inward only to identify the supporting block;
        // keep the rope endpoint at the exact collision point in that body's coordinates.
        val inside = Vector3d(point).sub(Vector3d(normal).normalize().mul(0.02))
        val pos = BlockPos.containing(inside.x, inside.y, inside.z)
        // Never anchor the rope back onto the block that launched it. Other blocks on
        // the same ship (or terrain body) are still legitimate targets.
        if (pos == shot.source.pos && (targetShip?.id ?: -1L) == shot.source.shipId) return false
        if (!level.hasChunkAt(pos) || level.getBlockState(pos).isAir) return false
        val face = Direction.getNearest(normal.x, normal.y, normal.z)
        val target = WandAnchor(pos, face, targetShip?.id ?: -1,
            offset = Vec3(point.x - pos.x - 0.5, point.y - pos.y - 0.5, point.z - pos.z - 0.5))
        val a = shot.source.world(level) ?: return false
        val b = target.world(level) ?: return false
        val distance = a.distanceTo(b)
        if (distance > maxRange) return false
        val link = WandLinks.get(level).add(level, shot.source, target, true,
            (distance + 0.25).coerceAtMost(maxRange), physical = true, harpoon = true) ?: return false
        shot.launcher.ropeId = link.id
        shot.launcher.setChanged()
        shot.launcher.sendData()
        WanderwandServer.syncLinks(level)
        level.playSound(null, b.x, b.y, b.z, SoundEvents.TRIDENT_HIT, SoundSource.BLOCKS, 1f, 1f)
        return true
    }

    private fun syncFlight(id: ParticleId, shot: Shot, active: Boolean = true, snap: Boolean = false) {
        val level = shot.level
        val tag = CompoundTag().also {
            it.putString("kind", "harpoon_flight")
            it.putString("dimension", level.dimension().location().toString())
            it.putLong("id", id.value)
            it.putBoolean("active", active)
            it.putBoolean("snap", snap)
            if (active) {
                it.put("source", shot.source.save())
                it.putDouble("x", shot.position.x); it.putDouble("y", shot.position.y); it.putDouble("z", shot.position.z)
            }
        }
        val source = shot.source.world(level) ?: shot.position
        val delta = shot.position.subtract(source)
        for (player in level.players()) {
            // Include viewers along the rope, and clear past observers even if they have moved away.
            val t = if (delta.lengthSqr() < 1e-8) 0.0 else
                player.position().subtract(source).dot(delta).div(delta.lengthSqr()).coerceIn(0.0, 1.0)
            if (!active || source.lerp(shot.position, t).distanceToSqr(player.position()) < 192 * 192)
                ClockworkPackets.sendTo(WanderwandStatePacket(tag), player)
        }
    }

    private fun remove(id: ParticleId, snap: Boolean = false) {
        val shot = shots.remove(id) ?: return
        if (!shot.launcher.isRemoved && shot.level.hasChunkAt(shot.launcher.blockPos) &&
            shot.level.getBlockEntity(shot.launcher.blockPos) === shot.launcher)
            shot.launcher.setProjectileInFlight(false)
        syncFlight(id, shot, active = false, snap = snap)
        if (snap) {
            val a = shot.source.world(shot.level) ?: shot.position
            ClockworkSounds.ROPE_SNAP.play(shot.level, null, a, 1f, 0.9f + shot.level.random.nextFloat() * 0.2f)
            if (a.distanceToSqr(shot.position) > 16 * 16)
                ClockworkSounds.ROPE_SNAP.play(shot.level, null, shot.position, 1f, 0.9f + shot.level.random.nextFloat() * 0.2f)
        }
        shot.level.shipObjectWorld.deleteParticle(id)
    }

    /** Restored transient particles are swept by their tag; never retain levels across sessions. */
    fun clear() { shots.clear() }
}
