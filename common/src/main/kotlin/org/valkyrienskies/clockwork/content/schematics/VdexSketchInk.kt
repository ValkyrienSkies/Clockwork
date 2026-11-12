package org.valkyrienskies.clockwork.content.schematics

import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexBuffer
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinkSchematics
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandLinks
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandRopeCurve
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object VdexSketchInk {
    enum class Kind { SPRING, ROPE, THINKABLE, BIND, ATTACHMENT }
    data class Stroke(val points: List<Vec3>, val color: Int, val width: Float, val alpha: Float = .85f)
    data class Symbol(val anchor: Vec3, val points: List<Vector2f>, val color: Int, val width: Float = .65f, val toward: Vec3? = null)
    data class Drawing(val strokes: List<Stroke>, val symbols: List<Symbol>)

    private val providers = linkedMapOf<ResourceLocation, (VdexBlueprintData) -> Drawing>(
        ResourceLocation("vs_clockwork", "connections") to ::connections
    )

    fun register(id: ResourceLocation, provider: (VdexBlueprintData) -> Drawing) {
        providers[id] = provider
        VdexBlueprintPreview.clear()
    }

    fun collect(data: VdexBlueprintData): Drawing {
        val drawings = providers.values.map { it(data) }
        return Drawing(drawings.flatMap { it.strokes }, drawings.flatMap { it.symbols })
    }

    private fun connections(data: VdexBlueprintData): Drawing {
        val payload = data.data.nbtData[WandLinkSchematics.ENTRY] ?: return Drawing(emptyList(), emptyList())
        val origins = payload.getList("origins", 10).filterIsInstance<CompoundTag>()
            .associate { it.getLong("ship") to BlockPos.of(it.getLong("pos")) }
        val ships = data.ships.associateBy { it.entry.previousId }
        fun point(anchor: WandAnchor): Vec3? {
            val origin = origins[anchor.shipId] ?: return null
            val ship = ships[anchor.shipId] ?: return null
            val local = anchor.local()
            val p = ship.relativePoint(Vector3d(local.x - origin.x, local.y - origin.y, local.z - origin.z))
            return Vec3(p.x, p.y, p.z)
        }
        val drawings = WandLinks.load(payload).links.values.mapNotNull { link ->
            val a = point(link.a) ?: return@mapNotNull null
            val b = point(link.b) ?: return@mapNotNull null
            val kind = when {
                link.spring -> Kind.SPRING
                link.thinkable -> Kind.THINKABLE
                link.rope -> if (link.physical) Kind.ROPE else Kind.BIND
                else -> Kind.ATTACHMENT
            }
            connection(kind, a, b, link.length)
        }
        return Drawing(drawings.flatMap { it.strokes }, drawings.flatMap { it.symbols })
    }

    @JvmStatic
    fun connection(kind: Kind, a: Vec3, b: Vec3, length: Double): Drawing {
        val distance = a.distanceTo(b)
        if (!distance.isFinite() || distance < .001 || !length.isFinite()) return Drawing(emptyList(), emptyList())
        val direction = b.subtract(a).normalize()
        val side = direction.cross(if (abs(direction.y) > .9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)).normalize()
        val up = direction.cross(side)
        val curve = WandRopeCurve(a, b, length.coerceAtLeast(distance))
        val strokes = ArrayList<Stroke>()
        val symbols = ArrayList<Symbol>()
        fun path(count: Int, sample: (Double) -> Vec3) = (0..count).map { sample(it.toDouble() / count) }
        fun ring(anchor: Vec3, color: Int, radius: Float) = Symbol(anchor, (0..20).map {
            val angle = it * PI / 10
            Vector2f(cos(angle).toFloat() * radius, sin(angle).toFloat() * radius)
        }, color)
        when (kind) {
            Kind.SPRING -> {
                val turns = (distance * 1.6).coerceIn(4.0, 14.0)
                val points = path((turns * 14).toInt()) { t ->
                    val coil = ((t - .10) / .80).coerceIn(0.0, 1.0)
                    val radius = .19 * sin(PI * ((t / .10).coerceAtMost(1.0) * ((1.0 - t) / .10).coerceAtMost(1.0)) / 2)
                    a.lerp(b, t).add(side.scale(cos(coil * turns * PI * 2) * radius))
                        .add(up.scale(sin(coil * turns * PI * 2) * radius))
                }
                strokes.add(Stroke(points, 0x426E8B, 1.05f))
                symbols.add(ring(a, 0x426E8B, 1.8f)); symbols.add(ring(b, 0x426E8B, 1.8f))
            }
            Kind.ROPE -> {
                val count = (distance * 14).toInt().coerceIn(48, 160)
                val twists = (distance * 2.2).coerceIn(4.0, 24.0)
                strokes.add(Stroke(path(count) { curve.point(it) }, 0x9A7047, 1.25f, .68f))
                for (sign in listOf(-1, 1)) strokes.add(Stroke(path(count) { t ->
                    curve.point(t).add(side.scale(sin(t * twists * PI * 2) * .045 * sign))
                }, 0x755134, .45f, .75f))
                if (distance > .5) {
                    val stitches = (distance * 1.5).toInt().coerceIn(4, 16)
                    for (i in 1 until stitches) {
                        val t = i.toDouble() / stitches
                        symbols.add(Symbol(curve.point(t), listOf(Vector2f(-.45f, -.7f), Vector2f(.45f, .7f)),
                            0xCCAA7D, .4f, curve.point(t + .005)))
                    }
                }
                for (anchor in listOf(a, b)) symbols.add(Symbol(anchor, (0..32).map {
                    val angle = it * PI / 16
                    Vector2f((sin(angle) * 2.5).toFloat(), (sin(angle * 2) * 1.6).toFloat())
                }, 0x755134))
            }
            Kind.THINKABLE -> {
                val points = path(80) { curve.point(it) }
                strokes.add(Stroke(points, 0x795699, .85f, .8f))
                for (t in listOf(.28, .53, .78)) {
                    val p = curve.point(t)
                    symbols.add(Symbol(p, listOf(Vector2f(-2f, 1.2f), Vector2f(0f, 0f), Vector2f(-2f, -1.2f)),
                        0xA26DAB, .7f, curve.point(t + .02)))
                }
                for (anchor in listOf(a, b)) symbols.add(Symbol(anchor, listOf(Vector2f(0f, 2.7f), Vector2f(2.7f, 0f),
                    Vector2f(0f, -2.7f), Vector2f(-2.7f, 0f), Vector2f(0f, 2.7f)), 0x795699))
            }
            Kind.BIND -> {
                strokes.add(Stroke(path(64) { curve.point(it) }, 0x9878B0, .7f, .25f))
                for (i in 0..7) strokes.add(Stroke(path(8) { curve.point((i + it * .65) / 8) }, 0x9878B0, .8f, .8f))
                symbols.add(ring(a, 0x9878B0, 1.7f)); symbols.add(ring(b, 0x9878B0, 1.7f))
            }
            Kind.ATTACHMENT -> {
                strokes.add(Stroke(listOf(a, b), 0x8B64A4, 1.2f, .65f))
                symbols.add(ring(a, 0x8B64A4, 2f)); symbols.add(ring(b, 0x8B64A4, 2f))
            }
        }
        return Drawing(strokes, symbols)
    }

    @JvmStatic
    fun upload(drawing: Drawing, model: Matrix4f, projection: Matrix4f, width: Float, height: Float): VertexBuffer? {
        if (drawing.strokes.isEmpty() && drawing.symbols.isEmpty()) return null
        val transform = Matrix4f(projection).mul(model)
        fun project(p: Vec3): Vector3f? {
            val clip = transform.transform(Vector4f(p.x.toFloat(), p.y.toFloat(), p.z.toFloat(), 1f))
            if (!clip.isFinite || clip.w <= .0001f) return null
            return Vector3f((clip.x / clip.w + 1) * .5f, (clip.y / clip.w + 1) * .5f, (clip.z / clip.w + 1) * .5f)
        }
        val builder = BufferBuilder(65536)
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR_TEX)
        var segments = 0
        fun segment(a: Vector3f, b: Vector3f, color: Int, lineWidth: Float, alpha: Float) {
            if (segments >= 65536 || (a.z < 0 && b.z < 0) || (a.z > 1 && b.z > 1)) return
            val delta = Vector2f((b.x - a.x) * width, (b.y - a.y) * height)
            if (delta.lengthSquared() < .0001f) return
            segments++
            delta.normalize()
            val normal = Vector2f(-delta.y / width, delta.x / height)
            val half = lineWidth.coerceIn(.2f, 4f) * .5f
            val offsets = floatArrayOf(-half - .45f, -half, half, half + .45f)
            val opacity = floatArrayOf(0f, alpha, alpha, 0f)
            fun vertex(p: Vector3f, edge: Int) {
                val x = p.x + normal.x * offsets[edge]
                val y = p.y + normal.y * offsets[edge]
                builder.vertex(x.toDouble(), y.toDouble(), p.z.toDouble())
                    .color((color shr 16) and 255, (color shr 8) and 255, color and 255, (opacity[edge].coerceIn(0f, 1f) * 255).toInt())
                    .uv(x, y).endVertex()
            }
            for (i in 0..2) { vertex(a, i); vertex(b, i); vertex(b, i + 1); vertex(a, i + 1) }
        }
        for (stroke in drawing.strokes) {
            var previous: Vector3f? = null
            for (point in stroke.points) {
                val projected = project(point)
                if (previous != null && projected != null) segment(previous, projected, stroke.color, stroke.width, stroke.alpha)
                previous = projected
            }
        }
        for (symbol in drawing.symbols) {
            val anchor = project(symbol.anchor) ?: continue
            val target = symbol.toward?.let(::project)
            val axis = if (target == null) Vector2f(1f, 0f) else Vector2f((target.x - anchor.x) * width, (target.y - anchor.y) * height)
            if (axis.lengthSquared() < .0001f) continue
            axis.normalize()
            for (pair in symbol.points.zipWithNext()) {
                fun offset(p: Vector2f) = Vector3f(anchor.x + (axis.x * p.x - axis.y * p.y) / width,
                    anchor.y + (axis.y * p.x + axis.x * p.y) / height, anchor.z)
                segment(offset(pair.first), offset(pair.second), symbol.color, symbol.width, .9f)
            }
        }
        val vertices = builder.end()
        if (vertices.isEmpty) { vertices.release(); return null }
        return VertexBuffer(VertexBuffer.Usage.STATIC).also { it.bind(); it.upload(vertices); VertexBuffer.unbind() }
    }
}
