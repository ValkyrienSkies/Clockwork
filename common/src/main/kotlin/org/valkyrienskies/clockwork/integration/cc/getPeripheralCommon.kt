package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity
import org.valkyrienskies.clockwork.content.curiosities.altmeter.AltMeterBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle.GasNozzleBlockEntity

val PERIPHERALS = mapOf<BlockEntityType<*>, (BlockEntity, Direction?) -> IPeripheral>(
    ClockworkBlockEntities.PHYS_BEARING.get() to {be, _ -> PhysBearingPeripheral(be as PhysBearingBlockEntity)},
    ClockworkBlockEntities.GAS_NOZZLE.get() to {be, _ -> GasNozzlePeripheral(be as GasNozzleBlockEntity)},
    ClockworkBlockEntities.FLAP_BEARING.get() to {be, _ -> FlapBearingPeripheral(be as FlapBearingBlockEntity)},
    ClockworkBlockEntities.SMART_FLAP_BEARING.get() to {be, _ -> FlapBearingPeripheral(be as FlapBearingBlockEntity)},
    ClockworkBlockEntities.ALT_METER.get() to {be, _ -> AltMeterPeripheral(be as AltMeterBlockEntity)}
)

fun getPeripheralCommon(be: BlockEntity, direction: Direction?): IPeripheral? = PERIPHERALS[be.type]?.invoke(be, direction)