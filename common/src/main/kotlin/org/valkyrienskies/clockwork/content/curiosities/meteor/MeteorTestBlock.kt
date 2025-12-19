package org.valkyrienskies.clockwork.content.curiosities.meteor

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.kelvin.util.KelvinExtensions.toVector3i

class MeteorTestBlock(properties: Properties) : Block(properties) {

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        if (level.isClientSide) return

        MeteorStorm.assembleMeteor(level as ServerLevel, pos.offset(0,30,0).toVector3i())
        //MeteorGenerator.generate(level as ServerLevel, pos.offset(25,25,25), 0.035, 10, 3)
    }


}