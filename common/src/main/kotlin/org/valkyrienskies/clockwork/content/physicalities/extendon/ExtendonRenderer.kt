package org.valkyrienskies.clockwork.content.physicalities.extendon

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import com.simibubi.create.foundation.render.CachedBufferer
import com.simibubi.create.foundation.outliner.Outliner
import com.simibubi.create.foundation.outliner.Outline
import com.simibubi.create.CreateClient
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.Direction
import net.minecraft.world.phys.AABB
import org.joml.Quaterniond
import org.joml.Quaterniondc
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.clockwork.util.render.outline.RotatedAABBOutline
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


        val outliner = CreateClient.OUTLINER

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
            var angleTriple = getEulerAngles(direction)


            axis0 = axis0.rotateCentered(Direction.UP, angleTriple.second.toFloat())
            axis1 = axis1.rotateCentered(Direction.UP, angleTriple.second.toFloat())

            axis1 = axis1.rotateCentered(Direction.WEST, angleTriple.first.toFloat())

            val minX = thisPos.x - 0.25
            val minY = thisPos.y - 0.25
            val minZ = thisPos.z
            val maxX = thisPos.x + 0.25
            val maxY = thisPos.y + 0.25
            val maxZ = thisPos.z + thisPos.distance(otherPos)
            
            println("$thisPos $otherPos ${thisPos.distance(otherPos)}")

            val aabb = AABB(minX, minY, minZ, maxX, maxY, maxZ)

            // Create and configure the outline
            val outline = RotatedAABBOutline(aabb)
            outline.rotationX = angleTriple.first.toFloat()
            outline.rotationY = angleTriple.second.toFloat()
            outline.rotationZ = angleTriple.third.toFloat()
            
            

            if (!outliner.getOutlines().containsKey(be) || (outliner.getOutlines()[be]?.outline !is RotatedAABBOutline)) outliner.showCustomOutline(be, outline)
            else outliner.editCustomOutline(be, outline)
            
            // Keep the outline alive for next frame
            outliner.keep(be)
        } else {
            // Remove outline if no connection
            outliner.remove(be)
        }

        axis0.light().renderInto(ms,vb)
        axis1.light().renderInto(ms,vb)



        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
    }

    companion object {

        fun getEulerAngles(direction: Vector3d): Triple<Double, Double, Double> {
            // Calculate yaw (rotation around the Y-axis)
            val yaw = atan2(direction.x, direction.z)

            // Calculate pitch (rotation around the X-axis)
            val pitch = atan2(direction.y, sqrt(direction.x * direction.x + direction.z * direction.z))

            // Calculate roll (rotation around the Z-axis)
            // For a direction vector, roll can be calculated using the cross product
            val up = Vector3d(0.0, 1.0, 0.0)
            val right = direction.cross(up, Vector3d())
            val roll = atan2(right.y, right.x)

            // Return the angles in radians (pitch, yaw, roll)
            return Triple(pitch + Math.PI*3/2, yaw, roll)
        }


    }
}

// TODO: MOVE THIS TO UTILS
public fun Outliner.showCustomOutline(key: Any, outline: Outline) {
    this::class.java.getDeclaredMethod("addOutline", Any::class.java, Outline::class.java)
        .apply { isAccessible = true }
        .invoke(this, key, outline)
}

public fun Outliner.editCustomOutline(key: Any, outline: Outline) {
    @Suppress("UNCHECKED_CAST")
    this::class.java.getDeclaredField("outlines")
        .apply { isAccessible = true }
        .get(this)
        .let { it as MutableMap<Any, Outliner.OutlineEntry> }[key] = Outliner.OutlineEntry(outline)
}

