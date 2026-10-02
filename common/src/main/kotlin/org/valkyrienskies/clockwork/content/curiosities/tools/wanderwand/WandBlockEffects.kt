package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.util.render.RenderUtil.addRibbonSegment
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.*

/** Incremental, double-buffered selection surfaces; large selections never mean unbounded draw work. */
class WandBlockEffects {
    private data class Surface(val pos: BlockPos, val state: BlockState, val quads: List<BakedQuad>)
    var boxes: List<AABBic> = emptyList()
        private set
    private var visible = mutableListOf<Surface>()
    private var building = mutableListOf<Surface>()
    private var scan: Iterator<BlockPos>? = null
    private var count = 0
    private var builtAt = -100L
    private var center = BlockPos.ZERO
    private val random = RandomSource.create()
    var pulseOrigin = Vec3.ZERO

    fun update(tag: CompoundTag) {
        boxes = WanderwandItem.readAABBSetFromNBT(tag).filter { WandSelectionMath.volume(it) in 1..100000 }.take(512)
        visible.clear(); building.clear(); scan = null; builtAt = -100
    }
    fun clear() = update(CompoundTag())

    fun tick(level: ClientLevel) {
        val player = Minecraft.getInstance().player ?: return
        if (boxes.isEmpty()) return
        if (scan == null && (level.gameTime - builtAt > 40 || center.distSqr(player.blockPosition()) > 64)) {
            center = player.blockPosition()
            val c = center
            scan = sequence {
                for (box in boxes) {
                    for (x in max(box.minX(), c.x - 24) until min(box.maxX(), c.x + 25))
                        for (y in max(box.minY(), c.y - 24) until min(box.maxY(), c.y + 25))
                            for (z in max(box.minZ(), c.z - 24) until min(box.maxZ(), c.z + 25)) yield(BlockPos(x, y, z))
                }
            }.iterator()
            count = 0; building = mutableListOf()
        }
        val cursor = scan ?: return
        var budget = 2048
        while (budget-- > 0 && cursor.hasNext() && count < 3072) {
            val pos = cursor.next()
            if (!level.hasChunkAt(pos)) continue
            val state = level.getBlockState(pos)
            if (state.isAir || state.renderShape != RenderShape.MODEL) continue
            val quads = quads(level, pos, state, true).take(3072 - count)
            if (quads.isNotEmpty()) building.add(Surface(pos, state, quads))
            count += quads.size
        }
        if (!cursor.hasNext() || count >= 3072) { visible = building; scan = null; builtAt = level.gameTime }
    }

    private fun quads(level: ClientLevel, pos: BlockPos, state: BlockState, cull: Boolean): List<BakedQuad> {
        val model = Minecraft.getInstance().blockRenderer.getBlockModel(state)
        val quads = mutableListOf<BakedQuad>()
        for (direction in Direction.values()) {
            if (cull && !Block.shouldRenderFace(state, level, pos, direction, pos.relative(direction))) continue
            random.setSeed(state.getSeed(pos)); quads.addAll(model.getQuads(state, direction, random))
        }
        random.setSeed(state.getSeed(pos)); quads.addAll(model.getQuads(state, null, random))
        return quads
    }

    fun renderSelection(level: ClientLevel, ms: PoseStack, buffers: MultiBufferSource, camera: Vec3, time: Float) {
        val vc = buffers.getBuffer(ClockworkRenderTypes.GRAVITRON_SURFACE)
        val matrix = ms.last().pose()
        for (surface in visible) {
            if (level.getBlockState(surface.pos) != surface.state) continue
            val origin = Vec3.atLowerCornerOf(surface.pos).subtract(camera)
            val distance = Vec3.atCenterOf(surface.pos).distanceTo(pulseOrigin).toFloat()
            val wave = (0.5f + 0.5f * sin(distance * 1.7f - time * 0.17f)).pow(6)
            for (quad in surface.quads) emit(vc, matrix, quad, origin, 0.28f + wave * 0.8f, distance, false)
        }
        val energy = buffers.getBuffer(ClockworkRenderTypes.GRAVITRON_ENERGY)
        var edges = 0
        for (surface in visible) {
            if (level.getBlockState(surface.pos) != surface.state) continue
            for (quad in surface.quads) {
                if (edges >= 640) return
                // Distribute energetic seams, instead of outlining every tessellated face.
                if ((surface.pos.asLong() xor quad.direction.ordinal.toLong()) and 3L != 0L) continue
                val data = quad.vertices; val stride = data.size / 4
                val points = (0..3).map { i -> Vec3(Float.fromBits(data[i * stride]).toDouble() + surface.pos.x - camera.x,
                    Float.fromBits(data[i * stride + 1]).toDouble() + surface.pos.y - camera.y,
                    Float.fromBits(data[i * stride + 2]).toDouble() + surface.pos.z - camera.z) }
                for (i in 0..3) {
                    val a = points[i]; val b = points[(i + 1) % 4]
                    val jitter = sin(time * 0.55 + surface.pos.x * 2.1 + surface.pos.z + i) * 0.015
                    val mid = a.add(b).scale(0.5).add(jitter, jitter * 0.4, -jitter)
                    addRibbonSegment(energy, matrix, a, mid, 0.009f, 0.765f, 0.627f, 0.89f, 0.55f)
                    addRibbonSegment(energy, matrix, mid, b, 0.009f, 0.765f, 0.627f, 0.89f, 0.55f)
                    edges++
                }
            }
        }
    }

    fun renderPreview(level: ClientLevel, from: WandAnchor, to: WandAnchor, ms: PoseStack,
                      buffers: MultiBufferSource, camera: Vec3, time: Float) {
        val source = level.shipObjectWorld.loadedShips.getById(from.shipId) ?: return
        if (to.shipId == from.shipId) return
        val target = level.shipObjectWorld.loadedShips.getById(to.shipId)
        val rotation = WandGridRotation.choose(from.face, to.face, source.renderTransform.shipToWorldRotation,
            target?.renderTransform?.shipToWorldRotation ?: Quaterniond())
        val destination = to.pos.relative(to.face)
        val transform = (target?.renderTransform?.shipToWorld?.let(::Matrix4d) ?: Matrix4d())
            .translate(destination.x.toDouble(), destination.y.toDouble(), destination.z.toDouble())
        transform.m30(transform.m30() - camera.x); transform.m31(transform.m31() - camera.y); transform.m32(transform.m32() - camera.z)
        ms.pushPose(); ms.mulPoseMatrix(Matrix4f(transform))
        val vc = buffers.getBuffer(ClockworkRenderTypes.GRAVITRON_SURFACE)
        var budget = 1536
        for (x in -2..2) for (y in -2..2) for (z in -2..2) {
            val pos = from.pos.offset(x, y, z)
            if (!level.hasChunkAt(pos)) continue
            val state = level.getBlockState(pos)
            if (state.isAir || state.renderShape != RenderShape.MODEL) continue
            val offset = rotation.offset(BlockPos(x, y, z))
            val transformed = rotation.steps.fold(state) { s, step -> step.apply(s) }
            val blocked = !level.getBlockState(destination.offset(offset)).isAir
            for (quad in quads(level, pos, transformed, false)) {
                if (budget-- <= 0) break
                emit(vc, ms.last().pose(), quad, Vec3.atLowerCornerOf(offset),
                    0.65f + sin(time * 0.12f) * 0.15f, (x + y + z).toFloat(), blocked)
            }
        }
        ms.popPose()
    }

    private fun emit(vc: VertexConsumer, matrix: Matrix4f, quad: BakedQuad, offset: Vec3, alpha: Float, distance: Float, red: Boolean) {
        val data = quad.vertices; val stride = data.size / 4
        for (i in 0..3) {
            val j = i * stride
            vc.vertex(matrix, Float.fromBits(data[j]) + offset.x.toFloat(), Float.fromBits(data[j + 1]) + offset.y.toFloat(), Float.fromBits(data[j + 2]) + offset.z.toFloat())
                .color(if (red) 1f else 0.765f, if (red) 0.2f else 0.627f, if (red) 0.3f else 0.89f, alpha)
                .uv(Float.fromBits(data[j + 4]), Float.fromBits(data[j + 5])).overlayCoords((distance * 256).toInt(), 0)
                .uv2(0xF000F0).normal(quad.direction.stepX.toFloat(), quad.direction.stepY.toFloat(), quad.direction.stepZ.toFloat()).endVertex()
        }
    }
}
