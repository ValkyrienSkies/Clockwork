package org.valkyrienskies.clockwork.util.sound

import com.simibubi.create.foundation.utility.AnimationTickHolder
import com.simibubi.create.foundation.utility.VecHelper
import com.simibubi.create.infrastructure.config.AllConfigs
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundEvent
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkSoundScapes
import org.valkyrienskies.clockwork.ClockworkSoundScapes.UPDATE_INTERVAL
import org.valkyrienskies.clockwork.ClockworkSoundScapes.getAllLocations
import org.valkyrienskies.core.api.ships.Ship
import java.util.function.Consumer

class SoundScape(
    private val pitch: Float = 0f,
    private val group: ClockworkSoundScapes.AmbienceGroup? = null,
    private var meanPos: Vec3? = null,
    private val pitchGroup: ClockworkSoundScapes.PitchGroup? = null,
    private var ship: Ship? = null
) {
    var continuous: MutableList<ContinuousSound> = ArrayList()
    var repeating: MutableList<RepeatingSound> = ArrayList()

    constructor(pitch: Float, group: ClockworkSoundScapes.AmbienceGroup, ship: Ship?) : this(pitch, group, null, ClockworkSoundScapes.getGroupFromPitch(pitch), ship)

    fun continuous(sound: SoundEvent, relativeVolume: Float, relativePitch: Float, ship: Ship?): SoundScape {
        return add(ContinuousSound(sound, this, pitch * relativePitch, relativeVolume, ship))
    }

    fun repeating(sound: SoundEvent, relativeVolume: Float, relativePitch: Float, delay: Int): SoundScape {
        return add(
            RepeatingSound(
                sound,
                this, pitch * relativePitch, relativeVolume, delay
            )
        )
    }

    fun add(continuousSound: ContinuousSound): SoundScape {
        continuous.add(continuousSound)
        return this
    }

    fun add(repeatingSound: RepeatingSound): SoundScape {
        repeating.add(repeatingSound)
        return this
    }

    fun play() {
        continuous.forEach(Consumer { sound: ContinuousSound? ->
            if (sound != null) {
                Minecraft.getInstance()
                    .soundManager.play(sound as SoundInstance)
            }
        })
    }

    fun tick() {
        if (AnimationTickHolder.getTicks() % UPDATE_INTERVAL == 0) meanPos = null
        repeating.forEach(Consumer { obj: RepeatingSound -> obj.tick() })
    }

    fun remove() {
        continuous.forEach(Consumer { obj: ContinuousSound -> obj.remove() })
    }

    fun getMeanPos(): Vec3 {
        return if (meanPos == null) determineMeanPos().also { meanPos = it }!! else meanPos!!
    }

    private fun determineMeanPos(): Vec3? {
        meanPos = Vec3.ZERO
        var amount = 0
        if (ship != null) {
            for (sound in continuous) {
                meanPos = meanPos!!.add(VecHelper.getCenterOf(sound.getNewPos()))
                amount++
            }
        } else {
            for (pos in getAllLocations(group, pitchGroup)) {
                meanPos = meanPos!!.add(VecHelper.getCenterOf(pos))
                amount++
            }
        }
        if (amount == 0) return meanPos
        return meanPos!!.scale((1f / amount).toDouble())
    }

    fun getVolume(): Float {
        val renderViewEntity = Minecraft.getInstance().cameraEntity
        var distanceMultiplier = 0f
        if (renderViewEntity != null) {
            val distanceTo = renderViewEntity.position()
                .distanceTo(getMeanPos())
            distanceMultiplier = Mth.lerp(distanceTo / ClockworkSoundScapes.MAX_AMBIENT_SOURCE_DISTANCE, 2.0, 0.0).toFloat()
        }
        val soundCount = ClockworkSoundScapes.getSoundCount(group, pitchGroup)
        val max = 0.75f
        val argMax = ClockworkSoundScapes.SOUND_VOLUME_ARG_MAX.toFloat()
        return Mth.clamp(soundCount / (argMax * 10f), 0.025f, max) * distanceMultiplier
    }
}
