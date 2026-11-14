package org.valkyrienskies.clockwork.content.curiosities.magnet

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.simibubi.create.foundation.utility.CreateLang
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.ClockworkTags
import org.valkyrienskies.clockwork.client.render.goggles.AeronautGoggleOverlayRenderer
import org.valkyrienskies.clockwork.util.render.RenderUtil.addRibbonSegment
import org.valkyrienskies.mod.common.shipObjectWorld
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

object MagneticFieldView {
    private const val VIEW_RANGE = 24.0
    private const val TRACE_RANGE = 12.0
    private const val STEPS = 144
    private const val STEP_LENGTH = 0.2
    private data class SourceId(val ship: Long, val position: BlockPos)
    private data class Sample(val source: MagneticFieldPacket.Source, val center: Vector3d,
                              val transform: Matrix4d, val inverse: Matrix4d, val poles: List<MagneticField.Pole>) {
        val id = SourceId(source.ship, source.position)
        val radiusSquared = transform.getScale(Vector3d()).lengthSquared() * .25
    }
    private data class Seed(val source: SourceId, val strand: Int)
    private data class Trace(val points: List<Vec3>, val target: SourceId?, val endpoint: Vec3?, val south: Boolean)
    private var level: ClientLevel? = null
    private var packet: MagneticFieldPacket? = null
    private var receivedAt = 0L
    private var builtAt = Long.MIN_VALUE
    private var sampledAt = Long.MIN_VALUE
    private var samples = emptyList<Sample>()
    private var activeSources = emptyList<SourceId>()
    private var seeds = emptyList<Seed>()
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
        if (sampledAt != Long.MIN_VALUE && world.gameTime - sampledAt < 2) return
        sampledAt = world.gameTime
        val update = packet ?: return
        if (update.crowded || update.sources.isEmpty()) {
            lines = emptyMap(); previous = emptyMap(); samples = emptyList(); seeds = emptyList(); activeSources = emptyList()
            return
        }
        val camera = mc.gameRenderer.mainCamera.position
        val eye = Vector3d(camera.x, camera.y, camera.z)
        val nextSamples = update.sources.sortedWith(compareBy<MagneticFieldPacket.Source> { it.ship }
            .thenBy { it.position.asLong() }).mapNotNull { source ->
            val ship = if (source.ship < 0) null else world.shipObjectWorld.loadedShips.getById(source.ship)
            if (source.ship >= 0 && ship == null) return@mapNotNull null
            val transform = ship?.renderTransform?.shipToWorld?.let(::Matrix4d) ?: Matrix4d()
            val center = Vector3d(source.position.x + 0.5, source.position.y + 0.5, source.position.z + 0.5)
            val poles = source.poles().map { MagneticField.Pole(transform.transformPosition(it.position), it.charge) }
            Sample(source, transform.transformPosition(center), transform, Matrix4d(transform).invert(), poles)
        }
        val byId = nextSamples.associateBy { it.id }
        val seedSources = nextSamples.filter { sample ->
            val moment = sample.source.moment ?: return@filter true
            val face = Direction.getNearest(moment.x.toFloat(), moment.y.toFloat(), moment.z.toFloat())
            SourceId(sample.source.ship, sample.source.position.relative(face)) !in byId
        }
        val eligible = seedSources.mapTo(HashSet()) { it.id }
        val poles = nextSamples.flatMap { it.poles }
        val count = (1_000_000 / (poles.size.coerceAtLeast(1) * STEPS * 2)).coerceIn(4, 48)
        val retained = activeSources.filter { it in eligible && byId[it]?.center?.distanceSquared(eye)?.let { distance -> distance < (VIEW_RANGE + TRACE_RANGE) * (VIEW_RANGE + TRACE_RANGE) } == true }
        val incoming = seedSources.filter { it.id !in retained && it.center.distanceSquared(eye) < VIEW_RANGE * VIEW_RANGE }
            .sortedWith(compareBy<Sample> { it.source.moment != null }.thenBy { it.center.distanceSquared(eye) }).map { it.id }
        val candidates = retained + incoming
        val magnets = candidates.filter { byId[it]?.source?.moment == null }
        val cores = candidates.filter { byId[it]?.source?.moment != null }
        val limit = min(8, count)
        val coreCount = if (magnets.isEmpty()) limit else min(limit / 2, cores.size)
        activeSources = magnets.take(limit - coreCount) + cores.take(coreCount)
        val nextSeeds = (0 until 8).flatMap { strand -> activeSources.map { Seed(it, strand) } }.take(count)
        if (nextSeeds == seeds && nextSamples.size == samples.size && nextSamples.zip(samples).all { (a, b) ->
                a.source == b.source && a.transform.equals(b.transform, 1e-6)
            }) return
        previous = lines
        builtAt = world.gameTime
        seeds = nextSeeds
        samples = nextSamples
        lines = seeds.mapNotNull { seed ->
            val sample = byId[seed.source] ?: return@mapNotNull null
            val trace = trace(seed, sample, nextSamples, poles) ?: return@mapNotNull null
            seed to trace
        }.toMap()
    }

    private fun trace(seed: Seed, sample: Sample, samples: List<Sample>, poles: List<MagneticField.Pole>): Trace? {
        val normal = sample.source.normal()
        val side = if (abs(normal.x) < 0.9) Vector3d(1.0, 0.0, 0.0) else Vector3d(0.0, 1.0, 0.0)
        val tangent = Vector3d(normal).cross(side).normalize()
        val other = Vector3d(normal).cross(tangent)
        val angle = (seed.strand % 4) * PI / 2 + PI / 4
        val radius = if (seed.strand < 4) 0.55 else 0.30
        val block = seed.source.position
        val local = Vector3d(block.x + 0.5, block.y + 0.5, block.z + 0.5)
            .fma(0.63, normal).fma(cos(angle) * radius, tangent).fma(sin(angle) * radius, other)
        val start = sample.transform.transformPosition(local)
        val points = ArrayList<Vec3>()
        var position = start
        var target: SourceId? = null
        var endpoint: Vec3? = null
        var south = false
        val field = Vector3d()
        val middleField = Vector3d()
        for (step in 0 until STEPS) {
            if (position.distanceSquared(sample.center) > TRACE_RANGE * TRACE_RANGE) break
            val p = sample.inverse.transformPosition(position, Vector3d())
            points.add(Vec3(p.x, p.y, p.z))
            val hit = if (step > 1) samples.firstOrNull { source ->
                if (source.center.distanceSquared(position) > source.radiusSquared) return@firstOrNull false
                val end = source.inverse.transformPosition(position, Vector3d())
                val pos = source.source.position
                end.x in pos.x.toDouble()..pos.x + 1.0 && end.y in pos.y.toDouble()..pos.y + 1.0 && end.z in pos.z.toDouble()..pos.z + 1.0
            } else null
            if (hit != null) {
                target = hit.id
                val end = hit.inverse.transformPosition(position, Vector3d())
                endpoint = Vec3(end.x, end.y, end.z)
                south = Vector3d(end).sub(hit.source.position.x + .5, hit.source.position.y + .5, hit.source.position.z + .5)
                    .dot(hit.source.normal()) < 0.0
                break
            }
            MagneticField.field(poles, position, field)
            if (!field.isFinite || field.lengthSquared() < 1e-12) break
            val middle = Vector3d(position).add(field.normalize(STEP_LENGTH / 2))
            MagneticField.field(poles, middle, middleField)
            if (!middleField.isFinite || middleField.lengthSquared() < 1e-12) break
            val next = Vector3d(position).add(middleField.normalize(STEP_LENGTH))
            if (step > 12 && next.distanceSquared(start) < 0.04) break
            position = next
        }
        return if (points.size > 3) Trace(points, target, endpoint, south) else null
    }

    fun render(stack: PoseStack, buffers: MultiBufferSource.BufferSource, camera: Vec3, partialTick: Float) {
        if (!AeronautGoggleOverlayRenderer.enabled() || Minecraft.getInstance().level !== level) return
        val world = level ?: return
        if (world.gameTime - receivedAt > 20 || builtAt == Long.MIN_VALUE) return
        val blend = ((world.gameTime - builtAt + partialTick) / 2f).coerceIn(0f, 1f).toDouble()
        val consumer = ClockworkRenderTypes.softEnergyBuffer(buffers)
        val pose = stack.last().pose()
        val transforms = HashMap<Long, Matrix4d>()
        fun transform(ship: Long): Matrix4d? {
            if (ship < 0) return transforms.getOrPut(ship) { Matrix4d() }
            return transforms[ship] ?: world.shipObjectWorld.loadedShips.getById(ship)?.renderTransform?.shipToWorld
                ?.let { Matrix4d(it).also { matrix -> transforms[ship] = matrix } }
        }
        fun points(seed: Seed, trace: Trace): List<Vec3>? {
            val source = transform(seed.source.ship) ?: return null
            fun worldPoint(matrix: Matrix4d, p: Vec3): Vec3 {
                val result = matrix.transformPosition(Vector3d(p.x, p.y, p.z))
                return Vec3(result.x, result.y, result.z)
            }
            val target = trace.target?.let { transform(it.ship) }
            val correction = if (target != null && trace.endpoint != null)
                worldPoint(target, trace.endpoint).subtract(worldPoint(source, trace.points.last())) else Vec3.ZERO
            return trace.points.mapIndexed { index, p ->
                val t = index.toDouble() / (trace.points.size - 1)
                worldPoint(source, p).add(correction.scale(t * t * (3 - 2 * t)))
            }
        }
        for ((seed, trace) in lines) {
            val current = points(seed, trace) ?: continue
            val old = previous[seed]
            val before = old?.let { points(seed, it) }
            if (old != null && before != null && old.target == trace.target) {
                val interpolated = current.mapIndexed { i, p ->
                    val index = i.toDouble() * (before.size - 1) / (current.size - 1)
                    val a = index.toInt()
                    before[a].lerp(before[min(a + 1, before.lastIndex)], index - a).lerp(p, blend)
                }
                draw(consumer, pose, interpolated, trace, camera, 1f)
            } else {
                if (old != null && before != null && blend < 1) draw(consumer, pose, before, old, camera, (1 - blend).toFloat())
                draw(consumer, pose, current, trace, camera, if (old == null || before != null) blend.toFloat() else 1f)
            }
        }
        if (blend < 1) for ((seed, trace) in previous) {
            if (seed !in lines) points(seed, trace)?.let { draw(consumer, pose, it, trace, camera, (1 - blend).toFloat()) }
        }
    }

    private fun draw(consumer: VertexConsumer, pose: Matrix4f, points: List<Vec3>, trace: Trace, camera: Vec3, opacity: Float) {
        if (opacity <= 0f) return
        val lengths = DoubleArray(points.size)
        for (i in 1 until points.size) lengths[i] = lengths[i - 1] + points[i].distanceTo(points[i - 1])
        val total = lengths.last()
        if (total < 0.1) return
        val red = Vec3(.88, .42, .32)
        val brass = Vec3(.80, .72, .51)
        val blue = Vec3(.37, .66, .79)
        val sides = points.mapIndexed { i, p ->
            val tangent = points[min(i + 1, points.lastIndex)].subtract(points[(i - 1).coerceAtLeast(0)]).normalize()
            var side = tangent.cross(p.subtract(camera))
            if (side.lengthSqr() < 1e-10) side = tangent.cross(Vec3(0.0, 1.0, 0.0))
            if (side.lengthSqr() < 1e-10) side = tangent.cross(Vec3(1.0, 0.0, 0.0))
            side.normalize().scale(.007 + min(p.distanceTo(camera), 20.0) * .0002)
        }
        fun vertex(i: Int, sign: Double, u: Float, v: Float) {
            val p = points[i].add(sides[i].scale(sign)).subtract(camera)
            val start = (1 - lengths[i] / .9).coerceIn(0.0, 1.0)
            val end = if (trace.target != null) (1 - (total - lengths[i]) / .9).coerceIn(0.0, 1.0) else 0.0
            val color = brass.lerp(red, start).lerp(if (trace.south) blue else red, end)
            val fade = ((VIEW_RANGE - points[i].distanceTo(camera)) / 4.0).coerceIn(0.0, 1.0)
            val tail = if (trace.target == null) ((total - lengths[i]) / 1.2).coerceIn(0.0, 1.0) else 1.0
            consumer.vertex(pose, p.x.toFloat(), p.y.toFloat(), p.z.toFloat())
                .color(color.x.toFloat(), color.y.toFloat(), color.z.toFloat(), (fade * tail * .72 * opacity).toFloat())
                .uv(u, v).endVertex()
        }
        for (i in 1 until points.size) {
            vertex(i - 1, -1.0, 0f, 0f); vertex(i - 1, 1.0, 1f, 0f)
            vertex(i, 1.0, 1f, 1f); vertex(i, -1.0, 0f, 1f)
        }
        val markers = (total / 4.0).toInt().coerceIn(1, 4)
        for (marker in 1..markers) {
            val distance = total * marker / (markers + 1)
            val index = lengths.indexOfFirst { it >= distance }.coerceAtLeast(1)
            val a = points[index - 1]
            val b = points[index]
            val span = lengths[index] - lengths[index - 1]
            val tip = a.lerp(b, if (span > 1e-6) (distance - lengths[index - 1]) / span else 0.0)
            val direction = b.subtract(a).normalize()
            val side = sides[index].normalize().scale(.065)
            val back = tip.subtract(direction.scale(.12))
            val alpha = ((VIEW_RANGE - tip.distanceTo(camera)) / 4.0).coerceIn(0.0, 1.0).toFloat() * opacity * .9f
            for (sign in listOf(-1.0, 1.0)) addRibbonSegment(consumer, pose, tip.subtract(camera),
                back.add(side.scale(sign)).subtract(camera), .009f, .98f, .87f, .61f, alpha)
        }
    }

    fun tooltip(): List<Component> {
        val mc = Minecraft.getInstance()
        val hit = mc.hitResult as? BlockHitResult ?: return emptyList()
        val state = mc.level?.getBlockState(hit.blockPos) ?: return emptyList()
        val magnet = state.block as? MagnetBlock
        if (magnet == null && !ClockworkTags.AllBlockTags.MAGNETIC.matches(state)) return emptyList()
        val result = ArrayList<Component>()
        fun line(key: String, color: ChatFormatting, vararg values: Any) = CreateLang.builder()
            .add(Component.translatable("vs_clockwork.magnet.$key", *values)).style(color).forGoggles(result, if (result.isEmpty()) 0 else 1)
        line("field", ChatFormatting.GOLD)
        if (magnet?.electromagnetic == true) line("power", ChatFormatting.GRAY, state.getValue(MagnetBlock.POWER))
        if (packet?.crowded == true) line("crowded", ChatFormatting.RED)
        else if (packet == null) line("loading", ChatFormatting.GRAY)
        else {
            line("sources", ChatFormatting.GRAY, packet!!.sources.count { it.moment == null })
            if (packet!!.coresEnabled) line("cores", ChatFormatting.GRAY, packet!!.sources.count { it.moment != null })
            else line("cores_disabled", ChatFormatting.DARK_GRAY)
            line("direction", ChatFormatting.DARK_GRAY)
        }
        return result
    }

    private fun clear() {
        level = null; packet = null; lines = emptyMap(); previous = emptyMap(); samples = emptyList()
        activeSources = emptyList(); seeds = emptyList(); builtAt = Long.MIN_VALUE; sampledAt = Long.MIN_VALUE
    }
}
