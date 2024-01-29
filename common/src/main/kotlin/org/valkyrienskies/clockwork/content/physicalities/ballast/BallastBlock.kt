package org.valkyrienskies.clockwork.content.physicalities.ballast

import com.simibubi.create.content.logistics.vault.ItemVaultBlockEntity
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.platform.SharedValues

class BallastBlock(properties: Properties) : Block(properties), IBE<BallastBlockEntity> {

    override fun getBlockEntityClass(): Class<BallastBlockEntity> {
        return BallastBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out BallastBlockEntity> {
        return SharedValues.ballast.get()
    }

}