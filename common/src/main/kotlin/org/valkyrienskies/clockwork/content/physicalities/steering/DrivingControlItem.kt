package org.valkyrienskies.clockwork.content.physicalities.steering

import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.hinge.HingeBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalPistonBlock
import org.valkyrienskies.clockwork.content.contraptions.phys.linear.PhysicalGantryBlock
import org.valkyrienskies.clockwork.content.physicalities.gas_thruster.GasThrusterBlock
import org.valkyrienskies.clockwork.content.physicalities.wheel.WheelHubBlock

class DrivingControlItem(block: Block, properties: Properties) : BlockItem(block, properties) {
    private fun accepts(target: Block) = when (block) {
        is HandbrakeBlock -> brakeTarget(target) || target is SteeringWheelBlock
        is ThrottleLeverBlock -> throttleTarget(target) || target is SteeringWheelBlock
        else -> steeringTarget(target)
    }

    internal fun linkSelected(level: Level, pos: BlockPos, placer: LivingEntity?, stack: ItemStack) {
        if (level !is ServerLevel) return
        val targets = selected(stack, level)
        writeSelection(stack, level, emptyList())
        if (targets.isEmpty()) return
        val links = DrivingControlLinks.get(level)
        val valid = targets.filter { level.hasChunkAt(it) && accepts(level.getBlockState(it).block) &&
            (placer !is Player || level.mayInteract(placer, it)) }
        for (target in valid) {
            if (level.getBlockState(target).block is SteeringWheelBlock) links.bind(pos, target)
            else links.bind(target, pos, block is HandbrakeBlock)
        }
        (placer as? Player)?.displayClientMessage(Component.translatable("block.vs_clockwork.steering_wheel.linked", valid.size), true)
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val level = context.level
        val pos = context.clickedPos
        if (!accepts(level.getBlockState(pos).block)) return super.useOn(context)
        val player = context.player ?: return InteractionResult.PASS
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return InteractionResult.FAIL
        if (!level.isClientSide) {
            val stack = context.itemInHand
            val hubs = selected(stack, level).toMutableSet()
            val added = hubs.add(pos)
            if (!added) hubs.remove(pos)
            else if (level.getBlockState(pos).block is SteeringWheelBlock)
                hubs.removeIf { it != pos && level.hasChunkAt(it) && level.getBlockState(it).block is SteeringWheelBlock }
            writeSelection(stack, level, hubs)
            val key = if (added) "selected" else "deselected"
            player.displayClientMessage(Component.translatable("block.vs_clockwork.steering_wheel.$key", hubs.size), true)
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    companion object {
        internal fun steeringTarget(block: Block) =
            block is WheelHubBlock || block is HandbrakeBlock || block is GasThrusterBlock || block is ThrottleLeverBlock

        internal fun throttleTarget(block: Block) = block === ClockworkBlocks.GAS_COMBUSTOR.get()

        internal fun brakeTarget(block: Block) = block is WheelHubBlock || block is PhysBearingBlock || block is HingeBlock ||
            block is PhysicalPistonBlock || block is PhysicalGantryBlock

        internal fun selected(stack: ItemStack, level: Level): List<BlockPos> {
            val tag = stack.tag ?: return emptyList()
            if (tag.getString("HubDimension") != level.dimension().location().toString()) return emptyList()
            return tag.getLongArray("SelectedHubs").map(BlockPos::of)
        }

        internal fun writeSelection(stack: ItemStack, level: Level, hubs: Collection<BlockPos>) {
            if (hubs.isEmpty()) {
                stack.removeTagKey("HubDimension")
                stack.removeTagKey("SelectedHubs")
                return
            }
            stack.orCreateTag.putString("HubDimension", level.dimension().location().toString())
            stack.orCreateTag.putLongArray("SelectedHubs", hubs.map(BlockPos::asLong).toLongArray())
        }
    }
}
