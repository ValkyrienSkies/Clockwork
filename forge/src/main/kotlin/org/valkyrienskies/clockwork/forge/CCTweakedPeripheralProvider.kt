package org.valkyrienskies.clockwork.forge

import dan200.computercraft.api.peripheral.IPeripheral
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.CapabilityManager
import net.minecraftforge.common.capabilities.CapabilityToken
import net.minecraftforge.common.capabilities.ICapabilityProvider
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.event.AttachCapabilitiesEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.integration.cc.getPeripheralCommon

class CCTweakedForgeEvents() {
    @SubscribeEvent
    fun attachCapability(event: AttachCapabilitiesEvent<BlockEntity>) {
        val be = event.`object` ?: return
        CCTweakedPeripheralProvider.PeripheralProvider.attach(event, be)
    }
}

object CCTweakedPeripheralProvider {
    val CAPABILITY_PERIPHERAL = CapabilityManager.get(object : CapabilityToken<IPeripheral>(){})
    val PERIPHERAL = ResourceLocation(ClockworkMod.MOD_ID, "peripheral")

    class PeripheralProvider<O : BlockEntity>(
        private val blockEntity: O?,
    ) : ICapabilityProvider {
        private var peripheral: LazyOptional<IPeripheral?>? = null

        fun invalidate() {
            if (peripheral != null) peripheral!!.invalidate()
            peripheral = null
        }

        override fun <T : Any?> getCapability(capability: Capability<T>, direction: Direction?): LazyOptional<T?> {
            if (capability !== CAPABILITY_PERIPHERAL) return LazyOptional.empty<T?>()
            if (blockEntity!!.isRemoved) return LazyOptional.empty<T?>()

            if (peripheral == null) {
                this.peripheral = getPeripheralCommon(blockEntity, direction)?.let { LazyOptional.of{ it } } ?: LazyOptional.empty<IPeripheral>()
            }

            return peripheral!!.cast<T>()
        }

        companion object {
            fun <O : BlockEntity> attach(
                event: AttachCapabilitiesEvent<BlockEntity>,
                blockEntity: O?,
            ) {
                val provider = PeripheralProvider<O>(blockEntity)
                event.addCapability(PERIPHERAL, provider)
                event.addListener(Runnable { provider.invalidate() })
            }
        }
    }
}