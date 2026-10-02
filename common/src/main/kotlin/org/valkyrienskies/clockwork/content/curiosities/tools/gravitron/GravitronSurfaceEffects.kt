package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.sqrt

/** Bounded, incremental sampling of actual block meshes near the hit, never a ship-sized box. */
object GravitronSurfaceEffects {
    private const val MAX_CACHES = 16
    private const val SCAN_BUDGET = 4096
    private const val MAX_QUADS = 4096
    private const val FRAME_QUAD_BUDGET = 8192
    private data class Key(val shipId: Long, val origin: BlockPos)
    private data class Surface(val pos: BlockPos, val state: BlockState, val quads: List<BakedQuad>)
    private class Mesh(val createdAt: Long) {
        val surfaces = mutableListOf<Surface>()
        var cursor = 0
        var quadCount = 0
        var lastUsed = createdAt
    }

    private val meshes = linkedMapOf<Key, Mesh>()
    private val random = RandomSource.create()
    private var frameQuads = 0
    // Near-first scanning makes the advancing wave visible immediately while farther faces are gathered.
    private val offsets = buildList {
        val radius = GravitronVisuals.SURFACE_RADIUS
        for (x in -radius..radius) for (y in -radius..radius) for (z in -radius..radius) {
            if (x * x + y * y + z * z <= radius * radius) add(BlockPos(x, y, z))
        }
    }.sortedBy { it.x * it.x + it.y * it.y + it.z * it.z }

    fun clear() = meshes.clear()
    fun beginFrame() { frameQuads = 0 }

    fun tick(level: ClientLevel) {
        val now = level.gameTime
        meshes.entries.removeIf { now - it.value.lastUsed > 40 || level.shipObjectWorld.loadedShips.getById(it.key.shipId) == null }
        val budget = SCAN_BUDGET / meshes.size.coerceAtLeast(1)
        for ((key, mesh) in meshes) {
            // Rebuild long-lived grabs occasionally so added/removed blocks are reflected as well.
            if (now - mesh.createdAt > 80 && (mesh.cursor >= offsets.size || mesh.quadCount >= MAX_QUADS)) {
                meshes[key] = Mesh(now)
                continue
            }
            val ship = level.shipObjectWorld.loadedShips.getById(key.shipId) ?: continue
            val bounds = ship.shipAABB ?: continue
            for (scan in 0 until budget) {
                if (mesh.cursor >= offsets.size || mesh.quadCount >= MAX_QUADS) break
                val pos = key.origin.offset(offsets[mesh.cursor++])
                if (pos.x !in bounds.minX()..bounds.maxX() || pos.y !in bounds.minY()..bounds.maxY() ||
                    pos.z !in bounds.minZ()..bounds.maxZ() || !level.hasChunkAt(pos)) continue
                val state = level.getBlockState(pos)
                if (state.isAir || state.renderShape != RenderShape.MODEL) continue
                val model = Minecraft.getInstance().blockRenderer.getBlockModel(state)
                val quads = mutableListOf<BakedQuad>()
                for (direction in Direction.values()) {
                    if (!Block.shouldRenderFace(state, level, pos, direction, pos.relative(direction))) continue
                    random.setSeed(state.getSeed(pos))
                    quads.addAll(model.getQuads(state, direction, random))
                }
                random.setSeed(state.getSeed(pos))
                quads.addAll(model.getQuads(state, null, random))
                if (quads.isNotEmpty()) {
                    val limited = quads.take(MAX_QUADS - mesh.quadCount)
                    mesh.surfaces.add(Surface(pos, state, limited))
                    mesh.quadCount += limited.size
                }
            }
        }
    }

    fun render(level: ClientLevel, ship: ClientShip, anchor: Vec3, action: GravitronAction, age: Float,
               ms: PoseStack, buffers: MultiBufferSource, camera: Vec3) {
        val key = Key(ship.id, BlockPos.containing(anchor))
        val mesh = meshes[key] ?: run {
            if (meshes.size >= MAX_CACHES) meshes.remove(meshes.minBy { it.value.lastUsed }.key)
            Mesh(level.gameTime).also { meshes[key] = it }
        }
        mesh.lastUsed = level.gameTime
        if (mesh.surfaces.isEmpty() || frameQuads >= FRAME_QUAD_BUDGET) return
        val mc = Minecraft.getInstance()
        val freeze = action == GravitronAction.FREEZE || action == GravitronAction.UNFREEZE
        val vc = buffers.getBuffer(ClockworkRenderTypes.GRAVITRON_SURFACE)
        // Keep shipyard coordinates in doubles until the small camera-relative matrix is formed.
        val transform = Matrix4d(ship.renderTransform.shipToWorld)
            .translate(key.origin.x.toDouble(), key.origin.y.toDouble(), key.origin.z.toDouble())
        transform.m30(transform.m30() - camera.x)
        transform.m31(transform.m31() - camera.y)
        transform.m32(transform.m32() - camera.z)
        ms.pushPose()
        ms.mulPoseMatrix(Matrix4f(transform))
        val matrix = ms.last().pose()
        val ax = anchor.x - key.origin.x
        val ay = anchor.y - key.origin.y
        val az = anchor.z - key.origin.z
        for (surface in mesh.surfaces) {
            if (frameQuads >= FRAME_QUAD_BUDGET) break
            // Deleted or replaced blocks must never leave a floating hologram behind.
            if (level.getBlockState(surface.pos) != surface.state) continue
            val ox = surface.pos.x - key.origin.x
            val oy = surface.pos.y - key.origin.y
            val oz = surface.pos.z - key.origin.z
            val light = LevelRenderer.getLightColor(level, surface.state, surface.pos)
            for (quad in surface.quads) {
                if (frameQuads >= FRAME_QUAD_BUDGET) break
                val data = quad.vertices
                val stride = data.size / 4
                val distances = FloatArray(4)
                val strengths = FloatArray(4)
                for (i in 0..3) {
                    val x = Float.fromBits(data[i * stride]) + ox - ax
                    val y = Float.fromBits(data[i * stride + 1]) + oy - ay
                    val z = Float.fromBits(data[i * stride + 2]) + oz - az
                    distances[i] = sqrt(x * x + y * y + z * z).toFloat()
                    strengths[i] = GravitronVisuals.surfaceStrength(action, distances[i], age)
                }
                if (strengths.all { it < 0.005f }) continue
                val tint = if (freeze && quad.isTinted) mc.blockColors.getColor(surface.state, level, surface.pos, quad.tintIndex)
                    else if (freeze) 0xFFFFFF else GravitronVisuals.WANDERLITE
                val shade = if (freeze) level.getShade(quad.direction, true) else 1f
                for (i in 0..3) {
                    val j = i * stride
                    vc.vertex(matrix, Float.fromBits(data[j]) + ox, Float.fromBits(data[j + 1]) + oy, Float.fromBits(data[j + 2]) + oz)
                        .color((tint shr 16 and 255) / 255f * shade, (tint shr 8 and 255) / 255f * shade,
                            (tint and 255) / 255f * shade, strengths[i])
                        .uv(Float.fromBits(data[j + 4]), Float.fromBits(data[j + 5]))
                        // The dedicated shader uses overlay coordinates for distance and mode.
                        .overlayCoords((distances[i] * 256).toInt(), if (freeze) 1 else 0)
                        .uv2(light)
                        .normal(quad.direction.stepX.toFloat(), quad.direction.stepY.toFloat(), quad.direction.stepZ.toFloat())
                        .endVertex()
                }
                frameQuads++
            }
        }
        ms.popPose()
    }
}
