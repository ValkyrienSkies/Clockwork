package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity

val PERIPHERALS = mapOf<BlockEntityType<*>, (BlockEntity, Direction?) -> IPeripheral>(
    ClockworkBlockEntities.PHYS_BEARING.get() to {be, _ -> PhysBearingPeripheral(be as PhysBearingBlockEntity)}
)

fun getPeripheralCommon(be: BlockEntity, direction: Direction?): IPeripheral? = PERIPHERALS[be.type]?.invoke(be, direction)