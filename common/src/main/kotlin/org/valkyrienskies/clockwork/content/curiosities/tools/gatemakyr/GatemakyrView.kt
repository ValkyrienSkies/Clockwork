package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import com.mojang.blaze3d.pipeline.MainTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import org.valkyrienskies.clockwork.mixin.content.gatemakyr.GateFramebufferAccessor
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import com.mojang.blaze3d.platform.GlStateManager
import org.lwjgl.opengl.GL32
import org.lwjgl.opengl.GL30
import org.lwjgl.opengl.GL11
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.clockwork.ClockworkShaders
import org.valkyrienskies.clockwork.content.schematics.VdexPreviewRenderState
import org.valkyrienskies.clockwork.platform.PlatformUtils
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronEffects
import org.valkyrienskies.clockwork.util.render.ShaderPackCompat
import java.util.UUID
import kotlin.math.*

object GatemakyrView {
    @JvmField var shaderCaptureSupported = false
    @JvmField var releaseShaderPass: Runnable? = null
    @JvmField var captureKey: Pair<UUID, Int>? = null
    @JvmField var beforeHandDepth = 0
    @JvmField var afterHandDepth = 0
    @JvmField var captureSerial = 0
    @JvmField var exitClip = Vector4f()
    @JvmField var isolateShaders = false
    internal var playerCamera: Camera? = null
        private set
    @JvmField var remoteTerrainCenter: Vec3? = null
    @JvmField var camera: Camera? = null
    @JvmField var target: TextureTarget? = null
    @JvmStatic val vanillaTerrain by lazy {
        listOf("sodium", "embeddium", "rubidium").none { PlatformUtils.isModLoaded(it) }
    }
    private class Feed(val texture: TextureTarget, val width: Int, val height: Int) {
        var rendered = Long.MIN_VALUE
        var region = Region(0, 0, 1, 1)
        var sign = 0.0
        var source: GatemakyrTarget? = null
        var destination: GatemakyrTarget? = null
    }
    private data class Region(val x: Int, val y: Int, val width: Int, val height: Int)
    private data class Request(val key: Pair<UUID, Int>, val entrance: GatemakyrTarget.Frame, val exit: GatemakyrTarget.Frame,
                               val source: GatemakyrTarget, val destination: GatemakyrTarget, val sign: Double,
                               val pixels: Float, val width: Int, val height: Int, val region: Region)
    private val feeds = mutableMapOf<Pair<UUID, Int>, Feed>()
    private val visible = mutableSetOf<Pair<UUID, Int>>()
    @JvmStatic fun currentFrame() = frame
    internal var frame = 0L
        private set
    private var packTarget: MainTarget? = null

    private inline fun preserveFramebuffers(release: () -> Unit) {
        val draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        try {
            release()
        } finally {
            if (draw == 0 || GL30.glIsFramebuffer(draw)) GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, draw)
            if (read == 0 || GL30.glIsFramebuffer(read)) GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read)
        }
    }

    private fun releasePack() {
        if (releaseShaderPass == null && packTarget == null) return
        preserveFramebuffers {
            releaseShaderPass?.run()
            packTarget?.destroyBuffers()
            packTarget = null
        }
    }

    private inline fun discardFeeds(predicate: (Map.Entry<Pair<UUID, Int>, Feed>) -> Boolean) {
        val discarded = feeds.entries.filter(predicate)
        if (discarded.isEmpty()) return
        preserveFramebuffers {
            for ((key, feed) in discarded) {
                feeds.remove(key)
                feed.texture.destroyBuffers()
            }
        }
    }

    fun clear() {
        beforeHandDepth = 0
        afterHandDepth = 0
        releasePack()
        discardFeeds { true }
        visible.clear()
    }

    private class ExitCamera : Camera() {
        fun place(level: ClientLevel, main: Camera, passage: GatemakyrPassage, pt: Float) {
            setup(level, main.entity, false, false, pt)
            val forward = passage.direction(Vec3(main.lookVector))
            val up = passage.direction(Vec3(main.upVector))
            val left = up.cross(forward)
            setRotation(Math.toDegrees(atan2(-forward.x, forward.z)).toFloat(), Math.toDegrees(-asin(forward.y.coerceIn(-1.0, 1.0))).toFloat())
            rotation().setFromNormalized(Matrix3f().setColumn(0, left.toVector3f()).setColumn(1, up.toVector3f()).setColumn(2, forward.toVector3f()))
            lookVector.set(forward.toVector3f())
            upVector.set(up.toVector3f())
            leftVector.set(left.toVector3f())
            setPosition(passage.point(main.position))
        }
        override fun isDetached() = true
    }

    private fun bucket(size: Float, limit: Int): Int {
        var n = 32
        while (n < size && n < limit) n *= 2
        return n.coerceAtMost(limit)
    }

    private var outlineGate: Pair<UUID, Int>? = null

    @JvmStatic fun selectOutline(enabled: Boolean): Boolean {
        val path = (Minecraft.getInstance().hitResult as? GatemakyrHit)?.gatePath
        outlineGate = if (enabled) path?.gates?.singleOrNull()?.packet?.let { it.owner to it.side } else null
        return enabled && path == null
    }

    @JvmStatic fun capture(pose: PoseStack, pt: Float, deadline: Long) {
        if (target != null || ShaderPackCompat.shadowPass()) return
        frame++
        visible.clear()
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        GravitronEffects.beginWorldFrame(pose, mc.gameRenderer.mainCamera.position, pt)
        val rifts = GatemakyrClient.rifts
        discardFeeds { (key, feed) -> key !in rifts || frame - feed.rendered > 120 }
        GatemakyrEffects.prepareShaderComposite(RenderSystem.getProjectionMatrix())
        if (rifts.isEmpty()) releasePack()
        if (rifts.size < 2) return
        if (ClockworkShaders.gateSurface == null || (ShaderPackCompat.enabled() && !shaderCaptureSupported)) return
        val main = mc.gameRenderer.mainCamera
        val eye = main.position
        val projection = Matrix4f(RenderSystem.getProjectionMatrix())
        val clip = Matrix4f(projection).mul(pose.last().pose())
        val frustum = Frustum(pose.last().pose(), projection).also { it.prepare(eye.x, eye.y, eye.z) }
        val requests = mutableListOf<Request>()
        for ((key, rift) in rifts) {
            val source = rift.packet.target ?: continue
            val destination = rifts[key.first to (1 - key.second)]?.packet?.target ?: continue
            val a = source.frame(level, true) ?: continue
            val b = destination.frame(level, true) ?: continue
            if (eye.distanceToSqr(a.position) >= 96.0 * 96.0 || !level.hasChunkAt(BlockPos.containing(b.position))) continue
            val side = eye.subtract(a.position).dot(a.normal.normalize())
            if (side < 0 && (source.surface || destination.surface)) continue
            val facing = when {
                abs(side) > 0.000001 -> sign(side)
                source.surface || destination.surface || Vec3(main.lookVector).dot(a.normal) <= 0 -> 1.0
                else -> -1.0
            }
            val u = a.u.scale(source.width / 2.0)
            val v = a.v.scale(source.height / 2.0)
            val corners = listOf(a.position.subtract(u).subtract(v), a.position.add(u).subtract(v), a.position.add(u).add(v), a.position.subtract(u).add(v))
            if (!frustum.isVisible(AABB(corners[0], corners[2]).minmax(AABB(corners[1], corners[3])).inflate(0.15))) continue
            val screen = corners.map { point ->
                val p = clip.transform(Vector4f(point.subtract(eye).toVector3f(), 1f))
                if (p.w <= 0.001f) null else Vector2f(p.x * mc.mainRenderTarget.viewWidth / (2 * p.w), p.y * mc.mainRenderTarget.viewHeight / (2 * p.w))
            }
            fun edge(i: Int, j: Int): Float {
                val first = screen[i] ?: return 1024f
                return screen[j]?.distance(first) ?: 1024f
            }
            val w = max(edge(0, 1), edge(3, 2))
            val h = max(edge(0, 3), edge(1, 2))
            if (w * h < 4f) continue
            val viewport = mc.mainRenderTarget
            val region = if (screen.any { it == null }) Region(0, 0, viewport.viewWidth, viewport.viewHeight) else {
                val points = screen.filterNotNull()
                val x = floor(points.minOf { it.x } + viewport.viewWidth * 0.5f - 1).toInt().coerceAtLeast(0)
                val y = floor(points.minOf { it.y } + viewport.viewHeight * 0.5f - 1).toInt().coerceAtLeast(0)
                val right = ceil(points.maxOf { it.x } + viewport.viewWidth * 0.5f + 1).toInt().coerceAtMost(viewport.viewWidth)
                val top = ceil(points.maxOf { it.y } + viewport.viewHeight * 0.5f + 1).toInt().coerceAtMost(viewport.viewHeight)
                if (right <= x || top <= y) continue
                Region(x, y, right - x, top - y)
            }
            val width = bucket(region.width.toFloat(), viewport.viewWidth)
            val height = bucket(region.height.toFloat(), viewport.viewHeight)
            requests.add(Request(key, a, b, source, destination, facing, sqrt(w * h), width, height, region))
        }
        val selected = requests.sortedByDescending { it.pixels }.take(8)
        val keep = selected.mapTo(mutableSetOf()) { it.key }
        discardFeeds { it.key !in keep }
        val due = selected.filter { req ->
            val feed = feeds[req.key]
            val valid = feed != null && feed.source == req.source && feed.destination == req.destination && feed.sign == req.sign
            if (valid) visible.add(req.key)
            !valid || feed!!.region != req.region || frame - feed.rendered >= when {
                req.pixels < 32 -> 8
                req.pixels < 80 -> 3
                else -> 1
            }
        }
        if (due.isEmpty()) return
        val primary = due.first()
        val second = due.drop(1).minByOrNull { feeds[it.key]?.rendered ?: Long.MIN_VALUE }
        val sorting = RenderSystem.getVertexSorting()
        val inverse = Matrix3f(RenderSystem.getInverseViewRotationMatrix())
        VdexPreviewRenderState().use {
            try {
                for (request in listOfNotNull(primary, second)) {
                    render(level, main, request, pt, deadline, pose, projection)
                    visible.add(request.key)
                }
            } finally {
                camera = null
                target = null
                RenderSystem.setProjectionMatrix(projection, sorting)
                RenderSystem.setInverseViewRotationMatrix(inverse)
            }
        }
    }

    private fun render(level: ClientLevel, main: Camera, req: Request, pt: Float, deadline: Long, playerPose: PoseStack, playerProjection: Matrix4f) {
        val mc = Minecraft.getInstance()
        var feed = feeds[req.key]
        if (feed == null || feed.width != req.width || feed.height != req.height) {
            feed?.texture?.destroyBuffers()
            feed = Feed(TextureTarget(req.width, req.height, true, Minecraft.ON_OSX), req.width, req.height)
            feed.texture.setFilterMode(GL11.GL_LINEAR)
            feeds[req.key] = feed
        }
        val passage = GatemakyrPassage(req.entrance, req.exit)
        val cam = ExitCamera().also { it.place(level, main, passage, pt) }
        val forward = req.exit.normal.normalize().scale(req.sign)
        val offset = req.exit.position.subtract(cam.position)
        val packed = ShaderPackCompat.enabled()
        val playerTarget = mc.mainRenderTarget
        val output = if (packed) {
            val buffer = packTarget ?: MainTarget(playerTarget.width, playerTarget.height).also { packTarget = it }
            if (buffer.width != playerTarget.width || buffer.height != playerTarget.height)
                buffer.resize(playerTarget.width, playerTarget.height, Minecraft.ON_OSX)
            buffer
        } else feed.texture
        val framebuffer = mc as GateFramebufferAccessor
        val region = req.region
        val rotation = Matrix3f().setColumn(0, passage.direction(Vec3(1.0, 0.0, 0.0)).toVector3f())
            .setColumn(1, passage.direction(Vec3(0.0, 1.0, 0.0)).toVector3f())
            .setColumn(2, passage.direction(Vec3(0.0, 0.0, 1.0)).toVector3f())
        val pose = PoseStack()
        pose.last().pose().set(playerPose.last().pose())
        pose.last().normal().set(playerPose.last().normal())
        pose.mulPose(Quaternionf().setFromNormalized(rotation.transpose()))
        val cullingProjection = if (packed) playerProjection else {
            val sx = playerTarget.viewWidth.toFloat() / region.width
            val sy = playerTarget.viewHeight.toFloat() / region.height
            Matrix4f().translation(sx - 1f - 2f * region.x / region.width, sy - 1f - 2f * region.y / region.height, 0f)
                .scale(sx, sy, 1f).mul(playerProjection)
        }
        val projection = if (packed) playerProjection else GatemakyrClip.projection(cullingProjection, pose.last().pose(), offset, forward)
        if (packed) {
            exitClip = GatemakyrClip.windowPlane(projection, pose.last().pose(), offset, forward, output.width, output.height)
            captureSerial++
            captureKey = req.key
        }
        playerCamera = main
        isolateShaders = packed
        val viewDistance = mc.options.effectiveRenderDistance
        remoteTerrainCenter = req.exit.position.takeIf {
            maxOf(abs(it.x - main.position.x), abs(it.z - main.position.z)) > (viewDistance - 2).coerceAtLeast(2) * 16
        }
        camera = cam
        target = feed.texture
        try {
            framebuffer.`clockwork$setGateFramebuffer`(output)
            RenderSystem.disableScissor()
            RenderSystem.depthMask(true)
            output.clear(Minecraft.ON_OSX)
            output.bindWrite(true)
            RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN)
            RenderSystem.setInverseViewRotationMatrix(Matrix3f(pose.last().normal()).invert())
            mc.levelRenderer.prepareCullFrustum(pose, cam.position, cullingProjection)
            mc.profiler.push("gatemakyr")
            try {
                (mc.levelRenderer as GatemakyrRenderPass).withGatemakyrTarget {
                    mc.levelRenderer.renderLevel(pose, pt, deadline, req.key == outlineGate, cam, mc.gameRenderer, mc.gameRenderer.lightTexture(), projection)
                }
            } finally {
                mc.profiler.pop()
            }
            output.bindWrite(true)
            RenderSystem.setProjectionMatrix(projection, VertexSorting.DISTANCE_TO_ORIGIN)
            drawNested(level, pose, cullingProjection, cam.position, req.key, req.exit.position, forward, pt)
            GatemakyrEffects.renderProjectedBeams(pose, cam.position, pt)
            if (packed) {
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, output.frameBufferId)
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, feed.texture.frameBufferId)
                GL30.glBlitFramebuffer(region.x, region.y, region.x + region.width, region.y + region.height,
                    0, 0, feed.width, feed.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR)
            }
        } finally {
            remoteTerrainCenter = null
            framebuffer.`clockwork$setGateFramebuffer`(playerTarget)
            isolateShaders = false
            playerCamera = null
            captureKey = null
            target = null
            camera = null
        }
        feed.rendered = frame
        feed.region = region
        feed.sign = req.sign
        feed.source = req.source
        feed.destination = req.destination
    }

    private fun drawNested(level: ClientLevel, pose: PoseStack, lens: Matrix4f, eye: Vec3, entranceKey: Pair<UUID, Int>,
                           exit: Vec3, forward: Vec3, pt: Float) {
        val exitKey = entranceKey.first to (1 - entranceKey.second)
        val frustum = Frustum(pose.last().pose(), lens).also { it.prepare(eye.x, eye.y, eye.z) }
        for ((key, rift) in GatemakyrClient.rifts) {
            if (key == exitKey) continue
            val mouth = rift.packet.target ?: continue
            val frame = mouth.frame(level, true) ?: continue
            if (frame.position.distanceToSqr(eye) >= 96.0 * 96.0) continue
            val relative = frame.position.subtract(exit)
            val side = eye.subtract(frame.position).dot(frame.normal)
            if (side < 0 && (mouth.surface || GatemakyrClient.rifts[key.first to (1 - key.second)]?.packet?.target?.surface == true)) continue
            val u = frame.u.scale(mouth.width * 0.5)
            val v = frame.v.scale(mouth.height * 0.5)
            if (relative.dot(forward) + abs(u.dot(forward)) + abs(v.dot(forward)) <= 0.00001) continue
            val diagonal = u.add(v)
            val other = u.subtract(v)
            if (!frustum.isVisible(AABB(frame.position.subtract(diagonal), frame.position.add(diagonal)).minmax(
                    AABB(frame.position.subtract(other), frame.position.add(other))).inflate(0.01))) continue
            val plane = Vector3f((2 * u.dot(forward)).toFloat(), (2 * v.dot(forward)).toFloat(),
                relative.subtract(diagonal).dot(forward).toFloat())
            drawSurface(pose, frame, eye, rift, (level.gameTime - rift.packet.startedAt + pt).coerceAtLeast(0f),
                (level.gameTime % 24000) + pt, null, plane)
        }
    }

    fun available(owner: UUID, side: Int): Boolean {
        if (target != null || (owner to side) !in visible) return false
        val view = vsApi.currentShipRenderView
        return view == null || view.camera === Minecraft.getInstance().gameRenderer.mainCamera
    }

    fun draw(ms: PoseStack, frame: GatemakyrTarget.Frame, eye: Vec3, rift: GatemakyrClient.Rift, age: Float, time: Float) {
        val feed = feeds[rift.packet.owner to rift.packet.side] ?: return
        drawSurface(ms, frame, eye, rift, age, time, feed, null)
    }

    private fun drawSurface(ms: PoseStack, frame: GatemakyrTarget.Frame, eye: Vec3, rift: GatemakyrClient.Rift,
                            age: Float, time: Float, feed: Feed?, clip: Vector3f?) {
        val shader = ClockworkShaders.gateSurface ?: return
        val source = rift.packet.target ?: return
        val bounds = source.opening(frame, age)
        val width = source.width.toFloat()
        val height = source.height.toFloat()
        VdexPreviewRenderState(3).use {
            RenderSystem.enableDepthTest()
            RenderSystem.depthMask(true)
            RenderSystem.disableCull()
            RenderSystem.disableBlend()
            RenderSystem.setShaderTexture(0, feed?.texture?.colorTextureId ?: 0)
            RenderSystem.setShader { shader }
            val maskHand = feed != null && ShaderPackCompat.enabled() && beforeHandDepth != 0 && afterHandDepth != 0
            shader.safeGetUniform("MaskHand").set(if (maskHand) 1 else 0)
            if (maskHand) {
                RenderSystem.setShaderTexture(1, beforeHandDepth)
                RenderSystem.setShaderTexture(2, afterHandDepth)
            }
            feed?.region?.let { region ->
                shader.safeGetUniform("ScreenUV").set(1f / region.width, 1f / region.height,
                    -region.x.toFloat() / region.width, -region.y.toFloat() / region.height)
            }
            if (clip != null) shader.safeGetUniform("NestedClip").set(clip.x, clip.y, clip.z)
            shader.safeGetUniform("Gate").set(width, height, time, if (clip == null) 0f else rift.packet.side + 1f)
            shader.safeGetUniform("Opening").set((bounds.minX / width + 0.5).toFloat(), (bounds.minY / height + 0.5).toFloat(),
                (bounds.maxX / width + 0.5).toFloat(), (bounds.maxY / height + 0.5).toFloat())
            val mesh = Tesselator.getInstance().builder
            mesh.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
            val normal = frame.normal.normalize()
            val distance = eye.subtract(frame.position).dot(normal)
            val sign = feed?.sign ?: sign(distance)
            var center = frame.position.subtract(eye)
            if (feed != null && abs(distance) < 0.125) {
                val transform = Matrix4f(RenderSystem.getProjectionMatrix()).mul(RenderSystem.getModelViewMatrix()).mul(ms.last().pose())
                val origin = transform.invert().transform(Vector4f(0f, 0f, 1f, 0f))
                if (abs(origin.w) > 1e-6f) {
                    val offset = Vec3(origin.x / origin.w.toDouble(), origin.y / origin.w.toDouble(), origin.z / origin.w.toDouble())
                    val separation = (distance + offset.dot(normal)) * sign
                    center = center.subtract(normal.scale(sign * max(0.0, 0.001 - separation)))
                }
            }
            fun vertex(x: Double, y: Double) {
                val p = center.add(frame.u.scale(x)).add(frame.v.scale(y))
                mesh.vertex(ms.last().pose(), p.x.toFloat(), p.y.toFloat(), p.z.toFloat())
                    .uv((x / width + 0.5).toFloat(), (y / height + 0.5).toFloat()).endVertex()
            }
            vertex(bounds.minX, bounds.minY)
            vertex(bounds.maxX, bounds.minY)
            vertex(bounds.maxX, bounds.maxY)
            vertex(bounds.minX, bounds.maxY)
            val clamp = GL11.glIsEnabled(GL32.GL_DEPTH_CLAMP)
            try {
                if (!clamp) GL11.glEnable(GL32.GL_DEPTH_CLAMP)
                BufferUploader.drawWithShader(mesh.end())
            } finally {
                if (!clamp) GL11.glDisable(GL32.GL_DEPTH_CLAMP)
            }
        }
    }
}
