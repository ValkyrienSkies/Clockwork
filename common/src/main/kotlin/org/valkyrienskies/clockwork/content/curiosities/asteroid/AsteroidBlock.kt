package org.valkyrienskies.clockwork.content.curiosities.asteroid

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState

class AsteroidBlock(properties: Properties) : Block(properties) {

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        if (level.isClientSide) return

        MeteoroidGenerator.generate(level as ServerLevel, pos, 0.035, 10, 3)

    }


}