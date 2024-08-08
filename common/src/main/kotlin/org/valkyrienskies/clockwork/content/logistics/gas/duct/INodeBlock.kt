package org.valkyrienskies.clockwork.content.logistics.gas.duct

import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.GasHeatLevel
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlock.Companion.GAS_HEAT_LEVEL
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.DOWN_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.EAST_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.NORTH_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.SOUTH_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.UP_CONNECTION
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct.Companion.WEST_CONNECTION
import org.valkyrienskies.clockwork.kelvin.api.DuctNetwork
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.mod.common.util.toJOMLD

interface INodeBlock : IDuct {
    fun _createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(
            NORTH_CONNECTION,
            EAST_CONNECTION,
            SOUTH_CONNECTION,
            WEST_CONNECTION,
            UP_CONNECTION,
            DOWN_CONNECTION,
            GAS_HEAT_LEVEL
        )
    }

    fun ductConnectionsDefault(defaultBlockState: BlockState): BlockState {
        return defaultBlockState
            .setValue(NORTH_CONNECTION, DuctConnectionType.NONE)
            .setValue(EAST_CONNECTION, DuctConnectionType.NONE)
            .setValue(SOUTH_CONNECTION, DuctConnectionType.NONE)
            .setValue(WEST_CONNECTION, DuctConnectionType.NONE)
            .setValue(UP_CONNECTION, DuctConnectionType.NONE)
            .setValue(DOWN_CONNECTION, DuctConnectionType.NONE)
            .setValue(GAS_HEAT_LEVEL, GasHeatLevel.COOL)
    }

    fun _onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide) {
            if (state.isAir || state.block !is INodeBlock || oldState.`is`(state.block)) {
                return
            }
            ClockworkMod.getKelvin().addNode(pos.toJOMLD(), createNode(pos.toJOMLD(), ClockworkMod.getKelvin()))
        }
    }

    fun _onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide) {
            if (newState.isAir || newState.block !is INodeBlock) {
                ClockworkMod.getKelvin().removeNode(pos.toJOMLD())
            }
        }
    }

    fun createNode(pos: DuctNodePos, network: DuctNetwork): PipeDuctNode
}