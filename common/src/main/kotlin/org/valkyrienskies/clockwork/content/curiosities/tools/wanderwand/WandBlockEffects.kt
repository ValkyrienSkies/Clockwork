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
        boxes.firstOrNull()?.let { pulseOrigin = Vec3(it.minX().toDouble(), it.minY().toDouble(), it.minZ().toDouble()) }
    }
    fun clear() { update(CompoundTag()); weldPulses.clear() }

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

    fun renderSelection(level: ClientLevel, ms: PoseStack, buffers: MultiBufferSource, camera: Vec3, time: Float,
                        deselect: AABBic? = null) {
        val vc = buffers.getBuffer(ClockworkRenderTypes.GRAVITRON_SURFACE)
        for (surface in visible) {
            if (level.getBlockState(surface.pos) != surface.state) continue
            val origin = Vec3.atLowerCornerOf(surface.pos).subtract(camera)
            val red = deselect?.let { WandSelectionMath.contains(it, surface.pos.x, surface.pos.y, surface.pos.z) } == true
            for (quad in surface.quads) emit(vc, ms.last().pose(), quad, origin, 0.65f, 0f, red,
                mode = 3, pulseCenter = pulseOrigin.subtract(camera))
        }
    }

    private class WeldPulse(val anchor: WandAnchor, val born: Long, val scan: Iterator<BlockPos>) {
        val surfaces = mutableListOf<Surface>()
        var quads = 0
    }
    private val weldPulses = mutableListOf<WeldPulse>()
    fun weldPulse(anchor: WandAnchor, now: Long) {
        if (weldPulses.size >= 8) weldPulses.removeAt(0)
        weldPulses.add(WeldPulse(anchor, now, pulseOffsets.asSequence().map { anchor.pos.offset(it) }.iterator()))
    }
    fun tickPulses(level: ClientLevel) {
        weldPulses.removeAll { level.gameTime - it.born > 22 }
        var budget = 2048
        for (pulse in weldPulses) {
            if (level.gameTime <= pulse.born) continue // Let the welded block updates arrive first.
            while (budget > 0 && pulse.scan.hasNext() && pulse.quads < 3072) {
                budget--
                val pos = pulse.scan.next()
                if (!level.hasChunkAt(pos)) continue
                val state = level.getBlockState(pos)
                if (state.isAir || state.renderShape != RenderShape.MODEL) continue
                val faces = quads(level, pos, state, true).take(3072 - pulse.quads)
                if (faces.isNotEmpty()) pulse.surfaces.add(Surface(pos, state, faces))
                pulse.quads += faces.size
            }
        }
    }
    fun renderPulses(level: ClientLevel, ms: PoseStack, buffers: MultiBufferSource, camera: Vec3, partial: Float) {
        val vc = buffers.getBuffer(ClockworkRenderTypes.GRAVITRON_SURFACE)
        for (pulse in weldPulses) {
            val age = (level.gameTime - pulse.born).toFloat() + partial
            val ship = level.shipObjectWorld.loadedShips.getById(pulse.anchor.shipId)
            if (pulse.anchor.shipId >= 0 && ship == null) continue
            val transform = ship?.renderTransform?.shipToWorld?.let(::Matrix4d) ?: Matrix4d()
            // Keep shipyard coordinates out of float vertices, including on very distant ships.
            transform.translate(pulse.anchor.pos.x.toDouble(), pulse.anchor.pos.y.toDouble(), pulse.anchor.pos.z.toDouble())
            transform.m30(transform.m30() - camera.x); transform.m31(transform.m31() - camera.y); transform.m32(transform.m32() - camera.z)
            ms.pushPose(); ms.mulPoseMatrix(Matrix4f(transform))
            val center = pulse.anchor.local().subtract(Vec3.atLowerCornerOf(pulse.anchor.pos))
            for (surface in pulse.surfaces) {
                if (level.getBlockState(surface.pos) != surface.state) continue
                val offset = Vec3.atLowerCornerOf(surface.pos.subtract(pulse.anchor.pos))
                for (quad in surface.quads) emit(vc, ms.last().pose(), quad, offset,
                    (1 - age / 22f).coerceIn(0f, 1f), -age * 0.65f, false, mode = 4, pulseCenter = center)
            }
            ms.popPose()
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

    private fun emit(vc: VertexConsumer, matrix: Matrix4f, quad: BakedQuad, offset: Vec3, alpha: Float, distance: Float, red: Boolean,
                     mode: Int = 0, pulseCenter: Vec3? = null) {
        val data = quad.vertices; val stride = data.size / 4
        for (i in 0..3) {
            val j = i * stride
            val p = offset.add(Float.fromBits(data[j]).toDouble(), Float.fromBits(data[j + 1]).toDouble(), Float.fromBits(data[j + 2]).toDouble())
            val phase = if (pulseCenter != null) p.distanceTo(pulseCenter).toFloat() + distance else distance
            vc.vertex(matrix, p.x.toFloat(), p.y.toFloat(), p.z.toFloat())
                .color(if (red) 1f else 0.765f, if (red) 0.2f else 0.627f, if (red) 0.3f else 0.89f, alpha)
                .uv(Float.fromBits(data[j + 4]), Float.fromBits(data[j + 5])).overlayCoords((phase.coerceIn(-127f, 127f) * 256).toInt(), mode)
                .uv2(0xF000F0).normal(quad.direction.stepX.toFloat(), quad.direction.stepY.toFloat(), quad.direction.stepZ.toFloat()).endVertex()
        }
    }

    companion object {
        private val pulseOffsets by lazy {
            ( -8..8).flatMap { x -> (-8..8).flatMap { y -> (-8..8).map { z -> BlockPos(x, y, z) } } }
                .filter { it.distSqr(BlockPos.ZERO) <= 64 }.sortedBy { it.distSqr(BlockPos.ZERO) }
        }
    }
}
