package org.valkyrienskies.clockwork

import net.createmod.ponder.api.registration.PonderPlugin
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper
import net.minecraft.resources.ResourceLocation

class ClockworkPonderPlugin : PonderPlugin {
    override fun getModId(): String {
        return ClockworkMod.MOD_ID
    }

    override fun registerScenes(helper: PonderSceneRegistrationHelper<ResourceLocation>) {
        ClockworkPonders.init(helper)
    }

}
