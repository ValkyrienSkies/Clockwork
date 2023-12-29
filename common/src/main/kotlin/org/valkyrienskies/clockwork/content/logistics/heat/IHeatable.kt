package org.valkyrienskies.clockwork.content.logistics.heat

import net.minecraft.core.Direction

interface IHeatable {
    var heat: Int
    val maxHeat: Int
    var flowDir: MutableSet<Direction>
    val flowRate: Int

    fun canTransferHeat(direction: Direction): Boolean

    fun getAttachedNeighbors(): List<IHeatable>
    fun getNeighborFlowRate(direction: Direction): Int
    fun getNeighborFlowDir(direction: Direction): MutableSet<Direction>

    fun isNeighborPipe(direction: Direction): Boolean
}