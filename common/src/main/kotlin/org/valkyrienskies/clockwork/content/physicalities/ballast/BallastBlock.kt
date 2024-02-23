package org.valkyrienskies.clockwork.content.physicalities.ballast

import com.simibubi.create.foundation.block.IBE
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.clockwork.platform.SharedValues

class BallastBlock(properties: Properties) : Block(properties), IBE<BallastBlockEntity> {

    override fun getBlockEntityClass(): Class<BallastBlockEntity> {
        return BallastBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out BallastBlockEntity> {
        return SharedValues.ballast.get()
    }

}