package org.valkyrienskies.clockwork

import com.mojang.logging.LogUtils
import com.simibubi.create.foundation.data.CreateRegistrate
import com.simibubi.create.foundation.outliner.Outliner
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import org.joml.Quaternionf
import org.slf4j.LoggerFactory
import org.valkyrienskies.clockwork.util.CWEntityDataSerializers
import org.valkyrienskies.core.impl.hooks.VSEvents

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

    @JvmField
    val C_CREATIVE_TAB = ResourceKey.create(Registries.CREATIVE_MODE_TAB, ResourceLocation("clockwork"))

    @JvmStatic
    fun init() {
        ClockworkContraptions.init()
        ClockworkPackets.init()

        VSEvents.ShipLoadEvent.on { event ->
            event.ship
        }
    }

    @JvmStatic
    fun initClient() {}

    fun asResource(path: String): ResourceLocation {
        return ResourceLocation(MOD_ID, path)
    }

    fun createCreativeTab(): CreativeModeTab {
        return CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
            .title(Component.translatable("itemGroup.valkyrienSkies"))
            .icon { ClockworkBlocks.PHYSICS_INFUSER.asStack() }
            .displayItems { _, output ->
                output.accept(ClockworkItems.AURIC_DESIGNATOR)
                output.accept(ClockworkItems.BLUUGUU)
                output.accept(ClockworkItems.BLUPERGLUE)
                output.accept(ClockworkItems.GRAVITRON)

                output.accept(ClockworkBlocks.PHYSICS_INFUSER.asStack())
                output.accept(ClockworkBlocks.ALT_METER.asStack())
                output.accept(ClockworkBlocks.BALLOON_CASING.asStack())
                output.accept(ClockworkBlocks.COMMAND_SEAT.asStack())
                output.accept(ClockworkBlocks.BALLOON_ENCASED_SHAFT.asStack())
                output.accept(ClockworkBlocks.FLAP.asStack())
                output.accept(ClockworkBlocks.FLAP_BEARING.asStack())
                output.accept(ClockworkBlocks.HEAT_PIPE.asStack())
                output.accept(ClockworkBlocks.PHYS_BEARING.asStack())
                output.accept(ClockworkBlocks.PROPELLER_BEARING.asStack())
                output.accept(ClockworkBlocks.REACTIONWHEEL.asStack())
                output.accept(ClockworkBlocks.REDSTONE_RESISTOR.asStack())
                output.accept(ClockworkBlocks.WING.asStack())
            }
            .build()
    }
}
