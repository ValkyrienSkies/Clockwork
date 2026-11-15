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
        SMALL -> .25 / cos(PI / 8)
        MEDIUM -> .5 / cos(PI / 8)
        LARGE -> 1.0 / cos(PI / 8)
    }
    val minimumWidthPercent get() = if (this == MEDIUM) 75 else 50
    val maximumThicknessPercent get() = ((2.0 - rimRadius) / (radius - rimRadius) * 20).toInt() * 5

    fun tire(widthPercent: Int = 100, thicknessPercent: Int = 100, pressureKpa: Int = 800, type: TireType = TireType.RUBBER) =
        WheelTire(this, widthPercent.coerceIn(minimumWidthPercent, 200), thicknessPercent.coerceIn(25, maximumThicknessPercent), pressureKpa.coerceIn(100, 1600), type)

    fun shape() = tire().shape()
    fun inertia() = tire().inertia()

    companion object {
        fun byId(id: String) = entries.firstOrNull { it.id == id }
        fun axleRotation(x: Double, y: Double, z: Double) =
            Quaterniond().rotationTo(Vector3d(1.0, 0.0, 0.0), Vector3d(x, y, z))
    }
}

data class WheelTire internal constructor(val size: WheelSize, val widthPercent: Int,
                                         val thicknessPercent: Int, val pressureKpa: Int, val type: TireType = TireType.RUBBER) {
    val width = size.width * widthPercent / 100.0
    val rimRadius = size.rimRadius
    val rimWidth = .375
    val thickness = (size.radius - rimRadius) * thicknessPercent / 100.0
    val radius = rimRadius + thickness
    private val rubberRatio = width / size.width * (radius * radius - rimRadius * rimRadius) /
        (size.radius * size.radius - rimRadius * rimRadius)
    val mass = size.mass * (.35 + .65 * rubberRatio * type.physics.massMultiplier)

    fun shape(): WheelBodyShapeData {
        val material = type.physics
        val widthRatio = widthPercent / 100.0
        val depthRatio = 100.0 / thicknessPercent
        val pressureRatio = pressureKpa / 800.0
        val radial = widthRatio * depthRatio * (.2 + .8 * pressureRatio)
        val tread = widthRatio * depthRatio * (.7 + .3 * pressureRatio)
        return WheelBodyShapeData(radius, width / 2, tire = TireProperties(
            maximumCompression = thickness * material.compression,
            radialStiffness = material.radialStiffness * size.radius * radial,
            radialDamping = material.radialDamping * size.radius * sqrt(radial * mass / size.mass),
            longitudinalStiffness = material.longitudinalStiffness * size.radius * tread,
            lateralStiffness = material.lateralStiffness * size.radius * tread,
            treadDamping = material.treadDamping * size.radius * sqrt(tread * mass / size.mass),
            slidingGrip = material.slidingGrip, slipTransitionSpeed = material.slipTransitionSpeed,
            adhesiveForce = material.adhesion * width * radius,
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
