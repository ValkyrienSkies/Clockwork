package org.valkyrienskies.clockwork.content.schematics

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.AllPartialModels
import net.createmod.catnip.client.render.model.BakedModelBufferer
import net.createmod.catnip.client.render.model.ShadeSeparatedBufferSource
import net.createmod.catnip.levelWrappers.SchematicLevel
import net.createmod.catnip.render.CachedBuffers
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.levelgen.structure.BoundingBox
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPartials
import org.valkyrienskies.clockwork.content.physicalities.wheel.LayeredTireRenderer
import org.valkyrienskies.clockwork.content.physicalities.wheel.TireMesh
import org.valkyrienskies.clockwork.content.physicalities.wheel.WheelVisuals
import org.valkyrienskies.clockwork.content.physicalities.wheel.TireType
import org.valkyrienskies.clockwork.content.physicalities.wheel.WheelItem
import org.valkyrienskies.clockwork.content.physicalities.wheel.WheelSize
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkSchematics
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandRopeCurve
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object VdexBlueprintPreview {
    private class PreviewLevel(level: Level, ship: VdexBlueprintData.Ship) : SchematicLevel(level) {
        private val savedEntities = HashMap<BlockPos, CompoundTag>()

        init {
            renderMode = true
            setBounds(BoundingBox(0, 0, 0, ship.size.x - 1, ship.size.y - 1, ship.size.z - 1))
            for (entry in ship.blocks) {
                blockMap[entry.pos] = entry.state
                entry.nbt?.let { savedEntities[entry.pos] = it }
            }
        }

        override fun getBlockEntity(pos: BlockPos): BlockEntity? {
            val entity = super.getBlockEntity(pos) ?: return null
            val tag = savedEntities.remove(pos) ?: return entity
            try {
                entity.load(tag.copy())
            } catch (e: RuntimeException) {
                ClockworkMod.LOGGER.warn("Could not read blueprint preview data for ${entity.blockState} at $pos", e)
            }
            return entity
        }

        override fun processBlockStateForPrinting(state: BlockState) = state
        override fun getMinBuildHeight() = 0
        override fun getHeight() = 256
    }

    internal class Mesh(val data: VdexBlueprintData) : AutoCloseable {
        private val builder = BufferBuilder(262144)
        private var previewLevel: PreviewLevel? = null
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
            val level = Minecraft.getInstance().level ?: return
            var remaining = budget
            val atlas = MeshConsumer(true)
            val buffers = ShadeSeparatedBufferSource { _, _ -> atlas }
            while (remaining > 0 && ship < data.ships.size) {
                val source = data.ships[ship]
                val count = minOf(remaining, source.blocks.size - block)
                if (quads < 300000) {
                    val world = previewLevel ?: PreviewLevel(level, source).also { previewLevel = it }
                    val batch = source.blocks.subList(block, block + count)
                    val pose = shipPose(source)
                    BakedModelBufferer.bufferBlocks(batch.asSequence().map { it.pos }.iterator(), world, pose, false, buffers)
                    for (entry in batch) wheel(source, entry)
                }
                processed += count
                block += count
                remaining -= count
                if (block == source.blocks.size) {
                    ship++
                    block = 0
                    previewLevel = null
                }
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

        private fun shipPose(source: VdexBlueprintData.Ship): PoseStack {
            val pose = PoseStack()
            pose.translate(source.entry.relativePos.x(), source.entry.relativePos.y(), source.entry.relativePos.z())
            pose.mulPose(Quaternionf(source.entry.relativeRot))
            val scale = source.entry.scale.toFloat()
            pose.scale(scale, scale, scale)
            pose.translate(-source.pivot.x, -source.pivot.y, -source.pivot.z)
            return pose
        }

        private fun wheel(source: VdexBlueprintData.Ship, entry: VdexBlueprintData.Block) {
            val wheel = entry.wheelSize() ?: return
            val tire = entry.tire(wheel)
            val facing = entry.state.getValue(BlockStateProperties.FACING)
            val pose = shipPose(source)
            pose.translate(entry.pos.x.toDouble(), entry.pos.y.toDouble(), entry.pos.z.toDouble())
            val atlas = MeshConsumer(true)
            CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, entry.state, facing.opposite).renderInto(pose, atlas)
            pose.translate(.5, .5, .5)
            pose.mulPose(Quaternionf(WheelSize.axleRotation(facing.stepX.toDouble(), facing.stepY.toDouble(), facing.stepZ.toDouble())))
            val buffers = MultiBufferSource { atlas }
            val wood = WheelItem.wood(entry.nbt)
            if (tire != null) {
                if (WheelVisuals.treadTexture(tire.type, wood) != null)
                    TireMesh.render(tire, emptyList(), pose, MeshConsumer(false), 0, 0)
                else LayeredTireRenderer.render(null, null, tire, emptyList(), pose, buffers, 0, 0, 0f,
                    if (tire.type == TireType.WOOD) WheelVisuals.planks(wood) else null)
                LayeredTireRenderer.cleats(tire, pose, buffers, 0, 0)
            }
            pose.mulPose(Quaternionf().rotationY((-PI / 2).toFloat()))
            pose.translate(-.5, -.5, -.5)
            val rim = when (wheel) {
                WheelSize.SMALL -> ClockworkPartials.WHEEL_RIM
                WheelSize.MEDIUM -> ClockworkPartials.MEDIUM_WHEEL_RIM
                WheelSize.LARGE -> ClockworkPartials.LARGE_WHEEL_RIM
            }
            CachedBuffers.partial(rim, entry.state).renderInto(pose,
                WheelVisuals.rimBuffer(atlas, tire?.type, wood))
        }

        private inner class MeshConsumer(private val textured: Boolean) : VertexConsumer {
            private val point = Vector3d()
            private val normal = Vector3d()
            private var u = -1f
            private var v = -1f
            private var tint = 0xFFFFFF
            private var vertices = 0
            override fun vertex(x: Double, y: Double, z: Double) = apply { point.set(x, y, z) }
            override fun color(r: Int, g: Int, b: Int, a: Int) = apply {
                val shade = if (textured) 1.0 else .45
                tint = ((r * shade).toInt() shl 16) or ((g * shade).toInt() shl 8) or (b * shade).toInt()
            }
            override fun uv(u: Float, v: Float) = apply { this.u = if (textured) u else -1f; this.v = if (textured) v else -1f }
            override fun overlayCoords(u: Int, v: Int) = this
            override fun uv2(u: Int, v: Int) = this
            override fun normal(x: Float, y: Float, z: Float) = apply { normal.set(x.toDouble(), y.toDouble(), z.toDouble()) }
            override fun defaultColor(r: Int, g: Int, b: Int, a: Int) { color(r, g, b, a) }
            override fun unsetDefaultColor() { tint = 0xFFFFFF }
            override fun endVertex() {
                if (quads >= 300000) return
                vertex(point, normal, u, v, tint)
                if (++vertices % 4 == 0) quads++
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
                for (i in 0 until segments) {
                    val p = sample(i.toDouble() / segments)
                    val q = sample((i + 1.0) / segments)
                    val tangent = q.subtract(p).normalize()
                    val right = tangent.cross(if (abs(tangent.y) > .9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)).normalize()
                    val top = tangent.cross(right)
                    for (j in 0..3) {
                        fun offset(k: Int) = right.scale(cos(k * PI / 2) * .035).add(top.scale(sin(k * PI / 2) * .035))
                        val n = offset(j).add(offset(j + 1)).normalize()
                        for (v in listOf(p.add(offset(j)), q.add(offset(j)), q.add(offset(j + 1)), p.add(offset(j + 1))))
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
