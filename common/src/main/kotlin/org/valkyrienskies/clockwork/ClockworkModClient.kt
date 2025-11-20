package org.valkyrienskies.clockwork

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
import org.valkyrienskies.clockwork.effekseer.api.client.effekseer.ParticleEmitter
import org.valkyrienskies.clockwork.effekseer.api.common.DynamicParameter
import org.valkyrienskies.clockwork.effekseer.api.common.ParticleEmitterInfo
import org.valkyrienskies.clockwork.effekseer.client.installer.JarExtractor
import org.valkyrienskies.clockwork.platform.NativePlatform
import org.valkyrienskies.clockwork.effekseer.client.loader.EffekAssetLoader
import org.valkyrienskies.clockwork.effekseer.client.registry.EffectRegistry
import java.io.IOException
import java.util.*
import dev.architectury.event.events.common.TickEvent
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
    val WANDERWAND_EFFECT_RENDERER = WanderwandEffectRenderer()

    @JvmStatic
    val RESOURCE_RELOAD_LISTENER: ResourceManagerReloadListener = ClockworkReloadListener()


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
        })


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
