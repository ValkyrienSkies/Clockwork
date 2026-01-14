package org.valkyrienskies.clockwork.content.logistics.gas.generation.steam_generator

import com.simibubi.create.content.fluids.tank.FluidTankBlock
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock
import com.simibubi.create.foundation.block.IBE
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.AttachFace
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import org.valkyrienskies.clockwork.ClockworkGasses
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.util.gui.IHaveDuctStats
import org.valkyrienskies.clockwork.util.gui.ProductionInfo
import org.valkyrienskies.clockwork.util.gui.ProductionMethod
import org.valkyrienskies.clockwork.util.gui.ProductionType
import org.valkyrienskies.kelvin.util.INodeBlock

class SteamGeneratorBlock(properties: Properties) : FaceAttachedHorizontalDirectionalBlock(properties), IBE<SteamGeneratorBlockEntity>, IDuct, IHaveDuctStats {

    init {
        registerDefaultState(defaultBlockState()
            .setValue(FACE, AttachFace.FLOOR)
            .setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block?, BlockState?>) {
        super.createBlockStateDefinition(pBuilder.add(FACE, FACING))
    }

    override fun canSurvive(pState: BlockState, pLevel: LevelReader, pPos: BlockPos): Boolean {
        return canBlockAttach(pLevel, pPos, getConnectedDirection(pState).getOpposite())
    }

    override fun onPlace(pState: BlockState, pLevel: Level, pPos: BlockPos, pOldState: BlockState, pIsMoving: Boolean) {
        println(pState)
        FluidTankBlock.updateBoilerState(pState, pLevel, pPos.relative(SteamEngineBlock.getFacing(pState).opposite))
        nodePlace(pState, pLevel, pPos, pOldState, pIsMoving)
    }

    override fun onRemove(pState: BlockState, pLevel: Level, pPos: BlockPos, pNewState: BlockState, pIsMoving: Boolean) {
        nodeRemove(pState, pLevel, pPos, pNewState, pIsMoving)
        if (pState.hasBlockEntity() && (!pState.`is`(pNewState.getBlock()) || !pNewState.hasBlockEntity())) pLevel.removeBlockEntity(pPos)
        FluidTankBlock.updateBoilerState(pState, pLevel, pPos.relative(SteamEngineBlock.getFacing(pState).getOpposite()))

    }

    override fun getBlockEntityClass(): Class<SteamGeneratorBlockEntity> {
        return SteamGeneratorBlockEntity::class.java
    }

    override fun getBlockEntityType(): BlockEntityType<out SteamGeneratorBlockEntity> {
        return ClockworkBlockEntities.STEAM_GENERATOR.get()
    }

    override fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        if (direction != getFacing(level.getBlockState(self))) return false

        return super<IDuct>.canConnectTo(self, other, direction, level)
    }

    override fun getProductionStats(): Map<ResourceLocation, ProductionInfo> {
        val stats = mutableMapOf<ResourceLocation, ProductionInfo>()
        val steamLocation = ClockworkGasses.STEAM.resourceLocation
        stats[steamLocation] = ProductionInfo(
            ProductionMethod.BOILER,
            ProductionType.ALWAYS
        )
        return stats
    }

    companion object {
        @JvmStatic
        fun getFacing(sideState: BlockState): Direction {
            return getConnectedDirection(sideState)
        }

        @JvmStatic
        fun canBlockAttach(reader: LevelReader, pos: BlockPos, direction: Direction): Boolean {
            return reader.getBlockState(pos.relative(direction)).block is FluidTankBlock
        }
    }
}
