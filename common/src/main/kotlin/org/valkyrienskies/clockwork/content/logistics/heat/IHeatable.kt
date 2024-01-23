package org.valkyrienskies.clockwork.content.logistics.heat

import net.minecraft.core.Direction
import java.util.EnumMap

interface IHeatable {

    fun canTransferHeat(direction: Direction): Boolean

    fun getAttachedNeighbors(): EnumMap<Direction, IHeatable>
    fun getNeighborFlowRate(direction: Direction): Int
    fun getNeighborFlowDir(direction: Direction): MutableSet<Direction>

    fun isNeighborPipe(direction: Direction): Boolean
}