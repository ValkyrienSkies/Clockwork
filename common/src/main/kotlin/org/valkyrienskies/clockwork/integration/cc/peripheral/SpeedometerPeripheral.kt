package org.valkyrienskies.clockwork.integration.cc.peripheral

import dan200.computercraft.api.peripheral.IPeripheral

class SpeedometerPeripheral(): IPeripheral {
    override fun equals(peripheral: IPeripheral?) = peripheral is SpeedometerPeripheral

    override fun getType() = "speedometer"

    //TODO: I kinda need the Speedometer to do... this
}