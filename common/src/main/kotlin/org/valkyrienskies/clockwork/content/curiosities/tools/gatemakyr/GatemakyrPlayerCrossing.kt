package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.player.LocalPlayer
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPackets
import java.util.UUID
import kotlin.math.abs

object GatemakyrPlayerCrossing {
    private var world: ClientLevel? = null
    private var renderPartialTick = -1f
    private var previousEye: Vec3? = null
    private var previous = emptyMap<Pair<UUID, Int>, GatemakyrPlayerGate>()

    private var tickGates = emptyMap<Pair<UUID, Int>, GatemakyrPlayerGate>()
    private var lastTickGates = emptyMap<Pair<UUID, Int>, GatemakyrPlayerGate>()

    private var roll = 0f
    private var rollStart = 0.0

    private fun rollAt(time: Double): Float {
        val t = ((time - rollStart) / 10.0).coerceIn(0.0, 1.0)
        return (roll * (1.0 - t * t * (3.0 - 2.0 * t))).toFloat()
    }

    @JvmStatic fun applyCameraRoll(pose: PoseStack, pt: Float) {
        if (roll == 0f) return
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val camera = mc.gameRenderer.mainCamera
        if (camera.entity !== player || player.level() !== world) return
        val value = rollAt(player.level().gameTime + pt.toDouble())
        if (value == 0f) {
            roll = 0f
            return
        }
        val angle = if (mc.options.cameraType.isMirrored) -value else value
        pose.mulPose(Axis.ZP.rotationDegrees(angle))
        val rotation = camera.rotation().rotateZ(Math.toRadians(angle.toDouble()).toFloat())
        rotation.transformPositiveX(camera.leftVector)
        rotation.transformPositiveY(camera.upVector)
    }

    @JvmStatic fun finishTick() {
        update(1f)
        lastTickGates = tickGates
        tickGates = Minecraft.getInstance().level?.let { GatemakyrPlayerGate.snapshot(it, GatemakyrClient.rifts, false) { rift -> rift.packet } }.orEmpty()
    }

    @JvmStatic fun clear() {
        roll = 0f
        world = null
        previousEye = null
        previous = emptyMap()
        tickGates = emptyMap()
        lastTickGates = emptyMap()
    }

    @JvmStatic fun beginRender(partialTick: Float) {
        renderPartialTick = partialTick
    }

    @JvmStatic fun endRender() {
        renderPartialTick = -1f
    }

    @JvmStatic fun beforeRenderingEntities() {
        val pt = renderPartialTick
        if (pt < 0f || !RenderSystem.isOnRenderThread()) return
        endRender()
        update(pt)
    }

    private fun update(partialTick: Float) {
        update(partialTick, 0, null)
    }

    private fun update(partialTick: Float, hops: Int, departed: Pair<UUID, Int>?) {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return
        val level = mc.level ?: return
        if (GatemakyrClient.rifts.size < 2 || !GatemakyrPlayerGate.eligible(player)) {
            clear()
            return
        }
        val gates = GatemakyrPlayerGate.snapshot(level, GatemakyrClient.rifts, true) { it.packet }
        val eye = player.getEyePosition(partialTick)
        val from = previousEye
        val old = previous
        previousEye = eye
        previous = gates
        if (world !== level || from == null) {
            world = level
            return
        }
        var first: GatemakyrPlayerGate? = null
        var nearest = Double.POSITIVE_INFINITY
        for ((key, gate) in gates) {
            if (key == departed) continue
            val last = old[key]?.takeIf(gate::matches) ?: continue
            val fraction = gate.crossing(last, from, eye) ?: continue
            if (fraction < nearest) {
                nearest = fraction
                first = gate
            }
        }
        val gate = first ?: return
        val passage = gate.passage
        val key = gate.packet.owner to gate.packet.side
        val current = tickGates[key]?.takeIf(gate::matches)?.passage ?: passage
        val last = lastTickGates[key]?.takeIf(gate::matches)?.passage ?: current
        val oldEye = last.point(player.getEyePosition(0f))
        val currentEye = current.point(player.eyePosition)
        val sampledEye = passage.point(eye)
        val adjustment = sampledEye.subtract(oldEye.lerp(currentEye, partialTick.toDouble()))
        val offset = player.position().subtract(player.eyePosition)
        val entranceContact = gate.crossingPoint(old.getValue(key), from, eye, nearest)
        val contact = passage.point(entranceContact)
        val arrival = GatemakyrSurfaceCollision.exitAnchor(player, contact, currentEye.add(adjustment), offset, gate) ?: return
        val feet = arrival.end.add(offset)
        val oldFeet = oldEye.add(adjustment).add(offset).add(arrival.contact.subtract(contact))
        val velocity = current.velocity(player.deltaMovement, entranceContact, last)
        player.connection.send(ServerboundMovePlayerPacket.PosRot(player.x, player.y, player.z, player.yRot, player.xRot, player.onGround()))
        ClockworkPackets.sendToServer(GatemakyrCrossingPacket(level.dimension().location(), gate.packet.owner, gate.packet.side,
            gate.packet.startedAt, gate.partner.startedAt, feet))
        rotate(player, passage, level.gameTime + partialTick.toDouble())
        player.setPos(feet)
        player.xo = oldFeet.x
        player.yo = oldFeet.y
        player.zo = oldFeet.z
        player.xOld = oldFeet.x
        player.yOld = oldFeet.y
        player.zOld = oldFeet.z
        player.deltaMovement = velocity
        player.fallDistance = 0f
        gate.refreshSupport(player)
        previousEye = arrival.contact
        if (hops < 7) update(partialTick, hops + 1, gate.partner.owner to gate.partner.side)
    }

    private fun rotate(player: LocalPlayer, passage: GatemakyrPassage, time: Double) {
        val rotation = passage.angles(player.xRot, player.yRot)
        roll = Mth.wrapDegrees(rollAt(time) + passage.cameraRoll(player.yRot, rotation))
        if (abs(roll) < 1e-4f) roll = 0f
        rollStart = time
        val old = passage.angles(player.xRotO, player.yRotO)
        val bob = passage.angles(player.xBob, player.yBob)
        val oldBob = passage.angles(player.xBobO, player.yBobO)
        player.yRot = rotation.y
        player.yRotO = rotation.y - Mth.wrapDegrees(rotation.y - old.y)
        player.xRot = rotation.x
        player.xRotO = old.x
        player.yBob = rotation.y + Mth.wrapDegrees(bob.y - rotation.y)
        player.yBobO = player.yBob - Mth.wrapDegrees(bob.y - oldBob.y)
        player.xBob = bob.x
        player.xBobO = oldBob.x
        player.yHeadRot = player.yRot
        player.yHeadRotO = player.yRotO
        player.yBodyRot = player.yRot
        player.yBodyRotO = player.yRotO
    }
}
