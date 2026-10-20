package org.valkyrienskies.clockwork.client.render.goggles

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlot
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.*
import org.valkyrienskies.clockwork.util.render.ShaderPackCompat
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/** Project world anchors into the HUD: markers remain visible through the ship without touching world depth. */
object AeronautGoggleOverlayRenderer {
    private const val GOLD = 0xFFFFCC71.toInt()
    private const val CYAN = 0xFF65DCE8.toInt()
    private const val GREEN = 0xFF9AE6A1.toInt()
    private const val WHITE = 0xFFE3EAF0.toInt()
    private const val MUTED = 0xFFA2B3C1.toInt()
    private var level: ClientLevel? = null
    private var target: ClientShip? = null
    private val matrix = Matrix4f()
    private val cameraPosition = Vector3d()
    private var capturedAt = 0L
    private var requestAt = Long.MIN_VALUE
    private var receivedAt = 0L
    private var readout: AeronautShipReadoutPacket? = null

    private fun enabled(): Boolean {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return false
        return mc.level != null && mc.screen == null && !mc.options.hideGui && mc.options.cameraType.isFirstPerson &&
            player.isAlive && !player.isSpectator && player.getItemBySlot(EquipmentSlot.HEAD).item is AeronautGogglesItem &&
            AeronautGogglesState.getGogglesAreDown(player)
    }

    @JvmStatic
    fun capture(poseStack: PoseStack, projection: Matrix4f, camera: Camera) {
        if (ShaderPackCompat.shadowPass()) return
        val mc = Minecraft.getInstance()
        if (!enabled() || level !== mc.level) {
            reset()
            level = mc.level
            if (!enabled()) return
        }
        val world = mc.level ?: return
        cameraPosition.set(camera.position.x, camera.position.y, camera.position.z)
        matrix.set(projection).mul(poseStack.last().pose())
        val look = Vector3d(camera.lookVector)
        var nearest = AeronautForceReadout.MAX_RANGE
        var selected: ClientShip? = null
        for (ship in world.shipObjectWorld?.loadedShips ?: return) {
            val bounds = ship.shipAABB ?: continue
            val inverse = ship.renderTransform.worldToShip
            val origin = inverse.transformPosition(cameraPosition, Vector3d())
            val direction = inverse.transformDirection(look, Vector3d())
            val entry = AeronautHudMath.rayEntry(origin, direction,
                Vector3d(bounds.minX().toDouble(), bounds.minY().toDouble(), bounds.minZ().toDouble()),
                Vector3d(bounds.maxX() + 1.0, bounds.maxY() + 1.0, bounds.maxZ() + 1.0), nearest) ?: continue
            if (selected == null || entry < nearest) { nearest = entry; selected = ship }
        }
        if (target?.id != selected?.id) {
            readout = null
            requestAt = Long.MIN_VALUE
        }
        target = selected
        capturedAt = System.nanoTime()
    }

    @JvmStatic
    fun tick() {
        if (!enabled()) { reset(); return }
        val world = Minecraft.getInstance().level ?: return
        if (world !== level) { reset(); return }
        val ship = target ?: return
        if (requestAt == Long.MIN_VALUE || world.gameTime - requestAt >= AeronautForceReadout.UPDATE_TICKS) {
            ClockworkPackets.sendToServer(AeronautShipRequestPacket(ship.id))
            requestAt = world.gameTime
        }
    }

    fun accept(packet: AeronautShipReadoutPacket) {
        val world = Minecraft.getInstance().level ?: return
        if (!enabled() || world !== level || packet.dimension != world.dimension().location() || packet.shipId != target?.id) return
        readout = packet
        receivedAt = System.nanoTime()
    }

    private fun reset() {
        level = null; target = null; readout = null
        requestAt = Long.MIN_VALUE; capturedAt = 0L
    }

    @JvmStatic
    fun render(graphics: GuiGraphics) {
        if (!enabled() || System.nanoTime() - capturedAt > 250_000_000L) return
        val ship = target ?: return
        val mc = Minecraft.getInstance()
        val width = mc.window.guiScaledWidth
        val height = mc.window.guiScaledHeight
        val snapshot = readout?.takeIf { System.nanoTime() - receivedAt < 1_000_000_000L }?.snapshot
        fun point(position: Vector3dc): AeronautHudMath.Point? = AeronautHudMath.project(
            ship.renderTransform.shipToWorld.transformPosition(position, Vector3d()).sub(cameraPosition), matrix, width, height)
        val mass = point(snapshot?.massCenter ?: ship.renderTransform.positionInShip)
        val hovering = mass != null && abs(mass.x - width / 2f) <= 12f && abs(mass.y - height / 2f) <= 12f

        graphics.pose().pushPose()
        try {
            graphics.pose().translate(0.0, 0.0, 200.0)
            if (snapshot != null) {
                fun force(center: AeronautForceCenter?, color: Int, label: String, potential: Boolean) {
                    center ?: return
                    val p = point(center.position) ?: return
                    marker(graphics, p, color, label, potential, false)
                    if (!potential && center.force.lengthSquared() > 1e-8) {
                        val end = point(Vector3d(center.position).add(Vector3d(center.force).normalize().mul(3.0)))
                        if (end != null) arrow(graphics, p, end, color)
                    }
                }
                force(snapshot.potentialThrust, CYAN, "T", true)
                force(snapshot.potentialLift, GREEN, "L", true)
                force(snapshot.liveThrust, CYAN, "T", false)
                force(snapshot.liveLift, GREEN, "L", false)
            }
            if (mass != null) marker(graphics, mass, GOLD, "M", false, hovering)
            legend(graphics, snapshot != null)
            if (hovering && mass != null) {
                val name = ship.slug ?: "#${ship.id}"
                val velocity = ship.velocity
                val lines = listOf(
                    Component.literal(mc.font.plainSubstrByWidth(name, 210)),
                    text("mass", snapshot?.mass?.let { if (it >= 1000) format(it / 1000) + " t" else format(it) + " kg" } ?: "…"),
                    text("speed", format(velocity.length()) + " m/s"),
                    text("velocity", "${format(velocity.x())}, ${format(velocity.y())}, ${format(velocity.z())} m/s")
                )
                val panelWidth = (lines.maxOf { mc.font.width(it) } + 20).coerceAtMost(width - 12)
                val x = (mass.x.toInt() + 20).coerceIn(6, max(6, width - panelWidth - 6))
                val y = (mass.y.toInt() - 27).coerceIn(6, max(6, height - 70))
                graphics.fill(x, y, x + panelWidth, y + 59, 0xE615202B.toInt())
                graphics.fill(x, y, x + 2, y + 59, GOLD)
                lines.forEachIndexed { i, line -> graphics.drawString(mc.font,
                    mc.font.plainSubstrByWidth(line.string, panelWidth - 20), x + 10, y + 8 + i * 12, if (i == 0) GOLD else WHITE, false) }
            }
        } finally { graphics.pose().popPose() }
    }

    private fun legend(graphics: GuiGraphics, ready: Boolean) {
        val font = Minecraft.getInstance().font
        val x = 10
        graphics.drawString(font, text("title"), x, 10, GOLD, false)
        graphics.drawString(font, text("legend"), x, 23, WHITE, false)
        graphics.drawString(font, text(if (ready) "modes" else "loading"), x, 35, MUTED, false)
        graphics.drawString(font, text("reference"), x, 47, MUTED, false)
    }

    private fun marker(graphics: GuiGraphics, point: AeronautHudMath.Point, color: Int, label: String, hollow: Boolean, hovered: Boolean) {
        val x = point.x.roundToInt(); val y = point.y.roundToInt()
        val radius = if (hollow) 8 else if (hovered) 6 else 4
        line(graphics, x, y - radius, x + radius, y, color)
        line(graphics, x + radius, y, x, y + radius, color)
        line(graphics, x, y + radius, x - radius, y, color)
        line(graphics, x - radius, y, x, y - radius, color)
        if (!hollow) graphics.fill(x - 2, y - 2, x + 3, y + 3, color)
        val font = Minecraft.getInstance().font
        val labelX = if (hollow) x - radius - font.width(label) - 4 else x + radius + 4
        // Keep all three labels readable when a balanced ship's centers coincide.
        val labelY = y + when (label) { "T" -> -15; "L" -> 7; else -> -4 }
        graphics.drawString(font, label, labelX, labelY, color, true)
    }

    private fun arrow(graphics: GuiGraphics, from: AeronautHudMath.Point, to: AeronautHudMath.Point, color: Int) {
        val dx = to.x - from.x; val dy = to.y - from.y
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        if (length < 4f) return
        val ux = dx / length; val uy = dy / length
        line(graphics, from.x.toInt(), from.y.toInt(), to.x.toInt(), to.y.toInt(), color)
        line(graphics, to.x.toInt(), to.y.toInt(), (to.x - ux * 6 - uy * 3).toInt(), (to.y - uy * 6 + ux * 3).toInt(), color)
        line(graphics, to.x.toInt(), to.y.toInt(), (to.x - ux * 6 + uy * 3).toInt(), (to.y - uy * 6 - ux * 3).toInt(), color)
    }

    private fun line(graphics: GuiGraphics, x0: Int, y0: Int, x1: Int, y1: Int, color: Int) {
        val dx = (x1 - x0).toDouble(); val dy = (y1 - y0).toDouble()
        graphics.pose().pushPose()
        graphics.pose().translate(x0.toDouble(), y0.toDouble(), 0.0)
        graphics.pose().mulPose(Quaternionf().rotationZ(kotlin.math.atan2(dy, dx).toFloat()))
        graphics.fill(0, 0, kotlin.math.hypot(dx, dy).roundToInt().coerceAtLeast(1), 1, color)
        graphics.pose().popPose()
    }

    private fun format(value: Double) = String.format(Locale.ROOT, "%.1f", value)
    private fun text(key: String, vararg arguments: Any) = Component.translatable("vs_clockwork.aeronaut.$key", *arguments)
}
