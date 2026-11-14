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
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautForceCenter
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautForceReadout
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautGogglesItem
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautGogglesState
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautShipReadoutPacket
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautShipRequestPacket
import org.valkyrienskies.clockwork.content.curiosities.magnet.MagneticFieldView
import org.valkyrienskies.clockwork.ClockworkItems
import com.simibubi.create.content.equipment.goggles.GoggleOverlayRenderer
import com.simibubi.create.foundation.gui.RemovedGuiUtils
import com.simibubi.create.foundation.utility.CreateLang
import com.simibubi.create.infrastructure.config.AllConfigs
import net.createmod.catnip.gui.element.BoxElement
import net.createmod.catnip.gui.element.GuiGameElement
import net.createmod.catnip.theme.Color
import net.minecraft.ChatFormatting
import org.valkyrienskies.clockwork.util.render.ShaderPackCompat
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.shipObjectWorld
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

object AeronautGoggleOverlayRenderer {
    private const val GOLD = 0xFFFFCC71.toInt()
    private const val CYAN = 0xFF65DCE8.toInt()
    private const val GREEN = 0xFF9AE6A1.toInt()
    private var level: ClientLevel? = null
    private var target: ClientShip? = null
    private val matrix = Matrix4f()
    private val cameraPosition = Vector3d()
    private var capturedAt = 0L
    private var requestAt = Long.MIN_VALUE
    private var receivedAt = 0L
    private var readout: AeronautShipReadoutPacket? = null

    fun enabled(): Boolean {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return false
        return mc.level != null && mc.screen == null && !mc.options.hideGui && mc.options.cameraType.isFirstPerson &&
            player.isAlive && !player.isSpectator && player.getItemBySlot(EquipmentSlot.HEAD).item is AeronautGogglesItem &&
            AeronautGogglesState.getGogglesAreDown(player)
    }

    fun stressColor(stress: Float): Int {
        if (!stress.isFinite() || stress <= 0f || !enabled() || ShaderPackCompat.shadowPass()) return 0xFFFFFF
        val intensity = sqrt(stress.coerceIn(0f, 1f))
        val channel = (255 - 215 * intensity).roundToInt()
        return 0xFF0000 or (channel shl 8) or channel
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
        if (!enabled()) return
        val mc = Minecraft.getInstance()
        val width = mc.window.guiScaledWidth
        val height = mc.window.guiScaledHeight
        val tooltip = ArrayList<Component>()
        val magnetic = MagneticFieldView.tooltip()
        val ship = target
        if (ship != null && System.nanoTime() - capturedAt <= 250_000_000L) {
            val snapshot = readout?.takeIf { System.nanoTime() - receivedAt < 1_000_000_000L }?.snapshot
            fun point(position: Vector3dc) = AeronautHudMath.project(
                ship.renderTransform.shipToWorld.transformPosition(position, Vector3d()).sub(cameraPosition), matrix, width, height)
            fun hovered(p: AeronautHudMath.Point) = abs(p.x - width / 2f) <= 9f && abs(p.y - height / 2f) <= 9f
            fun row(key: String, value: String, color: ChatFormatting = ChatFormatting.GRAY) {
                CreateLang.builder().add(text(key, value)).style(color).forGoggles(tooltip, 1)
            }
            graphics.pose().pushPose()
            try {
                graphics.pose().translate(0.0, 0.0, 200.0)
                point(snapshot?.massCenter ?: ship.renderTransform.positionInShip)?.let { p ->
                    marker(graphics, p, GOLD, false, hovered(p))
                    if (hovered(p)) {
                        CreateLang.builder().add(text("mass_center")).style(ChatFormatting.GOLD).forGoggles(tooltip)
                        row("mass", snapshot?.mass?.let { if (it >= 1000) format(it / 1000) + " t" else format(it) + " kg" } ?: "…")
                        row("speed", format(ship.velocity.length()) + " m/s")
                        if (mc.player?.isShiftKeyDown == true)
                            row("velocity", "${format(ship.velocity.x())}, ${format(ship.velocity.y())}, ${format(ship.velocity.z())} m/s")
                    }
                }
                fun force(center: AeronautForceCenter?, color: Int, key: String, potential: Boolean) {
                    center ?: return
                    val p = point(center.position) ?: return
                    marker(graphics, p, color, potential, hovered(p))
                    if (!potential && center.force.lengthSquared() > 1e-8) {
                        point(Vector3d(center.position).add(Vector3d(center.force).normalize().mul(2.0)))?.let { arrow(graphics, p, it, color) }
                    }
                    if (hovered(p)) {
                        if (tooltip.isNotEmpty()) tooltip.add(Component.empty())
                        CreateLang.builder().add(text(key)).style(if (potential) ChatFormatting.GRAY else ChatFormatting.AQUA).forGoggles(tooltip)
                        row("force", format(center.force.length() / 1000.0) + " kN")
                        if (potential) CreateLang.builder().add(text("design_reference")).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1)
                    }
                }
                if (snapshot != null) {
                    force(snapshot.potentialThrust, CYAN, "potential_thrust", true)
                    force(snapshot.potentialLift, GREEN, "potential_lift", true)
                    force(snapshot.liveThrust, CYAN, "live_thrust", false)
                    force(snapshot.liveLift, GREEN, "live_lift", false)
                }
            } finally { graphics.pose().popPose() }
        }
        if (magnetic.isNotEmpty()) renderTooltip(graphics, magnetic)
        else if (tooltip.isNotEmpty() && GoggleOverlayRenderer.hoverTicks == 0) renderTooltip(graphics, tooltip)
    }

    private fun renderTooltip(graphics: GuiGraphics, tooltip: List<Component>) {
        val mc = Minecraft.getInstance()
        val width = mc.window.guiScaledWidth
        val height = mc.window.guiScaledHeight
        val config = AllConfigs.client()
        val textWidth = tooltip.maxOf { mc.font.width(it) }
        val x = (width / 2 + config.overlayOffsetX.get()).coerceAtMost(width - textWidth - 20).coerceAtLeast(4)
        val y = (height / 2 + config.overlayOffsetY.get()).coerceAtMost(height - tooltip.size * 10 - 20).coerceAtLeast(20)
        val custom = config.overlayCustomColor.get()
        val background = if (custom) Color(config.overlayBackgroundColor.get()) else BoxElement.COLOR_VANILLA_BACKGROUND.copy().scaleAlpha(0.75f)
        val top = if (custom) Color(config.overlayBorderColorTop.get()) else BoxElement.COLOR_VANILLA_BORDER.getFirst().copy()
        val bottom = if (custom) Color(config.overlayBorderColorBot.get()) else BoxElement.COLOR_VANILLA_BORDER.getSecond().copy()
        graphics.pose().pushPose()
        try {
            GuiGameElement.of(ClockworkItems.AERONAUT_GOGGLES.asStack())
                .at<GuiGameElement.GuiRenderBuilder>((x + 10).toFloat(), (y - 16).toFloat(), 450f).render(graphics)
            RemovedGuiUtils.drawHoveringText(graphics, tooltip, x, y, width, height, -1,
                background.getRGB(), top.getRGB(), bottom.getRGB(), mc.font)
        } finally { graphics.pose().popPose() }
    }

    private fun marker(graphics: GuiGraphics, point: AeronautHudMath.Point, color: Int, hollow: Boolean, hovered: Boolean) {
        val x = point.x.roundToInt(); val y = point.y.roundToInt()
        val radius = if (hollow) 7 else if (hovered) 5 else 3
        line(graphics, x - radius, y - radius, x + radius, y - radius, color)
        line(graphics, x + radius, y - radius, x + radius, y + radius, color)
        line(graphics, x + radius, y + radius, x - radius, y + radius, color)
        line(graphics, x - radius, y + radius, x - radius, y - radius, color)
        if (!hollow) graphics.fill(x - 1, y - 1, x + 2, y + 2, color)
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
