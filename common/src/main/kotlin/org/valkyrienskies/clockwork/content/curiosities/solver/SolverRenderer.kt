package org.valkyrienskies.clockwork.content.curiosities.solver

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.blockEntity.renderer.SmartBlockEntityRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.util.beam.BeamRenderer.renderBeamTube
import org.valkyrienskies.clockwork.util.beam.BeamRenderer.renderImpactBurst
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.util.toDoubles
import org.valkyrienskies.mod.common.util.toFloat
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toMinecraft

class SolverRenderer(context: BlockEntityRendererProvider.Context) : SmartBlockEntityRenderer<SolverBlockEntity>(
    context
) {
    override fun shouldRenderOffScreen(blockEntity: SolverBlockEntity): Boolean {
        return true
    }

    override fun shouldRender(blockEntity: SolverBlockEntity, cameraPos: Vec3): Boolean {
        return true
    }


    override fun renderSafe(
        be: SolverBlockEntity?,
        partialTicks: Float,
        ms: PoseStack,
        buffer: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        if (be == null) return

        val level = be.level ?: return

        val startWorld = be.getWorldPos()
        val dirWorld = be.getWorldFacing().normalize()
        val desiredEndWorld = startWorld.add(dirWorld.scale(64.0))

        val hit = be.raycastBeam(level, startWorld, desiredEndWorld, null)
        val endWorld = hit?.location ?: desiredEndWorld

        val shipOn = level.getLoadedShipManagingPos(be.blockPos)

        // --- convert WORLD -> LOCAL (relative to this block) ---
        var blockOriginWorld = Vec3.atLowerCornerOf(be.blockPos) // (x,y,z)
        var hitNormalWorld = hit?.direction?.normal?.toDoubles() ?: Vec3.ZERO
        if (shipOn != null) {

            blockOriginWorld = (shipOn as ClientShip).renderTransform.shipToWorld.transformPosition(blockOriginWorld.toJOML(),
                Vector3d()).toMinecraft()
            hitNormalWorld = shipOn.renderTransform.rotation.transform(hitNormalWorld.toJOML()).toMinecraft()
        }
        val start = startWorld.subtract(blockOriginWorld)
        val end = endWorld.subtract(blockOriginWorld)

        val time = be.clientTime + partialTicks / 20f

        val vc = buffer.getBuffer(ClockworkRenderTypes.BEAM)

        ms.pushPose()

        if (shipOn != null) {
            ms.mulPose(shipOn.renderTransform.rotation.invert(Quaterniond()).toFloat())
        }

        renderBeamTube(
            poseStack = ms,
            vc = vc,
            camPosWorld = Minecraft.getInstance().gameRenderer.mainCamera.position, // WORLD
            blockOriginWorld = blockOriginWorld, // WORLD
            startLocal = start,
            endLocal = end,
            width = 1f,
            argb = 0xFFFFFFFF.toInt(),
            segments = maxOf(32, (start.distanceTo(end) * 12.0).toInt()),
            rampSteps = 4,          // <<< thin → full width in first 4 segments
            minWidthFrac = 0.15f,   // <<< start at 15% width
            tileWorldUnits = 1f,    // 1 texture tile per this many blocks
            scrollSpeed = -4.0f,
            timeSeconds = time,
            uScale = 1.0f,
            seed = be.beamSeed.toLong()
        )

        if (hit != null) {
            val hitLocal = end
            renderImpactBurst(
                poseStack = ms,
                vc = vc,
                camPosWorld = Minecraft.getInstance().gameRenderer.mainCamera.position,
                blockOriginWorld = blockOriginWorld,
                hitPosLocal = hitLocal,
                normalWorld = hitNormalWorld,
                seed = (be.beamSeed xor 0xBADC0FFEEuL).toLong(),
                time = time,
                rays = 14,
                burstLen = 1.45f,
                width = 0.12f,
                argb = 0xFFFFFFFF.toInt()
            )
        }

        ms.popPose()

        super.renderSafe(be, partialTicks, ms, buffer, light, overlay)
    }
}
