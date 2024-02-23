package org.valkyrienskies.clockwork

import com.mojang.logging.LogUtils
import com.simibubi.create.foundation.data.CreateRegistrate
import dev.architectury.registry.CreativeTabRegistry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import org.slf4j.LoggerFactory
import org.valkyrienskies.core.impl.config.VSConfigClass
import org.valkyrienskies.core.impl.hooks.VSEvents

object ClockworkMod {
    const val MOD_ID = "vs_clockwork"

    // versioning
    const val BUILD_VERSION = 1
    const val NETWORK_VERSION = 1
    const val NETWORK_VERSION_STR = NETWORK_VERSION.toString()

    val NETWORK_CHANNEL: ResourceLocation = asResource("main")

    val REGISTRATE: CreateRegistrate = CreateRegistrate.create(MOD_ID)
    val MIXIN_LOGGER = LoggerFactory.getLogger("ClockworkMixins")
    val LOGGER = LogUtils.getLogger()

    val BASE_CREATIVE_TAB: CreativeModeTab = CreativeTabRegistry
        .create(ResourceLocation(MOD_ID, "clockwork")) { ClockworkItems.GRAVITRON.get().defaultInstance }

    @JvmStatic
    fun init() {
        ClockworkContraptions.init()
        ClockworkPackets.init()
        ClockworkTags.init()
        ClockworkWorldgen.init()

        VSEvents.ShipLoadEvent.on { event ->
            event.ship
        }

        VSConfigClass.registerConfig("vs_clockwork", ClockworkConfig::class.java)
    }

    @JvmStatic
    fun asResource(path: String): ResourceLocation {
        return ResourceLocation(MOD_ID, path)
    }
}
