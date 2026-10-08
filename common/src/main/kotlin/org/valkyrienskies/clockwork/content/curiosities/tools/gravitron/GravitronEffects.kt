package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.util.render.ShaderPackCompat
import org.valkyrienskies.clockwork.util.render.SurfaceEffectPass
import org.valkyrienskies.clockwork.mixin.content.gravitron.GameRendererAccessor
import org.valkyrienskies.clockwork.util.render.RenderUtil.addRibbonSegment
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.UUID
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/** Client-only presentation. Ship anchors stay in ship space until each render frame. */
object GravitronEffects {
    class State {
        val animation = GravitronAnimation()
        var shipId = -1L
        var anchor = Vec3.ZERO
        var supercharged = false
        var lastSync = 0L
        var slot = -1
        var item: net.minecraft.world.item.Item? = null
        // Three articulated prongs and the central body emitter.
        val worldTips = arrayOfNulls<Vec3>(4)
        val viewTips = arrayOfNulls<Vec3>(4)
        var worldTipsTime = -1L
        var viewTipsTime = -1L
    }

    private data class ShipPulse(val shipId: Long, val anchor: Vec3, val action: GravitronAction, val birth: Long)
    private data class SurfacePass(val ship: ClientShip, val anchor: Vec3, val action: GravitronAction,
                                   val age: Float, val frozenAge: Float = 40f)
    private data class FrozenField(val anchor: Vec3, val animation: GravitronFreezeAnimation, var lastSync: Long)
    private data class RenderOwner(val player: Player?)
    private val owners = ArrayDeque<RenderOwner>()
    private val states = mutableMapOf<UUID, State>()
    private val pulses = mutableListOf<ShipPulse>()
    private val frozenFields = mutableMapOf<Long, FrozenField>()
    private var world: ClientLevel? = null

    @JvmStatic
    fun isGravitron(stack: ItemStack): Boolean = stack.item is GravitronItem || stack.item is CreativeGravitronItem

    private fun checkWorld(): ClientLevel? {
        val level = Minecraft.getInstance().level
        if (world !== level) {
            states.clear()
            pulses.clear()
            frozenFields.clear()
            owners.clear()
            GravitronSurfaceEffects.clear()
            world = level
        }
        return level
    }

    @JvmStatic
    fun tick() {
        val level = checkWorld() ?: return
        val now = level.gameTime
        val present = level.players().map { it.uuid }.toSet()
        states.keys.retainAll(present)
        for (player in level.players()) {
            val stack = player.mainHandItem
            if (!isGravitron(stack) || !player.isAlive || player.isSpectator) {
                states.remove(player.uuid)
                continue
            }
            val state = states.getOrPut(player.uuid) { State() }
            if (state.item != stack.item || state.slot != player.inventory.selected) {
                state.item = stack.item
                state.slot = player.inventory.selected
                state.supercharged = stack.item is CreativeGravitronItem
                // Do not replace a confirmed action received before the equipment update.
                if (now - state.lastSync > 2 || state.animation.action == GravitronAction.IDLE) {
                    state.animation.accept(GravitronAction.DRAW, now)
                    state.shipId = -1
                }
            }
            if (state.animation.holding && now - state.lastSync > 30) {
                state.animation.accept(GravitronAction.RELEASE, now)
            }
        }
        pulses.removeAll { now - it.birth > pulseLife(it.action) }
        frozenFields.entries.removeIf { (id, field) ->
            field.animation.finished(now) || (now - field.lastSync > 40 && level.shipObjectWorld.loadedShips.getById(id) == null)
        }
        GravitronSurfaceEffects.tick(level)
    }

    fun acceptFreeze(packet: GravitronFreezePacket) {
        val now = checkWorld()?.gameTime ?: return
        val previous = frozenFields[packet.shipId]
        if (packet.frozen) {
            if (previous != null && !previous.animation.thawing && previous.anchor == packet.anchor) {
                previous.lastSync = now // Heartbeats must never restart the freeze front.
            } else {
                frozenFields[packet.shipId] = FrozenField(packet.anchor,
                    GravitronFreezeAnimation(now, packet.age.coerceIn(0f, 40f)), now)
            }
        } else {
            previous?.animation?.thaw(now)
        }
    }

    fun accept(packet: GravitronAnimationPacket) {
        val level = checkWorld() ?: return
        val now = level.gameTime
        val state = states.getOrPut(packet.owner) { State() }
        state.lastSync = now
        state.supercharged = packet.supercharged
        if (packet.action == GravitronAction.DRAW && state.animation.action == GravitronAction.DRAW &&
            state.animation.age(now, 0f) < 10f) return
        state.animation.accept(packet.action, now, if (packet.supercharged) 0f else packet.load)
        // Idle heartbeats must not erase a launch/freeze effect that is still playing.
        if (packet.action != GravitronAction.IDLE) {
            state.shipId = packet.shipId
            state.anchor = packet.anchor
        }
        if (packet.action == GravitronAction.GRAB || packet.action == GravitronAction.HOLD) {
            // Grabbing also unfreezes the ship on the server.
            pulses.removeAll { it.shipId == packet.shipId }
        }
        if (packet.shipId >= 0 && packet.action in listOf(GravitronAction.LAUNCH, GravitronAction.FREEZE,
                GravitronAction.UNFREEZE, GravitronAction.RELEASE)) {
            pulses.removeAll { it.shipId == packet.shipId }
            if (pulses.size >= 64) pulses.removeAt(0)
            pulses.add(ShipPulse(packet.shipId, packet.anchor, packet.action, now))
        }
    }

    /** ItemInHandRenderer supplies the actual owner in both camera modes. GUI renders have none. */
    @JvmStatic
    fun beginItem(entity: LivingEntity, stack: ItemStack, context: ItemDisplayContext) {
        val player = entity as? Player
        val right = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
        val held = context.firstPerson() || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
        owners.addLast(RenderOwner(player?.takeIf {
            held && isGravitron(stack) && (it.mainArm == HumanoidArm.RIGHT) == right && it.mainHandItem.item == stack.item
        }))
    }

    @JvmStatic
    fun endItem() { if (owners.isNotEmpty()) owners.removeLast() }

    fun renderedPlayer(): Player? = owners.lastOrNull()?.player
    fun state(player: Player?): State? = player?.let { states[it.uuid] }

    /** Capture the articulated tips after their joint transforms, in the render owner's space. */
    fun captureTip(player: Player, context: ItemDisplayContext, ms: PoseStack, index: Int, tip: Vector3f) {
        val state = state(player) ?: return
        val mc = Minecraft.getInstance()
        val point = ms.last().pose().transformPosition(tip)
        if (context.firstPerson()) {
            state.viewTips[index] = Vec3(point.x.toDouble(), point.y.toDouble(), point.z.toDouble())
            state.viewTipsTime = mc.level?.gameTime ?: -1
            return
        }
        state.worldTips[index] = Vec3(point.x.toDouble(), point.y.toDouble(), point.z.toDouble())
        state.worldTipsTime = mc.level?.gameTime ?: -1
    }

    private fun tips(player: Player, state: State, pt: Float, inverseView: Matrix4f): List<Vec3> {
        val mc = Minecraft.getInstance()
        val now = mc.level!!.gameTime
        if (player !== mc.player || !mc.options.cameraType.isFirstPerson) {
            if (state.worldTipsTime == now && state.worldTips.all { it != null }) return state.worldTips.map {
                val point = inverseView.transformPosition(Vector3f(it!!.x.toFloat(), it.y.toFloat(), it.z.toFloat()))
                mc.gameRenderer.mainCamera.position.add(point.x.toDouble(), point.y.toDouble(), point.z.toDouble())
            }
        } else if (now - state.viewTipsTime <= 1 && state.viewTips.all { it != null }) {
            // Hands render after the world and use a separate FOV. Reproject the previous hand
            // frame into the current world camera, so sprinting/zoom and camera motion stay aligned.
            val camera = mc.gameRenderer.mainCamera
            val accessor = mc.gameRenderer as GameRendererAccessor
            val worldFov = accessor.`clockwork$getFov`(camera, pt, true)
            val handFov = accessor.`clockwork$getFov`(camera, pt, false)
            val ratio = tan(worldFov * PI / 360) / tan(handFov * PI / 360)
            return state.viewTips.map {
                // Undo the complete world view (including view bob/hurt), not just camera rotation.
                val point = inverseView.transformPosition(Vector3f((it!!.x * ratio).toFloat(), (it.y * ratio).toFloat(), it.z.toFloat()))
                camera.position.add(point.x.toDouble(), point.y.toDouble(), point.z.toDouble())
            }
        }
        val direction = player.getViewVector(pt)
        val right = Vec3(direction.z, 0.0, -direction.x).normalize()
        val sign = if (player.mainArm == HumanoidArm.RIGHT) 1.0 else -1.0
        val pose = state.animation.sample(now, pt)
        val center = player.getEyePosition(pt).add(direction.scale(0.85 - pose.recoil * 0.2))
            .add(right.scale(sign * 0.32)).add(0.0, -0.28 - pose.lowering * 0.25, 0.0)
        val (u, v) = basis(direction)
        return (0..2).map { center.add(u.scale(cos(it * PI * 2 / 3) * 0.12))
            .add(v.scale(sin(it * PI * 2 / 3) * 0.12)) } + center.subtract(direction.scale(0.3))
    }

    @JvmStatic
    fun render(ms: PoseStack, partialTick: Float) {
        if (ShaderPackCompat.shadowPass()) return
        val level = checkWorld() ?: return
        val mc = Minecraft.getInstance()
        val camera = mc.gameRenderer.mainCamera.position
        val now = level.gameTime
        val time = (now % 24000).toFloat() + partialTick
        val buffers = mc.renderBuffers().bufferSource()
        val surfaces = mutableListOf<SurfacePass>()
        val legacy = ClockworkConfig.CLIENT.gravitronLegacyShipContours
        GravitronSurfaceEffects.beginFrame()
        val type = ClockworkRenderTypes.energyType()
        val vc = ClockworkRenderTypes.energyBuffer(buffers)
        val pose = ms.last().pose()
        val inverseView = Matrix4f(pose).invert()
        for (player in level.players()) {
            val state = states[player.uuid] ?: continue
            if (!isGravitron(player.mainHandItem) || player.isInvisible || !player.isAlive) continue
            if (player === mc.player && mc.options.cameraType.isFirstPerson && mc.options.hideGui) continue
            if (player.position().distanceToSqr(camera) > 128.0 * 128.0) continue
            val animation = state.animation
            val age = animation.age(now, partialTick)
            val holding = animation.holding && now - state.lastSync <= 30
            val action = animation.action
            val transient = action in listOf(GravitronAction.LAUNCH, GravitronAction.FREEZE,
                GravitronAction.UNFREEZE, GravitronAction.OVERLOAD, GravitronAction.RELEASE) && age < action.duration
            if (!holding && !transient) continue
            val ship = level.shipObjectWorld.loadedShips.getById(state.shipId) ?: continue
            val end = shipPoint(ship, state.anchor).subtract(camera)
            val starts = tips(player, state, partialTick, inverseView).map { it.subtract(camera) }
            val start = starts[3]
            val fade = if (holding) GravitronAnimation.smooth(age / 3f) else 1f - age / action.duration
            val rgb = GravitronVisuals.WANDERLITE
            if (action == GravitronAction.LAUNCH) {
                launchBurst(vc, pose, start, end, age)
            } else {
                tether(vc, pose, starts.take(3), end, time, player.id, rgb, fade, action, age)
            }
            if (holding) {
                if (legacy) {
                    shipField(vc, pose, ship, camera, time, rgb, 0.16f + animation.load.coerceAtMost(1f) * 0.08f, 1f)
                } else {
                    surfaces.add(SurfacePass(ship, state.anchor, GravitronAction.HOLD, age))
                }
                ring(vc, pose, end, end.subtract(start).normalize(), 0.2 + 0.04 * sin(time * 0.2), rgb, fade * 0.65f, time)
            }
        }
        for (pulse in pulses) {
            val ship = level.shipObjectWorld.loadedShips.getById(pulse.shipId) ?: continue
            if (ship.renderTransform.positionInWorld.distanceSquared(Vector3d(camera.x, camera.y, camera.z)) > 192.0 * 192.0) continue
            val age = now - pulse.birth + partialTick
            if (!legacy) {
                // Freeze/thaw coverage belongs to the ship's persistent field, not the weapon's one-shot.
                if (pulse.action == GravitronAction.FREEZE || pulse.action == GravitronAction.UNFREEZE) continue
                // The launch packet reaches the surface after the short central burst travels out.
                surfaces.add(SurfacePass(ship, pulse.anchor, pulse.action,
                    if (pulse.action == GravitronAction.LAUNCH) (age - 3f).coerceAtLeast(0f) else age))
                continue
            }
            // Preserve the original contour timing as well as its geometry for comparison.
            if (pulse.action == GravitronAction.RELEASE) continue
            val legacyLife = if (pulse.action == GravitronAction.FREEZE) 80f else 28f
            val progress = (age / legacyLife).coerceIn(0f, 1f)
            val scale = when (pulse.action) {
                GravitronAction.UNFREEZE -> 1f - progress * 0.7f
                GravitronAction.LAUNCH -> 1f + progress * 0.7f
                else -> 1.15f - GravitronAnimation.smooth(progress * 3f) * 0.15f
            }
            shipField(vc, pose, ship, camera, time, GravitronVisuals.WANDERLITE, (1f - progress) * 0.7f, scale)
        }
        if (!legacy) for ((id, field) in frozenFields) {
            val ship = level.shipObjectWorld.loadedShips.getById(id) ?: continue
            if (shipPoint(ship, field.anchor).distanceToSqr(camera) > 192.0 * 192.0) continue
            val animation = field.animation
            val age = animation.freezeAge(now, partialTick)
            surfaces.add(SurfacePass(ship, field.anchor,
                if (animation.thawing) GravitronAction.UNFREEZE else GravitronAction.FREEZE,
                if (animation.thawing) animation.thawAge(now, partialTick) else age, age))
        }
        buffers.endBatch(type)
        // These share BufferSource's fallback builder: finish all ribbons before changing formats.
        for (surface in surfaces.sortedBy { shipPoint(it.ship, it.anchor).distanceToSqr(camera) }) {
            GravitronSurfaceEffects.render(level, surface.ship, surface.anchor, surface.action,
                surface.age, ms, buffers, camera, surface.frozenAge)
        }
        SurfaceEffectPass.endBatch(buffers)
    }

    /** A single impulse from the body, including when no grab preceded the launch. */
    private fun launchBurst(vc: VertexConsumer, pose: Matrix4f, start: Vec3, end: Vec3, age: Float) {
        if (age >= 8f) return
        val direction = end.subtract(start).normalize()
        val head = (age / 3f).coerceIn(0f, 1f)
        val tail = ((age - 2f) / 3f).coerceIn(0f, 1f)
        val fade = 1f - GravitronAnimation.smooth(age / 8f)
        if (head > tail) {
            val a = start.lerp(end, tail.toDouble())
            val b = start.lerp(end, head.toDouble())
            ribbon(vc, pose, a, b, 0.13f, GravitronVisuals.WANDERLITE, fade * 0.22f)
            ribbon(vc, pose, a, b, 0.047f, GravitronVisuals.WANDERLITE, fade * 0.85f)
            ribbon(vc, pose, a, b, 0.016f, GravitronVisuals.WANDERLITE_LIGHT, fade)
        }
        ring(vc, pose, start.add(direction.scale(age * 0.035)), direction,
            0.07 + age * 0.025, GravitronVisuals.WANDERLITE, fade * 0.7f, 0f)
    }

    /** Three thin, bowed force strands; no solid core or death-ray beam geometry. */
    private fun tether(vc: VertexConsumer, pose: Matrix4f, starts: List<Vec3>, end: Vec3, time: Float,
                       seed: Int, rgb: Int, alpha: Float, action: GravitronAction, age: Float) {
        val start = starts.reduce(Vec3::add).scale(1.0 / 3)
        val delta = end.subtract(start)
        if (delta.lengthSqr() < 0.001) return
        val (u, v) = basis(delta.normalize())
        val segments = (delta.length() * 3).toInt().coerceIn(16, 48)
        val spread = (delta.length() * 0.035).coerceIn(0.08, 0.48)
        val reverse = action == GravitronAction.UNFREEZE || action == GravitronAction.GRAB || action == GravitronAction.HOLD
        val pulsePosition = if (action == GravitronAction.LAUNCH || action == GravitronAction.FREEZE || action == GravitronAction.UNFREEZE)
            (age / 9f).coerceIn(0f, 1f) else ((time * 0.06f) % 1f)
        val travel = if (reverse) 1f - pulsePosition else pulsePosition
        for (strand in 0..2) {
            val phase = strand * PI * 2 / 3 + seed * 0.71
            fun point(t: Double): Vec3 {
                val envelope = sin(t * PI)
                val angle = phase + t * 5.5 - time * 0.12
                val radius = spread * envelope
                val flutter = sin(t * 23 + time * 0.45 + phase) * 0.025 * envelope
                val reach = if (action == GravitronAction.RELEASE)
                    1f - GravitronAnimation.smooth((age - 2f) / 10f) else 1f
                return starts[strand].lerp(end, t * reach).add(u.scale((cos(angle) * radius + flutter) * reach))
                    .add(v.scale(sin(angle) * radius * reach))
            }
            var previous = point(0.0)
            for (i in 1..segments) {
                val t = i.toFloat() / segments
                val next = point(t.toDouble())
                val packet = (1f - abs(t - travel) / 0.14f).coerceIn(0f, 1f)
                val flicker = 0.65f + 0.2f * sin(time * 0.7f + i * 0.9f + strand)
                ribbon(vc, pose, previous, next, 0.008f + packet * 0.014f, rgb, alpha * (flicker + packet * 0.25f))
                if (packet > 0) ribbon(vc, pose, previous, next, 0.034f, rgb, alpha * packet * 0.12f)
                previous = next
            }
        }
    }

    /** Sparse moving contours wrap the ship's oriented bounds without re-rendering its blocks. */
    private fun shipField(vc: VertexConsumer, pose: Matrix4f, ship: ClientShip, camera: Vec3,
                          time: Float, rgb: Int, alpha: Float, scale: Float) {
        val bounds = ship.shipAABB ?: return
        val center = Vec3((bounds.minX() + bounds.maxX() + 1) * 0.5,
            (bounds.minY() + bounds.maxY() + 1) * 0.5, (bounds.minZ() + bounds.maxZ() + 1) * 0.5)
        val rx = ((bounds.maxX() - bounds.minX() + 1) * 0.5 + 0.08) * scale
        val ry = ((bounds.maxY() - bounds.minY() + 1) * 0.5 + 0.08) * scale
        val rz = ((bounds.maxZ() - bounds.minZ() + 1) * 0.5 + 0.08) * scale
        for (axis in 0..2) {
            val sweep = sin(time * 0.035 + axis * 2.1) * 0.7
            var previous: Vec3? = null
            for (i in 0..32) {
                val angle = i * PI * 2 / 32
                // A rounded rectangular contour hugs block-built ships more closely than a sphere.
                val x = cos(angle)
                val y = sin(angle)
                val norm = maxOf(abs(x), abs(y)).coerceAtLeast(0.001)
                val a = x / norm
                val b = y / norm
                val local = when (axis) {
                    0 -> center.add(rx * a, ry * sweep, rz * b)
                    1 -> center.add(rx * sweep, ry * a, rz * b)
                    else -> center.add(rx * a, ry * b, rz * sweep)
                }
                val point = shipPoint(ship, local).subtract(camera)
                previous?.let { ribbon(vc, pose, it, point, 0.018f, rgb, alpha) }
                previous = point
            }
        }
    }

    private fun ring(vc: VertexConsumer, pose: Matrix4f, center: Vec3, normal: Vec3,
                     radius: Double, rgb: Int, alpha: Float, time: Float) {
        val (u, v) = basis(normal)
        var previous: Vec3? = null
        for (i in 0..24) {
            val angle = i * PI * 2 / 24 + time * 0.05
            val point = center.add(u.scale(cos(angle) * radius)).add(v.scale(sin(angle) * radius))
            previous?.let { ribbon(vc, pose, it, point, 0.013f, rgb, alpha) }
            previous = point
        }
    }

    private fun basis(direction: Vec3): Pair<Vec3, Vec3> {
        val ref = if (abs(direction.y) < 0.9) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
        val u = direction.cross(ref).normalize()
        return u to direction.cross(u).normalize()
    }

    private fun shipPoint(ship: ClientShip, point: Vec3): Vec3 {
        val transformed = ship.renderTransform.shipToWorld.transformPosition(Vector3d(point.x, point.y, point.z))
        return Vec3(transformed.x, transformed.y, transformed.z)
    }

    private fun ribbon(vc: VertexConsumer, pose: Matrix4f, a: Vec3, b: Vec3, width: Float, rgb: Int, alpha: Float) =
        addRibbonSegment(vc, pose, a, b, width, (rgb shr 16 and 255) / 255f,
            (rgb shr 8 and 255) / 255f, (rgb and 255) / 255f, alpha.coerceIn(0f, 1f))

    private fun pulseLife(action: GravitronAction): Float = when (action) {
        GravitronAction.FREEZE -> 120f
        GravitronAction.RELEASE -> 14f
        GravitronAction.LAUNCH -> 31f
        else -> 28f
    }
}
