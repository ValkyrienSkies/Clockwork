package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import com.simibubi.create.AllEnchantments
import com.simibubi.create.content.equipment.armor.BacktankBlockEntity
import com.simibubi.create.content.equipment.armor.BacktankItem
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtUtils
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.gas.INodeBlock
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.NodeBehaviorType
import org.valkyrienskies.kelvin.api.nodes.TankDuctNode
import org.valkyrienskies.kelvin.serialization.NodeNBTUtil
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import java.util.*
import java.util.function.Consumer

class GasBacktankBlock(properties: Properties) : HorizontalDirectionalBlock(properties), IBE<GasBacktankBlockEntity>, INodeBlock {
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
        return TankDuctNode(pos = pos, behavior =  NodeBehaviorType.TANK, volume = 0.05, maxPressure = 16375049.0, maxTemperature = 1478.0, size = 3.0)
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun getCloneItemStack(blockGetter: BlockGetter?, pos: BlockPos?, state: BlockState?): ItemStack {
        var item = asItem()
        if (item is BacktankItem.BacktankBlockItem) item = item.actualItem

        val blockEntityOptional: Optional<GasBacktankBlockEntity> = getBlockEntityOptional(blockGetter, pos)

        val tag = CompoundTag()
        println(tag)
        if (pos != null) NodeNBTUtil.serializeNodeServer(pos.toDuctNodePos(), tag)
        val stack = ItemStack(item, 1)
        stack.tag = tag

        return stack
    }


    override fun use(
        state: BlockState?, world: Level, pos: BlockPos?, player: Player?, hand: InteractionHand?,
        hit: BlockHitResult?
    ): InteractionResult {
        if (player == null) return InteractionResult.PASS
        if (player.isShiftKeyDown) return InteractionResult.PASS
        if (player.mainHandItem.item is BlockItem) return InteractionResult.PASS
        if (!player.getItemBySlot(EquipmentSlot.CHEST).isEmpty) return InteractionResult.PASS
        if (!world.isClientSide) {
            world.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, .75f, 1f)
            player.setItemInHand(hand,getCloneItemStack(world, pos, state))
            world.destroyBlock(pos, false)
        }
        return InteractionResult.SUCCESS
    }

    override fun setPlacedBy(
        worldIn: Level,
        pos: BlockPos?,
        state: BlockState?,
        placer: LivingEntity?,
        stack: ItemStack?
    ) {
        super.setPlacedBy(worldIn, pos, state, placer, stack)
        if (worldIn.isClientSide) return
        if (stack == null) return
        val tag = stack.getOrCreateTag()
        if (pos == null) return
        println(tag)
        NodeNBTUtil.deserializeNodeServer(pos.toDuctNodePos(), tag)

    }
}