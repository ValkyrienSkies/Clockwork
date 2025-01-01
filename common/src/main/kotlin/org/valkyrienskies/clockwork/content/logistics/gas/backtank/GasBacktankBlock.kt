package org.valkyrienskies.clockwork.content.logistics.gas.backtank

import com.simibubi.create.content.equipment.armor.BacktankBlockEntity
import com.simibubi.create.content.equipment.armor.BacktankItem
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.content.logistics.gas.INodeBlock
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.NodeBehaviorType
import org.valkyrienskies.kelvin.api.nodes.TankDuctNode
import java.util.*

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
        println()
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun getCloneItemStack(blockGetter: BlockGetter?, pos: BlockPos?, state: BlockState?): ItemStack {
        var item = asItem()
        if (item is BacktankItem.BacktankBlockItem) item = item.actualItem

        val blockEntityOptional: Optional<GasBacktankBlockEntity> = getBlockEntityOptional(blockGetter, pos)

        val tag = CompoundTag()
        blockEntityOptional.map { be -> be.saveGasToTag(tag) }
        val stack = ItemStack(item, 1)
        stack.tag = tag

        return stack
    }
}