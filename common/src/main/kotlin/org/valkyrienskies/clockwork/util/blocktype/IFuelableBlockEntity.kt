package org.valkyrienskies.clockwork.util.blocktype

interface IFuelableBlockEntity {
    val fuelQuality: LiquidFuelType
    val remainingFuel: Int
    val drainRate: Int
}