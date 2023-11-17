package org.valkyrienskies.clockwork

import com.mojang.logging.LogUtils
import com.simibubi.create.foundation.data.CreateRegistrate
import com.simibubi.create.foundation.outliner.Outliner
import dev.architectury.registry.CreativeTabRegistry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.Blocks
import org.slf4j.LoggerFactory

object ClockworkMod {
    const val MOD_ID = "vs_clockwork"

    // versioning
    const val BUILD_VERSION = 1
    const val NETWORK_VERSION = 1
    const val NETWORK_VERSION_STR = NETWORK_VERSION.toString()

    val NETWORK_CHANNEL: ResourceLocation = asResource("main")

    val REGISTRATE: CreateRegistrate = CreateRegistrate.create(MOD_ID)
    //val BASE_CREATIVE_TAB: CreativeModeTab = CreativeTabRegistry
    //        .create(ResourceLocation(MOD_ID, "clockwork")) { ClockworkBlocks.PHYSICS_INFUSER.asStack() }
    val MIXIN_LOGGER = LoggerFactory.getLogger("ClockworkMixins")
    val LOGGER = LogUtils.getLogger()

    val OUTLINER: Outliner = Outliner()

    @JvmStatic
    fun init() {
        ClockworkContraptions.init()
        ClockworkPackets.init()
    }

    @JvmStatic
    fun initClient() {}

    fun asResource(path: String): ResourceLocation {
        return ResourceLocation(MOD_ID, path)
    }
}
