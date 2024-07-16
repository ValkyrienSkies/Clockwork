package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.ComputerCraftAPI
import dan200.computercraft.api.peripheral.IPeripheral
import dan200.computercraft.shared.computer.core.ServerComputer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.gyro.GyroBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.clockwork.content.forces.DragController
import org.valkyrienskies.clockwork.content.forces.PropellerController
import org.valkyrienskies.clockwork.integration.cc.api.AerodynamicAPI
import org.valkyrienskies.clockwork.integration.cc.api.DragAPI
import org.valkyrienskies.clockwork.integration.cc.peripheral.AltMeterPeripheral
import org.valkyrienskies.clockwork.integration.cc.peripheral.GyroscopePeripheral
import org.valkyrienskies.clockwork.integration.cc.peripheral.PhysBearingPeripheral
import org.valkyrienskies.clockwork.integration.cc.peripheral.PropellerBearingPeripheral
import org.valkyrienskies.clockwork.platform.integration.cc.ComputerCraftUtils
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.getAttachment
import org.valkyrienskies.mod.common.getShipObjectManagingPos

object ClockworkComputerCraftIntegration {
    /**
     * This method exists solely to keep CC classes and imports out of any non-CC classes
     */
    fun integrate() {
        ComputerCraftAPI.registerPeripheralProvider(ComputerCraftUtils.getClockworkPeripheralProvider())

        //Fuck it ill add AerodynamicAPI to the Mixin
        //ComputerCraftAPI.registerAPIFactory { AerodynamicAPI() }
    }

    /**
     * This method is a helper method to bring grabbing a peripheral into Common rather than being Forge/Fabric specific
     */
    fun getPerpheral(level: Level, pos: BlockPos, direction: Direction): IPeripheral? {
        val be = level.getBlockEntity(pos)
        if (be is AltMeterBlockEntity)
            return AltMeterPeripheral(be)
        if (be is GyroBlockEntity)
            return GyroscopePeripheral(be)
        if (be is PhysBearingBlockEntity)
            return PhysBearingPeripheral(be)
        if (be is PropellerBearingBlockEntity)
            return PropellerBearingPeripheral(be, (level as ServerLevel).getShipObjectManagingPos(pos)?.getAttachment<PropellerController>())
        //TODO: Needing Speedometer to Add SpeedometerPeripheral
        return null
    }

    /**
     * This method exists as a helper to add APIs via Mixins that need special fields
     */
    fun addAPIs(computer: ServerComputer, ship: ServerShip?) {
        ship?.let{
            computer.addAPI(DragAPI(computer.apiEnvironment, it))
        }
        computer.addAPI(AerodynamicAPI())
    }
}