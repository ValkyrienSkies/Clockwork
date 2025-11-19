package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.simibubi.create.api.behaviour.movement.MovementBehaviour
import com.simibubi.create.content.contraptions.behaviour.MovementContext
import com.simibubi.create.content.contraptions.render.ContraptionMatrices
import com.simibubi.create.foundation.virtualWorld.VirtualRenderWorld
import net.createmod.catnip.animation.AnimationTickHolder
import net.fabricmc.loader.impl.lib.sat4j.core.Vec
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft

class BladeControllerMovementBehaviour: MovementBehaviour {

    var previousRotation = Vec3.ZERO

    var durabilityTick = 60

    override fun tick(context: MovementContext) {
        val blockEntityData = context.blockEntityData
        val blades = blockEntityData.getCompound("Blades")
        if (!blades.isEmpty) {
            val bladeCount = blockEntityData.getInt("BladeCount")
            val bladeList = mutableListOf<ItemStack>()
            for (i in 1 .. bladeCount) {
                bladeList.add(ItemStack.of(blades.getCompound("Blade$i")))
            }
            val rotation = context.rotation.apply(Vec3.ZERO)
            val deltaRotation = rotation.subtract(previousRotation)
            if (deltaRotation.length() > 128.0 && ClockworkConfig.SERVER.bladeControllerUsesDurability) {
                durabilityTick--
                if (durabilityTick <= 0) {
                    durabilityTick = 60
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
                for (i in 1 .. bladeList.size) {
                    newBlades.put("Blade$i", bladeList[i - 1].save(CompoundTag()))
                }
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
        val bladeList = mutableListOf<ItemStack>()
        for (i in 1 .. bladeCount) {
            bladeList.add(ItemStack.of(blades.getCompound("Blade$i")))
        }

        val bladeAngle = if (blockEntityData.contains("BladeAngle")) blockEntityData.getDouble("BladeAngle") else 0.0

        val bladeRotations = ArrayList<Float>()

        for (i in bladeList.indices) {
            bladeRotations.add((360f / bladeCount.toFloat()) * i.toFloat())
        }

        BladeControllerRenderer.renderShared(bladeList, bladeAngle.toFloat(), context.state, AnimationTickHolder.getPartialTicks(context.world), matrices.viewProjection, buffer, bladeRotations, true, matrices)
    }
}
