package org.valkyrienskies.clockwork.content.logistics.gas.combustor

import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.GasTransfer
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.content.physicalities.steering.DrivingControlLinks
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos

class GasCombustorBlock(properties: Properties) :
    HorizontalDirectionalBlock(properties), IBE<GasCombustorBlockEntity>, IDuct, IHaveDuctStats {

    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState =
        defaultBlockState().setValue(FACING, context.horizontalDirection.opposite)

    override fun rotate(state: BlockState, rotation: Rotation): BlockState =
        state.setValue(FACING, rotation.rotate(state.getValue(FACING)))

    override fun mirror(state: BlockState, mirror: Mirror): BlockState =
        rotate(state, mirror.getRotation(state.getValue(FACING)))

    override fun getBlockEntityClass(): Class<GasCombustorBlockEntity> = GasCombustorBlockEntity::class.java

    override fun getBlockEntityType(): BlockEntityType<out GasCombustorBlockEntity> =
        ClockworkBlockEntities.GAS_COMBUSTOR.get()

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        val state = level.getBlockState(self)
        if (state.block != this) return false
        return direction.axis == state.getValue(FACING).axis
    }

    override fun getAdditionalInfoLines(): List<Component> =
        listOf(Component.translatable("vs_clockwork.gas_combustor.function"))

    override fun getInternalVolume(): Double = ClockworkConfig.SERVER.ducts.ductVolme

    override fun getMaximumPressure(): Double = GasTransfer.MAX_PRESSURE

    override fun getMaximumTemperature(): Double = COMBUSTOR_MAX_TEMPERATURE

    override fun createNode(pos: DuctNodePos): DuctNode =
        CombustorDuctNode(
            pos = pos,
            volume = getInternalVolume(),
            maxPressure = getMaximumPressure(),
            maxTemperature = getMaximumTemperature(),
            heatCapacity = 44.9,
        )

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
        if (!level.isClientSide) {
            val nodePos = pos.toDuctNodePos(level.dimension().location())
            (ClockworkMod.getKelvin(level).getNodeAt(nodePos) as? CombustorDuctNode)?.facing = state.getValue(FACING)
            withBlockEntityDo(level, pos) {
                it.syncNodeState()
                it.refreshConnections(level, pos)
            }
        }
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        neighbor: Block,
        neighborPos: BlockPos,
        moved: Boolean,
    ) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, moved)
        if (level.isClientSide) return
        val delta = neighborPos.subtract(pos)
        val dir = Direction.fromDelta(delta.x, delta.y, delta.z) ?: return
        if (dir.axis == state.getValue(FACING).axis) {
            withBlockEntityDo(level, pos) { it.updateConnection(level, pos, dir) }
        }
    }

    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hit: BlockHitResult,
    ): InteractionResult {
        if (!player.getItemInHand(hand).isEmpty) return InteractionResult.PASS
        if (level is ServerLevel) {
            withBlockEntityDo(level, pos) { it.triggerManualSpark(level) }
        }
        return InteractionResult.sidedSuccess(level.isClientSide)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (level is ServerLevel && state.block != newState.block) {
            DrivingControlLinks.get(level).endpointRemoved(pos)
        }
        nodeRemove(state, level, pos, newState, isMoving)
        IBE.onRemove(state, level, pos, newState)
        super.onRemove(state, level, pos, newState, isMoving)
    }

    companion object {
        const val COMBUSTOR_MAX_TEMPERATURE = 1900.0
    }
}
