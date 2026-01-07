package org.valkyrienskies.clockwork.content.curiosities.meteor

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.createmod.catnip.render.CachedBuffers
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import org.joml.Matrix4f
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.hooks.VSGameEvents
import org.valkyrienskies.mod.common.util.toFloat
import kotlin.math.absoluteValue

object MeteorRenderer {

    val meteorList = hashMapOf<Long, MeteorVfxState>()

    fun onShipRender(ship: ClientShip, ms: PoseStack, camera: Camera, bufferSource: MultiBufferSource, pt: Float) {
        //if (!ClockworkModClient.METEOR_SHIP_IDS.contains(ship.id)) return
        //renderMeteor(ship, ms, camera, bufferSource, pt)
    }

    fun renderMeteor(ship: ClientShip, ms: PoseStack, camera: Camera, bufferSource: MultiBufferSource, pt: Float) {
        val x = ship.renderTransform.positionInWorld.x()
        val y = ship.renderTransform.positionInWorld.y()
        val z = ship.renderTransform.positionInWorld.z()
        if (!meteorList.keys.contains(ship.id)) {
            val newState = MeteorVfxState()
            newState.lastX = ship.renderTransform.positionInWorld.x()
            newState.lastY = ship.renderTransform.positionInWorld.y()
            newState.lastZ = ship.renderTransform.positionInWorld.z()
            val shipVelNormal = ship.velocity.normalize(Vector3d())
            newState.velDirX = shipVelNormal.x
            newState.velDirY = shipVelNormal.y
            newState.velDirZ = shipVelNormal.z
            newState.speed = ship.velocity.length().absoluteValue
            newState.hasLast = true
            newState.samples.clear()
            newState.samples.addFirst(TrailSample(x, y, z, age = 0f, speed = 0f))
            meteorList[ship.id] = newState
            return
        }

        val state = meteorList[ship.id]!!

        // LOD gate
        val mc = Minecraft.getInstance()
        val cam = camera
        val camPos = cam.position
        val dx = (ship.renderTransform.positionInWorld.x()) - camPos.x
        val dy = (ship.renderTransform.positionInWorld.y()) - camPos.y
        val dz = (ship.renderTransform.positionInWorld.z()) - camPos.z
        val dist2 = dx*dx + dy*dy + dz*dz
        if (dist2 > 128.0*128.0) return
        if (state.speed < 0.75) {
            if (state.velDirX.isNaN()) {
                val shipVelNormal = ship.velocity.normalize(Vector3d())
                state.velDirX = shipVelNormal.x
                state.velDirY = shipVelNormal.y
                state.velDirZ = shipVelNormal.z
                state.speed = ship.velocity.length().absoluteValue
            }
            return
        }

        //update

        val poseStack = ms
        val buffers = bufferSource
        ms.pushPose()
        // view-relative first
        ms.translate(-camPos.x, -camPos.y, -camPos.z)

        // then ship->world so ship-local verts end up in world, relative to camera
        //ms.mulPoseMatrix(Matrix4f(ship.renderTransform.shipToWorld))
//        ms.translate( ship.renderTransform.positionInWorld.x() ,
//            ship.renderTransform.positionInWorld.y(),
//            ship.renderTransform.positionInWorld.z())
//
//        ms.mulPose(ship.renderTransform.rotation.toFloat())
        val partial = mc.frameTime
        val time = (mc.level?.gameTime ?: 0L).toFloat() + partial

        val pose = ms.last()

        // 1) Ribbon trail
        renderTrailRibbon(
            vc = buffers.getBuffer(ClockworkRenderTypes.METEOR_TRAIL),
            pose = pose.pose(),
            samples = state.samples,
            camPosX = camPos.x, camPosY = camPos.y, camPosZ = camPos.z,
            camLookX = cam.lookVector.x.toDouble(), camLookY = cam.lookVector.y.toDouble(), camLookZ = cam.lookVector.z.toDouble(),
            time = time
        )

        // 2) Heated plasma shell (proxy sphere-ish)
        val head = ship.renderTransform.position
        val radius = ship.worldAABB.extent(Vector3d()).length().absoluteValue * 0.5
//        renderPlasmaShell(
//            vc = buffers.getBuffer(ClockworkRenderTypes.METEOR_PLASMA),
//            pose = pose,
//            centerX = head.x(), centerY = head.y(), centerZ = head.z(),
//            velDirX = state.velDirX, velDirY = state.velDirY, velDirZ = state.velDirZ,
//            speed = state.speed,
//            time = time,
//            radius = radius
//        )

        ms.popPose()

        // update here actually
    }

    fun renderTrailRibbon(
        vc: VertexConsumer,
        pose: Matrix4f,
        samples: ArrayDeque<TrailSample>,
        camPosX: Double, camPosY: Double, camPosZ: Double,
        camLookX: Double, camLookY: Double, camLookZ: Double,
        time: Float
    ) {
        if (samples.size < 2) return

        // Convert look to unit vector
        var lx = camLookX; var ly = camLookY; var lz = camLookZ
        run {
            val inv = 1.0 / Math.sqrt(lx*lx + ly*ly + lz*lz + 1e-12)
            lx *= inv; ly *= inv; lz *= inv
        }

        val it = samples.iterator()
        var prev = it.next()
        var segIndex = 0
        val total = samples.size - 1

        while (it.hasNext()) {
            val cur = it.next()

            // positions relative to camera
            val p0x = prev.x //- camPosX
            val p0y = prev.y //- camPosY
            val p0z = prev.z //- camPosZ
            val p1x = cur.x //- camPosX
            val p1y = cur.y //- camPosY
            val p1z = cur.z //- camPosZ

            // segment direction
            var dx = p1x - p0x
            var dy = p1y - p0y
            var dz = p1z - p0z
            val dLen2 = dx*dx + dy*dy + dz*dz
            if (dLen2 < 1e-8) {
                prev = cur
                segIndex++
                continue
            }
            val invD = 1.0 / Math.sqrt(dLen2)
            dx *= invD; dy *= invD; dz *= invD

            // "right" = segmentDir x cameraLook (billboard-ish)
            var rx = dy*lz - dz*ly
            var ry = dz*lx - dx*lz
            var rz = dx*ly - dy*lx
            val rLen2 = rx*rx + ry*ry + rz*rz
            if (rLen2 < 1e-8) {
                prev = cur
                segIndex++
                continue
            }
            val invR = 1.0 / Math.sqrt(rLen2)
            rx *= invR; ry *= invR; rz *= invR

            // Width: taper + speed influence, clamped
            val t0 = segIndex.toFloat() / total.toFloat()
            val t1 = (segIndex + 1).toFloat() / total.toFloat()
            val headness0 = 1f - t0
            val headness1 = 1f - t1

            val w0 = (0.18 + prev.speed * 0.010) * (0.35 + 0.65 * headness0)
            val w1 = (0.18 + cur.speed * 0.010) * (0.35 + 0.65 * headness1)
            val ww0 = w0.coerceIn(0.12, 0.9)
            val ww1 = w1.coerceIn(0.12, 0.9)

            // Optional turbulence: tiny sideways wobble that increases with speed
            val wob0 = (Math.sin((time * 8f + segIndex * 0.7f).toDouble()) * 0.06 * (prev.speed * 0.01).coerceIn(0.0, 1.0))
            val wob1 = (Math.sin((time * 8f + (segIndex+1) * 0.7f).toDouble()) * 0.06 * (cur.speed * 0.01).coerceIn(0.0, 1.0))

            val off0 = ww0 + wob0
            val off1 = ww1 + wob1

            // quad corners
            val v00x = p0x - rx * off0; val v00y = p0y - ry * off0; val v00z = p0z - rz * off0
            val v01x = p0x + rx * off0; val v01y = p0y + ry * off0; val v01z = p0z + rz * off0
            val v10x = p1x - rx * off1; val v10y = p1y - ry * off1; val v10z = p1z - rz * off1
            val v11x = p1x + rx * off1; val v11y = p1y + ry * off1; val v11z = p1z + rz * off1

            // alpha fades with age and towards tail
            val a0 = ((1f - prev.age / 2.0f) * (0.25f + 0.75f * headness0)).coerceIn(0f, 1f)
            val a1 = ((1f - cur.age / 2.0f) * (0.25f + 0.75f * headness1)).coerceIn(0f, 1f)

            // UV: V runs along trail; scroll can be done by shifting V with time in shader or here
            fun fract(x: Float) = x - kotlin.math.floor(x)

            val scroll = time * 0.05f
            val v0 = fract(t0 + scroll)
            val v1 = fract(t1 + scroll)

            val nnx = (-lx).toFloat()
            val nny = (-ly).toFloat()
            val nnz = (-lz).toFloat()

            // Emit two triangles. Color: keep near-white, alpha does the work.
            // (Use your preferred vertex format; below is conceptual)
            putVert(vc, pose, v00x, v00y, v00z, 0f, v0, a0, nnx, nny, nnz)
            putVert(vc, pose, v10x, v10y, v10z, 0f, v1, a1, nnx, nny, nnz)
            putVert(vc, pose, v11x, v11y, v11z, 1f, v1, a1, nnx, nny, nnz)

            putVert(vc, pose, v00x, v00y, v00z, 0f, v0, a0, nnx, nny, nnz)
            putVert(vc, pose, v11x, v11y, v11z, 1f, v1, a1, nnx, nny, nnz)
            putVert(vc, pose, v01x, v01y, v01z, 1f, v0, a0, nnx, nny, nnz)

            prev = cur
            segIndex++
        }
    }

    fun updateMeteorStateWorld(ship: ClientShip, state: MeteorVfxState) {
        val p = ship.renderTransform.positionInWorld
        val x = p.x()
        val y = p.y()
        val z = p.z()

        // Velocity direction + speed (prefer ship.velocity if it's reliable in your setup)
        val v = ship.velocity
        val sp = v.length().absoluteValue
        state.speed = sp

        if (sp > 1e-6) {
            val n = v.normalize(Vector3d())
            state.velDirX = n.x
            state.velDirY = n.y
            state.velDirZ = n.z
        }

        // push newest world position
        state.samples.addFirst(TrailSample(x, y, z, age = 0f, speed = sp.toFloat()))
        while (state.samples.size > 40) state.samples.removeLast()

        // age out old samples
        val tmp = ArrayDeque<TrailSample>(state.samples.size)
        while (state.samples.isNotEmpty()) {
            val s = state.samples.removeLast()
            val aged = s.copy(age = s.age + 1f / 20f)
            if (aged.age <= 2.0f) tmp.addFirst(aged)
        }
        state.samples.addAll(tmp)
    }

    // Replace this with the exact VertexConsumer calls matching your RenderType's vertex format.
    private fun putVert(vc: VertexConsumer, poseStack: Matrix4f, x: Double, y: Double, z: Double, u: Float, v: Float, a: Float, normalX: Float, normalY: Float, normalZ: Float, r: Int = 255, g: Int = 255, b: Int = 255) {
        vc.vertex(poseStack, x.toFloat(), y.toFloat(), z.toFloat())
            .color(r, g, b, (a * 255f).toInt())
            .uv(u, v)
            .overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(0xF000F0) // fullbright; or remove if you want lighting
            .normal(normalX, normalY, normalZ)
            .endVertex()
    }

//    fun renderPlasmaShell(
//        vc: VertexConsumer,
//        pose: PoseStack.Pose,
//        centerX: Double, centerY: Double, centerZ: Double,
//        velDirX: Double, velDirY: Double, velDirZ: Double,
//        speed: Double,
//        time: Float,
//        radius: Double
//    ) {
//        // Normalize -velDir
//        var fx = velDirX; var fy = velDirY; var fz = velDirZ
//        run {
//            val inv = 1.0 / Math.sqrt(fx*fx + fy*fy + fz*fz + 1e-12)
//            fx *= inv; fy *= inv; fz *= inv
//        }
//
//        val heat = ((speed - 0.75) / 8.0).coerceIn(0.0, 1.0)
//        val power = 2.5 + 3.5 * heat
//        val baseAlpha = (0.18 + 0.42 * heat)
//        val glowBias = (0.35 + 0.65 * heat)
//        val scale = radius * (1.05 + 0.08 * heat)
//
//        val packedLight = 0xF000F0
//
//        val mesh = SphereMesh.mesh
//        val pos = mesh.positions
//        val nrm = mesh.normals
//        val uv  = mesh.uvs
//        val ind = mesh.indices
//
//        for (i in ind.indices step 3) {
//            for (k in 0..2) {
//                val vi = ind[i + k]
//
//                val nx = nrm[vi*3+0].toDouble()
//                val ny = nrm[vi*3+1].toDouble()
//                val nz = nrm[vi*3+2].toDouble()
//
//                var d = nx*fx + ny*fy + nz*fz
//                if (d < 0.0) d = 0.0
//                val front = Math.pow(d, power)
//
//                // breakup
//                val n = Math.sin((nx*7.0 + ny*11.0 + nz*5.0) * 3.0 + time * 6.0)
//                val breakup = (0.75 + 0.25 * n)
//
//                val a = (baseAlpha * front * breakup).coerceIn(0.0, 0.85)
//                //if (a <= 0.002) continue
//
//                val px = centerX + nx * scale
//                val py = centerY + ny * scale
//                val pz = centerZ + nz * scale
//
//                val hot = (front * glowBias).coerceIn(0.0, 1.0)
//                val r = (180 + 75 * hot).toInt().coerceIn(0, 255)
//                val g = ( 80 + 120 * hot).toInt().coerceIn(0, 255)
//                val b = ( 20 + 100 * hot).toInt().coerceIn(0, 255)
//
//                val u0 = uv[vi*2+0]
//                val v0 = uv[vi*2+1]
//
//                putVert(
//                    vc, pose.pose(),
//                    px, py, pz,
//                    u0, v0,
//                    a.toFloat(),
//                    nx.toFloat(), ny.toFloat(), nz.toFloat(),
//                    r,g,b
//                )
//            }
//        }
//    }
}

data class TrailSample(
    val x: Double, val y: Double, val z: Double,
    val age: Float,
    val speed: Float
)

class MeteorVfxState {
    var hasLast = false
    var lastX = 0.0
    var lastY = 0.0
    var lastZ = 0.0

    var velDirX = 0.0
    var velDirY = 1.0
    var velDirZ = 0.0
    var speed = 0.0

    // newest at index 0
    val samples = ArrayDeque<TrailSample>(48)
}

data class HeatQuad(
    val x0: Float, val y0: Float, val z0: Float,
    val x1: Float, val y1: Float, val z1: Float,
    val nx: Float, val ny: Float, val nz: Float,
    val u0: Float, val v0: Float, val u1: Float, val v1: Float
)

class MeteorHeatMesh(
    val quads: List<HeatQuad>,
    val builtForVersion: Int // increment when meteor blocks change
)

