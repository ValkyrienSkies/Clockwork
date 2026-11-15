package org.valkyrienskies.clockwork.platform

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType
import dev.architectury.injectables.annotations.ExpectPlatform
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState

object HarpoonArmMethods {
    @JvmStatic @ExpectPlatform
    fun createPoint(type: ArmInteractionPointType, level: Level, pos: BlockPos, state: BlockState): ArmInteractionPoint =
        throw AssertionError()
}
