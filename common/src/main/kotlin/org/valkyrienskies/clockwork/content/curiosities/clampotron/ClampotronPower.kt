package org.valkyrienskies.clockwork.content.curiosities.clampotron

import kotlin.math.abs

object ClampotronPower {
    private const val SNAP_STRAIN = 1.2f
    fun overloadTicks(previous: Int, strain: Float) =
        if (strain > SNAP_STRAIN) previous + 1 else (previous - 2).coerceAtLeast(0)
    fun shouldSnap(overloadTicks: Int, positionError: Double) = overloadTicks >= 45 || positionError > 24.0
    fun warning(strain: Float) = (strain / SNAP_STRAIN * 2f - 1f).coerceIn(0f, 1f)
    fun running(rpm: Double, overstressed: Boolean) = !overstressed && rpm.isFinite() && abs(rpm) >= 1.0
    fun stress(mass: Double, massPerStress: Double): Double =
        1.0 + if (mass.isFinite() && mass > 0.0) mass / massPerStress.coerceAtLeast(1.0) else 0.0
    fun force(rpm: Double, forcePerRpm: Double) = if (rpm.isFinite()) abs(rpm) * forcePerRpm.coerceAtLeast(1.0) else 0.0
}
