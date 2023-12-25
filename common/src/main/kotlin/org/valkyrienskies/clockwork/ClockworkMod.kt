package org.valkyrienskies.clockwork

import com.mojang.logging.LogUtils
import com.simibubi.create.foundation.data.CreateRegistrate
import com.simibubi.create.foundation.outliner.Outliner
import dev.architectury.registry.CreativeTabRegistry
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.minecraft.client.renderer.RenderType
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import org.slf4j.LoggerFactory
import org.valkyrienskies.clockwork.util.render.RenderUtil
import org.valkyrienskies.core.impl.hooks.VSEvents

object ClockworkMod {
    const val MOD_ID = "vs_clockwork"

    // versioning
    const val BUILD_VERSION = 1
    const val NETWORK_VERSION = 1
    const val NETWORK_VERSION_STR = NETWORK_VERSION.toString()

    val NETWORK_CHANNEL: ResourceLocation = asResource("main")

    val REGISTRATE: CreateRegistrate = CreateRegistrate.create(MOD_ID)
    val BASE_CREATIVE_TAB: CreativeModeTab = CreativeTabRegistry
        .create(ResourceLocation(MOD_ID, "clockwork")) { ClockworkItems.GRAVITRON.get().defaultInstance }
    val MIXIN_LOGGER = LoggerFactory.getLogger("ClockworkMixins")
    val LOGGER = LogUtils.getLogger()

    @JvmStatic
    val OUTLINER: Outliner = Outliner()

    @JvmStatic
    val AURIC_OUTLINER: Outliner = Outliner()

    @JvmStatic
    fun init() {
        ClockworkContraptions.init()
        ClockworkPackets.init()
        ClockworkTags.init()

        VSEvents.ShipLoadEvent.on { event ->
            event.ship
        }
    }

    @JvmStatic
    fun initClient() {
        ClockworkPonderScenes.init()
    }

    @JvmStatic
    fun asResource(path: String): ResourceLocation {
        return ResourceLocation(MOD_ID, path)
    }


}
