package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.simibubi.create.content.contraptions.behaviour.MovementBehaviour
import com.simibubi.create.content.contraptions.behaviour.MovementContext
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkConfig

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
            }
        }
    }
}