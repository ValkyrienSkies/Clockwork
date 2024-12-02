package org.valkyrienskies.clockwork

import com.simibubi.create.content.fluids.tank.BoilerHeaters
import com.simibubi.create.content.fluids.tank.BoilerHeaters.Heater
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState


object ClockworkBoilerHeaters {



    fun registerHeater(block: Block, heater: Heater) {
        BoilerHeaters.registerHeater(block, heater)
    }

    fun init() {
        registerHeater(ClockworkBlocks.GAS_HEATER.get()) { level: Level?, pos: BlockPos?, state: BlockState ->
            val value = state.getValue(BlazeBurnerBlock.HEAT_LEVEL)
            if (value == BlazeBurnerBlock.HeatLevel.NONE)  return@registerHeater 1f
            if (value == BlazeBurnerBlock.HeatLevel.SEETHING) return@registerHeater 2f
            if (value.isAtLeast(BlazeBurnerBlock.HeatLevel.FADING)) return@registerHeater 1f
            0f
        }
    }
}

