package org.valkyrienskies.clockwork.util.sound

import net.minecraft.client.sounds.AudioStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import kotlin.random.Random
import kotlin.math.pow
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

internal data class ThrusterControls(
    val flow: Double, val speed: Double, val heat: Double, val steam: Double, val exhaust: Double, val expansion: Double
) {
    companion object {
        val SILENT = ThrusterControls(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }
}

internal class ThrusterAudioStream(seed: Long, private val controls: () -> ThrusterControls) : AudioStream {
    companion object {
        private const val RATE = 24000
        private const val TABLE_SIZE = 2048
        private val pcmFormat = AudioFormat(RATE.toFloat(), 16, 1, true, false)
        private val sine = DoubleArray(TABLE_SIZE) { sin(2 * PI * it / TABLE_SIZE) }
        private fun bell(hz: Double, center: Double, width: Double): Double {
            val distance = (hz - center) / width
            return exp(-0.5 * distance * distance)
        }
        private fun growlTable(root: Double): DoubleArray {
            val table = DoubleArray(TABLE_SIZE)
            for (harmonic in 1..30) {
                val hz = harmonic * root
                val weight = 0.68 * bell(hz, 273.0, 82.0) + 0.29 * bell(hz, 620.0, 123.0) +
                    0.15 * bell(hz, 1080.0, 240.0) + 0.40 * bell(hz, 69.0, 33.0)
                for (i in table.indices) {
                    table[i] += weight * sin(harmonic * (2 * PI * i / TABLE_SIZE + 0.83))
                }
            }
            return DoubleArray(TABLE_SIZE) { tanh(0.8 * table[it]) }
        }
        private val bogWave = growlTable(54.0)
        private val boostWave = growlTable(70.0)
        private val dryWave = growlTable(46.0)

        private fun lookup(table: DoubleArray, phase: Double): Double {
            val index = phase * TABLE_SIZE
            val whole = index.toInt()
            val left = table[whole and (TABLE_SIZE - 1)]
            return left + (table[(whole + 1) and (TABLE_SIZE - 1)] - left) * (index - whole)
        }
    }

    private class NoiseBand(hz: Double, q: Double) {
        private val angle = 2 * PI * hz / RATE
        private val alpha = sin(angle) / (2 * q)
        private val gain = sqrt(alpha / (1 + alpha))
        private val feedback = 2 * cos(angle) / (1 + alpha)
        private val damping = (1 - alpha) / (1 + alpha)
        private var first = 0.0
        private var second = 0.0

        fun sample(input: Double): Double {
            val out = gain * input + first
            first = feedback * out + second
            second = -gain * input - damping * out
            return out
        }
    }

    private class ContinuousGrowl(seed: Long) {
        companion object {
            private const val HOP = 540
            private const val LENGTH = 2520
            private const val VOICES = 5
            private const val MASK = 4095
            private val window = DoubleArray(LENGTH) { sin(PI * it / LENGTH).let { amplitude -> amplitude * amplitude } }
            private val normalization = DoubleArray(HOP) { phase ->
                var power = 0.0
                for (age in phase until LENGTH step HOP) power += window[age] * window[age]
                1.0 / sqrt(power)
            }
        }

        private val history = DoubleArray(MASK + 1)
        private val random = Random(seed)
        private val cursor = DoubleArray(VOICES)
        private val rate = DoubleArray(VOICES)
        private val age = IntArray(VOICES) { LENGTH }
        private var write = 0
        private var phase = 0
        private var nextVoice = 0
        private var fastPower = 0.0
        private var slowPower = 0.0
        private var level = 1.0
        private var targetLevel = 1.0

        fun sample(input: Double): Double {
            history[write] = input
            if (phase == 0) {
                cursor[nextVoice] = (write - random.nextDouble(270.0, 2650.0) + history.size) % history.size
                rate[nextVoice] = random.nextDouble(0.9, 1.1)
                age[nextVoice] = 0
                nextVoice = (nextVoice + 1) % VOICES
            }
            var sum = 0.0
            for (i in age.indices) {
                if (age[i] >= LENGTH) continue
                val index = cursor[i].toInt()
                val left = history[index]
                val interpolated = left + (history[(index + 1) and MASK] - left) * (cursor[i] - index)
                sum += interpolated * window[age[i]++]
                cursor[i] += rate[i]
                if (cursor[i] >= history.size) cursor[i] -= history.size
            }
            val mixed = 0.39 * input + 0.88 * sum * normalization[phase]
            phase = (phase + 1) % HOP
            write = (write + 1) and MASK
            val power = mixed * mixed
            fastPower += (power - fastPower) * 0.0046
            slowPower += (power - slowPower) * 0.00024
            if (write and 15 == 0) targetLevel = ((slowPower + 1e-9) / (fastPower + 1e-9)).pow(0.385).coerceIn(0.62, 1.85)
            level += (targetLevel - level) * 0.04
            return tanh(2.5 * mixed * level) / 2.5
        }
    }

    private val growl = ContinuousGrowl(seed)
    private val pcm = ByteBuffer.allocateDirect(RATE / 20 * 2).order(ByteOrder.LITTLE_ENDIAN)
    private var noiseState = (seed xor (seed ushr 32)).toInt() or 1
    private val rush = NoiseBand(3200.0, 0.68)
    private val hiss = NoiseBand(1500.0, 0.70)
    private val rumble = NoiseBand(68.0, 0.95)
    private val grain = NoiseBand(300.0, 0.85)
    private val edge = NoiseBand(1400.0, 0.8)
    private var rootPhase = 0.0
    private var beatPhase = 0.0
    private var whinePhase = 0.0
    private var wandering = 0.0
    private var gain = 0.0
    private var hot = 0.0
    private var wet = 0.0
    private var boost = 0.0
    private var dry = 0.0

    override fun getFormat() = pcmFormat
    override fun close() {}

    override fun read(size: Int): ByteBuffer {
        val target = controls()
        val hotTarget = sqrt(min(1.0, target.exhaust * 2)) * min(1.0, target.heat * 2.6)
        val boostTarget = ((target.heat - 0.39) / 0.42).coerceIn(0.0, 1.0) *
            (0.55 + 0.45 * min(1.0, target.expansion / 0.65))
        val dryTarget = (target.exhaust - target.steam).coerceIn(0.0, 1.0) * hotTarget
        pcm.clear()
        repeat(min(size, pcm.capacity()) / 2) {
            gain += (target.flow - gain) * 0.0016
            hot += (hotTarget - hot) * 0.0012
            wet += (target.steam - wet) * 0.0012
            dry += (dryTarget - dry) * 0.0012
            boost += (boostTarget - boost) * 0.0012
            noiseState = noiseState xor (noiseState shl 13)
            noiseState = noiseState xor (noiseState ushr 17)
            noiseState = noiseState xor (noiseState shl 5)
            val noise = noiseState * (1.7320508 / Int.MAX_VALUE)
            wandering += 0.0008 * (noise - wandering)
            val root = 54 + 16 * boost * (1 - dry) - 8 * dry + 65 * wandering
            rootPhase = (rootPhase + root / RATE) % 2
            beatPhase = (beatPhase + (8 + 3 * target.speed + 24 * wandering) / RATE) % 1
            whinePhase = (whinePhase + (250 + 170 * target.speed) / RATE) % 1
            val motion = 0.5 + 0.5 * lookup(sine, beatPhase)
            val warped = rootPhase + 1 + 0.054 * lookup(sine, rootPhase * 0.5)
            val bog = lookup(bogWave, warped)
            val boosted = bog + (lookup(boostWave, warped) - bog) * boost
            val voice = boosted + (lookup(dryWave, warped) - boosted) * dry
            val rasp = grain.sample(noise)
            val pulse = (0.45 + 0.95 * motion) * (1.65 * voice + 0.32 * rasp)
            val air = rush.sample(noise) * (0.015 + 0.008 * target.speed) * (1 - wet)
            val vapor = hiss.sample(noise) * wet * (0.11 - 0.045 * hot)
            val body = rumble.sample(noise) * (0.051 + 0.039 * dry) + 0.035 * rasp
            val bite = edge.sample(noise) * (0.024 + 0.055 * boost) +
                lookup(sine, whinePhase) * 0.028 * boost * (1 - dry)
            val combustion = body + bite + pulse * (0.086 + 0.053 * boost + 0.072 * dry)
            val source = air + vapor + hot * combustion
            val continuous = growl.sample(source)
            val sample = gain * (source + hot * (continuous * (1.10 + 0.12 * boost + 0.10 * dry) - source))
            pcm.putShort((sample.coerceIn(-1.0, 1.0) * 32767).toInt().toShort())
        }
        return pcm.flip()
    }
}
