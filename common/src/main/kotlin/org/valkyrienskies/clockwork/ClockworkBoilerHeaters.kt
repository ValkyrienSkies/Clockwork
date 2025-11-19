package org.valkyrienskies.clockwork

import com.simibubi.create.api.boiler.BoilerHeater
import com.simibubi.create.content.fluids.tank.BoilerHeaters
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState


object ClockworkBoilerHeaters {



    fun registerHeater(block: Block, heater: BoilerHeater) {
        BoilerHeater.REGISTRY.register(block, heater)
    }

    fun init() {
        registerHeater(ClockworkBlocks.GAS_HEATER.get()) { level: Level?, pos: BlockPos?, state: BlockState ->

            val value = state.getValue(BlazeBurnerBlock.HEAT_LEVEL)
            when(value) {
                HeatLevel.SMOULDERING -> 0f
                HeatLevel.FADING -> 1f
                HeatLevel.KINDLED -> 2f
                HeatLevel.SEETHING -> 3f
                else -> -1f
            }
        }
    }
}

