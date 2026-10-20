package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.util.EaseHelper
import kotlin.math.*

/** General visual control class */
object InfuserVisuals {
    data class CorePose(val offset: Vec3 = Vec3.ZERO, val pitch: Float = 0f, val yaw: Float = 0f,
                        val roll: Float = 0f, val scale: Float = 1.5f)

    const val BEAM_FLIGHT = 3f
    const val PULSE_PERIOD = 12f
    fun collapseDuration(sequence: InfuserSequence) = if (sequence.skippedAt >= 0) 6f else if (sequence.firstUse) 28f else 18f
    fun collapse(sequence: InfuserSequence, age: Float) =
        ((age - sequence.impact + collapseDuration(sequence)) / collapseDuration(sequence)).coerceIn(0f, 1f)

    fun impulse(age: Float): Float = if (age < 0f || age > 18f) 0f
        else sin(age * 1.12f) * exp(-age * 0.27f) * 1.65f

    fun sample(sequence: InfuserSequence, age: Float, directions: List<Vec3>): CorePose {
        if (age < 0 || age >= sequence.settled) return CorePose()
        val charge = sequence.charge(age)
        val fade = InfuserSequence.smooth(age / 12f) *
            (1f - InfuserSequence.smooth((age - sequence.impact) / 20f))
        val tremor = (0.014 + charge * charge * 0.065) * fade
        var kick = Vec3.ZERO
        var torqueX = 0f; var torqueZ = 0f
        for ((index, direction) in directions.withIndex()) {
            val shot = age - sequence.connection(index, directions.size)
            val force = impulse(shot) + impulse(shot - BEAM_FLIGHT) * 0.35f
            kick = kick.add(direction.normalize().scale(-force * 0.26)).add(0.0, force * 0.11, 0.0)
            torqueX += (direction.z * force * 23).toFloat()
            torqueZ -= (direction.x * force * 23).toFloat()
        }
        if (kick.length() > 0.4) kick = kick.normalize().scale(0.4)
        val inward = EaseHelper.easeInExpo(collapse(sequence, age))
        val after = age - sequence.impact
        val shock = impulse(after)
        val offset = kick.scale(fade.toDouble()).add(
            sin(age * 2.7) * tremor + sin(age * 0.61) * 0.06 * fade,
            sin(age * 3.9) * tremor + sin(age * 0.83) * 0.045 * fade + inward * 0.1 * fade - shock * 0.22,
            cos(age * 3.3) * tremor + cos(age * 0.73) * 0.06 * fade)
        val rotation = (5f + charge * 13f) * fade
        return CorePose(offset,
            torqueX.coerceIn(-28f, 28f) * fade + sin(age * 0.71f) * rotation + shock * 25,
            sin(age * 0.46f) * rotation * 2f,
            torqueZ.coerceIn(-28f, 28f) * fade + cos(age * 0.93f) * rotation,
            1.5f + charge * 0.25f - inward * 0.24f * fade + max(0f, shock) * 0.34f)
    }

    fun beamPulse(elapsed: Float, t: Float): Float {
        if (elapsed < BEAM_FLIGHT) return 0f
        val head = ((elapsed - BEAM_FLIGHT) % PULSE_PERIOD) / PULSE_PERIOD
        return exp(-((t - head) / 0.085f).pow(2))
    }

    fun burstRadius(after: Float) = 9f * EaseHelper.easeOutExpo((after / 14f).coerceIn(0f, 1f))

    fun gatherPoint(sequence: InfuserSequence, age: Float, index: Int): Vec3 {
        val collapseStart = sequence.impact - collapseDuration(sequence)
        val sampleAge = min(age, collapseStart)
        val cycle = gatherCycle(sampleAge, index)
        val inward = EaseHelper.easeInExpo(collapse(sequence, age))
        val azimuth = index * 2.399963 + sampleAge * 0.022 + cycle * cycle * 2.2
        val y = sin(index * 1.71) * 0.72
        val radius = (0.7 + (1 - cycle * cycle) * (3.8 + index % 5 * 0.55)) * (1 - inward)
        val twist = inward * 0.9
        return Vec3(cos(azimuth + twist) * sqrt(1 - y * y), y + sin(azimuth * 1.3) * 0.12,
            sin(azimuth + twist) * sqrt(1 - y * y)).scale(radius)
    }

    fun gatherCycle(age: Float, index: Int) = ((age + index * 17.13f) / 52f % 1f).toDouble()
}
