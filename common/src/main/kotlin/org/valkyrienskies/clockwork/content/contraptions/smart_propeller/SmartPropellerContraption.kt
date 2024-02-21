package org.valkyrienskies.clockwork.content.contraptions.smart_propeller

import com.simibubi.create.content.contraptions.Contraption
import com.simibubi.create.content.contraptions.ContraptionType
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.ClockworkContraptions

class SmartPropellerContraption : Contraption {

    var facing: Direction? = null
        protected set

    constructor()

    constructor(facing: Direction?) {
        this.facing = facing
    }

    override fun assemble(world: Level?, pos: BlockPos?): Boolean {
        return searchMovedStructure(world, pos, facing)
    }

    override fun canBeStabilized(facing: Direction, localPos: BlockPos?): Boolean {
        return if (facing.opposite == this.facing && BlockPos.ZERO == localPos) false else facing.axis === this.facing!!.axis
    }

    override fun getType(): ContraptionType {
        return ClockworkContraptions.SMART_PROPELLOR
    }
}