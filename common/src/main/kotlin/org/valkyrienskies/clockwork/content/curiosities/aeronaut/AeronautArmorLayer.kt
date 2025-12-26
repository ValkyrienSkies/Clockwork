package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import com.mojang.blaze3d.vertex.PoseStack
import net.createmod.catnip.animation.AnimationTickHolder
import net.createmod.catnip.render.CachedBuffers
import net.createmod.catnip.render.SuperByteBuffer
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback
import net.minecraft.client.Minecraft
import net.minecraft.client.model.EntityModel
import net.minecraft.client.model.HumanoidModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.RenderLayerParent
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.block.Blocks
import org.joml.Quaternionf
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautGogglesRenderer.Companion.fromModelSpace
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.IAeronautEquipment.Companion.isWearingAnyAeronaut
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.IAeronautEquipment.Companion.wearingAeronautInSlot
import org.valkyrienskies.mod.api.getShipManagingEntity
import org.valkyrienskies.mod.api.toJOML
import org.valkyrienskies.mod.api.transformPosition
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.IEntityDraggingInformationProvider
import kotlin.math.min
import kotlin.math.sin

open class AeronautArmorLayer<T : LivingEntity, M : EntityModel<T>?>(renderer: RenderLayerParent<T, M>?) : RenderLayer<T, M>(renderer!!) {

    override fun render(
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        entity: T,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTick: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        if (!entity.isWearingAnyAeronaut()) return
        val entityModel = parentModel
        if (entityModel !is HumanoidModel<*>) return

        val model = entityModel as HumanoidModel<*>
        val renderType = Sheets.cutoutBlockSheet()

        val player = Minecraft.getInstance().player!!
        val partialTicks = AnimationTickHolder.getPartialTicks()

        val goggleState = AeronautGogglesState.getState(player)

        //goggleState.prevFlapAngle = goggleState.flapAngle
        val maxFlapAngle = Math.toRadians(75.0).toFloat()
        val shipOn = (player as IEntityDraggingInformationProvider).draggingInformation.lastShipStoodOn ?: vsApi.getShipMountedTo(player)?.id ?: player.getShipManagingEntity()?.id
        val velocity = player.deltaMovement.toJOML().mul(20.0)
        if (shipOn != null) {
            val ship = player.clientLevel.shipObjectWorld!!.loadedShips.getById(shipOn)
            if (ship != null) {
                val playerPosInShip = ship.worldToShip.transformPosition(player.position().toJOML())
                val velAtPlayerPos = ship.angularVelocity.cross(playerPosInShip, Vector3d()).add(ship.velocity)
                    //val dragDecay = (player as IEntityDraggingInformationProvider).draggingInformation.ticksSinceStoodOnShip /
                velocity.set(velAtPlayerPos)
            }
        }
        val speed = velocity.length()
        goggleState.flapAngle = -min(maxFlapAngle, (speed.toFloat() /200f) * maxFlapAngle) //+ sin(partialTicks * 0.05f) * (speed.toFloat() / 20000f)
        //if (goggleState.flapAngle > 1) println("am i even real bro")
        //println(goggleState.flapAngle)
        if (player.wearingAeronautInSlot(EquipmentSlot.HEAD)) {
            ms.pushPose()

//        ms.scale(0.5f, 0.5f, 0.5f)
            val hatBase = ClockworkPartials.HAT_BASE

            val air = Blocks.AIR.defaultBlockState()

            //

            model.head.translateAndRotate(ms)

            hatBase.get().transforms.head.apply(false, ms)

            //flip it since it's upside down for some reason



            val quaternion = org.joml.Quaternionf()
            quaternion.setAngleAxis(Math.toRadians(180.0).toFloat(), 0f, 0f, 1f)
            ms.mulPose(quaternion)
            //ms.translate(model.head.x, model.head.y, model.head.z)

            ms.translate(-0.325, 0.195, -0.325)

            ms.pushPose()

            ms.scale(0.65f, 0.65f, 0.65f)

            //ms.translate(-0.5, -0.5, -0.5)

            CachedBuffers.partial(hatBase, air)
                .disableDiffuse<SuperByteBuffer?>()
                .light<SuperByteBuffer?>(light)
                .renderInto(ms, buffer.getBuffer(renderType))

            val flapAngle = goggleState.flapAngle
            val prevFlapAngle = goggleState.prevFlapAngle
            val interpolatedFlapAngle = prevFlapAngle + (flapAngle - prevFlapAngle) * partialTicks
            goggleState.prevFlapAngle = interpolatedFlapAngle
            ms.pushPose()
            val originLeft = fromModelSpace(2.725, 10.0, 8.0)
            //ms.translate(-originLeft.x(), -originLeft.y(), -originLeft.z())
            ms.translate(0.0, 10.0/16.0, 0.0)
            ms.mulPose(org.joml.Quaternionf().setAngleAxis(interpolatedFlapAngle.toFloat(), 0f, 0f, 1f))
            //ms.translate(originLeft.x(), originLeft.y(), originLeft.z())
            ms.translate(0.0, -10.0/16.0, 0.0)

            val hatFlapLeft = ClockworkPartials.HAT_FLAP_LEFT
            CachedBuffers.partial(hatFlapLeft, air)
                .disableDiffuse<SuperByteBuffer?>()
                .light<SuperByteBuffer?>(light)
                .renderInto(ms, buffer.getBuffer(renderType))
            ms.popPose()
            ms.pushPose()
            val originRight = fromModelSpace(13.275, 10.0, 8.0)
            //ms.translate(-originRight.x(), -originRight.y(), -originRight.z())
            ms.translate(16/16.0, 10.0/16.0, 0.0)
            ms.mulPose(org.joml.Quaternionf().setAngleAxis(interpolatedFlapAngle.toFloat(), 0f, 0f, -1f))
            //ms.translate(originRight.x(), originRight.y(), originRight.z())
            ms.translate(-16/16.0, -10.0/16.0, 0.0)

            val hatFlapRight = ClockworkPartials.HAT_FLAP_RIGHT
            CachedBuffers.partial(hatFlapRight, air)
                .disableDiffuse<SuperByteBuffer?>()
                .light<SuperByteBuffer?>(light)
                .renderInto(ms, buffer.getBuffer(renderType))
            ms.popPose()

            val wearingGoggles = AeronautGogglesState.getState(player).gogglesDown
            ms.pushPose()
            ms.pushPose()
            if (!wearingGoggles) {
                //rotate so goggles are facing up
                //println("truth")
                ms.mulPose(Quaternionf().setAngleAxis(Math.toRadians(90.0).toFloat(), 1f, 0f, 0f))
                //flip it around so it faces forwards
                ms.translate(0.0, 0.1, -0.85)

            }


            val hatGoggles = ClockworkPartials.HAT_GOGGLES
            CachedBuffers.partial(hatGoggles, air)
                .disableDiffuse<SuperByteBuffer?>()
                .light<SuperByteBuffer?>(light)
                .renderInto(ms, buffer.getBuffer(renderType))
            ms.popPose()
            ms.popPose()

            ms.popPose()

            ms.popPose()
        }

    }

    companion object {

        @JvmStatic
        fun registerOn(
            entityRenderer: EntityRenderer<*>?,
            helper: LivingEntityFeatureRendererRegistrationCallback.RegistrationHelper
        ) {
            if (entityRenderer !is LivingEntityRenderer<*, *>) return
            if (entityRenderer.model !is HumanoidModel<*>) return
            val layer: AeronautArmorLayer<*, *> = AeronautArmorLayer(entityRenderer)
            helper.register(layer)
        }


    }
}
