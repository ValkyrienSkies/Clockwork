package org.valkyrienskies.clockwork.content.schematics

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.item.render.CustomRenderedItemModel
import com.simibubi.create.foundation.item.render.CustomRenderedItemModelRenderer
import com.simibubi.create.foundation.item.render.PartialItemModelRenderer
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkSchematics
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandRopeCurve
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.sqrt

object VdexBlueprintPreview {
    private data class Edge(val a: Vector3d, val b: Vector3d)
    private class Mesh(val data: VdexBlueprintData) {
        val edges = ArrayList<Edge>()
        val keys = HashSet<String>()
        val occupied = data.ships.map { ship -> ship.blocks.associateBy { it.pos } }
        var ship = 0
        var block = 0
        init {
            val payload = data.data.nbtData[WandLinkSchematics.ENTRY]
            if (payload != null) {
                val origins = payload.getList("origins", 10).filterIsInstance<CompoundTag>().associate { it.getLong("ship") to BlockPos.of(it.getLong("pos")) }
                val definitions = data.ships.associateBy { it.entry.previousId }
                fun point(anchor: WandAnchor): Vec3? {
                    val origin = origins[anchor.shipId] ?: return null
                    val ship = definitions[anchor.shipId] ?: return null
                    val p = anchor.local()
                    val v = ship.relativePoint(Vector3d(p.x - origin.x, p.y - origin.y, p.z - origin.z))
                    return Vec3(v.x, v.y, v.z)
                }
                for (link in WandLinks.load(payload).links.values) {
                    val a = point(link.a) ?: continue; val b = point(link.b) ?: continue
                    val curve = WandRopeCurve(a, b, link.length)
                    val direction = b.subtract(a).normalize()
                    val side = direction.cross(Vec3(.1, 1.0, .1)).normalize()
                    val up = direction.cross(side)
                    fun sample(t: Double): Vec3 = if (link.spring) a.lerp(b, t)
                        .add(side.scale(cos(t * PI * 24) * .12)).add(up.scale(sin(t * PI * 24) * .12))
                        else if (link.rope) curve.point(t) else a.lerp(b, t)
                    val segments = if (link.spring) 96 else 32
                    for (i in 0 until segments) {
                        val p = sample(i.toDouble() / segments); val q = sample((i + 1.0) / segments)
                        edges.add(Edge(Vector3d(p.x, p.y, p.z), Vector3d(q.x, q.y, q.z)))
                    }
                }
            }
        }
    }
    private val meshes = object : LinkedHashMap<VdexBlueprintData, Mesh>(4, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<VdexBlueprintData, Mesh>?) = size > 2
    }
    private fun mesh(data: VdexBlueprintData): List<Edge> = meshes.getOrPut(data) { Mesh(data) }.edges

    fun tick() {
        val dispatcher = Minecraft.getInstance().blockRenderer
        val random = RandomSource.create(42)
        val budget = 512 / max(1, meshes.size)
        for (mesh in meshes.values) repeat(budget) {
            if (mesh.ship >= mesh.data.ships.size || mesh.edges.size >= 150000) return@repeat
            val ship = mesh.data.ships[mesh.ship]
            val block = ship.blocks[mesh.block++]
            val model = dispatcher.getBlockModel(block.state)
            for (face in Direction.values().toList() + listOf(null)) {
                if (face != null && mesh.occupied[mesh.ship][block.pos.relative(face)]?.state?.canOcclude() == true) continue
                random.setSeed(block.state.getSeed(block.pos))
                for (quad in model.getQuads(block.state, face, random)) {
                    val stride = quad.vertices.size / 4
                    val points = (0..3).map { i -> ship.relativePoint(Vector3d(
                        Float.fromBits(quad.vertices[i * stride]).toDouble() + block.pos.x,
                        Float.fromBits(quad.vertices[i * stride + 1]).toDouble() + block.pos.y,
                        Float.fromBits(quad.vertices[i * stride + 2]).toDouble() + block.pos.z)) }
                    for (i in 0..3) {
                        val a = points[i]; val b = points[(i + 1) % 4]
                        fun key(v: Vector3d) = "${(v.x * 1024).toLong()},${(v.y * 1024).toLong()},${(v.z * 1024).toLong()}"
                        val ka = key(a); val kb = key(b)
                        if (mesh.keys.add(if (ka < kb) "$ka:$kb" else "$kb:$ka")) mesh.edges.add(Edge(a, b))
                    }
                }
            }
            if (mesh.block == ship.blocks.size) { mesh.ship++; mesh.block = 0 }
        }
    }

    private fun line(pose: PoseStack, buffers: MultiBufferSource, a: Vector3d, b: Vector3d, alpha: Int) {
        val normal = Vector3f((b.x - a.x).toFloat(), (b.y - a.y).toFloat(), (b.z - a.z).toFloat())
        if (normal.lengthSquared() < 1e-10f) return
        normal.normalize()
        val consumer = buffers.getBuffer(RenderType.lines())
        for (point in listOf(a, b)) consumer.vertex(pose.last().pose(), point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
            .color(144, 223, 242, alpha).normal(pose.last().normal(), normal.x, normal.y, normal.z).endVertex()
    }

    fun renderWorld(pose: PoseStack, buffers: MultiBufferSource, camera: Vec3) {
        val stack = Minecraft.getInstance().player?.mainHandItem ?: return
        if (stack.item !is VdexBlueprintItem) return
        val tag = stack.tag ?: return
        if (!tag.getBoolean("Deployed")) return
        val data = VdexBlueprintClient.get(tag.getString("VdexFile")) ?: return
        pose.pushPose()
        pose.translate(tag.getDouble("VdexX") - camera.x, tag.getDouble("VdexY") - camera.y, tag.getDouble("VdexZ") - camera.z)
        pose.mulPose(Quaternionf().rotationY(Math.toRadians(tag.getFloat("VdexYaw").toDouble()).toFloat()))
        for (edge in mesh(data)) line(pose, buffers, edge.a, edge.b, 175)
        pose.popPose()
    }

    fun sketch(pose: PoseStack, buffers: MultiBufferSource, stack: ItemStack) {
        val data = VdexBlueprintClient.get(stack.tag?.getString("VdexFile").orEmpty()) ?: return
        val rotation = Quaterniond().rotationXYZ(Math.toRadians(25.0), Math.toRadians(-35.0), 0.0)
        val center = Vector3d(data.bounds.center.x, data.bounds.center.y, data.bounds.center.z)
        val scale = .75 / max(1.0, sqrt(data.bounds.xsize * data.bounds.xsize + data.bounds.ysize * data.bounds.ysize + data.bounds.zsize * data.bounds.zsize))
        fun project(point: Vector3d): Vector3d {
            val p = Vector3d(point).sub(center).rotate(rotation).mul(scale)
            return Vector3d(p.x, p.y, .001)
        }
        val edges = mesh(data)
        val step = max(1, edges.size / 12000)
        for (i in edges.indices step step) line(pose, buffers, project(edges[i].a), project(edges[i].b), 255)
    }

    fun drawScreen(graphics: GuiGraphics, stack: ItemStack?, x: Int, y: Int, width: Int, height: Int) {
        graphics.fill(x, y, x + width, y + height, 0xFF133245.toInt())
        if (stack == null) return
        val pose = graphics.pose()
        pose.pushPose()
        pose.translate(x + width / 2.0, y + height / 2.0, 100.0)
        pose.scale(height.toFloat(), -height.toFloat(), 1f)
        sketch(pose, graphics.bufferSource(), stack)
        graphics.flush()
        pose.popPose()
    }
}

class VdexBlueprintItemRenderer : CustomRenderedItemModelRenderer() {
    override fun render(stack: ItemStack, model: CustomRenderedItemModel, renderer: PartialItemModelRenderer,
                        transformType: ItemDisplayContext, pose: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
        renderer.renderSolid(model.originalModel, light)
        pose.pushPose()
        pose.translate(0.0, 0.0, .035)
        VdexBlueprintPreview.sketch(pose, buffer, stack)
        pose.popPose()
    }
}
