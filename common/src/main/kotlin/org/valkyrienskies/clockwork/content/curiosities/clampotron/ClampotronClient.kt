package org.valkyrienskies.clockwork.content.curiosities.clampotron

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraft.util.RandomSource
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkRenderTypes
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.curiosities.rope.LinkStressWarning
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronAction
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.GravitronSurfaceEffects
import org.valkyrienskies.clockwork.util.render.RibbonLine
import org.valkyrienskies.clockwork.util.render.SurfaceEffectPass
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.WeakHashMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object ClampotronClient {
    private class State(var birth: Long, var ship: Long) {
        var hold: FieldSound? = null
        var overload: FieldSound? = null
        var contacts: List<Vector3d>? = null
        var heat = 0f
        var previousHeat = 0f
    }
    private val devices = WeakHashMap<ClampotronBlockEntity, State>()

    fun track(be: ClampotronBlockEntity) {
        val mc = Minecraft.getInstance()
        if (be.level !== mc.level) return
        val state = devices.getOrPut(be) { State(be.level!!.gameTime, be.heldShip) }
        if (state.ship != be.heldShip) {
            state.ship = be.heldShip
            state.birth = be.level!!.gameTime
            state.contacts = null
            state.heat = 0f
            state.previousHeat = 0f
        }
        state.previousHeat = state.heat
        state.heat += (ClampotronPower.warning(be.strain) - state.heat) * 0.2f
        if (be.heldShip < 0 || !be.enabled || be.isOverStressed || be.getSpeed() == 0f) return
        if (state.hold?.isStopped != false) {
            state.hold = FieldSound(be, false).also { mc.soundManager.play(it) }
        }
        if (be.strain >= LinkStressWarning.START && state.overload?.isStopped != false) {
            state.overload = FieldSound(be, true).also { mc.soundManager.play(it) }
        }
    }

    fun render(ms: PoseStack, buffers: MultiBufferSource.BufferSource, camera: Vec3, partialTick: Float) {
        val level = Minecraft.getInstance().level ?: return
        devices.entries.removeIf { (be, _) -> be.isRemoved || be.level !== level || !level.hasChunkAt(be.blockPos) }
        val time = (level.gameTime % 24000).toDouble() + partialTick
        val visible = devices.entries.filter { (be, _) -> be.enabled && be.heldShip >= 0 &&
            position(be, Vector3d(0.5, 0.875, 0.5)).distanceToSqr(camera) < 128 * 128 }
        var vc = ClockworkRenderTypes.energyBuffer(buffers)
        val pose = ms.last().pose()
        var heat = 0f
        fun line(ribbon: RibbonLine, a: Vec3, b: Vec3, width: Float, alpha: Float, bright: Boolean = false) {
            ribbon.segment(a, b, width, if (bright) 0.97f + heat * 0.03f else 0.765f + heat * 0.235f,
                if (bright) 0.81f - heat * 0.26f else 0.627f - heat * 0.447f,
                if (bright) 0.94f - heat * 0.69f else 0.89f - heat * 0.77f, alpha)
        }
        for ((be, state) in visible) {
            val ship = level.shipObjectWorld.loadedShips.getById(be.heldShip) ?: continue
            vc = ClockworkRenderTypes.energyBuffer(buffers)
            val end = ClampotronBlockEntity.vec(ship.renderTransform.shipToWorld.transformPosition(be.anchor, Vector3d())).subtract(camera)
            val center = position(be, Vector3d(0.5, 0.875, 0.5)).subtract(camera)
            val axis = end.subtract(center).normalize()
            val reference = if (kotlin.math.abs(axis.y) < 0.9) Vec3(0.0, 1.0, 0.0) else Vec3(1.0, 0.0, 0.0)
            val u = axis.cross(reference).normalize()
            val v = axis.cross(u).normalize()
            val stress = be.strain.coerceIn(0f, 1f)
            heat = state.previousHeat + (state.heat - state.previousHeat) * partialTick
            val contacts = state.contacts ?: tips.map { tip ->
                val start = position(be, Vector3d(tip))
                val localStart = ship.renderTransform.worldToShip.transformPosition(Vector3d(start.x, start.y, start.z))
                val offset = start.subtract(position(be, Vector3d(0.5, 1.0, 0.5))).scale(1.8)
                val localOffset = ship.renderTransform.worldToShip.transformDirection(Vector3d(offset.x, offset.y, offset.z))
                var contact: Vector3d? = null
                for (scale in listOf(1.0, 0.5, 0.25, 0.125)) {
                    val target = Vector3d(be.anchor).fma(scale, localOffset)
                    val beyond = Vector3d(target).add(Vector3d(target).sub(localStart).normalize().mul(1.5))
                    val hit = level.clip(ClipContext(ClampotronBlockEntity.vec(localStart), ClampotronBlockEntity.vec(beyond),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, null))
                    if (hit.type == HitResult.Type.BLOCK && level.getShipManagingPos(hit.blockPos)?.id == ship.id) {
                        contact = Vector3d(hit.location.x, hit.location.y, hit.location.z)
                        break
                    }
                }
                contact ?: Vector3d(be.anchor).fma(0.125, localOffset)
            }.also { state.contacts = it }
            for ((index, tip) in tips.withIndex()) {
                val start = position(be, Vector3d(tip)).subtract(camera)
                val beamEnd = ClampotronBlockEntity.vec(ship.renderTransform.shipToWorld.transformPosition(contacts[index], Vector3d())).subtract(camera)
                var previous = start
                val core = RibbonLine(vc, pose)
                val glow = RibbonLine(vc, pose)
                val spark = RibbonLine(vc, pose)
                for (i in 1..32) {
                    val t = i / 32.0
                    val phase = index * PI / 2 + t * 5 - time * (0.13 + stress * 0.09)
                    val radius = sin(t * PI) * (0.10 + stress * 0.06)
                    val point = start.lerp(beamEnd, t).add(u.scale(cos(phase) * radius)).add(v.scale(sin(phase) * radius))
                    val pulse = (1.0 - kotlin.math.abs(t - (1 - (time * 0.06 % 1))) / 0.16).coerceIn(0.0, 1.0).toFloat()
                    line(core, previous, point, 0.022f + pulse * 0.015f, 0.65f + pulse * 0.3f)
                    line(glow, previous, point, 0.065f, 0.09f + pulse * 0.06f)
                    if (pulse > 0) line(spark, previous, point, 0.009f, pulse * 0.8f, true)
                    previous = point
                }
                core.end()
                glow.end()
                spark.end()
            }
            buffers.endBatch(ClockworkRenderTypes.energyType())
            vc = ClockworkRenderTypes.softEnergyBuffer(buffers)
            val endRadius = (0.34 + center.distanceTo(end) * 0.16).coerceAtMost(1.5)
            fun conePoint(t: Double, angle: Double): Vec3 {
                val radius = 0.22 + (endRadius - 0.22) * t
                return center.lerp(end, t).add(u.scale(cos(angle) * radius)).add(v.scale(sin(angle) * radius))
            }
            fun coneVertex(t: Double, angle: Double) {
                val point = conePoint(t, angle)
                val alpha = ((0.30 + stress * 0.10) * (1 - t * t) *
                    (0.8 + 0.2 * sin(t * 18 - time * 0.12))).toFloat()
                vc.vertex(pose, point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
                    .color(0.765f + heat * 0.235f, 0.627f - heat * 0.447f, 0.89f - heat * 0.77f, alpha)
                    .uv(0.5f, 0.5f).endVertex()
            }
            for (section in 0 until 16) {
                val t0 = section / 16.0
                val t1 = (section + 1) / 16.0
                for (segment in 0 until 48) {
                    val a0 = segment * PI / 24
                    val a1 = (segment + 1) * PI / 24
                    coneVertex(t0, a0)
                    coneVertex(t0, a1)
                    coneVertex(t1, a1)
                    coneVertex(t1, a0)
                }
            }
            for (ring in 0..4) {
                val t = ((time * 0.018 + ring / 5.0) % 1.0)
                var previous: Vec3? = null
                val ribbon = RibbonLine(vc, pose)
                for (i in 0..32) {
                    val angle = i * PI / 16
                    val point = conePoint(t, angle)
                    previous?.let { line(ribbon, it, point, 0.012f, (sin(t * PI) * 0.55).toFloat()) }
                    previous = point
                }
                ribbon.close()
            }
            buffers.endBatch(ClockworkRenderTypes.softEnergyType())
        }
        for ((be, state) in visible) {
            val ship = level.shipObjectWorld.loadedShips.getById(be.heldShip) ?: continue
            GravitronSurfaceEffects.render(level, ship, ClampotronBlockEntity.vec(be.anchor), GravitronAction.HOLD,
                (level.gameTime - state.birth + partialTick) * 1.3f, ms, buffers, camera)
        }
        SurfaceEffectPass.endBatch(buffers)
    }

    private val tips = listOf(Vector3d(3.0 / 16, 1.0, 3.0 / 16), Vector3d(13.0 / 16, 1.0, 3.0 / 16),
        Vector3d(13.0 / 16, 1.0, 13.0 / 16), Vector3d(3.0 / 16, 1.0, 13.0 / 16))

    private fun position(be: ClampotronBlockEntity, local: Vector3d): Vec3 {
        val facing = be.facing
        Quaterniond().rotationTo(Vector3d(0.0, 1.0, 0.0), Vector3d(facing.stepX.toDouble(), facing.stepY.toDouble(), facing.stepZ.toDouble()))
            .transform(local.sub(0.5, 0.5, 0.5)).add(be.blockPos.x + 0.5, be.blockPos.y + 0.5, be.blockPos.z + 0.5)
        val carrier = be.level?.getShipManagingPos(be.blockPos)
        if (carrier is ClientShip) carrier.renderTransform.shipToWorld.transformPosition(local)
        return ClampotronBlockEntity.vec(local)
    }

    private class FieldSound(val be: ClampotronBlockEntity, val overload: Boolean) : AbstractTickableSoundInstance(
        (if (overload) ClockworkSounds.CLAMPOTRON_OVERLOAD else ClockworkSounds.CLAMPOTRON_HOLD).mainEvent!!,
        SoundSource.BLOCKS, RandomSource.create()) {
        init { looping = true; delay = 0; volume = 0f; updatePosition() }
        override fun canStartSilent() = true
        private fun updatePosition() {
            val p = position(be, Vector3d(0.5, 0.875, 0.5))
            x = p.x; y = p.y; z = p.z
        }
        override fun tick() {
            val world = Minecraft.getInstance().level
            if (world == null || be.level !== world || be.isRemoved || !world.hasChunkAt(be.blockPos)) { stop(); return }
            val holding = be.enabled && be.heldShip >= 0 && !be.isOverStressed && be.getSpeed() != 0f
            val target = if (!holding) 0f else if (overload) LinkStressWarning.volume(be.strain) else 0.45f
            volume += (target - volume) * 0.25f
            updatePosition()
            if (target == 0f && volume < 0.005f) stop()
        }
    }
}
