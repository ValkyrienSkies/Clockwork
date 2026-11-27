package org.valkyrienskies.clockwork.content.logistics.gas.pump

import com.simibubi.create.AllShapes
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel
import com.simibubi.create.foundation.block.IBE
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IConnectable
import org.valkyrienskies.clockwork.content.logistics.gas.GasTransfer
import org.valkyrienskies.clockwork.content.logistics.gas.MeteredGasInlet
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctPlacementHelper
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.clockwork.util.gui.ProductionInfo
import org.valkyrienskies.clockwork.util.gui.ProductionMethod
import org.valkyrienskies.clockwork.util.gui.ProductionType
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.nodes.PumpDuctNode
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.kelvin.util.INodeBlock

class PumpDuctBlock(properties: Properties, val mode: Mode = Mode.PUMP) :
    DirectionalKineticBlock(properties), IBE<PumpDuctBlockEntity>, ICogWheel, IHaveDuctStats, IDuct, MeteredGasInlet {

    enum class Mode { PUMP, SMART, OZONITE }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(BlockStateProperties.WATERLOGGED)
        super.createBlockStateDefinition(builder)
    }

    override fun getPreferredFacing(context: BlockPlaceContext): Direction? {
        val preferred = DuctPlacementHelper.getPreferredFacing(context) ?: return null
        // Ozonite generators only connect on FACING (FACING.opposite is the air intake), while
        // DirectionalKineticBlock sets FACING = preferred.opposite.
        return if (mode == Mode.OZONITE) preferred.opposite else preferred
    }

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(BlockStateProperties.FACING).axis
    }

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return AllShapes.SIX_VOXEL_POLE.get(state.getValue(BlockStateProperties.FACING).axis)
    }

    override fun getBlockEntityClass(): Class<PumpDuctBlockEntity> {
        return PumpDuctBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out PumpDuctBlockEntity> {
        return ClockworkBlockEntities.PUMP_DUCT.get()
    }


    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
        if (!level.isClientSide) {
            withBlockEntityDo(level, pos) { it.updatePumpNode() }
        }

        val direction = state.getValue(FACING) ?: return
        for (dir in listOf(direction, direction.opposite)) {
            withBlockEntityDo(level, pos) { it.updateConnection(it.level!!, pos, dir) }
        }
    }

    override fun onRemove(pState: BlockState, pLevel: Level, pPos: BlockPos, pNewState: BlockState, pIsMoving: Boolean) {
        if (mode == Mode.OZONITE && !pLevel.isClientSide && !pIsMoving && pState.block !== pNewState.block) {
            val generator = pLevel.getBlockEntity(pPos) as? OzoniteGeneratorBlockEntity
            if (generator != null) Containers.dropContents(pLevel, pPos, generator)
        }
        nodeRemove(pState, pLevel, pPos, pNewState, pIsMoving)
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving)
    }

    override fun use(
        state: BlockState, level: Level, pos: BlockPos, player: Player, hand: InteractionHand, hit: BlockHitResult,
    ): InteractionResult {
        val stack = player.getItemInHand(hand)
        if (mode != Mode.OZONITE || !stack.`is`(Items.LAPIS_LAZULI)) return super.use(state, level, pos, player, hand, hit)
        val generator = level.getBlockEntity(pos) as? OzoniteGeneratorBlockEntity ?: return InteractionResult.PASS
        if (!level.isClientSide) generator.loadLapis(stack, player.isCreative)
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        val state = level.getBlockState(self)
        if (state.block !is PumpDuctBlock) return false
        val facing = state.getValue(FACING)
        return if (mode == Mode.PUMP) direction.axis == facing.axis else direction == facing
    }

    override fun isMeteredInlet(state: BlockState, side: Direction): Boolean =
        mode == Mode.SMART && side == state.getValue(FACING).opposite

    override fun getProductionStats() = if (mode == Mode.OZONITE) mapOf(
        ClockworkGasses.OZONITE.resourceLocation to ProductionInfo(
            ProductionMethod.RPM, ProductionType.CONDITIONAL,
            Component.translatable("vs_clockwork.ozonite_generator.function")
        )
    ) else emptyMap()

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        neighborState: BlockState,
        level: LevelAccessor,
        currentPos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        if (state.getValue(BlockStateProperties.WATERLOGGED))
        {
            level.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(level))
        }

        return state
    }

    override fun neighborChanged(state: BlockState, level: Level, pos: BlockPos, neighborBlock: Block, neighborPos: BlockPos, movedByPiston: Boolean) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston)

        val direction = Direction.fromDelta(neighborPos.x-pos.x,neighborPos.y-pos.y,neighborPos.z-pos.z) ?: return
        if (direction.axis == state.getValue(BlockStateProperties.FACING).axis) withBlockEntityDo(level, pos) {
            it.updateConnection(it.level!!, pos, direction)
        }
    }

    override fun createNode(pos: DuctNodePos): DuctNode {
        if (mode != Mode.PUMP) {
            return PipeDuctNode(pos, volume = getInternalVolume(), maxPressure = GasTransfer.MAX_PRESSURE,
                maxTemperature = GasTransfer.MAX_TEMPERATURE, heatCapacity = 44.9)
        }
        return PumpDuctNode(
            pos = pos,
            volume = getInternalVolume(),
            maxPressure = 16375049.0,
            maxTemperature = 1478.0,
            heatCapacity = 44.9,
        )
    }


    override fun getInternalVolume(): Double {
        return ClockworkConfig.SERVER.ducts.ductVolme
    }

    override fun getMaximumPressure(): Double {
        return 0.0
    }

    override fun getMaximumTemperature(): Double {
        return 0.0
    }

    override fun getAdditionalInfoLines(): List<Component> {
        if (mode == Mode.SMART) return listOf(Component.translatable("vs_clockwork.smart_gas_pump.function"))
        if (mode == Mode.OZONITE) return listOf(Component.translatable("vs_clockwork.ozonite_generator.function"))
        return listOf(
            Component.translatable("vs_clockwork.pump_duct.function1").withStyle(ChatFormatting.GRAY).withStyle(
            ChatFormatting.ITALIC),

            Component.translatable("vs_clockwork.pump_duct.function2").withStyle(ChatFormatting.GRAY).withStyle(
            ChatFormatting.ITALIC)
        )
    }


}
