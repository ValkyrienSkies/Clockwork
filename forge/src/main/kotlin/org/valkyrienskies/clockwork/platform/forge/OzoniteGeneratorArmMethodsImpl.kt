package org.valkyrienskies.clockwork.platform.forge

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.items.IItemHandler
import net.minecraftforge.items.wrapper.InvWrapper
import org.valkyrienskies.clockwork.content.logistics.gas.pump.OzoniteGeneratorArmPoint

object OzoniteGeneratorArmMethodsImpl {
    @JvmStatic
    fun createPoint(type: ArmInteractionPointType, level: Level, pos: BlockPos, state: BlockState): ArmInteractionPoint =
        object : OzoniteGeneratorArmPoint(type, level, pos, state) {
            override fun getHandler(): IItemHandler? = generator()?.let(::InvWrapper)
        }
}
