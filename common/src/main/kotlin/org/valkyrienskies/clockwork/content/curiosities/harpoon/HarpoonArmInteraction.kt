package org.valkyrienskies.clockwork.content.curiosities.harpoon

import com.simibubi.create.api.registry.CreateBuiltInRegistries
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes.DepositOnlyArmInteractionPoint
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.platform.HarpoonArmMethods
import org.valkyrienskies.clockwork.platform.api.DeferredRegister

object HarpoonArmInteraction : ArmInteractionPointType() {
    fun register() {
        // Forge reopens this Create registry during RegisterEvent; Fabric registers during init.
        val types = DeferredRegister.create(CreateBuiltInRegistries.ARM_INTERACTION_POINT_TYPE, ClockworkMod.MOD_ID)
        types.register("harpoon_launcher") { this }
        types.registerAll()
    }

    override fun canCreatePoint(level: Level, pos: BlockPos, state: BlockState) = state.block is HarpoonBlock

    override fun createPoint(level: Level, pos: BlockPos, state: BlockState): ArmInteractionPoint =
        HarpoonArmMethods.createPoint(this, level, pos, state)
}

/** Arms load the muzzle and never extract ammunition from the launcher. */
abstract class HarpoonArmPoint(type: ArmInteractionPointType, level: Level, pos: BlockPos, state: BlockState) :
    DepositOnlyArmInteractionPoint(type, level, pos, state) {
    protected fun launcher(stack: ItemStack): HarpoonBlockEntity? {
        return (level.getBlockEntity(pos) as? HarpoonBlockEntity)?.takeIf { it.canLoad(stack) }
    }

    override fun getInteractionDirection(): Direction = cachedState.getValue(BlockStateProperties.FACING).opposite

    override fun getInteractionPositionVector(): Vec3 = Vec3.atCenterOf(pos)
        .add(Vec3.atLowerCornerOf(cachedState.getValue(BlockStateProperties.FACING).normal).scale(0.5))

    override fun updateCachedState() {
        val previous = cachedState
        super.updateCachedState()
        if (previous != cachedState) cachedAngles = null
    }
}
