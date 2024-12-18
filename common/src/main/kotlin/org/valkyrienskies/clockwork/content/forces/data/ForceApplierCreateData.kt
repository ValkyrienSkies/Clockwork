package org.valkyrienskies.clockwork.content.forces.data

import org.joml.Vector3dc
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.generic.IForceApplierBE

interface ForceApplierCreateData<out T> {
    val position: Vector3ic

    fun fromCreateData(): T {
        throw IllegalArgumentException("Invalid data type")
    }

    companion object {
        fun <B: IForceApplierBE<*, *, *, *>> fromBlockEntity(be: B): ForceApplierCreateData<*>? {
            throw IllegalArgumentException("Invalid data type")
        }
    }
}
