package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipObjectManagingPos


class GyroBlockEntity(typeIn: BlockEntityType<GyroBlockEntity>, pos: BlockPos, state: BlockState) :
    BlockEntity(typeIn, pos, state) {

    private val ship: ServerShip? get() = (level as ServerLevel).getShipObjectManagingPos(this.blockPos)
    private val control: GyroShipControl? get() = ship?.getAttachment(GyroShipControl::class.java)

    fun tick() {
        control?.ship = ship
    }
}