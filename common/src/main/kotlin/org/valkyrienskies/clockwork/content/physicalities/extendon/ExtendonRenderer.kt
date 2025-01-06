package org.valkyrienskies.clockwork.content.physicalities.extendon

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.*

class ExtendonRenderer(context: BlockEntityRendererProvider.Context?) : SmartBlockEntityRenderer<ExtendonBlockEntity>(context) {

    override fun renderSafe(
        be: ExtendonBlockEntity,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {

        val vb = buffer.getBuffer(RenderType.cutout())

        var axis0 = CachedBufferer.partial(ClockworkPartials.EXTENDON_AXIS0,be.blockState)
        var axis1 = CachedBufferer.partial(ClockworkPartials.EXTENDON_AXIS1,be.blockState)

        if (be.connectedBe != null) {
            val thisShip = be.level!!.getShipManagingPos(be.blockPos) as ClientShip?
            val thisPos = if (thisShip == null) be.blockPos.toJOMLD() else thisShip.renderTransform.shipToWorld.transformPosition(be.blockPos.toJOMLD())

            val otherShip = be.level!!.getShipManagingPos(be.connectedBe!!.pos) as ClientShip?
            val otherPos = if (otherShip == null)be.connectedBe!!.pos.toJOMLD() else otherShip.renderTransform.shipToWorld.transformPosition(be.connectedBe!!.pos.toJOMLD())

            var direction = otherPos.sub(thisPos)
            if (thisShip != null) direction = thisShip.worldToShip.transformDirection(direction)
            var anglePair = getEulerAngles(direction)



            axis0 = axis0.rotateCentered(Direction.UP, anglePair.second.toFloat())
            axis1 = axis1.rotateCentered(Direction.UP, anglePair.second.toFloat())

            axis1 = axis1.rotateCentered(Direction.WEST, anglePair.first.toFloat())
        }

        axis0.light().renderInto(ms,vb)
        axis1.light().renderInto(ms,vb)



        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
    }

    companion object {

        fun getEulerAngles(direction: Vector3d): Pair<Double, Double> {
            // Calculate the direction vector from the position to the target


            // Calculate yaw (rotation around the Y-axis)
            val yaw = atan2(direction.x, direction.z)

            // Calculate pitch (rotation around the X-axis)
            val pitch = atan2(direction.y, sqrt(direction.x * direction.x + direction.z * direction.z))

            // Return the angles in radians
            return Pair(pitch + Math.PI*3/2, yaw)
        }


    }
}