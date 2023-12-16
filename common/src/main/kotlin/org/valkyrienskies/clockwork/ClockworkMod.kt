package org.valkyrienskies.clockwork

import com.mojang.logging.LogUtils
import com.simibubi.create.foundation.data.CreateRegistrate
import com.simibubi.create.foundation.outliner.Outliner
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import org.slf4j.LoggerFactory
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

    @JvmStatic
    val OUTLINER: Outliner = Outliner()

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

    fun createCreativeTab(): CreativeModeTab {
        return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.clockwork"))
            .icon { ClockworkItems.GRAVITRON.asStack() }
            .displayItems { _, output ->
                output.accept(ClockworkItems.BLUUGUU)
                output.accept(ClockworkItems.BLUPERGLUE)
                output.accept(ClockworkItems.GRAVITRON)

                output.accept(ClockworkItems.AURIC_DESIGNATOR)

                output.accept(ClockworkBlocks.PHYSICS_INFUSER.asStack())
                output.accept(ClockworkBlocks.GYRO.asStack())
                output.accept(ClockworkBlocks.ALT_METER.asStack())
                output.accept(ClockworkBlocks.FLAP.asStack())
                output.accept(ClockworkBlocks.FLAP_BEARING.asStack())
                output.accept(ClockworkBlocks.PHYS_BEARING.asStack())
                output.accept(ClockworkBlocks.PROPELLER_BEARING.asStack())
                output.accept(ClockworkBlocks.REDSTONE_RESISTOR.asStack())
                output.accept(ClockworkBlocks.WING.asStack())
            }
            .build()
    }
}
