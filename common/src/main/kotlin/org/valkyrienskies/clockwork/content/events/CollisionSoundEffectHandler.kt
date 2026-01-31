package org.valkyrienskies.clockwork.content.events

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.events.CollisionEvent
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.pow

@OptIn(VsBeta::class)
object CollisionSoundEffectHandler {

    val collisionQueue = ConcurrentLinkedQueue<CollisionEvent>()
    private val collisionQueueSize = AtomicInteger(0)
    private const val MAX_COLLISION_EVENTS_PROCESSED_PER_TICK: Int = 16

    fun onCollide(event: CollisionEvent) {
        if (!ClockworkConfig.SERVER.collisionSoundEffects) return
        val max = ClockworkConfig.SERVER.collisionSoundEffectMax
        // ConcurrentLinkedQueue#size is O(n); track size ourselves and drop overflow events.
        if (collisionQueueSize.get() >= max) return
        collisionQueue.add(event)
        collisionQueueSize.incrementAndGet()
    }

    private fun getValidState(level: ServerLevel, pos: Vector3dc): BlockState {
        return level.getBlockState(BlockPos.containing(pos.x(), pos.y(), pos.z()))

    }

    private fun calculateVolume(velocitySquared: Double, mass: Double): Float {
        val energy = exp(mass*velocitySquared.pow(0.083))
        return energy.toFloat() / 100f
    }

    fun tick(level: ServerLevel) {
        val groundId = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId] ?: -1L

        val maxQueue = ClockworkConfig.SERVER.collisionSoundEffectMax
        val maxRotations = min(maxQueue, MAX_COLLISION_EVENTS_PROCESSED_PER_TICK * 4)

        // The queue is shared across dimensions; rotate non-matching events instead of looping forever.
        var rotations = 0
        var processed = 0
        if (collisionQueueSize.get() > maxQueue) {
            // If the queue somehow exceeds max (e.g. config change), drain extras gradually to avoid large hitches.
            var dropped = 0
            while (dropped < MAX_COLLISION_EVENTS_PROCESSED_PER_TICK && collisionQueueSize.get() > maxQueue) {
                val droppedEvent = collisionQueue.poll() ?: break
                collisionQueueSize.decrementAndGet()
                dropped++
                // Keep events from other dimensions by rotating them to the back.
                if (droppedEvent.dimensionId != level.dimensionId) {
                    collisionQueue.add(droppedEvent)
                    collisionQueueSize.incrementAndGet()
                    rotations++
                }
            }
        }

        while (processed < MAX_COLLISION_EVENTS_PROCESSED_PER_TICK && rotations < maxRotations) {
            val event = collisionQueue.poll() ?: break
            collisionQueueSize.decrementAndGet()
            if (event.dimensionId != level.dimensionId) {
                collisionQueue.add(event)
                collisionQueueSize.incrementAndGet()
                rotations++
                continue
            }
            processed++

            val contact = event.contactPoints.first()

            var posA = contact.position.add(contact.normal.mul(abs(contact.separation.toDouble()), Vector3d()),Vector3d())
            var posB = contact.position.sub(contact.normal.mul(abs(contact.separation.toDouble()), Vector3d()),Vector3d())

            var mass = 0.0

            if (event.shipIdA != groundId) {
                val ship = level.shipObjectWorld.loadedShips.getById(event.shipIdA)
                if (ship != null) {
                    posA = ship.transform.worldToShip.transformPosition(posA, Vector3d())
                    mass += ship.inertiaData.mass
                }
            }
            if (event.shipIdB != groundId) {
                val ship = level.shipObjectWorld.loadedShips.getById(event.shipIdB)
                if (ship != null) {
                    posB = ship.transform.worldToShip.transformPosition(posB, Vector3d())
                    mass += ship.inertiaData.mass
                }
            }

            val stateA = getValidState(level, posA)
            val stateB = getValidState(level, posB)



            val pos = contact.position

            val volume = calculateVolume(contact.velocity.lengthSquared(), mass)
            //println("$stateA $stateB ${contact.separation} ${contact.velocity.lengthSquared()} ${volume}")

            level.playSound(null, pos.x(), pos.y(), pos.z(), stateA.soundType.hitSound, SoundSource.BLOCKS, volume, 1f)
            level.playSound(null, pos.x(), pos.y(), pos.z(), stateB.soundType.hitSound, SoundSource.BLOCKS, volume, 1f)
        }
    }


}
