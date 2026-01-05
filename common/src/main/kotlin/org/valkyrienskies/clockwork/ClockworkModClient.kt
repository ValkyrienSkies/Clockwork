package org.valkyrienskies.clockwork

import com.mojang.blaze3d.platform.InputConstants
import com.simibubi.create.content.equipment.armor.BacktankArmorLayer
import dev.architectury.event.events.client.ClientTickEvent
import dev.architectury.registry.ReloadListenerRegistry
import net.createmod.catnip.outliner.Outliner
import net.createmod.ponder.foundation.PonderIndex
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRenderEvents
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.PackType
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandEffectRenderer
import org.valkyrienskies.clockwork.platform.NativePlatform
import java.io.IOException
import java.util.*
import dev.architectury.event.events.common.TickEvent
import dev.architectury.registry.client.keymappings.KeyMappingRegistry
import net.minecraft.client.KeyMapping
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.core.BlockPos
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import org.joml.Vector3ic
import org.valkyrienskies.clockwork.content.contraptions.flap.dual_link.DualLinkRenderer
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.SecondScrollValueRenderer
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautGogglesItem
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautGogglesState
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.IAeronautEquipment
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.IAeronautEquipment.Companion.wearingAeronautInSlot
import org.valkyrienskies.clockwork.content.curiosities.debug.DebugLightningArcerBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.backtank.GasBacktankArmorLayer
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotGlobals
import org.valkyrienskies.clockwork.mixinduck.MixinPlayerDuck
import org.valkyrienskies.clockwork.util.render.LightningManager
import org.valkyrienskies.clockwork.util.render.LightningRenderer
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.impl.client.DuctNetworkClient

object ClockworkModClient {

    @JvmStatic
    val OUTLINER: Outliner = Outliner()

    @JvmStatic
    val WANDER_OUTLINER: Outliner = Outliner()

    @JvmStatic
    val WANDERWAND_EFFECT_RENDERER = WanderwandEffectRenderer()

    @JvmStatic
    val RESOURCE_RELOAD_LISTENER: ResourceManagerReloadListener = ClockworkReloadListener()

    @JvmStatic
    val LIGHTNING_NODES: HashMap<BlockPos, DebugLightningArcerBlockEntity> = HashMap()

    var debugNodeCooldown = 0

    @JvmStatic
    val AERONAUT_GOGGLE_KEYBIND: KeyMapping = KeyMapping(
        "key.vs_clockwork.aeronaut_goggles_toggle",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_G,
        "category.vs_clockwork.clockwork_keys"
    )

    @JvmStatic
    val DEBUG_LIGHTNING_KEYBIND: KeyMapping = KeyMapping(
        "key.vs_clockwork.debug_lightning_toggle",
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_L,
        "category.vs_clockwork.clockwork_keys"
    )

    @JvmStatic
    val CLIENT_BALLOON_LEAKS = HashSet<Vector3ic>()


    @JvmStatic
    fun initClient() {
        PonderIndex.addPlugin(ClockworkPonderPlugin())
        ClientTickEvent.CLIENT_LEVEL_POST.register {
            WANDERWAND_EFFECT_RENDERER.clientTick(it)
        }
        ClockworkSoundScapes.init()
        SecondScrollValueRenderer.init()

        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, RESOURCE_RELOAD_LISTENER)

        // This is really stupid, but it's how create does it, so ¯\_(ツ)_/¯
        TickEvent.PLAYER_POST.register {
            FrequencySlotGlobals.tick()
        }

        ClientTickEvent.CLIENT_POST.register(ClientTickEvent.Client {
            DualLinkRenderer.tick()
            ClockworkSoundScapes.tick()
            SecondScrollValueRenderer.tickSecond()
            ClockworkModClient.tickDebugLightningNodes(it)
        })

        KeyMappingRegistry.register(AERONAUT_GOGGLE_KEYBIND)
        KeyMappingRegistry.register(DEBUG_LIGHTNING_KEYBIND)
        ClientTickEvent.CLIENT_POST.register {
            while (AERONAUT_GOGGLE_KEYBIND.consumeClick()) {
                val player = Minecraft.getInstance().player
                if (player != null && player.wearingAeronautInSlot(EquipmentSlot.HEAD)) {
                    val level = player.level()
                    if (level != null) {
                        val duck = (player as MixinPlayerDuck)
                        duck.gogglesNeedRefresh = true
                        if (!AeronautGogglesState.getState(player).gogglesDown) {
                            level.playSound(
                                player,
                                player.blockPosition(),
                                ClockworkSounds.GOGGLES_EQUIP.mainEvent,
                                SoundSource.PLAYERS,
                                1.0f,
                                1.0f
                            )
                            AeronautGogglesState.getState(player).gogglesDown = true
                        } else {
                            level.playSound(
                                player,
                                player.blockPosition(),
                                ClockworkSounds.GOGGLES_UNEQUIP.mainEvent,
                                SoundSource.PLAYERS,
                                1.0f,
                                1.0f
                            )
                            AeronautGogglesState.getState(player).gogglesDown = false
                        }
                    }
                }
            }
            while (DEBUG_LIGHTNING_KEYBIND.consumeClick()) {
                val level = Minecraft.getInstance().level
                if (level != null) {
                    LightningRenderer.spawnTestBolt()
                }
            }
        }

//        RenderEvent.AFTER_TRANSLUCENT.register(WorldRenderEvents.AfterTranslucent { context ->
//            if(!context.gameRenderer().minecraft.options.renderDebug) return@AfterTranslucent
//
//            val cameraPos = context.camera().position
//            val stack = context.matrixStack()
//            //stack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z())
//
//            KelvinNodeRenderer.render(context)

//            context.world().shipObjectWorld.loadedShips.forEach { clientShip ->
//                val stack = context.matrixStack() ?: return@forEach
//                stack.pushPose()
//
//                // Render the chunk coordinates as text
//                val text: Component = Component.nullToEmpty("Slug: ${clientShip.slug} | Client Id: ${clientShip.id}")
//
//                // Use Minecraft's built-in text rendering method
//                val font: Font = Minecraft.getInstance().font
//                val xOffset: Float = -font.width(text) / 2f
//
//                // Rotate to match the ships position
//                val height = clientShip.renderAABB.maxY() - clientShip.renderAABB.minY()
//                val position = clientShip.renderTransform.positionInWorld
//                val cameraPos = context.camera().position
//                stack.translate(-cameraPos.x(), -cameraPos.y(), -cameraPos.z())
//                stack.translate(position.x(), position.y() + (height * 0.5), position.z())
//
//                // Apply rotations
//                val yawRotation = Quaternionf().rotateY(Math.toRadians(-context.camera().yRot.toDouble()).toFloat())
//                val pitchRotation = Quaternionf().rotateX(Math.toRadians(context.camera().xRot.toDouble()).toFloat())
//                val combinedRotation = yawRotation.mul(pitchRotation)
//                stack.mulPose(combinedRotation.toMinecraft())
//
//                val scale = -0.025f
//                stack.scale(scale,scale,scale)
//
//                context.consumers()?.let {
//                    font.drawInBatch(text.string, xOffset, 0f, 0xFFFFFF, false, stack.last().pose(), it, false, 0x000000, 0xFFFFFF, false)
//                }
//
//                stack.popPose()
//            }
//        })
    }
    @JvmStatic
    fun getKelvin(): DuctNetworkClient {
        return KelvinMod.getClientKelvin() as DuctNetworkClient
    }

    fun tickDebugLightningNodes(mc: Minecraft) {
        val level = mc.level ?: return
        if (mc.isPaused) return
        if (debugNodeCooldown > 0) {
            debugNodeCooldown--
            return
        }
        val iterator = LIGHTNING_NODES.entries.iterator()
        val alreadyMadePairs = mutableSetOf<Pair<BlockPos, BlockPos>>()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val blockPos = entry.key
            val blockEntity = entry.value

            // If the block entity is no longer valid, remove it from the list
            if (level.getBlockEntity(blockPos) != blockEntity) {
                iterator.remove()
                continue
            }
            val worldPosThis = blockEntity.getWorldPos()
            for (node in LIGHTNING_NODES) {
                if (alreadyMadePairs.contains(
                        if (blockPos.hashCode() < node.key.hashCode()) {
                            Pair(blockPos, node.key)
                        } else {
                            Pair(node.key, blockPos)
                        }
                    )) {
                    continue
                }
                val otherPos = node.value.getWorldPos()
                if (worldPosThis.distanceToSqr(otherPos) <= 64.0 && node.key != blockPos) {
                    LightningManager.spawn(
                        {blockEntity.getWorldPos()},
                        {node.value.getWorldPos()},
                    )
                    val pair = if (blockPos.hashCode() < node.key.hashCode()) {
                        Pair(blockPos, node.key)
                    } else {
                        Pair(node.key, blockPos)
                    }
                    alreadyMadePairs.add(pair)
                }
            }
        }
        debugNodeCooldown = 3
    }

    class ClockworkReloadListener : ResourceManagerReloadListener {
        override fun onResourceManagerReload(resourceManager: ResourceManager) {
            ClockworkSoundScapes.invalidateAll()
        }
    }

    fun addEntityRendererLayers(
        entityType: EntityType<out LivingEntity?>?,
        entityRenderer: LivingEntityRenderer<*, *>?,
        registrationHelper: LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper?,
        context: EntityRendererProvider.Context?
    ) {
        BacktankArmorLayer.registerOn(entityRenderer, registrationHelper)
    }


}
