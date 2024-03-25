package org.valkyrienskies.clockwork

import com.mojang.logging.LogUtils
import com.simibubi.create.foundation.data.CreateRegistrate
import dev.architectury.event.events.common.BlockEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.TickEvent
import dev.architectury.registry.CreativeTabRegistry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import org.slf4j.LoggerFactory
import org.valkyrienskies.clockwork.content.forces.DragController
import org.valkyrienskies.clockwork.content.forces.PocketForcesController
import org.valkyrienskies.clockwork.content.logistics.heat.ClientAirPocketStorage
import org.valkyrienskies.core.api.ships.setAttachment
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TranslatableComponent
import org.valkyrienskies.clockwork.platform.PlatformUtils
import org.valkyrienskies.clockwork.platform.SharedValues
import org.valkyrienskies.core.impl.config.VSConfigClass
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.concurrent.thread

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

    private lateinit var kelvin: KelvinBackground

    private val kelvinThread: Thread = thread(start = false, priority = 8, name = "Kelvin thread") {
        kelvin.run()
    }

    @JvmStatic
    fun init() {
        ClockworkContraptions.init()
        ClockworkPackets.init()
        ClockworkTags.init()
        ClockworkWorldgen.init()
        VSConfigClass.registerConfig("clockwork", ClockworkConfig::class.java)

        VSEvents.ShipLoadEvent.on { event ->
            event.ship.setAttachment(PocketForcesController())
            event.ship.setAttachment(DragController())
        }

        VSEvents.airPocketModifyEvent.on { event ->
            if (event.removed) {
                ClientAirPocketStorage.pocketsToDeleteQueue.add(event.shipId to event.airPocketId)
            } else {
                ClientAirPocketStorage.pocketsToUpdateQueue.add(event.shipId to event.airPocketId)
            }
        }

        TickEvent.SERVER_LEVEL_POST.register {
            for (ship in it.shipObjectWorld.loadedShips) {
                ship.getAttachment(PocketForcesController::class.java)?.gameTick(it, ship)
                ship.getAttachment(DragController::class.java)?.gameTick(ship, it)
            }
            ClientAirPocketStorage.serverTick(it)
        }
        
        kelvin = KelvinBackground(ClockworkConfig.SERVER.kelvinTickRate, ClockworkConfig.SERVER.kelvinSubSteps)

        LifecycleEvent.SERVER_STARTING.register {
            kelvinThread.start()
        }

        LifecycleEvent.SERVER_STOPPING.register {
            kelvin.tellTaskToKillItself()
        }

        KelvinHandler.start()
    }

    fun getKelvinBackgroundTask(): KelvinBackground {
        return kelvin
    }

    @JvmStatic
    fun asResource(path: String): ResourceLocation {
        return ResourceLocation(MOD_ID, path)
    }
}
