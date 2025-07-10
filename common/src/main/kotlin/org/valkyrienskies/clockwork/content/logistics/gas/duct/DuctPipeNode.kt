package org.valkyrienskies.clockwork.content.logistics.gas.duct

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.NodeBehaviorType
import org.valkyrienskies.kelvin.api.nodes.ILeakNode
import org.valkyrienskies.kelvin.api.nodes.PipeDuctNode
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft

class DuctPipeNode(pos: DuctNodePos, volume: Double, maxPressure: Double, maxTemperature: Double) : PipeDuctNode(pos, NodeBehaviorType.PIPE, volume = volume, maxPressure = maxPressure, maxTemperature = maxTemperature), ILeakNode {

    override fun getLeakRatio(level: Level): Double {
        val state = level.getBlockState(pos.toMinecraft())
        if (state.block !is DuctBlock) return 0.0
        var leak = 0.0
        Direction.entries.forEach { leak += if (state.getValue(DuctBlock.DIR_TO_CONNECTION[it]!!) == DuctConnectionType.LEAK) 1.0/6.0 else 0.0 }
        return leak
    }

    override fun leakFromPos(level: Level, fromPos: DuctNodePos) {
        val state = level.getBlockState(pos.toMinecraft())
        if (state.block !is DuctBlock) return
        val sub = pos.toMinecraft().subtract(fromPos.toMinecraft())
        val direction = Direction.fromDelta(sub.x, sub.y, sub.z) ?: return
        state.setValue(DuctBlock.DIR_TO_CONNECTION[direction]!!, DuctConnectionType.LEAK)
        level.setBlockAndUpdate(pos.toMinecraft(), state)
        println("Updated leak at ${pos.toMinecraft()}")
    }

}