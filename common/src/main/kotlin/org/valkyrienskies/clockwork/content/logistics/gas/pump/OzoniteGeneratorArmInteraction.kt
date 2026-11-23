package org.valkyrienskies.clockwork.content.logistics.gas.pump

import com.simibubi.create.api.registry.CreateBuiltInRegistries
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes.DepositOnlyArmInteractionPoint
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.platform.OzoniteGeneratorArmMethods
import org.valkyrienskies.clockwork.platform.api.DeferredRegister

object OzoniteGeneratorArmInteraction : ArmInteractionPointType() {
    fun register() {
        val types = DeferredRegister.create(CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE, ClockworkMod.MOD_ID)
        types.register("ozonite_generator") { this }
        types.registerAll()
    }

    override fun canCreatePoint(level: Level, pos: BlockPos, state: BlockState) =
        state.`is`(ClockworkBlocks.OZONITE_GENERATOR.get())

    override fun createPoint(level: Level, pos: BlockPos, state: BlockState): ArmInteractionPoint =
        OzoniteGeneratorArmMethods.createPoint(this, level, pos, state)
}

abstract class OzoniteGeneratorArmPoint(type: ArmInteractionPointType, level: Level, pos: BlockPos, state: BlockState) :
    DepositOnlyArmInteractionPoint(type, level, pos, state) {

    protected fun generator() = (level.getBlockEntity(pos) as? OzoniteGeneratorBlockEntity)?.takeUnless { it.isRemoved }

    override fun getInteractionPositionVector(): Vec3 = Vec3.atBottomCenterOf(pos).add(0.0, 1.0, 0.0)
}
