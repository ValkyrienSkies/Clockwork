package org.valkyrienskies.clockwork.util.sound

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import kotlin.math.absoluteValue

class PropellerSoundInstance(val propeller: PropellerBearingBlockEntity, randomSource: RandomSource,
) : AbstractTickableSoundInstance(if (propeller.brass) ClockworkSounds.PROPELLER.mainEvent!! else ClockworkSounds.JUNK_PROPELLER.mainEvent!!,
    SoundSource.BLOCKS, randomSource) {

    val brass = propeller.brass

    init {
        this.looping = true
        this.delay = 0
        this.volume = 0.0F
        this.x = propeller.blockPos.x.toDouble()
        this.y = propeller.blockPos.y.toDouble()
        this.z = propeller.blockPos.z.toDouble()
    }

    override fun canStartSilent(): Boolean {
        return true
    }

    override fun tick() {
        if (this.propeller.isRemoved) {
            this.stop()
            return
        }

        if (!this.propeller.running || this.propeller.starting) {
            this.volume = 0.0F
            this.pitch = 0.01F
            this.stop()
            return
        }
        val proportionedSpeed = propeller.currentOmega.absoluteValue.toFloat() / KineticBlockEntity.convertToAngular(256.0f)

        if (proportionedSpeed < 0.01f) {
            this.volume = 0.0F
            this.pitch = 0.01F
            return
        }
        this.volume = Mth.clamp(
            0.1f + (proportionedSpeed * (if (brass) 0.7f else 0.8f)),
            0.0f,
            2.0f
        )
        this.pitch = Mth.clamp(
            0.1f + (proportionedSpeed * (if (brass) 0.7f else 0.9f)),
            0.1f,
            1.2f
        )

        this.x = propeller.blockPos.x.toDouble()
        this.y = propeller.blockPos.y.toDouble()
        this.z = propeller.blockPos.z.toDouble()
    }
}
