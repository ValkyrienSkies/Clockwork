package org.valkyrienskies.clockwork.platform.fabric

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.content.logistics.gas.pump.OzoniteGeneratorArmPoint

object OzoniteGeneratorArmMethodsImpl {
    @JvmStatic
    fun createPoint(type: ArmInteractionPointType, level: Level, pos: BlockPos, state: BlockState): ArmInteractionPoint =
        object : OzoniteGeneratorArmPoint(type, level, pos, state) {
            override fun getHandler(): Storage<ItemVariant>? = generator()?.let { InventoryStorage.of(it, null) }
        }
}
