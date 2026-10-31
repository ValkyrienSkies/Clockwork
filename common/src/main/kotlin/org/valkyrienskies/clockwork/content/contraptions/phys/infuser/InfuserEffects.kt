package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WanderwandItem
import org.valkyrienskies.clockwork.util.EaseHelper
import org.valkyrienskies.clockwork.util.render.RenderUtil
import org.valkyrienskies.clockwork.util.render.SurfaceEffectPass
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import kotlin.math.*

object InfuserEffects {
    private data class Key(val pos: BlockPos, val start: Long)
    private class Scene(var be: PhysicsInfuserBlockEntity, level: ClientLevel) {
        var animation = be.sequence!!.copy()
        var regions = be.regions.map { it.copy() }
        val surfaces = InfuserSurfaces(WanderwandItem.readAABBSetFromNBT(be.selection), regions)
        var center = worldPoint(level, be.blockPos, Vec3.atCenterOf(be.blockPos))
        var windup: InfuserWindupSound? = null
    }
    private var world: ClientLevel? = null
    private val scenes = linkedMapOf<Key, Scene>()

    fun clear() { scenes.values.forEach { it.windup?.cancel() }; scenes.clear(); world = null }

    fun track(be: PhysicsInfuserBlockEntity) {
        val level = be.level as? ClientLevel ?: return
        if (world !== level) { clear(); world = level }
        val animation = be.sequence ?: return
        if (animation.age(level.gameTime) >= animation.finished) return
        val key = Key(be.blockPos, animation.startedAt)
        val scene = scenes[key] ?: run {
            scenes.entries.removeIf { (_, scene) -> scene.animation.age(level.gameTime) >= scene.animation.finished }
            if (scenes.size >= 4) {
                val camera = Minecraft.getInstance().gameRenderer.mainCamera.position
                val farthest = scenes.maxBy { it.value.center.distanceToSqr(camera) }
                val center = worldPoint(level, be.blockPos, Vec3.atCenterOf(be.blockPos))
                if (center.distanceToSqr(camera) >= farthest.value.center.distanceToSqr(camera)) return
                scenes.remove(farthest.key)?.windup?.cancel()
            }
            Scene(be, level).also { scenes[key] = it }
        }
        if (scene.be !== be) { scene.windup?.cancel(); scene.windup = null; scene.be = be }
        scene.animation = animation.copy()
        scene.regions = be.regions.map { it.copy() }
        val age = animation.age(level.gameTime)
        if (scene.windup == null && animation.skippedAt < 0 &&
            age >= (if (animation.firstUse) 48 else 16) && age < animation.impact) {
            scene.windup = InfuserWindupSound(be).also { Minecraft.getInstance().soundManager.play(it) }
        }
    }

    fun pose(be: PhysicsInfuserBlockEntity, age: Float, animation: InfuserSequence? = be.sequence,
             regions: List<InfuserRegion> = be.regions): InfuserVisuals.CorePose {
        animation ?: return InfuserVisuals.CorePose()
        val level = be.level as? ClientLevel ?: return InfuserVisuals.CorePose()
        val transform = level.getLoadedShipManagingPos(be.blockPos)?.renderTransform?.worldToShip
        val origin = Vec3.atCenterOf(be.blockPos).add(0.0, animation.lift(age).toDouble(), 0.0)
        val directions = regions.map {
            val p = Vector3d(it.seed.x + 0.5, it.seed.y + 0.5, it.seed.z + 0.5)
            transform?.transformPosition(p)
            Vec3(p.x, p.y, p.z).subtract(origin).normalize()
        }
        return InfuserVisuals.sample(animation, age, directions)
    }

    private fun core(scene: Scene, level: ClientLevel, age: Float): Vec3 {
        val motion = pose(scene.be, age, scene.animation, scene.regions)
        return worldPoint(level, scene.be.blockPos, Vec3.atLowerCornerOf(scene.be.blockPos)
            .add(0.5, 0.65 + scene.animation.lift(age), 0.5).add(motion.offset))
    }

    fun render(ms: PoseStack, buffers: MultiBufferSource.BufferSource, camera: Vec3, partial: Float) {
        val level = Minecraft.getInstance().level ?: run { clear(); return }
        if (world !== level) { clear(); world = level }
        scenes.entries.removeIf { (_, scene) ->
            val age = scene.animation.age(level.gameTime)
            age > scene.animation.finished || (scene.be.isRemoved && age < scene.animation.impact)
        }
        var surfaceBudget = 8192
        for (scene in scenes.values) {
            val animation = scene.animation
            val age = animation.age(level.gameTime, partial)
            if (age < 0f) continue
            val base = worldPoint(level, scene.be.blockPos, Vec3.atCenterOf(scene.be.blockPos))
            if (base.distanceToSqr(camera) > 160.0 * 160.0) continue
            if (age < animation.impact) scene.center = base
            scene.surfaces.tick(level, scene.regions, BlockPos.containing(scene.center))
            if (surfaceBudget > 0) surfaceBudget -= scene.surfaces.render(level, ms, buffers, camera,
                scene.regions, animation, age, scene.center, surfaceBudget)
            SurfaceEffectPass.endBatch(buffers)
            val pen = EnergyPen(ClockworkRenderTypes.energyBuffer(buffers), ms.last().pose(), camera)
            val core = core(scene, level, age)
            val after = age - animation.impact
            if (after < 0) {
                crackle(pen, core, age, animation)
                trail(pen, scene, level, age)
                connections(pen, scene, core, age)
                gathering(pen, core, age, animation)
                val compression = EaseHelper.easeInExpo(InfuserVisuals.collapse(animation, age))
                pen.spark(core, 0.25f + compression * 0.65f, 0.2f + compression * 0.8f)
            } else if (after < 18f) {
                rebound(pen, core(scene, level, animation.impact.toFloat()), after)
            }
        }
    }

    private fun crackle(pen: EnergyPen, core: Vec3, age: Float, animation: InfuserSequence) {
        val charge = animation.charge(age)
        for (bolt in 0 until 5 + (charge * 7).toInt()) {
            val seed = (age / 2).toLong() * 811 + bolt * 31
            val angle = RenderUtil.rand01(seed) * Math.PI * 2
            val target = core.add(cos(angle) * (0.5 + charge * 0.55),
                (RenderUtil.rand01(seed + 1) - 0.5) * 1.6, sin(angle) * (0.5 + charge * 0.55))
            val points = (0..6).map { step ->
                val t = step / 6.0
                val jitter = sin(t * Math.PI) * 0.4
                core.lerp(target, t).add((RenderUtil.rand01(seed + step * 3) - 0.5) * jitter,
                    (RenderUtil.rand01(seed + step * 3 + 1) - 0.5) * jitter,
                    (RenderUtil.rand01(seed + step * 3 + 2) - 0.5) * jitter)
            }
            pen.stroke(points, List(7) { 0.012f + charge * 0.008f },
                List(7) { i -> (1 - i / 7f) * (0.9f - age % 2 * 0.25f) })
        }
    }

    private fun trail(pen: EnergyPen, scene: Scene, level: ClientLevel, age: Float) {
        val points = (0..10).map { i -> core(scene, level, max(0f, age - i * 0.45f)) }
        pen.stroke(points, List(11) { 0.08f * (1 - it / 11f) },
            List(11) { 0.26f * (1 - it / 10f) })
    }

    private fun connections(pen: EnergyPen, scene: Scene, core: Vec3, age: Float) {
        for ((index, region) in scene.regions.withIndex()) {
            val elapsed = age - scene.animation.connection(index, scene.regions.size)
            if (elapsed < 0) continue
            val target = Vec3.atCenterOf(region.seed)
            val reveal = EaseHelper.easeOutExpo((elapsed / InfuserVisuals.BEAM_FLIGHT).coerceIn(0f, 1f))
            val end = core.lerp(target, reveal.toDouble())
            val delta = end.subtract(core)
            if (delta.lengthSqr() < 1e-8) continue
            val direction = delta.normalize()
            val side = direction.cross(if (abs(direction.y) > 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)).normalize()
            val up = direction.cross(side).normalize()
            val segments = (delta.length() * 2).toInt().coerceIn(16, 48)
            val charge = scene.animation.charge(age)
            val frame = floor(age * 0.75).toLong()
            val blend = InfuserSequence.smooth((age * 0.75 % 1).toFloat())
            val points = (0..segments).map { i ->
                val t = i.toDouble() / segments
                val envelope = sin(t * Math.PI)
                fun noise(axis: Int): Double {
                    val seed = index * 991L + i * 37L + axis * 7919L
                    val a = RenderUtil.rand01(seed + frame * 103)
                    val b = RenderUtil.rand01(seed + (frame + 1) * 103)
                    return (a + (b - a) * blend - 0.5) * 2
                }
                val amplitude = envelope * (0.09 + charge * 0.24)
                core.lerp(end, t).add(side.scale(amplitude * (noise(0) + sin(t * 19 - age * 0.7) * 0.4)))
                    .add(up.scale(amplitude * noise(1)))
            }
            val widths = (0..segments).map { i ->
                val packet = InfuserVisuals.beamPulse(elapsed, i.toFloat() / segments)
                (0.025f + charge * 0.022f) * (1 + packet * 1.7f)
            }
            val alpha = (0..segments).map { i ->
                val packet = InfuserVisuals.beamPulse(elapsed, i.toFloat() / segments)
                (0.65f + packet * 0.35f) * min(1f, elapsed * 2)
            }
            pen.stroke(points, widths, alpha)
            val hitAge = elapsed - InfuserVisuals.BEAM_FLIGHT
            if (hitAge in 0f..9f) {
                val flash = exp(-hitAge * 0.5f)
                pen.spark(target, 0.2f + flash * 0.65f, flash)
                val radius = EaseHelper.easeOutExpo((hitAge / 9f).coerceIn(0f, 1f)) * 1.15
                for (spark in 0..7) {
                    val a = spark * Math.PI / 4 + index
                    val ray = side.scale(cos(a)).add(up.scale(sin(a)))
                    pen.stroke(listOf(target.add(ray.scale(radius * 0.6)), target.add(ray.scale(radius))),
                        listOf(0.005f, 0.025f), listOf(0f, (1 - hitAge / 9f) * 0.9f))
                }
            }
        }
    }

    private fun gathering(pen: EnergyPen, core: Vec3, age: Float, animation: InfuserSequence) {
        val charge = animation.charge(age)
        val strength = InfuserSequence.smooth((charge - 0.12f) / 0.5f)
        if (strength <= 0f) return
        val collapseStart = animation.impact - InfuserVisuals.collapseDuration(animation)
        val collapse = InfuserVisuals.collapse(animation, age)
        for (mote in 0..63) {
            val cycle = InfuserVisuals.gatherCycle(min(age, collapseStart), mote)
            val life = sin(cycle * Math.PI).toFloat()
            val alpha = strength * (life + (1 - life) * collapse)
            val point = core.add(InfuserVisuals.gatherPoint(animation, age, mote))
            pen.spark(point, 0.018f + charge * 0.025f + collapse * 0.035f, alpha)
            if (mote % 2 != 0) continue
            val points = mutableListOf<Vec3>()
            for (i in 0..10) {
                val past = age - i * 0.32f
                if (past < collapseStart && floor((past + mote * 17.13f) / 52f) !=
                    floor((min(age, collapseStart) + mote * 17.13f) / 52f)) break
                points.add(core.add(InfuserVisuals.gatherPoint(animation, past, mote)))
            }
            pen.stroke(points, List(points.size) { i -> (0.012f + collapse * 0.035f) * (1 - i / points.size.toFloat()) },
                List(points.size) { i -> alpha * (1 - i / points.size.toFloat()).pow(2) })
        }
    }

    private fun rebound(pen: EnergyPen, center: Vec3, after: Float) {
        val fade = (1 - after / 18f).coerceIn(0f, 1f).pow(2)
        val flash = exp(-after * 0.9f)
        pen.spark(center, 0.35f + EaseHelper.easeOutExpo((after / 3f).coerceIn(0f, 1f)) * 1.8f, flash)
        for (i in 0..39) {
            val angle = i * 2.399963
            val y = 1.0 - (i + 0.5) / 20.0
            val direction = Vec3(cos(angle) * sqrt(1 - y * y), y, sin(angle) * sqrt(1 - y * y))
            val speed = 0.65 + RenderUtil.rand01(i * 31L) * 0.6
            val points = (0..6).map { step ->
                val t = max(0f, after - step * 0.27f)
                center.add(direction.scale(InfuserVisuals.burstRadius(t) * speed))
            }
            pen.stroke(points, List(7) { (0.055f + flash * 0.06f) * (1 - it / 7f) },
                List(7) { fade * (1 - it / 6f) })
            pen.spark(points[0], 0.04f * fade, fade)
        }
    }

    private fun worldPoint(level: ClientLevel, pos: BlockPos, point: Vec3): Vec3 {
        val ship = level.getLoadedShipManagingPos(pos) ?: return point
        val p = ship.renderTransform.shipToWorld.transformPosition(Vector3d(point.x, point.y, point.z))
        return Vec3(p.x, p.y, p.z)
    }

    private class EnergyPen(val vc: VertexConsumer, val matrix: Matrix4f, val camera: Vec3) {
        fun stroke(points: List<Vec3>, widths: List<Float>, alphas: List<Float>) {
            if (points.size < 2) return
            val relative = points.map { it.subtract(camera) }
            InfuserRibbon.draw(vc, matrix, relative, widths.map { it * 3.2f }, alphas.map { it * 0.19f }, 0.60f, 0.34f, 0.86f)
            InfuserRibbon.draw(vc, matrix, relative, widths, alphas.map { it * 0.8f }, 0.765f, 0.627f, 0.89f)
            InfuserRibbon.draw(vc, matrix, relative, widths.map { it * 0.3f }, alphas, 0.98f, 0.88f, 1f)
        }

        fun spark(center: Vec3, size: Float, alpha: Float) {
            if (size <= 0.001f || alpha <= 0.001f) return
            val p = center.subtract(camera)
            val view = p.normalize()
            var right = view.cross(Vec3(0.0, 1.0, 0.0)).normalize()
            if (right.lengthSqr() < 1e-8) right = Vec3(1.0, 0.0, 0.0)
            val up = right.cross(view).normalize().scale(size.toDouble())
            right = right.scale(size.toDouble())
            fun vertex(radius: Float, angle: Double, opacity: Float) {
                val pos = p.add(right.scale(cos(angle) * radius)).add(up.scale(sin(angle) * radius))
                vc.vertex(matrix, pos.x.toFloat(), pos.y.toFloat(), pos.z.toFloat())
                    .color(0.88f, 0.73f, 1f, (alpha * opacity).coerceIn(0f, 1f)).uv(0.5f, 0.5f).endVertex()
            }
            val segments = if (size > 0.25f) 16 else 8
            val radii = floatArrayOf(0f, 0.28f, 1f)
            val opacity = floatArrayOf(1f, 0.38f, 0f)
            for (ring in 0..1) for (i in 0 until segments) {
                val a = i * Math.PI * 2 / segments; val b = (i + 1) * Math.PI * 2 / segments
                vertex(radii[ring], a, opacity[ring]); vertex(radii[ring + 1], a, opacity[ring + 1])
                vertex(radii[ring + 1], b, opacity[ring + 1]); vertex(radii[ring], b, opacity[ring])
            }
        }
    }
}
