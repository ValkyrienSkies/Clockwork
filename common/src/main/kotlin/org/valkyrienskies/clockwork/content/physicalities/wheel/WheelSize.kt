package org.valkyrienskies.clockwork.content.physicalities.wheel

import org.joml.Matrix3d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.core.api.bodies.shape.TireProperties
import org.valkyrienskies.core.api.bodies.shape.WheelBodyShapeData
import kotlin.math.sqrt
import kotlin.math.cos
import kotlin.math.PI

enum class WheelSize(val id: String, val radius: Double, val width: Double, val mass: Double) {
    SMALL("small_wheel", 0.625, 0.375, 40.0),
    MEDIUM("medium_wheel", 0.75, 0.5, 90.0),
    LARGE("large_wheel", 1.25, 0.75, 300.0);

    val rimRadius get() = when (this) {
        SMALL -> radius * .55
        MEDIUM -> .5 / cos(PI / 8)
        LARGE -> 1.0 / cos(PI / 8)
    }
    val minimumWidthPercent get() = if (this == MEDIUM) 75 else 50
    val maximumThicknessPercent get() = ((2.0 - rimRadius) / (radius - rimRadius) * 20).toInt() * 5

    fun tire(widthPercent: Int = 100, thicknessPercent: Int = 100, pressureKpa: Int = 800) =
        WheelTire(this, widthPercent.coerceIn(minimumWidthPercent, 200), thicknessPercent.coerceIn(25, maximumThicknessPercent), pressureKpa.coerceIn(100, 1600))

    fun shape() = tire().shape()
    fun inertia() = tire().inertia()

    companion object {
        fun byId(id: String) = entries.firstOrNull { it.id == id }
        fun axleRotation(x: Double, y: Double, z: Double) =
            Quaterniond().rotationTo(Vector3d(1.0, 0.0, 0.0), Vector3d(x, y, z))
    }
}

data class WheelTire internal constructor(val size: WheelSize, val widthPercent: Int,
                                         val thicknessPercent: Int, val pressureKpa: Int) {
    val width = size.width * widthPercent / 100.0
    val rimRadius = size.rimRadius
    val rimWidth = if (size == WheelSize.SMALL) width else .375
    val thickness = (size.radius - rimRadius) * thicknessPercent / 100.0
    val radius = rimRadius + thickness
    private val rubberRatio = width / size.width * (radius * radius - rimRadius * rimRadius) /
        (size.radius * size.radius - rimRadius * rimRadius)
    val mass = size.mass * (.35 + .65 * rubberRatio)

    fun shape(): WheelBodyShapeData {
        val widthRatio = widthPercent / 100.0
        val depthRatio = 100.0 / thicknessPercent
        val pressureRatio = pressureKpa / 800.0
        val radial = widthRatio * depthRatio * (.2 + .8 * pressureRatio)
        val tread = widthRatio * depthRatio * (.7 + .3 * pressureRatio)
        return WheelBodyShapeData(radius, width / 2, tire = TireProperties(
            maximumCompression = thickness * .4,
            radialStiffness = 1_000_000.0 * size.radius * radial,
            radialDamping = 100_000.0 * size.radius * sqrt(radial * mass / size.mass),
            longitudinalStiffness = 600_000.0 * size.radius * tread,
            lateralStiffness = 900_000.0 * size.radius * tread,
            treadDamping = 5_000.0 * size.radius * sqrt(tread * mass / size.mass),
            slidingGrip = .75, slipTransitionSpeed = 1.5,
        ))
    }

    fun inertia(): Matrix3d {
        val rimMass = size.mass * .35
        val rubberMass = mass - rimMass
        val axial = rimMass * rimRadius * rimRadius / 2 + rubberMass * (radius * radius + rimRadius * rimRadius) / 2
        val transverse = axial / 2 + mass * width * width / 12
        return Matrix3d().scaling(axial, transverse, transverse)
    }
}
