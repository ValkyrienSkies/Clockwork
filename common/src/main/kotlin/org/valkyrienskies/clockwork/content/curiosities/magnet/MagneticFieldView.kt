package org.valkyrienskies.clockwork.content.curiosities.magnet

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.utility.CreateLang
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4d
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.client.render.goggles.AeronautGoggleOverlayRenderer
import org.valkyrienskies.clockwork.util.render.RenderUtil.addRibbonSegment
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.abs
import kotlin.math.min

object MagneticFieldView {
    private data class Sample(val source: MagneticFieldPacket.Source, val center: Vector3d,
                              val transform: Matrix4d, val inverse: Matrix4d, val poles: List<MagneticField.Pole>)
    private data class Seed(val source: MagneticFieldPacket.Source, val side: Int, val corner: Int)
    private data class Trace(val points: List<Vec3>, val side: Int)
    private var level: ClientLevel? = null
    private var packet: MagneticFieldPacket? = null
    private var receivedAt = 0L
    private var builtAt = Long.MIN_VALUE
    private var lines = emptyMap<Seed, Trace>()
    private var previous = emptyMap<Seed, Trace>()

    fun accept(update: MagneticFieldPacket) {
        val world = Minecraft.getInstance().level ?: return
        if (!AeronautGoggleOverlayRenderer.enabled() || update.dimension != world.dimension().location()) return
        if (level !== world) clear()
        level = world
        packet = update
        receivedAt = world.gameTime
    }

    fun tick() {
        val mc = Minecraft.getInstance()
        val world = mc.level
        if (world == null || world !== level || !AeronautGoggleOverlayRenderer.enabled() || world.gameTime - receivedAt > 20) {
            clear()
            return
        }
        if (builtAt != Long.MIN_VALUE && world.gameTime - builtAt < 2) return
        val update = packet ?: return
        previous = lines
        builtAt = world.gameTime
        if (update.crowded || update.sources.isEmpty()) { lines = emptyMap(); previous = emptyMap(); return }
        val camera = mc.gameRenderer.mainCamera.position
        val eye = Vector3d(camera.x, camera.y, camera.z)
        val samples = update.sources.map { source ->
            val ship = if (source.ship < 0) null else world.shipObjectWorld.loadedShips.getById(source.ship)
            if (source.ship >= 0 && ship == null) { lines = emptyMap(); previous = emptyMap(); return }
            val transform = ship?.renderTransform?.shipToWorld?.let(::Matrix4d) ?: Matrix4d()
            val center = Vector3d(source.position.x + 0.5, source.position.y + 0.5, source.position.z + 0.5)
            val direction = source.direction
            val poles = MagneticField.poles(center, Vector3d(direction.stepX.toDouble(), direction.stepY.toDouble(), direction.stepZ.toDouble()), source.strength.toDouble())
                .map { MagneticField.Pole(transform.transformPosition(it.position), it.charge) }
            Sample(source, transform.transformPosition(center), transform, Matrix4d(transform).invert(), poles)
        }
        val poles = samples.flatMap { it.poles }
        val candidates = samples.filter { it.center.distanceSquared(eye) < 24.0 * 24.0 }.sortedBy { it.center.distanceSquared(eye) }
        val seeds = candidates.flatMap { sample -> (0..3).flatMap { corner -> listOf(-1, 1).map { Seed(sample.source, it, corner) to sample } } }
        val count = min(seeds.size, (1_000_000 / (poles.size.coerceAtLeast(1) * 128 * 2)).coerceIn(4, 48))
        val result = LinkedHashMap<Seed, Trace>()
        repeat(count) { index ->
            val (seed, sample) = seeds[index * seeds.size / count]
            val n = seed.source.direction
            val normal = Vector3d(n.stepX.toDouble(), n.stepY.toDouble(), n.stepZ.toDouble())
            val tangent = if (abs(normal.x) < 0.9) Vector3d(1.0, 0.0, 0.0) else Vector3d(0.0, 1.0, 0.0)
            val other = Vector3d(normal).cross(tangent)
            val local = Vector3d(seed.source.position.x + 0.5, seed.source.position.y + 0.5, seed.source.position.z + 0.5)
                .fma(seed.side * 0.63, normal).fma(if (seed.corner and 1 == 0) -0.28 else 0.28, tangent)
                .fma(if (seed.corner and 2 == 0) -0.28 else 0.28, other)
            val start = sample.transform.transformPosition(local)
            val points = ArrayList<Vec3>()
            var position = start
            val field = Vector3d()
            val middleField = Vector3d()
            for (step in 0 until 128) {
                if (position.distanceSquared(eye) > 24.0 * 24.0) break
                if (step > 1 && samples.any { source ->
                    if (source.center.distanceSquared(position) > 4.0) false else {
                        val p = source.inverse.transformPosition(position, Vector3d())
                        val block = source.source.position
                        p.x in block.x.toDouble()..block.x + 1.0 && p.y in block.y.toDouble()..block.y + 1.0 && p.z in block.z.toDouble()..block.z + 1.0
                    }
                }) break
                MagneticField.field(poles, position, field)
                if (!field.isFinite || field.lengthSquared() < 1e-12) break
                points.add(Vec3(position.x, position.y, position.z))
                val middle = Vector3d(position).add(field.normalize(seed.side * 0.12))
                MagneticField.field(poles, middle, middleField)
                if (!middleField.isFinite || middleField.lengthSquared() < 1e-12) break
                val next = Vector3d(position).add(middleField.normalize(seed.side * 0.24))
                if (step > 12 && next.distanceSquared(start) < 0.04) break
                position = next
            }
            if (points.size > 2) result[seed] = Trace(points, seed.side)
        }
        lines = result
    }

    fun render(stack: PoseStack, buffers: MultiBufferSource.BufferSource, camera: Vec3, partialTick: Float) {
        if (!AeronautGoggleOverlayRenderer.enabled() || Minecraft.getInstance().level !== level) return
        val world = level ?: return
        if (world.gameTime - receivedAt > 20) return
        val blend = ((world.gameTime - builtAt + partialTick) / 2f).coerceIn(0f, 1f).toDouble()
        val consumer = ClockworkRenderTypes.softEnergyBuffer(buffers)
        val pose = stack.last().pose()
        for ((seed, trace) in lines) {
            val old = previous[seed]?.points
            fun point(i: Int): Vec3 {
                val current = trace.points[i]
                return if (old != null && abs(old.size - trace.points.size) < 12 && i < old.size) old[i].lerp(current, blend) else current
            }
            var last = point(0)
            for (i in 1 until trace.points.size) {
                val next = point(i)
                val distance = next.distanceTo(camera)
                val alpha = ((24.0 - distance) / 3.0).coerceIn(0.0, 1.0).toFloat() * 0.6f
                val width = (0.012 + distance * 0.0007).toFloat()
                addRibbonSegment(consumer, pose, last.subtract(camera), next.subtract(camera), width, 0.55f, 0.80f, 1.0f, alpha)
                if (i % 12 == 6) {
                    val direction = next.subtract(last).normalize().scale(trace.side.toDouble())
                    val side = direction.cross(camera.subtract(next)).normalize().scale(0.08)
                    val back = next.subtract(direction.scale(0.14))
                    for (sign in listOf(-1.0, 1.0)) addRibbonSegment(consumer, pose, next.subtract(camera),
                        back.add(side.scale(sign)).subtract(camera), width, 0.95f, 0.86f, 0.55f, alpha)
                }
                last = next
            }
        }
    }

    fun tooltip(): List<Component> {
        val mc = Minecraft.getInstance()
        val hit = mc.hitResult as? BlockHitResult ?: return emptyList()
        val state = mc.level?.getBlockState(hit.blockPos) ?: return emptyList()
        val magnet = state.block as? MagnetBlock ?: return emptyList()
        val result = ArrayList<Component>()
        fun line(key: String, color: ChatFormatting, vararg values: Any) = CreateLang.builder()
            .add(Component.translatable("vs_clockwork.magnet.$key", *values)).style(color).forGoggles(result, if (result.isEmpty()) 0 else 1)
        line("field", ChatFormatting.GOLD)
        if (magnet.electromagnetic) line("power", ChatFormatting.GRAY, state.getValue(MagnetBlock.POWER))
        if (packet?.crowded == true) line("crowded", ChatFormatting.RED)
        else if (packet == null) line("loading", ChatFormatting.GRAY)
        else {
            line("sources", ChatFormatting.GRAY, packet!!.sources.size)
            line("direction", ChatFormatting.DARK_GRAY)
        }
        return result
    }

    private fun clear() {
        level = null; packet = null; lines = emptyMap(); previous = emptyMap(); builtAt = Long.MIN_VALUE
    }
}
