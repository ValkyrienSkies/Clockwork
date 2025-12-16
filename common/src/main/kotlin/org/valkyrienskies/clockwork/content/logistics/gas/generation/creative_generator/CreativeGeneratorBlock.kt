package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import com.simibubi.create.foundation.block.IBE
import net.createmod.catnip.gui.ScreenOpener
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.ChatFormatting
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.util.INodeBlock
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator


class CreativeGeneratorBlock(properties: Properties) : Block(properties), INodeBlock, IBE<CreativeGeneratorBlockEntity>, IHaveDuctStats {


    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) withBlockEntityDo(level, pos) { be: CreativeGeneratorBlockEntity -> displayScreen(be, player) }
        return  InteractionResult.SUCCESS
    }

    @Environment(value = EnvType.CLIENT)
    private fun displayScreen(be: CreativeGeneratorBlockEntity, player: Player) {
        if (player is LocalPlayer) ScreenOpener.open(CreativeGeneratorScreen(be))
    }






    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }


    override fun getBlockEntityClass(): Class<CreativeGeneratorBlockEntity> {
        return  CreativeGeneratorBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out CreativeGeneratorBlockEntity> {
        return ClockworkBlockEntities.CREATIVE_GENERATOR.get()
    }

    // Todo: This is sus as fuck but like it'd be too much of a mess with the normal production tags
    override fun getAdditionalInfoLines(): List<Component> {
        val list = ArrayList<Component>()

        list += Component.translatable("vs_clockwork.duct_stats.is_producer").withStyle(ChatFormatting.GOLD)
        list += Component.translatable("vs_clockwork.creative_generator.function").withStyle(ChatFormatting.WHITE).withStyle(ChatFormatting.ITALIC)

        return list
    }



}
