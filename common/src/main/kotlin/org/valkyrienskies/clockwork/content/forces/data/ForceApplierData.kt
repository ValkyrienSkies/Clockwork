package org.valkyrienskies.clockwork.content.forces.data

import org.joml.Vector3ic

interface ForceApplierData<in T> {

    val position: Vector3ic?

    fun updateData(data: T) {
        throw IllegalArgumentException("Invalid data type")
    }
}