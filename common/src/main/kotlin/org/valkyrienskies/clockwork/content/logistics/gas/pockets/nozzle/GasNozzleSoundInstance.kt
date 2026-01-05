package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Mth
import net.minecraft.util.RandomSource
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkSounds
import kotlin.math.absoluteValue
import kotlin.math.min

class GasNozzleSoundInstance(val nozzle: GasNozzleBlockEntity, randomSource: RandomSource,
) : AbstractTickableSoundInstance(
    ClockworkSounds.GAS_NOZZLE_LOOP.mainEvent!!,
    SoundSource.BLOCKS, randomSource) {

    init {
        this.looping = true
        this.delay = 0
        this.volume = 0.0F
        this.x = nozzle.blockPos.x.toDouble()
        this.y = nozzle.blockPos.y.toDouble()
        this.z = nozzle.blockPos.z.toDouble()
    }

    fun stopNow() {
        this.stop()
    }

    override fun canStartSilent(): Boolean {
        return true
    }

    override fun tick() {
        if (this.nozzle.isRemoved) {
            this.stop()
            return
        }

        if (!this.nozzle.hasPocket) {
            this.volume = 0.0F
            this.pitch = 0.01F
            this.stop()
            return
        }

        val dial = nozzle.pointer.value

        if (dial < 0.01f) {
            this.volume = 0.0F
            this.pitch = 0.01F
            return
        }

        val kelvin = ClockworkModClient.getKelvin()
        val pressure = kelvin.getPressureAt(nozzle.getDuctNodePosition())
        if (pressure <= 1e-2) {
            this.volume = 0.0F
            this.pitch = 0.01F
            return
        }

        val modifier = min(pressure.toFloat() * 10f / 16375049.0f, 2f) * dial

        this.volume = Mth.clamp(
            0.4f + (modifier * 0.8f),
            0.0f,
            2.0f
        )
        this.pitch = Mth.clamp(
            0.1f + (modifier * 0.9f),
            0.1f,
            1.2f
        )

        this.x = nozzle.blockPos.x.toDouble()
        this.y = nozzle.blockPos.y.toDouble()
        this.z = nozzle.blockPos.z.toDouble()
    }
}
