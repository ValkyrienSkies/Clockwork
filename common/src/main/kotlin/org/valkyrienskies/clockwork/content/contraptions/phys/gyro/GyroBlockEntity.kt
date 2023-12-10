package org.valkyrienskies.clockwork.content.contraptions.phys.gyro

import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.mod.common.getShipObjectManagingPos


class GyroBlockEntity(typeIn: BlockEntityType<GyroBlockEntity>, pos: BlockPos, state: BlockState) :
    KineticBlockEntity(typeIn, pos, state) {

    private val ship: ServerShip? get() = (level as ServerLevel).getShipObjectManagingPos(this.blockPos)
    private val control: GyroShipControl? get() = ship?.getAttachment(GyroShipControl::class.java)

    override fun tick() {
        super.tick()
        if (level is ServerLevel) {
            control?.ship = ship
            control?.speed = getSpeed()
        }


        println("Speed: ${getSpeed()}")
    }


}