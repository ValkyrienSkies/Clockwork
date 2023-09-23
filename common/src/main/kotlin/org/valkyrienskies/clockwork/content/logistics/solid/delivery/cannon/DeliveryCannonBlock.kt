package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import com.simibubi.create.content.kinetics.base.KineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class DeliveryCannonBlock(properties: Properties) : KineticBlock(properties), IBE<DeliveryCannonBlockEntity> {
    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return Direction.Axis.Y
    }

    override fun hasShaftTowards(world: LevelReader, pos: BlockPos, state: BlockState, face: Direction): Boolean {
        return face == Direction.DOWN
    }

    override fun getBlockEntityClass(): Class<DeliveryCannonBlockEntity> {
        return DeliveryCannonBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out DeliveryCannonBlockEntity> {
        return ClockworkBlockEntities.DELIVERY_CANNON.get()
    }
}