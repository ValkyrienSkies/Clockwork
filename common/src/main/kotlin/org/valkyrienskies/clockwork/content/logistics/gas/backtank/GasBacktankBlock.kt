package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import com.simibubi.create.AllShapes
import com.simibubi.create.content.equipment.armor.BacktankItem
import com.simibubi.create.content.kinetics.base.HorizontalKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.storage.loot.LootParams
import net.minecraft.world.level.storage.loot.parameters.LootContextParams
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.backtank.GasBackTankItem.Companion.AirKgsToAirTicks
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.NodeBehaviorType
import org.valkyrienskies.kelvin.api.nodes.TankDuctNode
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.kelvin.serialization.NodeNBTUtil
import org.valkyrienskies.kelvin.util.INodeBlock
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.executeOrSchedule
import java.util.*

class GasBacktankBlock(properties: Properties) : HorizontalDirectionalBlock(properties), IBE<GasBacktankBlockEntity>,
    INodeBlock, IHaveDuctStats {


    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.SOUTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(HorizontalKineticBlock.HORIZONTAL_FACING)
        super.createBlockStateDefinition(builder)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        return if (context.player!!.isShiftKeyDown) defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, context.horizontalDirection.opposite) else
            defaultBlockState().setValue(HorizontalKineticBlock.HORIZONTAL_FACING, context.horizontalDirection)
    }

    override fun getBlockEntityClass(): Class<GasBacktankBlockEntity> {
        return GasBacktankBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out GasBacktankBlockEntity> {
        return ClockworkBlockEntities.GAS_BACKTANK.get()
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        if (direction.axis != Direction.Axis.Y) return false
        return super.canConnectTo(self, other, direction, level)
    }

    override fun createNode(pos: DuctNodePos): DuctNode {
        return TankDuctNode(pos = pos, behavior =  NodeBehaviorType.TANK, volume = 0.75, maxPressure = 16375049.0, maxTemperature = 1478.0, size = 3.0, heatConductivity = 1687.5, heatCapacity = 44.9)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun getCloneItemStack(blockGetter: BlockGetter, pos: BlockPos, state: BlockState): ItemStack {
        var item = asItem()
        if (item is BacktankItem.BacktankBlockItem) item = item.actualItem

        val stack = ItemStack(item, 1)
        val be = blockGetter.getBlockEntity(pos) ?: return stack

        val tag = CompoundTag()
        NodeNBTUtil.serializeNodeServer(pos.toDuctNodePos(be.level!!.dimension().location()), tag)
        tag.putFloat("Air",(tag.getDouble("kelvin:air")*AirKgsToAirTicks).toFloat())
        stack.tag = tag

        return stack
    }

    override fun getDrops(state: BlockState, params: LootParams.Builder): List<ItemStack> {
        val origin = params.getParameter(LootContextParams.ORIGIN)
        val pos = BlockPos.containing(origin)
        val level = params.level

        return listOf(getCloneItemStack(level, pos, state))
    }

    override fun use(
        state: BlockState, world: Level, pos: BlockPos, player: Player, hand: InteractionHand,
        hit: BlockHitResult
    ): InteractionResult {
        if (player.isShiftKeyDown) return InteractionResult.PASS
        if (player.mainHandItem.item is BlockItem) return InteractionResult.PASS
        if (!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty) return InteractionResult.PASS
        if (!world.isClientSide) {
            world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .75f, 1f)
            player.setItemSlot(EquipmentSlot.CHEST,getCloneItemStack(world, pos, state))
            world.destroyBlock(pos, false)
        }
        return InteractionResult.SUCCESS
    }

    override fun setPlacedBy(
        worldIn: Level,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        stack: ItemStack
    ) {
        super.setPlacedBy(worldIn, pos, state, placer, stack)
        if (worldIn.isClientSide) return

        val tag = stack.getOrCreateTag()
        //println("$pos ${worldIn.dimension().location()}")
        val network = ClockworkMod.getKelvin(worldIn)
        if (network.nodeInfo[pos.toDuctNodePos(worldIn.dimension().location())] == null) {
            nodePlace(state, worldIn, pos, Blocks.AIR.defaultBlockState(), false)
        }
        deserializeNode(pos.toDuctNodePos(worldIn.dimension().location()), tag, worldIn)

    }

    fun deserializeNode(pos: DuctNodePos, tag: CompoundTag, level: Level) {
        val network = ClockworkMod.getKelvin(level)
        val temperature = tag.getDouble("KelvinTemperature")

        for (gasResourceLocation in tag.allKeys) {
            if (":" !in gasResourceLocation) continue

            val gasType = GasTypeRegistry.GAS_TYPES[ResourceLocation(gasResourceLocation)] ?: continue
            // Extremely stupid fix. TODO: Figure out why this is needed
            //network.modGasMass(pos,gasType,tag.getDouble(gasResourceLocation))
            //network.modGasMass(pos,gasType,tag.getDouble(gasResourceLocation))
            network.addGasAtTemperature(pos, gasType, tag.getDouble(gasResourceLocation), temperature)
        }
        //network.nodeInfo[pos]!!.currentTemperature = temperature

    }

    override fun getInternalVolume(): Double {
        return 0.75
    }

    override fun getAdditionalInfoLines(): List<Component> {
        return listOf(
            Component.translatable("vs_clockwork.gas_backtank.function1").withStyle(ChatFormatting.GRAY).withStyle(
            ChatFormatting.ITALIC),

            Component.translatable("vs_clockwork.gas_backtank.function2").withStyle(ChatFormatting.GRAY).withStyle(
            ChatFormatting.ITALIC)
            )
    }

    override fun getShape(
        p_220053_1_: BlockState,
        p_220053_2_: BlockGetter,
        p_220053_3_: BlockPos,
        p_220053_4_: CollisionContext
    ): VoxelShape {
        return AllShapes.BACKTANK
    }


}
