package org.valkyrienskies.clockwork.content.logistics.gas

import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.api.DuctNode
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.api.dimensionId

interface INodeBlock {

    fun nodePlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide) {
            if (state.isAir || state.block !is INodeBlock || oldState.`is`(state.block)) {
                return
            }
            ClockworkMod.getKelvin().addNode(pos.toDuctNodePos(level.dimension().location()), createNode(pos.toDuctNodePos(level.dimension().location())))
        } else {
            //nodeAddClient(state, level as ClientLevel, pos)
        }
    }

    fun nodeAddClient(state: BlockState, level: ClientLevel, pos: BlockPos) {
        KelvinMod.getClientKelvin().addNode(
            pos.toDuctNodePos(level.dimension().location()),
            createNode(pos.toDuctNodePos(level.dimension().location()))
        )
    }

    fun nodeRemoveClient(state: BlockState, level: ClientLevel, pos: BlockPos) {
        KelvinMod.getClientKelvin().removeNode(pos.toDuctNodePos(level.dimension().location()))
    }

    fun nodeRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, isMoving: Boolean) {
        if (!level.isClientSide) {
            if (newState.isAir || newState.block !is INodeBlock) {
                ClockworkMod.getKelvin().removeNode(pos.toDuctNodePos(level.dimension().location()))
            }
        } else {
            nodeRemoveClient(state, level as ClientLevel, pos)
        }
    }


    fun createNode(pos: DuctNodePos): DuctNode {
        return PipeDuctNode.DEFAULT(pos)
    }

    fun canConnectTo(self: BlockPos, other: BlockPos, direction: Direction, level: BlockGetter): Boolean {
        return true
    }

}