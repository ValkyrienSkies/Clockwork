package org.valkyrienskies.clockwork.content.logistics.gas

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.duct.DuctBlock
import org.valkyrienskies.clockwork.content.logistics.gas.duct.IDuct
import org.valkyrienskies.clockwork.kelvin.api.DuctNetwork
import org.valkyrienskies.clockwork.kelvin.api.DuctNode
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.mod.common.util.toJOMLD

interface INodeBlock : IDuct {

    fun nodePlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide) {
            if (state.isAir || state.block !is INodeBlock || oldState.`is`(state.block)) {
                return
            }
            ClockworkMod.getKelvin().addNode(pos.toJOMLD(), createNode(pos.toJOMLD(), ClockworkMod.getKelvin()))
        }
    }

    fun nodeRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide) {
            if (newState.isAir || newState.block !is INodeBlock) {
                ClockworkMod.getKelvin().removeNode(pos.toJOMLD())
            }
        }
    }


    fun createNode(pos: DuctNodePos, network: DuctNetwork): DuctNode {
        return PipeDuctNode.DEFAULT(pos, network)
    }
    


}