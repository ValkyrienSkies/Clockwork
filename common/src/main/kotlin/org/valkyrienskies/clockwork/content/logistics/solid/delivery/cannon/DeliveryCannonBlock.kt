package org.valkyrienskies.clockwork.content.logistics.solid.delivery.cannon

import com.simibubi.create.AllBlocks
import com.simibubi.create.content.kinetics.base.KineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlockEntity

class DeliveryCannonBlock(properties: Properties) : Block(properties), IBE<DeliveryCannonBlockEntity> {

    override fun getBlockEntityClass(): Class<DeliveryCannonBlockEntity> {
        return DeliveryCannonBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out DeliveryCannonBlockEntity> {
        return ClockworkBlockEntities.DELIVERY_CANNON.get()
    }

    override fun canSurvive(state: BlockState?, level: LevelReader, pos: BlockPos): Boolean {
        return level.getBlockState(pos.below()).block == AllBlocks.DEPOT
    }

}