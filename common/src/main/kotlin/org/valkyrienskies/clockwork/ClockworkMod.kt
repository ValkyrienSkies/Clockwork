package org.valkyrienskies.clockwork

import com.mojang.logging.LogUtils
import com.simibubi.create.foundation.data.CreateRegistrate
import dev.architectury.event.events.common.InteractionEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.TickEvent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import org.slf4j.LoggerFactory
import org.valkyrienskies.clockwork.content.contraptions.flap.dual_link.DualLinkHandler
import org.valkyrienskies.clockwork.content.forces.*
import org.valkyrienskies.clockwork.content.physicalities.gyro.GyroShipControl
import org.valkyrienskies.clockwork.platform.PlatformUtils
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.core.impl.hooks.VSEvents
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.impl.DuctNetworkServer
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.api.vsApi
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

    @JvmStatic
    fun init() {
        ClockworkContraptions.init()
        ClockworkPackets.init()
        ClockworkTags.init()
        ClockworkWorldgen.init()

        ValkyrienSkiesMod.vsCore.registerConfigLegacy("clockwork", ClockworkConfig::class.java)

        vsApi.registerAttachment(PocketForcesController::class.java)
        vsApi.registerAttachment(DragController::class.java)
        vsApi.registerAttachment(WanderShipControl::class.java)

        vsApi.registerAttachment(GasThrusterController::class.java)
        vsApi.registerAttachment(PropellerController::class.java)
        vsApi.registerAttachment(ReactionWheelController::class.java)
        vsApi.registerAttachment(EncasedFanController::class.java)
        vsApi.registerAttachment(GyroShipControl::class.java)
        vsApi.registerAttachment(GravitronController::class.java)
        vsApi.registerAttachment(SugarRocketController::class.java)

        //TODO remove when attachment bug is fixed
        VSEvents.shipLoadEvent.on { (ship) ->
            PocketForcesController.getOrCreate(ship)
            DragController.getOrCreate(ship)
            WanderShipControl.getOrCreate(ship)

            GasThrusterController.getOrCreate(ship)
            PropellerController.getOrCreate(ship)
            ReactionWheelController.getOrCreate(ship)
            EncasedFanController.getOrCreate(ship)
            GyroShipControl.getOrCreate(ship)
            GravitronController.getOrCreate(ship)
            SugarRocketController.getOrCreate(ship)
        }


        VSEvents.ShipLoadEvent.on { event ->
            val pocketController = PocketForcesController()
            val dragController = DragController()
            pocketController.dimensionId = event.ship.chunkClaimDimension
            dragController.dimensionId = event.ship.chunkClaimDimension
            event.ship.setAttachment(pocketController)
            event.ship.setAttachment(dragController)
            event.ship.setAttachment(WanderShipControl())
        }

        LifecycleEvent.SERVER_STARTED.register {
            ClockworkAugmentations.registerComponentAvgAugmentation("temperature", it.shipObjectWorld)
            ClockworkAugmentations.registerComponentAvgAugmentation("pressure", it.shipObjectWorld)
            for (gas in GasTypeRegistry.GAS_TYPES.values) {
                ClockworkAugmentations.registerComponentAvgAugmentation("gas_${gas.name.lowercase()}", it.shipObjectWorld)
            }
            //todo: gas registry
            ClockworkAugmentations.registerComponentSumAugmentation("airupdated", it.shipObjectWorld)
            ClockworkAugmentations.registerSumAugmentation("sealed", it.shipObjectWorld)
        }

        TickEvent.SERVER_LEVEL_POST.register {
            for (ship in it.shipObjectWorld.loadedShips) {
                ship.getAttachment(PocketForcesController::class.java)?.gameTick(it, ship)
                ship.getAttachment(DragController::class.java)?.gameTick(ship, it)
            }
            ClockworkUtils.tick(it)
        }

        InteractionEvent.RIGHT_CLICK_BLOCK.register(InteractionEvent.RightClickBlock { player, hand, pos, face ->
            DualLinkHandler.handler(player, hand, pos, face)
        })

    }

    @JvmStatic
    fun getKelvin(): DuctNetworkServer {
        return KelvinMod.getKelvin() as DuctNetworkServer
    }

    @JvmStatic
    fun asResource(path: String): ResourceLocation {
        return ResourceLocation(MOD_ID, path)
    }
}
