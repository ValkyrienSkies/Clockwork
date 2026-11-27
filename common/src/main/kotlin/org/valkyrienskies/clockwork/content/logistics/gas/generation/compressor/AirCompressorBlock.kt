package org.valkyrienskies.clockwork.content.logistics.gas.generation.compressor

import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctPlacementHelper
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.clockwork.util.gui.ProductionInfo
import org.valkyrienskies.clockwork.util.gui.ProductionMethod
import org.valkyrienskies.clockwork.util.gui.ProductionType
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.util.INodeBlock

class AirCompressorBlock(properties: Properties?) : DirectionalKineticBlock(properties), INodeBlock, IBE<AirCompressorBlockEntity>, IHaveDuctStats {

    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP))
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState {
        if (context.player?.isShiftKeyDown == true) {
            return defaultBlockState().setValue(FACING, context.nearestLookingDirection)
        }
        val preferredShaft = super.getPreferredFacing(context)
        if (preferredShaft != null) {
            return defaultBlockState().setValue(FACING, preferredShaft.opposite)
        }

        val defaultFacing = context.nearestLookingDirection.opposite
        val preferredDuctAxis = DuctPlacementHelper.getPreferredAxis(context, requireActiveConnector = true)
        val facing = DuctPlacementHelper.resolvePerpendicularFacing(
            defaultFacing = defaultFacing,
            preferredDuctAxis = preferredDuctAxis,
            nearestLookingDirections = context.nearestLookingDirections
        ) { dir -> context.level.getBlockState(context.clickedPos.relative(dir)).isAir }
        return defaultBlockState().setValue(FACING, facing)
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        val state = level.getBlockState(self)
        if (state.block !is AirCompressorBlock) return false
        return direction.axis != state.getValue(FACING).axis
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)
        nodePlace(state, level, pos, oldState, isMoving)
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        nodeRemove(state, level, pos, newState, isMoving)
        super.onRemove(state, level, pos, newState, isMoving)
    }

    override fun getRotationAxis(state: BlockState): Direction.Axis {
        return state.getValue(FACING).axis
    }

    override fun areStatesKineticallyEquivalent(oldState: BlockState, newState: BlockState): Boolean {
        return oldState == newState
    }

    override fun getBlockEntityClass(): Class<AirCompressorBlockEntity> {
        return AirCompressorBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out AirCompressorBlockEntity> {
        return ClockworkBlockEntities.AIR_COMPRESSOR.get()
    }

    override fun hasShaftTowards(world: LevelReader?, pos: BlockPos?, state: BlockState, face: Direction): Boolean {
        return face == state.getValue(FACING).opposite
    }

    override fun getProductionStats(): Map<ResourceLocation, ProductionInfo> {
        val stats = mutableMapOf<ResourceLocation, ProductionInfo>()
        val airLocation = ResourceLocation(KelvinMod.MOD_ID, "air")
        val heliumLocation = ClockworkGasses.HELIUM.resourceLocation
        stats[airLocation] = ProductionInfo(
            ProductionMethod.RPM,
            ProductionType.ALWAYS
        )
        stats[heliumLocation] = ProductionInfo(
            ProductionMethod.RPM,
            ProductionType.CONDITIONAL,
            Component.translatable("vs_clockwork.production_condition.air_compressor_helium").append(Component.literal("${ClockworkConfig.SERVER.airCompressor.airCompressorHeliumAirDensity} kg/m³"))
        )
        return stats
    }

}
