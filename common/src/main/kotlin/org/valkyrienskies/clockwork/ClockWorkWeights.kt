package org.valkyrienskies.clockwork

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.core.game.VSBlockType
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.BlockStateInfoProvider

object ClockWorkWeights : BlockStateInfoProvider {
    override val priority: Int
        get() = 200

    override fun getBlockStateMass(blockState: BlockState): Double? {
        if (blockState.block == ClockWorkBlocks.BALLAST.get()) {
            return ClockWorkConfig.SERVER.ballastWeight + (ClockWorkConfig.SERVER.ballastNoWeight - ClockWorkConfig.SERVER.ballastWeight) * (
                    (
                            blockState.getValue(
                                BlockStateProperties.POWER
                            ) + 1
                            ) / 16.0
                    )
        }

        return null
    }

    override fun getBlockStateType(blockState: BlockState): VSBlockType? {
        return null
    }

    fun register() {
        Registry.register(BlockStateInfo.REGISTRY, ResourceLocation(ClockWorkMod.MOD_ID, "ballast"), this)
    }
}
