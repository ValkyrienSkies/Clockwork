package org.valkyrienskies.clockwork

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.logging.LogUtils
import com.simibubi.create.foundation.data.CreateRegistrate
import com.simibubi.create.foundation.item.ItemDescription
import com.simibubi.create.foundation.item.TooltipModifier
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.event.events.common.InteractionEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.TickEvent
import dev.architectury.platform.Platform
import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.createmod.catnip.lang.FontHelper
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import org.slf4j.LoggerFactory
import org.valkyrienskies.clockwork.client.render.airpocket.AirpocketRenderer
import org.valkyrienskies.clockwork.content.contraptions.flap.dual_link.DualLinkHandler
import org.valkyrienskies.clockwork.content.events.CollisionSoundEffectHandler
import org.valkyrienskies.clockwork.content.forces.*
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController
import org.valkyrienskies.clockwork.content.physicalities.gyro.GyroShipControl
import org.valkyrienskies.clockwork.integration.cc.GenericPeripheralsCommon
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.clockwork.util.builder.ClockworkExpandedCreateRegistrate
import org.valkyrienskies.clockwork.util.gui.DuctStats
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.core.api.world.PhysLevel
import org.valkyrienskies.core.api.world.properties.DimensionId
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.impl.DuctNetworkServer
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt


object ClockworkMod {
    const val MOD_ID = "vs_clockwork"

    // versioning
    const val BUILD_VERSION = 1
    const val NETWORK_VERSION = 1
    const val NETWORK_VERSION_STR = NETWORK_VERSION.toString()

    val NETWORK_CHANNEL: ResourceLocation = asResource("main")

    val REGISTRATE: CreateRegistrate = ClockworkExpandedCreateRegistrate.create(MOD_ID).setTooltipModifierFactory { item ->
        ItemDescription . Modifier (item, FontHelper.Palette.STANDARD_CREATE)
        .andThen(TooltipModifier.mapNull(DuctStats.create(item)))
    }
    val MIXIN_LOGGER = LoggerFactory.getLogger("ClockworkMixins")
    val LOGGER = LogUtils.getLogger()

    private val TAB_REGISTRY = DeferredRegister.create(MOD_ID, Registries.CREATIVE_MODE_TAB)

    val BASE_CREATIVE_TAB: RegistrySupplier<CreativeModeTab> = TAB_REGISTRY.register("clockwork_main") {
        CreativeTabRegistry.create(Component.translatable("itemGroup.vs_clockwork")) {
            ItemStack(ClockworkItems.WANDERWAND.asItem())
        }
    }

    val PHYSICAL_CREATIVE_TAB: RegistrySupplier<CreativeModeTab> = TAB_REGISTRY.register("clockwork_physicalities") {
        CreativeTabRegistry.create(Component.translatable("itemGroup.vs_clockwork.physicalities")) {
            ItemStack(ClockworkBlocks.PHYSICS_INFUSER.asItem())
        }
    }

    val GAS_CREATIVE_TAB: RegistrySupplier<CreativeModeTab> = TAB_REGISTRY.register("clockwork_gasses") {
        CreativeTabRegistry.create(Component.translatable("itemGroup.vs_clockwork.gasses")) {
            ItemStack(ClockworkBlocks.AIR_COMPRESSOR.get())
        }
    }

    val BASE_CREATIVE_TABINFO: ResourceKey<CreativeModeTab> = BASE_CREATIVE_TAB.key
    val PHYSICAL_CREATIVE_TABINFO: ResourceKey<CreativeModeTab> = PHYSICAL_CREATIVE_TAB.key
    val GAS_CREATIVE_TABINFO: ResourceKey<CreativeModeTab> = GAS_CREATIVE_TAB.key

    val physTickOnce = ConcurrentLinkedQueue<Pair<DimensionId, (PhysLevel, Double, () -> Unit) -> Unit>>()

    @OptIn(VsBeta::class)
    @JvmStatic
    fun init() {
        ClockworkPackets.init()
        ClockworkTags.init()
        ClockworkRecipes.init()
        TAB_REGISTRY.register()

        vsCore.registerAttachment(PocketForcesController::class.java)
        vsCore.registerAttachment(WanderShipControl::class.java)
        //vsCore.registerAttachment(GasThrusterController::class.java)
        vsCore.registerAttachment(PropellerController::class.java)
        vsCore.registerAttachment(ReactionWheelController::class.java)
        vsCore.registerAttachment(EncasedFanController::class.java)
        vsCore.registerAttachment(GyroShipControl::class.java)
        vsCore.registerAttachment(SugarRocketController::class.java)
        vsCore.registerAttachment(GravitronController::class.java) { useTransientSerializer() }
        vsCore.registerAttachment(BearingController::class.java) { useTransientSerializer() }
        vsCore.registerAttachment(BalloonController::class.java)

        vsApi.shipLoadEvent.on { event -> val ship = event.ship;
            //TODO: UNCOMMENT WHEN POCKET FORCES IS FIXED
            //PocketForcesController.getOrCreate(ship)
            //DragController.getOrCreate(ship)
            WanderShipControl.getOrCreate(ship)

            //TODO remove when attachment bug is fixed
//            GasThrusterController.getOrCreate(ship)
//            PropellerController.getOrCreate(ship)
//            ReactionWheelController.getOrCreate(ship)
//            EncasedFanController.getOrCreate(ship)
//            GyroShipControl.getOrCreate(ship)
//            GravitronController.getOrCreate(ship)
//            SugarRocketController.getOrCreate(ship)
//            BearingController.getOrCreate(ship)
        }

        ClockworkWorldgen.register()

        ClockworkDamageTypes.init()

        //Register gas types
        ClockworkGasses.init()

        LifecycleEvent.SERVER_STARTED.register {
            ClockworkAugmentations.registerComponentAvgAugmentation("heatEnergy", it.shipObjectWorld)
            ClockworkAugmentations.registerComponentAvgAugmentation("pressure", it.shipObjectWorld)
            for (gas in GasTypeRegistry.GAS_TYPES.values) {
                ClockworkAugmentations.registerComponentAvgAugmentation("gas/${gas.resourceLocation}", it.shipObjectWorld)
            }
            //println("augments ${ClockworkAugmentations.componentAugmentKeys}")

            ClockworkAugmentations.registerComponentSumAugmentation("airupdated", it.shipObjectWorld)
            ClockworkAugmentations.registerSumAugmentation("sealed", it.shipObjectWorld)
        }

        TickEvent.SERVER_LEVEL_POST.register {
            for (ship in it.shipObjectWorld.loadedShips) {
                //TODO: UNCOMMENT WHEN POCKET FORCES IS FIXED
                //ship.getAttachment(PocketForcesController::class.java)?.gameTick(it, ship.id)
                ship.getAttachment(BalloonController::class.java)?.gameTick(it, ship)
            }

            ClockworkUtils.tick(it)
            if (ClockworkConfig.CLIENT.debugRender) AirpocketRenderer.tick(it)
            CollisionSoundEffectHandler.tick(it)

        }

        InteractionEvent.RIGHT_CLICK_BLOCK.register(InteractionEvent.RightClickBlock { player, hand, pos, face ->
            DualLinkHandler.handler(player, hand, pos, face)
        })

        vsApi.collisionStartEvent.on(CollisionSoundEffectHandler::onCollide)

        vsApi.physTickEvent.on {
            val temp = mutableListOf<Pair<DimensionId, (PhysLevel, Double, () -> Unit) -> Unit>>()
            while (physTickOnce.isNotEmpty()) {
                val (dimension, fn) = physTickOnce.poll() ?: continue
                if (it.world.dimension != dimension) {
                    temp.add(dimension to fn)
                    continue
                }
                fn(it.world, it.delta) {temp.add(dimension to fn)}
            }
            physTickOnce.addAll(temp)
        }

        if (Platform.isModLoaded("computercraft")) {
            GenericPeripheralsCommon.register()
        }
    }

    @JvmStatic
    fun physTickOnce(dimensionId: String, fn: (level: PhysLevel, delta: Double, tryNextTick: () -> Unit) -> Unit) {
        physTickOnce.add(dimensionId to fn)
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
