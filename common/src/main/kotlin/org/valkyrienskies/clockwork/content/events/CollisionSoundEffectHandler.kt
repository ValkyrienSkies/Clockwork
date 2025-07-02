package org.valkyrienskies.clockwork.content.events

import kotlinx.coroutines.MainScope
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.events.CollisionEvent
import org.valkyrienskies.core.util.squared
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.log
import kotlin.math.pow

object CollisionSoundEffectHandler {

    val CHECK_DISTANCE = 0.1

    val collisionQueue = ConcurrentLinkedQueue<CollisionEvent>()

    fun onCollide(event: CollisionEvent) {
        collisionQueue.add(event)
    }



    // This may be stupid
    private fun getValidState(level: ServerLevel, pos: Vector3dc): BlockState {
        if (!level.getBlockState(BlockPos.containing(pos.x(), pos.y(), pos.z())).isAir) return level.getBlockState(BlockPos.containing(pos.x(), pos.y(), pos.z()))

        for (x in -1..1) {
            for (y in -1..1) {
                for (z in -1..1) {
                    val newPos = Vector3d(pos.x()+x* CHECK_DISTANCE, pos.y()+y* CHECK_DISTANCE, pos.z()+z* CHECK_DISTANCE)

                    val state = level.getBlockState(BlockPos.containing(pos.x(), pos.y(), pos.z()))
                    if (!state.isAir) return state
                }
            }
        }

        return level.getBlockState(pos.toMinecraft())
    }

    private fun calculateVolume(velocitySquared: Double, mass: Double): Float {
        val energy = log(mass*velocitySquared/2, 10.0)
        return energy.toFloat()
    }

    fun tick(level: ServerLevel) {
        val groundId = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]

        while (!collisionQueue.isEmpty()) {
            val event = collisionQueue.first()
            if (event.dimensionId != level.dimensionId) continue
            collisionQueue.remove()

            var posA = event.contactPoints.first().position
            var posB = event.contactPoints.first().position

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

            println("$stateA $stateB ${event.contactPoints.first().separation}")

            val pos = event.contactPoints.first().position

            val volume = calculateVolume(event.contactPoints.first().velocity.lengthSquared(), mass)

            level.playSound(null, pos.x(), pos.y(), pos.z(), stateA.soundType.hitSound, SoundSource.BLOCKS, volume, 1f)
            level.playSound(null, pos.x(), pos.y(), pos.z(), stateB.soundType.hitSound, SoundSource.BLOCKS, volume, 1f)
        }
    }


}