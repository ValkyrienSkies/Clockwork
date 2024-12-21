package org.valkyrienskies.clockwork.util.sound

import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.core.BlockPos
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.mod.client.audio.VelocityTickableSoundInstance
import org.valkyrienskies.mod.common.toWorldCoordinates

class ContinuousSound(var soundEvent: SoundEvent, val soundSource: SoundSource, var sharedPitch: Float, var scape: SoundScape, var relativeVolume: Float, var ship: Ship? = null) : AbstractTickableSoundInstance(soundEvent, soundSource), VelocityTickableSoundInstance {

    override val velocity: Vector3dc
        get() = this.ship?.velocity ?: Vector3d()

    internal constructor(soundEvent: SoundEvent, scape: SoundScape, sharedPitch: Float, relativeVolume: Float, ship: Ship? = null) : this(soundEvent, SoundSource.AMBIENT, sharedPitch, scape, relativeVolume) {
        this.looping = true
        this.delay = 0
        this.relative = false
    }

    init {
        this.looping = true
        this.delay = 0
        this.relative = false
    }

    override fun getVolume(): Float {
        return scape.getVolume() * relativeVolume
    }

    override fun getPitch(): Float {
        return sharedPitch
    }

    override fun getX(): Double {
        return scape.getMeanPos().x
    }

    override fun getY(): Double {
        return scape.getMeanPos().y
    }

    override fun getZ(): Double {
        return scape.getMeanPos().z
    }

    val originalPos: BlockPos = BlockPos(x, y, z)

    fun getNewPos(): BlockPos {
        if (ship == null) return originalPos
        return BlockPos(ship!!.toWorldCoordinates(Vec3(x, y, z)))
    }

    override fun tick() {
        if (ship != null) {
            val newPosition = ship!!.toWorldCoordinates(BlockPos(x, y, z))
            this.x = newPosition.x
            this.y = newPosition.y
            this.z = newPosition.z
        }
    }

    fun remove() {
        stop()
    }

}
