package org.valkyrienskies.clockwork.registry

interface RegistrySupplier<T> {

    val name: String
    fun get(): T

}