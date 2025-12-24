package org.valkyrienskies.clockwork.content.events

import kotlinx.coroutines.MainScope
import net.fabricmc.loader.impl.lib.sat4j.core.Vec
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.events.CollisionEvent
import org.valkyrienskies.core.util.squared
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.log
import kotlin.math.pow

@OptIn(VsBeta::class)
object CollisionSoundEffectHandler {

    val collisionQueue = ConcurrentLinkedQueue<CollisionEvent>()

    fun onCollide(event: CollisionEvent) {
        if (ClockworkConfig.SERVER.collisionSoundEffects) collisionQueue.add(event)
    }

    private fun getValidState(level: ServerLevel, pos: Vector3dc): BlockState {
        return level.getBlockState(BlockPos.containing(pos.x(), pos.y(), pos.z()))

    }

    private fun calculateVolume(velocitySquared: Double, mass: Double): Float {
        val energy = exp(mass*velocitySquared.pow(0.083))
        return energy.toFloat() / 100f
    }

    fun tick(level: ServerLevel) {
        val groundId = level.shipObjectWorld.dimensionToGroundBodyIdImmutable[level.dimensionId]

        if (collisionQueue.size > ClockworkConfig.SERVER.collisionSoundEffectMax) collisionQueue.clear()

        while (!collisionQueue.isEmpty()) {
            //if (collisionQueue.isNotEmpty()) println("Collision Queue size: ${collisionQueue.size}")

            val event = collisionQueue.first()
            if (event.dimensionId != level.dimensionId) continue
            collisionQueue.remove()

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
