package org.valkyrienskies.clockwork

import com.mojang.logging.LogUtils
import com.simibubi.create.foundation.data.CreateRegistrate
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.TickEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import org.slf4j.LoggerFactory
import org.valkyrienskies.clockwork.content.forces.DragController
import org.valkyrienskies.clockwork.content.forces.PocketForcesController
import org.valkyrienskies.core.api.ships.setAttachment
import org.valkyrienskies.clockwork.content.forces.WanderShipControl
import org.valkyrienskies.clockwork.kelvin.api.DuctNetwork
import org.valkyrienskies.clockwork.kelvin.impl.DuctNetworkImpl
import org.valkyrienskies.clockwork.platform.PlatformUtils
import org.valkyrienskies.core.impl.config.VSConfigClass
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.shipObjectWorld

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

    val BASE_CREATIVE_TAB: CreativeModeTab = PlatformUtils.getCreativeTab()

    val Kelvin: DuctNetworkImpl = DuctNetworkImpl()

    @JvmStatic
    fun init() {
        ClockworkContraptions.init()
        ClockworkPackets.init()
        ClockworkTags.init()
        ClockworkWorldgen.init()
        ValkyrienSkiesMod.vsCore.registerConfigLegacy("clockwork", ClockworkConfig::class.java)

        VSEvents.ShipLoadEvent.on { event ->
            event.ship.setAttachment(PocketForcesController())
            event.ship.setAttachment(DragController())
            event.ship.setAttachment(WanderShipControl())
        }

        LifecycleEvent.SERVER_BEFORE_START.register {
            Kelvin.disabled = false
        }

        LifecycleEvent.SERVER_STOPPED.register {
            Kelvin.dump()
        }

        LifecycleEvent.SERVER_STARTED.register {
            ClockworkAugmentations.registerComponentAugmentation("temperature", it.shipObjectWorld)
            ClockworkAugmentations.registerComponentAugmentation("gas_air", it.shipObjectWorld)
            ClockworkAugmentations.registerComponentAugmentation("gas_phlogiston", it.shipObjectWorld)
            ClockworkAugmentations.registerComponentAugmentation("gas_helium", it.shipObjectWorld)
        }

        TickEvent.SERVER_LEVEL_POST.register {
            for (ship in it.shipObjectWorld.loadedShips) {
                ship.getAttachment(PocketForcesController::class.java)?.gameTick(it, ship)
                ship.getAttachment(DragController::class.java)?.gameTick(ship, it)
            }
            Kelvin.tick(it, ClockworkConfig.SERVER.kelvinSubSteps)
        }
    }

    fun getKelvin(): DuctNetwork {
        if (Kelvin.disabled) {
            throw IllegalStateException("Attempted to access Kelvin from the wrong place!")
        }
        return Kelvin
    }

    @JvmStatic
    fun asResource(path: String): ResourceLocation {
        return ResourceLocation(MOD_ID, path)
    }
}
