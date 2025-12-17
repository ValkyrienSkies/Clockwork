package org.valkyrienskies.clockwork.content.logistics.gas.storage.tank

import com.simibubi.create.api.connectivity.ConnectivityHandler
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.block.state.BlockState


class DuctTankCTBehaviour(layerShift: CTSpriteShiftEntry?, topShift: CTSpriteShiftEntry?, private val innerShift: CTSpriteShiftEntry) : HorizontalCTBehaviour(layerShift, topShift) {
    override fun getShift(state: BlockState?, direction: Direction, sprite: TextureAtlasSprite?): CTSpriteShiftEntry? {
        if (sprite != null && direction.getAxis() === Direction.Axis.Y && innerShift.getOriginal() === sprite) return innerShift

        return super.getShift(state, direction, sprite)
    }

    override fun buildContextForOccludedDirections(): Boolean {
        return true
    }

    override fun connectsTo(
        state: BlockState, other: BlockState, reader: BlockAndTintGetter, pos: BlockPos?, otherPos: BlockPos?,
        face: Direction?
    ): Boolean {
        return state.getBlock() === other.getBlock() && ConnectivityHandler.isConnected<DuctTankBlockEntity>(reader, pos, otherPos)
    }
}


//class DuctTankCTBehaviour: ConnectedTextureBehaviour.Base() {
//    override fun getShift(
//        state: BlockState,
//        direction: Direction,
//        sprite: TextureAtlasSprite?
//    ): CTSpriteShiftEntry? {
//        return when (direction) {
//            Direction.DOWN  -> if (state.getValue(DuctTankBlock.LARGE)) ClockworkSpriteShifts.DUCT_TANK_TOP else ClockworkSpriteShifts.DUCT_TANK
//            Direction.UP    -> if (state.getValue(DuctTankBlock.LARGE)) ClockworkSpriteShifts.DUCT_TANK_TOP else ClockworkSpriteShifts.DUCT_TANK
//            Direction.NORTH -> ClockworkSpriteShifts.DUCT_TANK
//            Direction.SOUTH -> ClockworkSpriteShifts.DUCT_TANK
//            Direction.WEST  -> ClockworkSpriteShifts.DUCT_TANK
//            Direction.EAST  -> ClockworkSpriteShifts.DUCT_TANK
//        }
//    }
//}