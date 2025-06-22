package org.valkyrienskies.clockwork

import com.simibubi.create.content.equipment.armor.BacktankArmorLayer
import com.simibubi.create.content.trains.schedule.TrainHatArmorLayer
import com.simibubi.create.foundation.outliner.Outliner
import com.simibubi.create.foundation.utility.ModelSwapper
import dev.architectury.event.events.client.ClientTickEvent
import dev.architectury.event.events.common.TickEvent
import dev.architectury.registry.ReloadListenerRegistry
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.server.packs.PackType
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import org.valkyrienskies.clockwork.content.contraptions.flap.dual_link.DualLinkRenderer
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.SecondScrollValueRenderer
import org.valkyrienskies.clockwork.content.logistics.gas.backtank.GasBacktankArmorLayer
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.frequency_slot.FrequencySlotGlobals
import org.valkyrienskies.kelvin.KelvinMod
import org.valkyrienskies.kelvin.impl.client.DuctNetworkClient


object ClockworkModClient {

    @JvmStatic
    val OUTLINER: Outliner = Outliner()

    @JvmStatic
    val WANDER_OUTLINER: Outliner = Outliner()

    @JvmStatic
    val RESOURCE_RELOAD_LISTENER: ResourceManagerReloadListener = ClockworkReloadListener()

    @JvmField
    val MODEL_SWAPPER: ModelSwapper = ModelSwapper()

    @JvmStatic
    fun initClient() {
        ClockworkPonders.init()
        ClockworkSoundScapes.init()
        SecondScrollValueRenderer.init()

        MODEL_SWAPPER.registerListeners()

        ReloadListenerRegistry.register(PackType.CLIENT_RESOURCES, RESOURCE_RELOAD_LISTENER)

        // This is really stupid, but it's how create does it, so ¯\_(ツ)_/¯
        TickEvent.PLAYER_POST.register {
            FrequencySlotGlobals.tick()
        }

        ClientTickEvent.CLIENT_POST.register(ClientTickEvent.Client {
            DualLinkRenderer.tick()
            ClockworkSoundScapes.tick()
            SecondScrollValueRenderer.tickSecond()
        })


        LivingEntityFeatureRendererRegistrationCallback.EVENT.register { entityType: EntityType<out LivingEntity>, livingEntityRenderer: LivingEntityRenderer<*, *>, registrationHelper: LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper, context: EntityRendererProvider.Context ->
            GasBacktankArmorLayer.registerOn(livingEntityRenderer, registrationHelper)
        }

//        WorldRenderEvents.AFTER_TRANSLUCENT.register(WorldRenderEvents.AfterTranslucent { context ->
//            if(!context.gameRenderer().minecraft.options.renderDebug) return@AfterTranslucent
//
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
        TrainHatArmorLayer.registerOn(entityRenderer, registrationHelper)
    }


}