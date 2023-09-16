package org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute

import com.simibubi.create.foundation.block.IBE
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.clockwork.ClockworkBlockEntities

class DeliveryChuteBlock(properties: Properties) : Block(properties), IBE<DeliveryChuteBlockEntity> {
    override fun getBlockEntityClass(): Class<DeliveryChuteBlockEntity> {
        return DeliveryChuteBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out DeliveryChuteBlockEntity> {
        return ClockworkBlockEntities.DELIVERY_CHUTE.get()
    }
}