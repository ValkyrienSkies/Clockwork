package org.valkyrienskies.clockwork

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.logging.LogUtils
import com.simibubi.create.foundation.data.CreateRegistrate
import dev.architectury.event.events.common.CommandRegistrationEvent
import dev.architectury.event.events.common.InteractionEvent
import dev.architectury.event.events.common.LifecycleEvent
import dev.architectury.event.events.common.TickEvent
import dev.architectury.registry.CreativeTabRegistry
import dev.architectury.registry.registries.DeferredRegister
import dev.architectury.registry.registries.RegistrySupplier
import net.minecraft.client.KeyMapping
import net.minecraft.commands.CommandSourceStack
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.OutgoingChatMessage
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.RecipeType
import org.slf4j.LoggerFactory
import org.valkyrienskies.clockwork.content.contraptions.flap.dual_link.DualLinkHandler
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item.CraftingTableBladeRecipe
import org.valkyrienskies.clockwork.content.events.CollisionSoundEffectHandler
import org.valkyrienskies.clockwork.content.forces.*
import org.valkyrienskies.clockwork.content.forces.contraption.BearingController
import org.valkyrienskies.clockwork.content.physicalities.gyro.GyroShipControl
import org.valkyrienskies.clockwork.util.ClockworkUtils
import org.valkyrienskies.core.api.VsBeta
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.impl.DuctNetworkServer
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore
import org.valkyrienskies.mod.event.RegistryEvents


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

    private val TAB_REGISTRY = DeferredRegister.create(MOD_ID, Registries.CREATIVE_MODE_TAB)

    val BASE_CREATIVE_TAB: RegistrySupplier<CreativeModeTab> = TAB_REGISTRY.register("example_tab") {
        CreativeTabRegistry.create(Component.translatable("itemGroup.vs_clockwork")) {
            ItemStack(ClockworkBlocks.PHYSICS_INFUSER.asItem())
        }
    }

    val BASE_CREATIVE_TABINFO: ResourceKey<CreativeModeTab> = BASE_CREATIVE_TAB.key

    @OptIn(VsBeta::class)
    @JvmStatic
    fun init() {
        ClockworkPackets.init()
        ClockworkTags.init()
        ClockworkRecipes.init()
        TAB_REGISTRY.register()

        vsCore.registerAttachment(PocketForcesController::class.java)
        vsCore.registerAttachment(WanderShipControl::class.java)
        vsCore.registerAttachment(GasThrusterController::class.java)
        vsCore.registerAttachment(PropellerController::class.java)
        vsCore.registerAttachment(ReactionWheelController::class.java)
        vsCore.registerAttachment(EncasedFanController::class.java)
        vsCore.registerAttachment(GyroShipControl::class.java)
        vsCore.registerAttachment(SugarRocketController::class.java)
        vsCore.registerAttachment(GravitronController::class.java) { useTransientSerializer() }
        vsCore.registerAttachment(BearingController::class.java) { useTransientSerializer() }

        vsApi.shipLoadEvent.on { event -> val ship = event.ship;
            PocketForcesController.getOrCreate(ship)
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
            //for (ship in it.shipObjectWorld.loadedShips) {
            //    ship.getAttachment(PocketForcesController::class.java)?.gameTick(it, ship)
            //}

            ClockworkUtils.tick(it)
            CollisionSoundEffectHandler.tick(it)
        }

        InteractionEvent.RIGHT_CLICK_BLOCK.register(InteractionEvent.RightClickBlock { player, hand, pos, face ->
            DualLinkHandler.handler(player, hand, pos, face)
        })

        //TODO remove when VS commands return
        CommandRegistrationEvent.EVENT.register { dispatcher, context, idk ->
            dispatcher.register(LiteralArgumentBuilder.literal<CommandSourceStack>("get-air-values").executes {
                val level = it.source.level!!
                val player = it.source.player!!

                val density = level.shipObjectWorld.aerodynamicUtils.getAirTemperatureForY(player.position().y(),level.dimensionId)
                val temperature = level.shipObjectWorld.aerodynamicUtils.getAirTemperatureForY(player.position().y(),level.dimensionId)

                player.sendSystemMessage(Component.literal("At y: ${player.position().y} density: $density temperature: $temperature"))

                0
            })
        }

        vsApi.collisionStartEvent.on(CollisionSoundEffectHandler::onCollide)
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
