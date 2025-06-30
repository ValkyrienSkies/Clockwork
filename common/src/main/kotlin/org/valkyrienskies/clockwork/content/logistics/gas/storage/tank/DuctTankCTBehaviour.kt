package org.valkyrienskies.clockwork.content.logistics.gas.storage.tank

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry
import com.simibubi.create.foundation.block.connected.ConnectedTextureBehaviour
import net.minecraft.client.renderer.texture.TextureAtlasSprite
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkSpriteShifts

class DuctTankCTBehaviour: ConnectedTextureBehaviour.Base() {
    //TODO doesn't really work
    override fun getShift(
        state: BlockState,
        direction: Direction,
        sprite: TextureAtlasSprite?
    ): CTSpriteShiftEntry? {
        return when (direction) {
            Direction.DOWN  -> if (state.getValue(DuctTankBlock.LARGE)) ClockworkSpriteShifts.DUCT_TANK_TOP else ClockworkSpriteShifts.DUCT_TANK
            Direction.UP    -> if (state.getValue(DuctTankBlock.LARGE)) ClockworkSpriteShifts.DUCT_TANK_TOP else ClockworkSpriteShifts.DUCT_TANK
            Direction.NORTH -> ClockworkSpriteShifts.DUCT_TANK
            Direction.SOUTH -> ClockworkSpriteShifts.DUCT_TANK
            Direction.WEST  -> ClockworkSpriteShifts.DUCT_TANK
            Direction.EAST  -> ClockworkSpriteShifts.DUCT_TANK
        }
    }
}