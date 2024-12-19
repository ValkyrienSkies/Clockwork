package org.valkyrienskies.clockwork.util

import org.joml.Vector3d
import org.joml.Vector3dc

inline operator fun Vector3dc.plus(other: Vector3dc) = this.add(other, Vector3d())
inline operator fun Vector3dc.minus(other: Vector3dc) = this.sub(other, Vector3d())
inline operator fun Vector3dc.times(other: Vector3dc) = this.mul(other, Vector3d())
inline operator fun Vector3dc.div(other: Vector3dc) = this.div(other, Vector3d())

inline operator fun Vector3dc.plus(other: Double) = this.add(Vector3d(other, other, other), Vector3d())
inline operator fun Vector3dc.minus(other: Double) = this.sub(Vector3d(other, other, other), Vector3d())
inline operator fun Vector3dc.times(other: Double) = this.mul(other, Vector3d())
inline operator fun Vector3dc.div(other: Double) = this.div(other, Vector3d())
