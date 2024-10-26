package org.valkyrienskies.clockwork.content.curiosities.tools.screwdriver

import com.simibubi.create.AllSoundEvents
import com.simibubi.create.Create
import net.minecraft.core.BlockPos
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

interface IScrewdrivable {

    fun onScrewdrived(state: BlockState, context: UseOnContext): InteractionResult

    fun onSneakScrewdrived(state: BlockState, context: UseOnContext): InteractionResult

    fun playUnscrewSound(world: Level, pos: BlockPos) {
        AllSoundEvents.WRENCH_REMOVE.playOnServer(world, pos, 1f, Create.RANDOM.nextFloat() * .5f + .5f)
    }

    fun playScrewSound(world: Level, pos: BlockPos) {
        AllSoundEvents.WRENCH_ROTATE.playOnServer(world, pos, 1f, Create.RANDOM.nextFloat() + .5f)
    }
}