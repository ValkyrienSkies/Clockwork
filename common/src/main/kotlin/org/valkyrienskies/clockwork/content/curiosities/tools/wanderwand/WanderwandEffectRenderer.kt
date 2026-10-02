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
        pulses.removeAll { level.gameTime - it.born > 32 }
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
            tether(vc, matrix, a, a.lerp(b, (shotAge / 7f).toDouble().coerceIn(0.0, 1.0)), time, true, state.length, 1f)
            ring(vc, matrix, b, a.subtract(b), 0.2, time, 0.8f)
        }
        for (pulse in pulses) {
            val age = (level.gameTime - pulse.born).toFloat() + partialTicks
            val life = (1 - age / 30f).coerceIn(0f, 1f)
            val end = pulse.anchor.world(level, true)?.subtract(camera) ?: continue
            val owner = level.getPlayerByUUID(pulse.owner)
            val start = pulse.other?.world(level, true)?.subtract(camera)
                ?: owner?.let { WanderwandHandEffects.tip(it, partialTicks, inverse).subtract(camera) } ?: end
            if (end.lengthSqr() > 192 * 192) continue
            val inward = pulse.action in listOf("weld_start", "dismiss", "cancel", "deselect_end")
            val progress = (age / 15f).coerceIn(0f, 1f)
            val a = if (inward) end else start
            val b = if (inward) start else end
            if (age < 20 && pulse.action != "bind_start") {
                val front = a.lerp(b, min(1.0, progress * 1.3))
                val back = a.lerp(b, max(0.0, progress * 1.3 - 0.35))
                tether(vc, matrix, back, front, time, true, back.distanceTo(front), life * 1.2f)
            }
            val radius = if (inward) 0.12 + (1 - progress) * 0.9 else 0.12 + progress * if (pulse.action.endsWith("end")) 1.7 else 0.7
            ring(vc, matrix, end, start.subtract(end), radius, time, life)
            ring(vc, matrix, end, Vec3(0.3, 1.0, 0.2), radius * 0.65, -time, life * 0.6f)
            if (pulse.action == "break") {
                for (i in 0..7) {
                    val d = Vec3(cos(i * PI / 4), sin(i * 1.7) * 0.5, sin(i * PI / 4))
                    addRibbonSegment(vc, matrix, end.add(d.scale(age * 0.03)), end.add(d.scale(age * 0.03 + 0.15)),
                        0.018f, 0.95f, 0.75f, 1f, life)
                }
            }
        }
        if (player.mainHandItem.item is WanderwandItem) {
            val tool = SharedValues.wanderwandHandler.currentTool ?: ToolType.SELECT
            val state = states[player.uuid]
            val hit = target(level, tool, state)
            if (tool == ToolType.SELECT || tool == ToolType.DESELECT) {
                blocks.renderSelection(level, ms, buffers, camera, time)
                vc = buffers.getBuffer(ClockworkRenderTypes.GRAVITRON_ENERGY)
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
    private fun ring(vc: VertexConsumer, matrix: Matrix4f, center: Vec3, normal: Vec3, radius: Double, time: Float, alpha: Float) {
        val (u, v) = basis(normal)
        var previous: Vec3? = null
        for (i in 0..32) {
            val angle = i * PI / 16 + time * 0.012
            val r = radius * (1 + sin(angle * 5 + time * 0.15) * 0.035)
            val p = center.add(u.scale(cos(angle) * r)).add(v.scale(sin(angle) * r))
            previous?.let { addRibbonSegment(vc, matrix, it, p, 0.012f, 0.765f, 0.627f, 0.89f, alpha) }
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
        val low = Vec3(min(first.x, second.x).toDouble(), min(first.y, second.y).toDouble(), min(first.z, second.z).toDouble()).subtract(camera)
        val high = Vec3(max(first.x, second.x) + 1.0, max(first.y, second.y) + 1.0, max(first.z, second.z) + 1.0).subtract(camera)
        val points = (0..7).map { i -> Vec3(if (i and 1 == 0) low.x else high.x, if (i and 2 == 0) low.y else high.y, if (i and 4 == 0) low.z else high.z) }
        for (i in 0..7) for (axis in listOf(1, 2, 4)) if (i and axis == 0) {
            val a = points[i]; val b = points[i or axis]
            var previous = a
            for (j in 1..8) {
                val t = j / 8.0
                val jitter = sin(t * PI) * sin(time * 0.4 + i + j) * 0.025
                val p = a.lerp(b, t).add(jitter, -jitter, jitter)
                addRibbonSegment(vc, matrix, previous, p, 0.013f, if (remove) 1f else 0.765f, if (remove) 0.3f else 0.627f, 0.89f, 0.75f)
                previous = p
            }
        }
    }
}
