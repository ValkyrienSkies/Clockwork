package org.valkyrienskies.clockwork.content.curiosities

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class GenericWanderliteSlab(properties: Properties) : SlabBlock(properties), IWanderliteBlock {
    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult = activate(level, pos)

    override fun attack(state: BlockState, level: Level, pos: BlockPos, player: Player) {
        if (level is ServerLevel) activate(level, pos)
        super.attack(state, level, pos, player)
    }

    override fun stepOn(level: Level, pos: BlockPos, state: BlockState, entity: Entity) {
        if (level is ServerLevel && entity is Player && !entity.isSteppingCarefully) activate(level, pos)
        super.stepOn(level, pos, state, entity)
    }
}
