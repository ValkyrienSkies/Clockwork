package org.valkyrienskies.clockwork

import com.simibubi.create.infrastructure.item.CreateCreativeModeTab
import net.minecraft.world.item.ItemStack

class ClockworkTab : CreateCreativeModeTab("clockwork") {
    override fun makeIcon(): ItemStack {
        return ClockworkBlocks.PHYSICS_INFUSER.asStack()
    }
}
