package org.valkyrienskies.clockwork.fabric

import dan200.computercraft.api.peripheral.PeripheralLookup
import org.valkyrienskies.clockwork.integration.cc.PERIPHERALS

fun registerPeripheralLookup() {
    PERIPHERALS.forEach { type, factory ->
        PeripheralLookup.get().registerForBlockEntity(factory, type)
    }
}