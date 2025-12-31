package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.mojang.logging.LogUtils
import com.simibubi.create.api.behaviour.movement.MovementBehaviour
import com.simibubi.create.content.contraptions.behaviour.MovementContext
import com.simibubi.create.content.contraptions.render.ContraptionMatrices
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld
import net.createmod.catnip.animation.AnimationTickHolder
import net.fabricmc.loader.impl.lib.sat4j.core.Vec
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.ContainerHelper
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.LightLayer
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.contraptions.propeller.contraption.PropellerContraption
import org.valkyrienskies.clockwork.content.forces.PropellerController
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft
import kotlin.math.absoluteValue

class BladeControllerMovementBehaviour: MovementBehaviour {

    var previousRotation = Vec3.ZERO

    var durabilityTick = 600

    override fun tick(context: MovementContext) {
        val blockEntityData = context.blockEntityData
        val blades = blockEntityData.getCompound("Blades")
        if (!blades.isEmpty) {
            val bladeCount = blockEntityData.getInt("BladeCount")
            val bladeList = NonNullList.withSize(8, ItemStack.EMPTY)
            for (i in 1 .. bladeCount) {
                bladeList[i] = ItemStack.of(blades.getCompound("Blade$i"))
            }
            val rotation = context.rotation.apply(Vec3.ZERO)
            val deltaRotation = rotation.subtract(previousRotation)
            if (deltaRotation.length().absoluteValue >= 128.0 && ClockworkConfig.SERVER.bladeControllerUsesDurability && (context.contraption is PropellerContraption && !(context.contraption as PropellerContraption).brass)) {
                durabilityTick--
                if (durabilityTick <= 0) {
                    durabilityTick = 600
                    val toRemove = ArrayList<ItemStack>()
                    bladeList.forEach {
                        val broken = it.hurt(1, context.world.random, null)
                        if (broken) toRemove.add(it)
                    }
                    toRemove.forEach {
                        bladeList.remove(it)
                    }
                }
            }
            if (bladeList.size != bladeCount) {
                blockEntityData.putInt("BladeCount", bladeList.size)
                blades.remove("Blades")
                val newBlades = CompoundTag()
                ContainerHelper.saveAllItems(newBlades, bladeList)
                blockEntityData.put("Blades", newBlades)
                blockEntityData.putBoolean("ShouldUpdatePhys", true)
            }
        }
    }

    override fun renderInContraption(
        context: MovementContext,
        renderWorld: VirtualRenderWorld,
        matrices: ContraptionMatrices,
        buffer: MultiBufferSource
    ) {
        val blockEntityData = context.blockEntityData
        val blades = blockEntityData.getCompound("Blades")
        val bladeCount = blockEntityData.getInt("BladeCount")

        val bladeNonNullList = NonNullList.withSize(bladeCount, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(blades, bladeNonNullList)

        val bladeList = mutableListOf<ItemStack>()
        for (i in 0..<bladeCount) {
            if (bladeNonNullList.size <= i) continue
            bladeList.add(bladeNonNullList[i])
        }

        val bladeAngle = if (blockEntityData.contains("BladeAngle")) blockEntityData.getDouble("BladeAngle") else 0.0

        val bladeRotations = ArrayList<Float>()

        for (i in bladeList.indices) {
            bladeRotations.add((360f / bladeCount.toFloat()) * i.toFloat())
        }

        val light : Int = LevelRenderer.getLightColor(renderWorld, context.localPos)
        BladeControllerRenderer.renderShared(bladeList, bladeAngle.toFloat(), context.state, AnimationTickHolder.getPartialTicks(context.world), matrices.viewProjection, buffer, light, bladeRotations, true, matrices, context.world)
    }
}
