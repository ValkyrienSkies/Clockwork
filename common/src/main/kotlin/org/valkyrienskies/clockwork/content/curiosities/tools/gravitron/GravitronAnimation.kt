package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import kotlin.math.PI
import kotlin.math.sin

/** Tick-clock animation: sampling never advances state, regardless of FPS or render passes. */
class GravitronAnimation {
    var action = GravitronAction.IDLE
        private set
    var startedAt = 0L
        private set
    var holding = false
        private set
    var load = 0f
        private set
    private var openingFrom = 0f
    private var dialFrom = 10f

    data class Pose(
        val opening: Float = 0f,
        val recoil: Float = 0f,
        val lowering: Float = 0f,
        val roll: Float = 0f,
        val energy: Float = 0f,
        val strain: Float = 0f,
        val dial: Float = 0f
    )

    fun accept(next: GravitronAction, now: Long, newLoad: Float = 0f) {
        val pose = sample(now, 0f)
        load = if (newLoad.isFinite()) newLoad.coerceIn(0f, 1.5f) else 0f
        if (next == GravitronAction.HOLD && holding) return
        if (next == GravitronAction.IDLE && !holding) return
        openingFrom = pose.opening
        dialFrom = pose.dial
        holding = next == GravitronAction.GRAB || next == GravitronAction.HOLD
        action = next
        startedAt = now
    }

    fun age(now: Long, partialTick: Float): Float = (now - startedAt).toFloat() + partialTick

    fun sample(now: Long, partialTick: Float): Pose {
        val age = age(now, partialTick).coerceAtLeast(0f)
        val progress = if (action.duration == 0f) 1f else (age / action.duration).coerceIn(0f, 1f)
        val settle = smooth(age / 8f)
        val baseOpening = if (holding) 22f else 0f
        var opening = openingFrom + (baseOpening - openingFrom) * settle
        var recoil = 0f
        var lowering = 0f
        var roll = 0f
        var energy = if (holding) 0.55f else 0f
        var strain = if (holding) ((load - 0.75f) / 0.25f).coerceIn(0f, 1f) else 0f
        val impulse = pulse(age, 1.2f, action.duration)
        when (action) {
            GravitronAction.DRAW -> {
                lowering = 1f - smooth(progress)
                opening += 17f * sin(progress * PI).toFloat()
                roll = -14f * lowering
                energy += 0.45f * sin(progress * PI).toFloat()
            }
            GravitronAction.GRAB -> {
                opening += 16f * pulse(age, 3f, 12f)
                recoil = 0.26f * pulse(age, 4f, 12f)
                energy += 0.35f * impulse
            }
            GravitronAction.RELEASE -> {
                // Ease the claws apart to let go, then latch them closed as the field drains.
                val relax = pulse(age, 3f, 14f)
                opening = openingFrom * (1f - smooth((age - 3f) / 11f)) + 9f * relax
                recoil = -0.1f * relax
                lowering = 0.055f * relax
                energy = 0.5f * (1f - smooth(age / 9f))
            }
            GravitronAction.LAUNCH -> {
                opening += 64f * impulse
                recoil = impulse
                roll = -7f * impulse
                energy += impulse
            }
            GravitronAction.FREEZE -> {
                opening += 38f * pulse(age, 3f, 18f)
                recoil = 0.22f * impulse
                energy += pulse(age, 3f, 18f)
            }
            GravitronAction.UNFREEZE -> {
                opening += 30f * pulse(age, 5f, 22f)
                recoil = -0.14f * pulse(age, 6f, 22f)
                energy += pulse(age, 8f, 22f)
            }
            GravitronAction.OVERLOAD -> {
                val shake = sin(age * 2.7f) * (1f - progress)
                opening += (16f + 8f * shake) * (1f - smooth((age - 14f) / 18f))
                recoil = 0.25f * impulse
                roll = 5f * shake
                lowering = 0.2f * pulse(age, 16f, 32f)
                strain = 1f - progress
                energy += (0.6f + 0.35f * shake) * (1f - progress)
            }
            else -> Unit
        }
        val dialTarget = when {
            holding -> (10f + load * 340f).coerceAtMost(350f)
            action == GravitronAction.OVERLOAD -> 350f * (1f - smooth((age - 16f) / 16f))
            else -> 10f
        }
        return Pose(opening, recoil, lowering, roll, energy.coerceIn(0f, 1.5f), strain,
            dialFrom + (dialTarget - dialFrom) * settle)
    }

    companion object {
        fun smooth(value: Float): Float {
            val t = value.coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }

        private fun pulse(age: Float, attack: Float, duration: Float): Float {
            if (duration <= 0f || age >= duration) return 0f
            return if (age < attack) smooth(age / attack)
            else 1f - smooth((age - attack) / (duration - attack))
        }
    }
}
