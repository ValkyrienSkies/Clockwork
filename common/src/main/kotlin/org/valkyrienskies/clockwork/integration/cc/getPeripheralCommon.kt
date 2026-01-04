package org.valkyrienskies.clockwork.integration.cc

import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.Direction
import net.minecraft.world.level.block.entity.BlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity

fun getPeripheralCommon(be: BlockEntity, direction: Direction?): IPeripheral? {
    return when(be) {
        is PhysBearingBlockEntity -> PhysBearingPeripheral(be)
        else -> null
    }
}