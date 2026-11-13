package org.valkyrienskies.clockwork.content.schematics

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.RandomSource
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkSchematics
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandRopeCurve
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object VdexBlueprintPreview {
    internal class Mesh(val data: VdexBlueprintData) : AutoCloseable {
        private val builder = BufferBuilder(262144)
        private val occupied = data.ships.map { ship -> ship.blocks.associateBy { it.pos } }
        private var ship = 0
        private var block = 0
        private var processed = 0
        private var quads = 0
        var buffer: VertexBuffer? = null
            private set
        var connectionBuffer: VertexBuffer? = null
            private set
        val ink by lazy { VdexSketchInk.collect(data) }
        val progress get() = processed.toFloat() / data.blockCount

        init { builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY) }

        private fun vertex(p: Vector3d, n: Vector3d, u: Float, v: Float, tint: Int) {
            builder.vertex(p.x, p.y, p.z).color((tint shr 16) and 255, (tint shr 8) and 255, tint and 255, 255)
                .uv(u, v).overlayCoords(0).uv2(0).normal(n.x.toFloat(), n.y.toFloat(), n.z.toFloat()).endVertex()
        }

        fun advance(budget: Int) {
            if (buffer != null) return
            val mc = Minecraft.getInstance()
            val random = RandomSource.create(42)
            repeat(budget) {
                if (ship >= data.ships.size) return@repeat
                val source = data.ships[ship]
                val entry = source.blocks[block++]
                processed++
                val model = mc.blockRenderer.getBlockModel(entry.state)
                for (face in Direction.values().toList() + listOf(null)) {
                    if (face != null && occupied[ship][entry.pos.relative(face)]?.state?.canOcclude() == true) continue
                    random.setSeed(entry.state.getSeed(entry.pos))
                    for (quad in model.getQuads(entry.state, face, random)) {
                        if (quads >= 300000) continue
                        val tint = if (quad.isTinted) mc.blockColors.getColor(entry.state, null, null, quad.tintIndex) else -1
                        val n = Vector3d(quad.direction.stepX.toDouble(), quad.direction.stepY.toDouble(), quad.direction.stepZ.toDouble()).rotate(source.entry.relativeRot)
                        val stride = quad.vertices.size / 4
                        for (i in 0..3) {
                            val j = i * stride
                            val p = source.relativePoint(Vector3d(Float.fromBits(quad.vertices[j]).toDouble() + entry.pos.x,
                                Float.fromBits(quad.vertices[j + 1]).toDouble() + entry.pos.y, Float.fromBits(quad.vertices[j + 2]).toDouble() + entry.pos.z))
                            vertex(p, n, Float.fromBits(quad.vertices[j + 4]), Float.fromBits(quad.vertices[j + 5]), tint)
                        }
                        quads++
                    }
                }
                if (block == source.blocks.size) { ship++; block = 0 }
            }
            if (ship == data.ships.size) {
                buffer = VertexBuffer(VertexBuffer.Usage.STATIC).also { it.bind(); it.upload(builder.end()); VertexBuffer.unbind() }
                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.NEW_ENTITY)
                connections()
                val vertices = builder.end()
                if (vertices.isEmpty) vertices.release()
                else connectionBuffer = VertexBuffer(VertexBuffer.Usage.STATIC).also { it.bind(); it.upload(vertices); VertexBuffer.unbind() }
            }
        }

        private fun connections() {
            val payload = data.data.nbtData[WandLinkSchematics.ENTRY] ?: return
            val origins = payload.getList("origins", 10).filterIsInstance<CompoundTag>().associate { it.getLong("ship") to BlockPos.of(it.getLong("pos")) }
            val definitions = data.ships.associateBy { it.entry.previousId }
            fun point(anchor: WandAnchor): Vec3? {
                val origin = origins[anchor.shipId] ?: return null
                val source = definitions[anchor.shipId] ?: return null
                val p = anchor.local()
                val v = source.relativePoint(Vector3d(p.x - origin.x, p.y - origin.y, p.z - origin.z))
                return Vec3(v.x, v.y, v.z)
            }
            for (link in WandLinks.load(payload).links.values) {
                val a = point(link.a) ?: continue
                val b = point(link.b) ?: continue
                val curve = WandRopeCurve(a, b, link.length)
                val direction = b.subtract(a).normalize()
                val side = direction.cross(if (abs(direction.y) > .9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)).normalize()
                val up = direction.cross(side)
                fun sample(t: Double): Vec3 = if (link.spring) a.lerp(b, t)
                    .add(side.scale(cos(t * PI * 24) * .12)).add(up.scale(sin(t * PI * 24) * .12))
                    else if (link.rope) curve.point(t) else a.lerp(b, t)
                val segments = if (link.spring) 96 else 32
                val points = (0..segments).map { sample(it.toDouble() / segments) }
                // Neighbouring segments share one cross-section per point so the tube stays closed at bends.
                // The frame is carried along the curve to keep the square from twisting between points.
                var right: Vec3? = null
                val rings = points.indices.map { i ->
                    val tangent = points[minOf(i + 1, segments)].subtract(points[maxOf(i - 1, 0)]).normalize()
                    var r = right?.let { it.subtract(tangent.scale(it.dot(tangent))) }
                    if (r == null || r.lengthSqr() < 1e-6)
                        r = tangent.cross(if (abs(tangent.y) > .9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0))
                    val frameRight = r.normalize().also { right = it }
                    val top = tangent.cross(frameRight)
                    (0..3).map { k -> frameRight.scale(cos(k * PI / 2) * .035).add(top.scale(sin(k * PI / 2) * .035)) }
                }
                for (i in 0 until segments) {
                    val p = points[i]
                    val q = points[i + 1]
                    for (j in 0..3) {
                        val k = (j + 1) % 4
                        val n = rings[i][j].add(rings[i][k]).add(rings[i + 1][j]).add(rings[i + 1][k]).normalize()
                        for (v in listOf(p.add(rings[i][j]), q.add(rings[i + 1][j]), q.add(rings[i + 1][k]), p.add(rings[i][k])))
                            vertex(Vector3d(v.x, v.y, v.z), Vector3d(n.x, n.y, n.z), -1f, -1f, 0x8A8074)
                    }
                }
            }
        }

        override fun close() { buffer?.close(); connectionBuffer?.close(); builder.discard() }
    }

    private val worldSketch = VdexWorldSketchRenderer()
    private val meshes = LinkedHashMap<VdexBlueprintData, Mesh>(4, .75f, true)
    internal fun mesh(data: VdexBlueprintData): Mesh = meshes.getOrPut(data) {
        if (meshes.size == 2) { val entry = meshes.entries.first(); entry.value.close(); meshes.remove(entry.key) }
        Mesh(data)
    }

    fun tick() { meshes.values.forEach { it.advance(512 / meshes.size.coerceAtLeast(1)) } }
    fun clear() { meshes.values.forEach { it.close() }; meshes.clear(); worldSketch.close() }

    fun renderWorld(pose: PoseStack, buffers: MultiBufferSource, camera: Vec3) {
        val stack = Minecraft.getInstance().player?.mainHandItem ?: return
        if (stack.item !is VdexBlueprintItem) return
        val tag = stack.tag ?: return
        val view = VdexPlacementClient.view() ?: if (tag.getBoolean("Deployed"))
            VdexPlacement(Vec3(tag.getDouble("VdexX"), tag.getDouble("VdexY"), tag.getDouble("VdexZ")), tag.getFloat("VdexYaw")) else return
        val data = VdexBlueprintClient.get(tag.getString("VdexFile")) ?: return
        val mesh = mesh(data)
        if (mesh.buffer == null) return
        if (buffers is MultiBufferSource.BufferSource) buffers.endBatch()
        val model = Matrix4f(RenderSystem.getModelViewMatrix()).mul(pose.last().pose())
            .translate((view.position.x - camera.x).toFloat(), (view.position.y - camera.y).toFloat(), (view.position.z - camera.z).toFloat())
            .rotate(Quaternionf().rotationY(Math.toRadians(view.yaw.toDouble()).toFloat()))
        worldSketch.render(mesh, model, RenderSystem.getProjectionMatrix())
        VdexPlacementClient.renderOutline(pose, camera)
    }
}
