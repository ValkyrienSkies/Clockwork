package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.createmod.catnip.render.SuperRenderTypeBuffer
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool.ToolType
import org.valkyrienskies.clockwork.platform.SharedValues
import org.valkyrienskies.clockwork.util.render.RenderUtil.addRibbonSegment
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.world.clipIncludeShips
import java.util.UUID
import kotlin.math.*

class WanderwandEffectRenderer {
    private data class State(val tool: ToolType, val first: WandAnchor?, val rope: WandAnchor?, val length: Double, val sync: Long) {
        var previousAnchor: Vec3? = null
    }
    private data class Link(val a: WandAnchor, val b: WandAnchor, val rope: Boolean, val length: Double)
    private data class Pulse(val owner: UUID, val action: String, val anchor: WandAnchor, val other: WandAnchor?, val born: Long)
    private val states = mutableMapOf<UUID, State>()
    private val links = mutableListOf<Link>()
    private val pulses = mutableListOf<Pulse>()
    private val blocks = WandBlockEffects()
    private var world: ClientLevel? = null
    private var selectionStack: net.minecraft.world.item.ItemStack? = null
    private val markerType = RenderType.entityTranslucentEmissive(ClockworkMod.asResource("textures/gui/wand_targeting.png"), false)

    private fun checkWorld(): ClientLevel? {
        val level = Minecraft.getInstance().level
        if (world !== level) {
            states.clear(); links.clear(); pulses.clear(); blocks.clear(); selectionStack = null; world = level
        }
        return level
    }
    fun holdingRope(): Boolean {
        checkWorld()
        return states[Minecraft.getInstance().player?.uuid]?.rope != null
    }
    fun ropeLength(): Double? = states[Minecraft.getInstance().player?.uuid]?.takeIf { it.rope != null }?.length
    fun isSwinging(player: net.minecraft.world.entity.player.Player): Boolean {
        if (player.onGround() || player.abilities.flying || player.isPassenger || player.isFallFlying ||
            player.mainHandItem.item !is WanderwandItem) return false
        val state = states[player.uuid] ?: return false
        val anchor = state.rope?.world(player.level(), true) ?: return false
        return anchor.y > player.y + 0.5 && anchor.distanceTo(player.position().add(0.0, 1.0, 0.0)) >= state.length - 0.6
    }
    fun accept(data: CompoundTag) {
        val level = checkWorld() ?: return
        when (data.getString("kind")) {
            "state" -> {
                val owner = data.getUUID("owner")
                val previous = states[owner]
                val rope = if (data.contains("rope")) WandAnchor.load(data.getCompound("rope")) else null
                states[owner] = State(ToolType.values()[data.getInt("tool").coerceIn(0, 4)],
                    if (data.contains("first")) WandAnchor.load(data.getCompound("first")) else null,
                    rope, data.getDouble("length"), level.gameTime).also {
                    if (previous?.rope == rope) it.previousAnchor = previous?.previousAnchor
                }
            }
            "links" -> {
                links.clear()
                for (entry in data.getList("links", Tag.TAG_COMPOUND.toInt()).take(256)) {
                    val t = entry as CompoundTag
                    links.add(Link(WandAnchor.load(t.getCompound("a")), WandAnchor.load(t.getCompound("b")), t.getBoolean("rope"), t.getDouble("length")))
                }
            }
            "event" -> {
                val pulse = Pulse(data.getUUID("owner"), data.getString("action"), WandAnchor.load(data.getCompound("anchor")),
                    if (data.contains("other")) WandAnchor.load(data.getCompound("other")) else null, level.gameTime)
                if (pulses.size >= 64) pulses.removeAt(0)
                pulses.add(pulse)
                WanderwandHandEffects.accept(pulse.owner, pulse.action)
                if (pulse.action.startsWith("select") || pulse.action.startsWith("deselect")) blocks.pulseOrigin = pulse.anchor.local()
                if (pulse.action == "weld_end") blocks.weldPulse(pulse.anchor, level.gameTime)
            }
        }
    }
    fun handlePacket(packet: WanderwandRenderUpdatePacket) {
        checkWorld()
        if (packet.tool == ToolType.SELECT || packet.tool == ToolType.DESELECT) blocks.update(packet.blocks ?: CompoundTag())
    }
    fun clientTick(clientLevel: ClientLevel) {
        val level = checkWorld() ?: return
        val player = Minecraft.getInstance().player ?: return
        pulses.removeAll { level.gameTime - it.born > 16 }
        blocks.tickPulses(level)
        states.entries.removeIf { (id, s) -> level.gameTime - s.sync > 40 || level.getPlayerByUUID(id)?.mainHandItem?.item !is WanderwandItem }
        if (player.mainHandItem.item is WanderwandItem) {
            // Read the equipped stack too: swapping wands or dimensions cannot leave a previous wand's selection.
            if (selectionStack !== player.mainHandItem) {
                selectionStack = player.mainHandItem
                val tag = player.mainHandItem.tag
                val dimension = level.dimension().location().toString()
                blocks.update(if (tag != null && (!tag.contains("selectionDimension") || tag.getString("selectionDimension") == dimension))
                    tag.getCompound("selectedBlocks") else CompoundTag())
            }
            blocks.tick(level)
        } else selectionStack = null
        val state = states[player.uuid] ?: return
        val rope = state.rope ?: return
        val anchor = rope.world(level) ?: return
        val velocity = anchor.subtract(state.previousAnchor ?: anchor)
        state.previousAnchor = anchor
        if (!player.abilities.flying && !player.isPassenger) {
            WandRopePhysics.constrain(player.position().add(0.0, 1.0, 0.0), player.deltaMovement, anchor, velocity, state.length)?.let {
                player.deltaMovement = it
                player.fallDistance = 0f
            }
        }
    }

    private fun target(level: ClientLevel, tool: ToolType, state: State?): WandAnchor? {
        val player = Minecraft.getInstance().player ?: return null
        val range = if (tool == ToolType.BIND) {
            if (state?.rope == null) 64.0 else Minecraft.getInstance().gameMode?.pickRange?.toDouble() ?: 4.5
        } else 15.0
        val eye = player.eyePosition
        val hit = level.clipIncludeShips(ClipContext(eye, eye.add(player.lookAngle.scale(range)),
            ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player), true,
            if (tool == ToolType.WELD) state?.first?.shipId else null)
        if (hit.type == HitResult.Type.BLOCK) return WandAnchor.at(level, hit.blockPos, hit.direction)
        return if (tool == ToolType.SELECT || tool == ToolType.DESELECT)
            WandAnchor(BlockPos.containing(eye.add(player.lookAngle.scale(4.0))), Direction.UP) else null
    }

    fun render(ms: PoseStack, buffer: SuperRenderTypeBuffer, camera: Vec3, partialTicks: Float) {
        val level = checkWorld() ?: return
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val time = (level.gameTime % 24000).toFloat() + partialTicks
        val buffers = mc.renderBuffers().bufferSource()
        val matrix = ms.last().pose()
        val inverse = Matrix4f(matrix).invert()
        var vc = buffers.getBuffer(ClockworkRenderTypes.GRAVITRON_ENERGY)
        for (link in links.sortedBy { min(it.a.world(level, true)?.distanceToSqr(camera) ?: Double.MAX_VALUE,
            it.b.world(level, true)?.distanceToSqr(camera) ?: Double.MAX_VALUE) }.take(64)) {
            val a = link.a.world(level, true)?.subtract(camera) ?: continue
            val b = link.b.world(level, true)?.subtract(camera) ?: continue
            if (min(a.lengthSqr(), b.lengthSqr()) > 192 * 192) continue
            tether(vc, matrix, a, b, time, link.rope, link.length, 1f)
            ring(vc, matrix, a, b.subtract(a), 0.14, time, 0.7f)
            ring(vc, matrix, b, a.subtract(b), 0.14, -time, 0.7f)
        }
        for ((id, state) in states) {
            val owner = level.getPlayerByUUID(id) ?: continue
            if (owner.isInvisible || owner.position().distanceToSqr(camera) > 128 * 128) continue
            val anchor = state.rope ?: continue
            val a = WanderwandHandEffects.tip(owner, partialTicks, inverse).subtract(camera)
            val b = anchor.world(level, true)?.subtract(camera) ?: continue
            val shotAge = pulses.lastOrNull { it.owner == id && it.action == "bind_start" }?.let { level.gameTime - it.born + partialTicks } ?: 20f
            tether(vc, matrix, a, a.lerp(b, (shotAge / 4f).toDouble().coerceIn(0.0, 1.0)), time, true, state.length, 1f)
            ring(vc, matrix, b, a.subtract(b), 0.2, time, 0.8f)
        }
        for (pulse in pulses) {
            val age = (level.gameTime - pulse.born).toFloat() + partialTicks
            val life = (1 - age / 14f).coerceIn(0f, 1f)
            val end = pulse.anchor.world(level, true)?.subtract(camera) ?: continue
            val owner = level.getPlayerByUUID(pulse.owner)
            val hand = owner?.let { WanderwandHandEffects.tip(it, partialTicks, inverse).subtract(camera) }
            val other = pulse.other?.world(level, true)?.subtract(camera)
            val start = hand ?: other ?: end
            if (end.lengthSqr() > 192 * 192) continue
            if (pulse.action.endsWith("break")) {
                snap(vc, matrix, other ?: start, end, age, pulse.action == "rope_break", time)
                continue
            }
            val inward = pulse.action in listOf("weld_start", "dismiss", "cancel", "deselect_end")
            val red = pulse.action.startsWith("deselect")
            val progress = (age / 8f).coerceIn(0f, 1f)
            val a = if (inward) end else start
            val b = if (inward) start else end
            cast(vc, matrix, a, b, age, time, if (pulse.action.endsWith("end")) 1.35f else 1f, red)
            if (other != null) cast(vc, matrix, end, other, age, time, 0.9f, false)
            val radius = if (inward) 0.12 + (1 - progress) * 0.9 else 0.12 + progress * if (pulse.action.endsWith("end")) 1.7 else 0.7
            ring(vc, matrix, end, start.subtract(end), radius, time, life, red)
            ring(vc, matrix, end, Vec3(0.3, 1.0, 0.2), radius * 0.65, -time, life * 0.8f, red)
            if (hand != null) ring(vc, matrix, hand, end.subtract(hand), 0.07 + progress * 0.27, time, life, red)
        }
        blocks.renderPulses(level, ms, buffers, camera, partialTicks)
        if (player.mainHandItem.item is WanderwandItem) {
            val tool = SharedValues.wanderwandHandler.currentTool ?: ToolType.SELECT
            val state = states[player.uuid]
            val hit = target(level, tool, state)
            if (tool == ToolType.SELECT || tool == ToolType.DESELECT) {
                val first = state?.first?.pos ?: hit?.pos
                val deselect = if (tool == ToolType.DESELECT && first != null && hit != null)
                    org.joml.primitives.AABBi(min(first.x, hit.pos.x), min(first.y, hit.pos.y), min(first.z, hit.pos.z),
                        max(first.x, hit.pos.x) + 1, max(first.y, hit.pos.y) + 1, max(first.z, hit.pos.z) + 1) else null
                blocks.renderSelection(level, ms, buffers, camera, time, deselect)
                vc = buffers.getBuffer(ClockworkRenderTypes.GRAVITRON_ENERGY)
                // Keep the region borders visible after completing a selection, including empty space.
                for (box in blocks.boxes) {
                    if (camera.x < box.minX() - 64.0 || camera.x > box.maxX() + 64.0 ||
                        camera.y < box.minY() - 64.0 || camera.y > box.maxY() + 64.0 ||
                        camera.z < box.minZ() - 64.0 || camera.z > box.maxZ() + 64.0) continue
                    selectionBox(vc, matrix, BlockPos(box.minX(), box.minY(), box.minZ()),
                        BlockPos(box.maxX() - 1, box.maxY() - 1, box.maxZ() - 1), camera, time, false)
                }
                if (hit != null && hit.shipId < 0) selectionBox(vc, matrix, state?.first?.pos ?: hit.pos, hit.pos, camera, time, tool == ToolType.DESELECT)
            }
            if (tool == ToolType.WELD && state?.first != null && hit != null)
                blocks.renderPreview(level, state.first, hit, ms, buffers, camera, time)
            if (hit != null && tool in listOf(ToolType.BIND, ToolType.ATTACH, ToolType.WELD)) {
                val valid = when (tool) {
                    ToolType.ATTACH -> hit.shipId >= 0 && hit.shipId != state?.first?.shipId
                    ToolType.WELD -> if (state?.first == null) hit.shipId >= 0 else hit.shipId != state.first.shipId
                    else -> state?.rope == null || state.rope.pos != hit.pos || state.rope.shipId != hit.shipId
                }
                marker(ms, buffers.getBuffer(markerType), hit, camera, time, valid)
            }
            state?.first?.let { marker(ms, buffers.getBuffer(markerType), it, camera, -time, true) }
        }
        buffers.endBatch(ClockworkRenderTypes.GRAVITRON_SURFACE)
        buffers.endBatch(ClockworkRenderTypes.GRAVITRON_ENERGY)
        buffers.endBatch(markerType)
    }

    /** A bright, short impulse from the crystal, independent of the fine persistent rope strands. */
    private fun cast(vc: VertexConsumer, matrix: Matrix4f, a: Vec3, b: Vec3, age: Float, time: Float, power: Float, red: Boolean) {
        if (age >= 8 || a.distanceToSqr(b) < 0.0001) return
        val progress = age / 5.5
        val front = min(1.0, progress * 1.5)
        val back = max(0.0, progress * 1.5 - 0.65).coerceAtMost(1.0)
        val alpha = (1 - age / 8f).coerceAtLeast(0f)
        val (u, v) = basis(b.subtract(a))
        var previous = a.lerp(b, back)
        for (i in 1..16) {
            val t = back + (front - back) * i / 16.0
            val p = a.lerp(b, t)
            addRibbonSegment(vc, matrix, previous, p, 0.15f * power, 0.65f, if (red) 0.1f else 0.3f, if (red) 0.15f else 0.9f, alpha * 0.25f)
            addRibbonSegment(vc, matrix, previous, p, 0.055f * power, if (red) 1f else 0.765f, if (red) 0.22f else 0.627f, if (red) 0.25f else 0.89f, alpha)
            addRibbonSegment(vc, matrix, previous, p, 0.019f * power, 1f, if (red) 0.75f else 0.9f, if (red) 0.7f else 1f, alpha)
            previous = p
        }
        for (strand in 0..2) {
            var old = a.lerp(b, back)
            for (i in 1..16) {
                val t = back + (front - back) * i / 16.0
                val phase = t * PI * 5 + strand * 2 * PI / 3 - time * 0.25
                val radius = sin(i * PI / 16) * 0.10 * power
                val p = a.lerp(b, t).add(u.scale(cos(phase) * radius)).add(v.scale(sin(phase) * radius))
                addRibbonSegment(vc, matrix, old, p, 0.011f, if (red) 1f else 0.85f, if (red) 0.32f else 0.7f, if (red) 0.3f else 1f, alpha * 0.7f)
                old = p
            }
        }
    }

    private fun snap(vc: VertexConsumer, matrix: Matrix4f, a: Vec3, b: Vec3, age: Float, rope: Boolean, time: Float) {
        val life = (1 - age / if (rope) 10f else 13f).coerceIn(0f, 1f)
        if (life <= 0) return
        val center = a.lerp(b, 0.5)
        val (u, v) = basis(b.subtract(a))
        if (rope) {
            // Two loose halves recoil toward their anchors, with an obvious gap at the fracture.
            for (end in listOf(a, b)) {
                var previous = end
                for (i in 1..20) {
                    val t = i / 20.0
                    val reach = life * 0.9
                    val point = end.lerp(center, t * reach)
                        .add(u.scale(sin(t * PI * 3 - age * 0.7) * t * (1 - life) * 0.8))
                        .add(v.scale(sin(t * PI) * (1 - life) * 0.5))
                    addRibbonSegment(vc, matrix, previous, point, 0.022f, 0.94f, 0.8f, 1f, life)
                    previous = point
                }
            }
        } else {
            // Glue tears into thicker globules, unlike the rope's whipping strands.
            ring(vc, matrix, center, b.subtract(a), 0.15 + age * 0.13, time, life)
            ring(vc, matrix, center, u, 0.1 + age * 0.09, -time, life * 0.7f)
        }
        for (i in 0 until if (rope) 8 else 14) {
            val angle = i * 2.39996
            val direction = Vec3(cos(angle), sin(i * 1.73) * 0.8, sin(angle)).normalize()
            val p = center.add(direction.scale(age * if (rope) 0.075 else 0.045)).add(0.0, if (rope) 0.0 else -age * age * 0.002, 0.0)
            val size = if (rope) 0.014f else 0.055f * life
            addRibbonSegment(vc, matrix, p, p.add(direction.scale(if (rope) 0.22 else 0.09)), size * 3, 0.7f, 0.4f, 0.95f, life * 0.25f)
            addRibbonSegment(vc, matrix, p, p.add(direction.scale(if (rope) 0.22 else 0.09)), size, 0.95f, 0.8f, 1f, life)
        }
    }

    private fun tether(vc: VertexConsumer, matrix: Matrix4f, a: Vec3, b: Vec3, time: Float, rope: Boolean, length: Double, alpha: Float) {
        val delta = b.subtract(a)
        val distance = delta.length()
        if (distance < 0.001) return
        val (u, v) = basis(delta)
        val sag = if (rope) min(4.0, sqrt(max(0.0, length * length - distance * distance)) * 0.25) else 0.0
        val count = (distance * 2).toInt().coerceIn(12, 48)
        val strands = if (rope) 2 else 5
        for (strand in 0 until strands) {
            var previous = a
            for (i in 1..count) {
                val t = i.toDouble() / count
                val radius = sin(t * PI) * (if (rope) 0.025 else 0.13 + 0.04 * sin(time * 0.08))
                val phase = t * PI * (if (rope) 6 else 2) + strand * PI * 2 / strands + time * 0.06
                val p = a.lerp(b, t).add(u.scale(cos(phase) * radius)).add(v.scale(sin(phase) * radius))
                    .add(0.0, -sin(t * PI) * sag, 0.0)
                val width = if (rope) 0.012f else (0.025 + sin(t * PI) * 0.035).toFloat()
                addRibbonSegment(vc, matrix, previous, p, width * 2.5f, 0.55f, 0.27f, 0.8f, alpha * 0.16f)
                addRibbonSegment(vc, matrix, previous, p, width, 0.765f, 0.627f, 0.89f, alpha * if (rope) 0.8f else 0.35f)
                if (rope) addRibbonSegment(vc, matrix, previous, p, width * 0.3f, 0.97f, 0.82f, 1f, alpha * 0.75f)
                previous = p
            }
        }
    }
    private fun basis(direction: Vec3): Pair<Vec3, Vec3> {
        val n = if (direction.lengthSqr() < 1e-8) Vec3(0.0, 1.0, 0.0) else direction.normalize()
        val u = n.cross(if (abs(n.y) > 0.9) Vec3(1.0, 0.0, 0.0) else Vec3(0.0, 1.0, 0.0)).normalize()
        return u to n.cross(u).normalize()
    }
    private fun ring(vc: VertexConsumer, matrix: Matrix4f, center: Vec3, normal: Vec3, radius: Double, time: Float, alpha: Float, red: Boolean = false) {
        val (u, v) = basis(normal)
        var previous: Vec3? = null
        for (i in 0..32) {
            val angle = i * PI / 16 + time * 0.012
            val r = radius * (1 + sin(angle * 5 + time * 0.15) * 0.035)
            val p = center.add(u.scale(cos(angle) * r)).add(v.scale(sin(angle) * r))
            previous?.let {
                addRibbonSegment(vc, matrix, it, p, 0.036f, if (red) 1f else 0.765f, if (red) 0.2f else 0.627f, if (red) 0.24f else 0.89f, alpha * 0.25f)
                addRibbonSegment(vc, matrix, it, p, 0.012f, if (red) 1f else 0.88f, if (red) 0.3f else 0.75f, if (red) 0.32f else 1f, alpha)
            }
            previous = p
        }
    }
    private fun marker(ms: PoseStack, vc: VertexConsumer, anchor: WandAnchor, camera: Vec3, time: Float, valid: Boolean) {
        val level = world ?: return
        val center = anchor.world(level, true)?.subtract(camera) ?: return
        val normalLocal = Vec3.atLowerCornerOf(anchor.face.normal)
        val ship = level.shipObjectWorld.loadedShips.getById(anchor.shipId)
        fun direction(local: Vec3): Vec3 {
            if (ship == null) return local
            val d = ship.renderTransform.shipToWorld.transformDirection(org.joml.Vector3d(local.x, local.y, local.z))
            return Vec3(d.x, d.y, d.z).normalize()
        }
        val normal = direction(normalLocal)
        val (localU, localV) = basis(normalLocal)
        val rawU = direction(localU)
        val rawV = direction(localV)
        val angle = sin(time * 0.035) * 0.12
        val radius = 0.31 + sin(time * 0.12) * 0.02
        val u = rawU.scale(cos(angle) * radius).add(rawV.scale(sin(angle) * radius))
        val v = rawV.scale(cos(angle) * radius).subtract(rawU.scale(sin(angle) * radius))
        val points = listOf(center.subtract(u).subtract(v), center.add(u).subtract(v), center.add(u).add(v), center.subtract(u).add(v))
        val uv = listOf(0f to 1f, 1f to 1f, 1f to 0f, 0f to 0f)
        for (i in 0..3) vc.vertex(ms.last().pose(), points[i].x.toFloat(), points[i].y.toFloat(), points[i].z.toFloat())
            .color(1f, if (valid) 1f else 0.25f, if (valid) 1f else 0.3f, 0.9f)
            .uv(uv[i].first, uv[i].second).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(0xF000F0)
            .normal(normal.x.toFloat(), normal.y.toFloat(), normal.z.toFloat()).endVertex()
    }
    private fun selectionBox(vc: VertexConsumer, matrix: Matrix4f, first: BlockPos, second: BlockPos, camera: Vec3, time: Float, remove: Boolean) {
        val low = Vec3(min(first.x, second.x) - 0.03, min(first.y, second.y) - 0.03, min(first.z, second.z) - 0.03).subtract(camera)
        val high = Vec3(max(first.x, second.x) + 1.03, max(first.y, second.y) + 1.03, max(first.z, second.z) + 1.03).subtract(camera)
        val points = (0..7).map { i -> Vec3(if (i and 1 == 0) low.x else high.x, if (i and 2 == 0) low.y else high.y, if (i and 4 == 0) low.z else high.z) }
        for (i in 0..7) for (axis in listOf(1, 2, 4)) if (i and axis == 0) {
            val a = points[i]; val b = points[i or axis]
            var previous = a
            for (j in 1..8) {
                val t = j / 8.0
                val jitter = sin(t * PI) * sin(time * 0.4 + i + j) * 0.025
                val p = a.lerp(b, t).add(jitter, -jitter, jitter)
                addRibbonSegment(vc, matrix, previous, p, 0.016f, if (remove) 1f else 0.765f, if (remove) 0.2f else 0.627f, if (remove) 0.24f else 0.89f, 0.85f)
                previous = p
            }
        }
    }
}
