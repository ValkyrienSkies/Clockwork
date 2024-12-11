package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.simibubi.create.foundation.block.IBE
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType

class BladeControllerBlock(properties: Properties) : DirectionalBlock(properties), IBE<BladeControllerBlockEntity> {
    override fun getBlockEntityClass(): Class<BladeControllerBlockEntity> {
        TODO("Not yet implemented")
    }

    override fun getBlockEntityType(): BlockEntityType<out BladeControllerBlockEntity> {
        TODO("Not yet implemented")
    }
}