package org.valkyrienskies.clockwork.content.curiosities.rope

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandAnchor
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.WandRopeCurve
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID
import kotlin.math.*

class RopeKnotEntityRenderer(context: EntityRendererProvider.Context) : EntityRenderer<RopeKnotEntity>(context) {
    override fun getTextureLocation(entity: RopeKnotEntity) = RopeKnotRenderer.TEXTURE

    override fun shouldRender(entity: RopeKnotEntity, frustum: Frustum, x: Double, y: Double, z: Double): Boolean {
        if (super.shouldRender(entity, frustum, x, y, z)) return true
        if (entity.end != 0) return false
        val link = RopeKnotRenderer.link(entity.linkId) ?: return false
        val a = link.a.world(entity.level(), true) ?: return false
        val b = link.b.world(entity.level(), true) ?: return false
        return min(a.distanceToSqr(x, y, z), b.distanceToSqr(x, y, z)) < 192 * 192 &&
            frustum.isVisible(AABB(a, b).inflate(4.0))
    }

    override fun render(entity: RopeKnotEntity, yaw: Float, partialTick: Float, ms: PoseStack,
                        buffers: MultiBufferSource, light: Int) {
        val level = entity.level() as ClientLevel
        val link = RopeKnotRenderer.link(entity.linkId) ?: return
        val anchor = if (entity.end == 0) link.a else link.b
        val point = anchor.world(level, true) ?: return
        val origin = entity.getPosition(partialTick)
        RopeKnotRenderer.knot(level, anchor, point.subtract(origin), ms, buffers)
        if (entity.end == 0) {
            val other = link.b.world(level, true) ?: return
            RopeKnotRenderer.rope(level, WandRopeCurve(point, other, link.length), origin, ms, buffers)
        }
        super.render(entity, yaw, partialTick, ms, buffers, light)
    }
}

object RopeKnotRenderer {
    val TEXTURE = ClockworkMod.asResource("textures/entity/rope.png")
    private val type = RenderType.entityCutoutNoCull(TEXTURE)
    data class Link(val a: WandAnchor, val b: WandAnchor, val length: Double)
    private data class Snap(val curve: WandRopeCurve, val born: Long, val untied: Boolean)
    private val links = mutableMapOf<UUID, Link>()
    private val snaps = mutableListOf<Snap>()
    private var world: ClientLevel? = null

    private fun world(): ClientLevel? {
        val level = Minecraft.getInstance().level
        if (world !== level) { links.clear(); snaps.clear(); world = level }
        return level
    }

    fun link(id: UUID?): Link? { world(); return links[id] }

    fun accept(data: CompoundTag) {
        val level = world() ?: return
        if (data.getString("kind") == "links") {
            links.clear()
            for (entry in data.getList("links", Tag.TAG_COMPOUND.toInt()).take(256)) {
                val tag = entry as CompoundTag
                if (!tag.getBoolean("physical") || !tag.hasUUID("id")) continue
                links[tag.getUUID("id")] = Link(WandAnchor.load(tag.getCompound("a")),
                    WandAnchor.load(tag.getCompound("b")), tag.getDouble("length"))
            }
        } else if (data.getString("kind") == "physical_rope_break") {
            val a = Vec3(data.getDouble("ax"), data.getDouble("ay"), data.getDouble("az"))
            val b = Vec3(data.getDouble("bx"), data.getDouble("by"), data.getDouble("bz"))
            if (snaps.size >= 32) snaps.removeAt(0)
            snaps.add(Snap(WandRopeCurve(a, b, data.getDouble("length")), level.gameTime, data.getBoolean("untied")))
        }
    }

    fun knot(level: ClientLevel, anchor: WandAnchor, offset: Vec3, ms: PoseStack, buffers: MultiBufferSource) {
        val point = anchor.world(level, true) ?: return
        ms.pushPose()
        ms.translate(offset.x, offset.y, offset.z)
        val ship = level.shipObjectWorld.loadedShips.getById(anchor.shipId) as? ClientShip
        if (ship != null) ms.mulPose(Quaternionf(ship.renderTransform.shipToWorldRotation))
        ms.mulPose(anchor.face.rotation)
        ms.translate(0.0, 0.1, 0.0)
        Minecraft.getInstance().itemRenderer.renderStatic(ItemStack(ClockworkItems.ROPE_KNOT.get()), ItemDisplayContext.NONE,
            LevelRenderer.getLightColor(level, BlockPos.containing(point)), OverlayTexture.NO_OVERLAY, ms, buffers, level, 0)
        ms.popPose()
    }

    fun rope(level: ClientLevel, curve: WandRopeCurve, origin: Vec3, ms: PoseStack, buffers: MultiBufferSource) {
        val count = ceil(curve.a.distanceTo(curve.b) * 0.5 + curve.sag * 12).toInt().coerceIn(4, 128)
        val points = (0..count).map { curve.point(it.toDouble() / count) }
        strand(level, points, origin, ms, buffers.getBuffer(type), 0.0625)
    }

    private fun strand(level: ClientLevel, points: List<Vec3>, origin: Vec3, ms: PoseStack, vc: VertexConsumer, radius: Double) {
        if (points.size < 2) return
        val horizontal = points.last().subtract(points.first()).cross(Vec3(0.0, 1.0, 0.0))
        val side = if (horizontal.lengthSqr() > 1e-6) horizontal.normalize() else Vec3(1.0, 0.0, 0.0)
        val rings = points.mapIndexed { i, p ->
            val tangent = points[min(points.lastIndex, i + 1)].subtract(points[max(0, i - 1)]).normalize()
            val up = tangent.cross(side).normalize()
            listOf(p.add(side.scale(-radius)).add(up.scale(-radius)), p.add(side.scale(radius)).add(up.scale(-radius)),
                p.add(side.scale(radius)).add(up.scale(radius)), p.add(side.scale(-radius)).add(up.scale(radius)))
        }
        var u = 0f
        for (i in 0 until points.lastIndex) {
            val nextU = u + (points[i].distanceTo(points[i + 1]) * 4).toFloat()
            val light = LevelRenderer.getLightColor(level, BlockPos.containing(points[i]))
            for (face in 0..3) {
                val next = (face + 1) % 4
                val normal = rings[i][face].add(rings[i][next]).scale(0.5).subtract(points[i]).normalize()
                fun vertex(p: Vec3, texU: Float, texV: Float) {
                    val v = p.subtract(origin)
                    vc.vertex(ms.last().pose(), v.x.toFloat(), v.y.toFloat(), v.z.toFloat()).color(255, 255, 255, 255)
                        .uv(texU, texV).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light)
                        .normal(ms.last().normal(), normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).endVertex()
                }
                vertex(rings[i][face], u, 0.25f)
                vertex(rings[i + 1][face], nextU, 0.25f)
                vertex(rings[i + 1][next], nextU, 0.75f)
                vertex(rings[i][next], u, 0.75f)
            }
            u = nextU
        }
    }

    fun renderEffects(ms: PoseStack, buffers: MultiBufferSource, camera: Vec3, partialTick: Float) {
        val level = world() ?: return
        val player = Minecraft.getInstance().player ?: return
        val stack = player.mainHandItem.takeIf { it.item is RopeKnotItem }
            ?: player.offhandItem.takeIf { it.item is RopeKnotItem }
        val anchor = stack?.let { RopeKnotItem.start(it, level) }
        val start = anchor?.world(level, true)
        if (start != null && start.distanceToSqr(player.position()) <= 128 * 128) {
            val hand = player.getEyePosition(partialTick).add(player.getViewVector(partialTick).scale(0.45)).add(0.0, -0.4, 0.0)
            knot(level, anchor, start.subtract(camera), ms, buffers)
            rope(level, WandRopeCurve(start, hand, RopeKnotPhysics.placedLength(start.distanceTo(hand))), camera, ms, buffers)
        }
        snaps.removeAll { level.gameTime - it.born > 16 }
        for (snap in snaps) {
            val age = level.gameTime - snap.born + partialTick.toDouble()
            val progress = (age / 16).coerceIn(0.0, 1.0)
            val recoil = 1 - (1 - progress).pow(3)
            for (end in 0..1) {
                val points = (0..24).map { i ->
                    val t = i / 24.0
                    val along = t * 0.5 * (1 - recoil * 0.85)
                    val p = snap.curve.point(if (end == 0) along else 1 - along)
                    p.add(0.0, -0.016 * age * age * t - sin(t * PI) * sin(age * 0.85) * (if (snap.untied) 0.08 else 0.3), 0.0)
                }
                strand(level, points, camera, ms, buffers.getBuffer(type), 0.0625 * (1 - progress * progress))
                if (!snap.untied && progress < 0.7) {
                    val tip = points.last()
                    for (fiber in 0..3) {
                        val angle = fiber * PI / 2 + age * 0.12
                        val tail = tip.add(cos(angle) * 0.15, -0.12 - progress * 0.3, sin(angle) * 0.15)
                        strand(level, listOf(tip, tail), camera, ms, buffers.getBuffer(type), 0.009 * (1 - progress))
                    }
                }
            }
        }
    }
}
