package org.valkyrienskies.clockwork.content.contraptions.propeller

import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.joml.Vector3dc

/** Model-space rotor geometry shared by rendering and aerodynamics. Axes and tilt must be unit length. */
internal object PropellerTransform {
    // Preserve the copter renderer's pivot: 0.9 blocks plus its 1/16-block inset behind the hub.
    private const val COPTER_PIVOT_DISTANCE = 0.9 + 1.0 / 16.0

    fun facingSign(baseAxis: Vector3dc): Double =
        if (baseAxis.x() + baseAxis.y() + baseAxis.z() < 0.0) -1.0 else 1.0

    fun orientation(baseAxis: Vector3dc, tilt: Quaterniondc, angleDegrees: Double): Quaterniond =
        Quaterniond(tilt).mul(Quaterniond().fromAxisAngleRad(
            baseAxis, Math.toRadians(angleDegrees) * facingSign(baseAxis)
        ))

    /** Displacement of the contraption hub from its untilted anchor, independent of rotor phase. */
    fun hubDisplacement(baseAxis: Vector3dc, tilt: Quaterniondc): Vector3d =
        tilt.transform(baseAxis, Vector3d()).sub(baseAxis).mul(COPTER_PIVOT_DISTANCE)
}
