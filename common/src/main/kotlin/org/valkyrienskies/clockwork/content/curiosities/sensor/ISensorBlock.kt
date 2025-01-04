package org.valkyrienskies.clockwork.content.curiosities.sensor

import com.simibubi.create.content.equipment.wrench.IWrenchable
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.IntegerProperty
import java.util.*

interface ISensorBlock : IWrenchable {
    companion object {
        val POWER: IntegerProperty = BlockStateProperties.POWER
    }

    fun updatePower(state: BlockState, level: ServerLevel, pos: BlockPos, random: Random): Int
}