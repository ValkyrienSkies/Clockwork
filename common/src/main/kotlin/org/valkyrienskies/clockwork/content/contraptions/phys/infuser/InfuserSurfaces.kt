package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandSelectionMath
import org.valkyrienskies.clockwork.util.render.SurfaceEffectPass
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.*

internal class InfuserSurfaces(private val boxes: List<AABBic>, regions: List<InfuserRegion>) {
    private data class Surface(val pos: BlockPos, val state: BlockState, val quads: List<BakedQuad>)
    private class Region(val seed: BlockPos) {
        val queue = ArrayDeque<BlockPos>().also { it.add(seed) }
        val visited = hashSetOf(seed)
        val surfaces = mutableListOf<Surface>()
        var count = 0
        var ship = -1L
    }
    private val meshes = regions.map { Region(it.seed) }
    private val regionLimit = (6144 / regions.size.coerceAtLeast(1)).coerceIn(48, 2048)
    private val terrain = mutableListOf<Surface>()
    private var terrainCount = 0
    private var lastTick = Long.MIN_VALUE
    private val random = RandomSource.create()
    private var terrainScan: Iterator<BlockPos>? = null

    fun tick(level: ClientLevel, regions: List<InfuserRegion>, center: BlockPos) {
        if (lastTick == level.gameTime) return
        lastTick = level.gameTime
        var budget = 2048
        for ((index, mesh) in meshes.withIndex()) {
            val region = regions.getOrNull(index) ?: continue
            if (region.shipId != mesh.ship) {
                mesh.ship = region.shipId
                if (mesh.surfaces.isEmpty()) { mesh.queue.clear(); mesh.queue.add(mesh.seed); mesh.visited.clear(); mesh.visited.add(mesh.seed) }
            }
            val offset = region.offset
            if (!level.hasChunkAt(region.seed.offset(offset))) continue
            if (mesh.surfaces.isEmpty() && level.getBlockState(region.seed.offset(offset)).isAir) continue
            while (budget > 0 && mesh.queue.isNotEmpty() && mesh.count < regionLimit && mesh.visited.size <= 100000) {
                budget--
                val pos = mesh.queue.removeFirst()
                val actual = pos.offset(offset)
                if (!level.hasChunkAt(actual)) continue
                val state = level.getBlockState(actual)
                if (state.isAir) continue
                val faces = quads(level, actual, state).take(regionLimit - mesh.count)
                if (faces.isNotEmpty()) mesh.surfaces.add(Surface(pos, state, faces))
                mesh.count += faces.size
                for (direction in Direction.values()) {
                    val next = pos.relative(direction)
                    if (boxes.any { WandSelectionMath.contains(it, next.x, next.y, next.z) } && mesh.visited.add(next))
                        mesh.queue.add(next)
                }
            }
            if (mesh.count >= regionLimit) { mesh.queue.clear(); mesh.visited.clear() }
        }
        if (terrainScan == null) terrainScan = sequence {
            for (offset in columns) {
                val x = center.x + offset.first; val z = center.z + offset.second
                val probe = BlockPos(x, center.y, z)
                if (!level.hasChunkAt(probe)) continue
                val top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1
                for (y in top downTo top - 2) yield(BlockPos(x, y, z))
                for (y in center.y - 5..center.y + 5)
                    if (abs(y - top) > 2) yield(BlockPos(x, y, z))
            }
        }.iterator()
        val cursor = terrainScan ?: return
        budget = 2048
        while (budget-- > 0 && cursor.hasNext() && terrainCount < 16000) {
            val pos = cursor.next()
            val state = level.getBlockState(pos)
            if (state.isAir) continue
            val faces = quads(level, pos, state).take(16000 - terrainCount)
            if (faces.isNotEmpty()) terrain.add(Surface(pos, state, faces))
            terrainCount += faces.size
        }
    }

    private fun quads(level: ClientLevel, pos: BlockPos, state: BlockState): List<BakedQuad> {
        if (state.renderShape != RenderShape.MODEL) return emptyList()
        val model = Minecraft.getInstance().blockRenderer.getBlockModel(state)
        val result = mutableListOf<BakedQuad>()
        for (direction in Direction.values()) {
            if (!Block.shouldRenderFace(state, level, pos, direction, pos.relative(direction))) continue
            random.setSeed(state.getSeed(pos)); result.addAll(model.getQuads(state, direction, random))
        }
        random.setSeed(state.getSeed(pos)); result.addAll(model.getQuads(state, null, random))
        return result
    }

    fun render(level: ClientLevel, ms: PoseStack, buffers: MultiBufferSource, camera: Vec3,
               regions: List<InfuserRegion>, animation: InfuserSequence, age: Float, center: Vec3, limit: Int): Int {
        val pass = SurfaceEffectPass(buffers, (level.gameTime % 24000).toFloat() + age % 1f)
        var budget = limit
        val after = age - animation.impact
        for ((index, mesh) in meshes.withIndex()) {
            val region = regions.getOrNull(index) ?: continue
            val connection = animation.connection(index, meshes.size) + InfuserVisuals.BEAM_FLIGHT
            if (age < connection && after < 0) continue
            if (after >= 0 && region.shipId < 0) continue
            val ship = if (region.shipId >= 0) level.shipObjectWorld.loadedShips.getById(region.shipId) ?: continue else null
            val local = region.seed.offset(region.offset)
            val transform = (ship?.renderTransform?.shipToWorld?.let(::Matrix4d) ?: Matrix4d())
                .translate(local.x.toDouble(), local.y.toDouble(), local.z.toDouble())
            transform.m30(transform.m30() - camera.x); transform.m31(transform.m31() - camera.y); transform.m32(transform.m32() - camera.z)
            val matrix = Matrix4f(ms.last().pose()).mul(Matrix4f(transform))
            val front = if (after >= 0) after * 0.85f else (age - connection) * 0.55f % 28f
            val alpha = if (after >= 0) (1f - after / 85f).coerceIn(0f, 1f) else 0.8f
            if (alpha <= 0f) continue
            for (surface in mesh.surfaces) {
                val offset = Vec3.atLowerCornerOf(surface.pos.subtract(region.seed))
                if (abs(offset.length() - front) > 5.5) continue
                if (level.getBlockState(surface.pos.offset(region.offset)) != surface.state) continue
                for (quad in surface.quads) {
                    if (budget-- <= 0) return limit
                    emit(pass, matrix, quad, offset, Vec3(0.5, 0.5, 0.5), front, alpha, false)
                }
            }
        }
        if (after in 0f..100f) {
            val front = after * 0.6f
            val alpha = (1f - InfuserSequence.smooth((after - 65f) / 35f)) * 0.9f
            val relativeCenter = center.subtract(camera)
            for (surface in terrain) {
                val offset = Vec3.atLowerCornerOf(surface.pos).subtract(camera)
                if (abs(hypot(offset.x - relativeCenter.x, offset.z - relativeCenter.z) - front) > 5.5) continue
                if (level.getBlockState(surface.pos) != surface.state) continue
                for (quad in surface.quads) {
                    if (budget-- <= 0) return limit
                    emit(pass, ms.last().pose(), quad, offset, relativeCenter, front, alpha, true)
                }
            }
        }
        return limit - budget
    }

    private fun emit(pass: SurfaceEffectPass, matrix: Matrix4f, quad: BakedQuad, offset: Vec3,
                     center: Vec3, front: Float, alpha: Float, planar: Boolean) {
        pass.quad(quad, matrix, 4)
        val data = quad.vertices; val stride = data.size / 4
        for (i in 0..3) {
            val j = i * stride
            val p = offset.add(Float.fromBits(data[j]).toDouble(), Float.fromBits(data[j + 1]).toDouble(), Float.fromBits(data[j + 2]).toDouble())
            val distance = if (planar) hypot(p.x - center.x, p.z - center.z) else p.distanceTo(center)
            pass.vertex(matrix, p.x.toFloat(), p.y.toFloat(), p.z.toFloat(), 0.765f, 0.627f, 0.89f, alpha,
                Float.fromBits(data[j + 4]), Float.fromBits(data[j + 5]), (distance.toFloat() - front) / 1.8f, 0xF000F0)
        }
    }

    companion object {
        private val columns by lazy {
            (-60..60).flatMap { x -> (-60..60).map { z -> x to z } }
                .filter { (x, z) -> x * x + z * z <= 3600 }.sortedBy { (x, z) -> x * x + z * z }
        }
    }
}
