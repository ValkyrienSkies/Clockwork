package org.valkyrienskies.clockwork.content.physicalities.extendon

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.atan2
import kotlin.math.sqrt

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
            val thisShip = be.level!!.getShipManagingPos(be.blockPos)
            val thisPos = if (thisShip == null) be.blockPos.toJOMLD() else thisShip.transform.shipToWorld.transformPosition(be.blockPos.toJOMLD())

            val otherShip = be.level!!.getShipManagingPos(be.connectedBe!!.pos)
            val otherPos = if (otherShip == null)be.connectedBe!!.pos.toJOMLD() else otherShip.transform.shipToWorld.transformPosition(be.connectedBe!!.pos.toJOMLD())


            val anglePair = getEulerAngles(thisPos, otherPos)

            axis0 = axis0.rotateCentered(Direction.UP, anglePair.first.toFloat())
            axis1 = axis1.rotateCentered(Direction.UP, anglePair.first.toFloat())

            axis1 = axis1.rotateCentered(Direction.WEST, anglePair.second.toFloat())
        }

        axis0.light().renderInto(ms,vb)
        axis1.light().renderInto(ms,vb)



        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
    }

    companion object {
        fun getEulerAngles(position: Vector3d, target: Vector3d): Pair<Double, Double> {
            // Calculate the direction vector from the position to the target
            val direction = Vector3d(target.x - position.x, target.y - position.y, target.z - position.z)

            // Calculate yaw (rotation around the Y-axis)
            val yaw = atan2(direction.x, direction.z)

            // Calculate pitch (rotation around the X-axis)
            val pitch = atan2(direction.y, sqrt(direction.x * direction.x + direction.z * direction.z))

            // Return the angles in radians
            return Pair(yaw, pitch-Math.PI/2)
        }

    }
}