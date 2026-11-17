package org.valkyrienskies.clockwork.client.render.goggles

import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.MultiBufferSource
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautAssemblyReadout
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautForceCenter
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautForceReadout
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautGogglesItem
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautGogglesState
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautShipReadoutPacket
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautShipReadout
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautShipRequestPacket
import org.valkyrienskies.clockwork.content.curiosities.aeronaut.AeronautSupportGeometry
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
    private const val SUPPORT = 0xFF6BC6A4.toInt()
    private const val JOINT = 0xFFB9A5FB.toInt()
    private const val OUTSIDE = 0xFFF38C80.toInt()
    private var level: ClientLevel? = null
    private var target: ClientShip? = null
    private val matrix = Matrix4f()
    private val cameraPosition = Vector3d()
    private var capturedAt = 0L
    private var lastShipHitAt = 0L
    private var requestAt = Long.MIN_VALUE
    private var receivedAt = 0L
    private var readout: AeronautShipReadoutPacket? = null
    private var visibleReadout: AeronautShipReadout.Snapshot? = null
    private val supportRim = AeronautSupportRim()

    fun enabled(): Boolean {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return false
        return mc.level != null && mc.screen == null && !mc.options.hideGui && mc.options.cameraType.isFirstPerson &&
            player.isAlive && !player.isSpectator && player.getItemBySlot(EquipmentSlot.HEAD).item is AeronautGogglesItem &&
            AeronautGogglesState.getGogglesAreDown(player)
    }

    fun stressBuffers(source: MultiBufferSource, stress: Float): MultiBufferSource {
        val tint = stressColor(stress)
        if (tint == 0xFFFFFF) return source
        return MultiBufferSource { type -> StressConsumer(source.getBuffer(type), tint) }
    }

    private class StressConsumer(private val target: VertexConsumer, private val tint: Int) : VertexConsumer {
        override fun vertex(x: Double, y: Double, z: Double): VertexConsumer = apply { target.vertex(x, y, z) }
        override fun color(r: Int, g: Int, b: Int, a: Int): VertexConsumer = apply {
            target.color(r * (tint shr 16 and 255) / 255, g * (tint shr 8 and 255) / 255, b * (tint and 255) / 255, a)
        }
        override fun uv(u: Float, v: Float): VertexConsumer = apply { target.uv(u, v) }
        override fun overlayCoords(u: Int, v: Int): VertexConsumer = apply { target.overlayCoords(u, v) }
        override fun uv2(u: Int, v: Int): VertexConsumer = apply { target.uv2(u, v) }
        override fun normal(x: Float, y: Float, z: Float): VertexConsumer = apply { target.normal(x, y, z) }
        override fun endVertex() = target.endVertex()
        override fun defaultColor(r: Int, g: Int, b: Int, a: Int) = target.defaultColor(
            r * (tint shr 16 and 255) / 255, g * (tint shr 8 and 255) / 255, b * (tint and 255) / 255, a)
        override fun unsetDefaultColor() = target.unsetDefaultColor()
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
        cameraPosition.set(camera.position.x, camera.position.y, camera.position.z)
        matrix.set(projection).mul(poseStack.last().pose())
        capturedAt = System.nanoTime()
    }

    private fun findTarget(world: ClientLevel, sight: AeronautShipReadout.TerrainSight, view: Vec3): ClientShip? {
        val eye = sight.eye
        val position = Vector3d(eye.x, eye.y, eye.z)
        val look = Vector3d(view.x, view.y, view.z)
        var nearest = AeronautForceReadout.MAX_RANGE
        var selected: ClientShip? = null
        for (ship in world.shipObjectWorld?.loadedShips ?: return null) {
            val bounds = ship.shipAABB ?: continue
            val inverse = ship.renderTransform.worldToShip
            val origin = inverse.transformPosition(position, Vector3d())
            val direction = inverse.transformDirection(look, Vector3d())
            val entry = AeronautHudMath.rayEntry(origin, direction,
                Vector3d(bounds.minX().toDouble(), bounds.minY().toDouble(), bounds.minZ().toDouble()),
                Vector3d(bounds.maxX().toDouble(), bounds.maxY().toDouble(), bounds.maxZ().toDouble()), nearest) ?: continue
            if (selected == null || entry < nearest) { nearest = entry; selected = ship }
        }
        return selected?.takeIf { sight.sees(eye.add(view.scale(nearest))) }
    }

    private fun aimedAtReadout(world: ClientLevel, ship: ClientShip, now: Long, allowGrace: Boolean): Boolean {
        val snapshot = visibleReadout ?: return false
        if (world.shipObjectWorld?.loadedShips?.getById(ship.id) == null) return false
        if (allowGrace && now - lastShipHitAt < 600_000_000L) return true
        val window = Minecraft.getInstance().window
        val width = window.guiScaledWidth
        val height = window.guiScaledHeight
        fun aimedAt(position: Vector3dc): Boolean {
            val point = AeronautHudMath.project(Vector3d(position).sub(cameraPosition), matrix, width, height) ?: return false
            return abs(point.x - width / 2f) <= 9f && abs(point.y - height / 2f) <= 9f
        }
        val assembly = snapshot.assembly
        if (snapshot.massVisible) {
            val center = ship.renderTransform.shipToWorld.transformPosition(snapshot.massCenter, Vector3d())
            if (aimedAt(center)) return true
            val support = assembly.outline.firstOrNull()
            if (snapshot.visibleGuides and AeronautShipReadout.PROJECTION != 0 && support != null &&
                aimedAt(Vector3d(center.x, support.y(), center.z))) return true
        }
        return assembly.feet.any { anchorPosition(world, it.anchor)?.let(::aimedAt) == true } ||
            assembly.contactFaces.any { modelPosition(world, it.shipId, it.center)?.let(::aimedAt) == true } ||
            assembly.joints.any { anchorPosition(world, it.anchor)?.let(::aimedAt) == true }
    }

    private fun anchorPosition(world: ClientLevel, anchor: AeronautAssemblyReadout.Anchor) =
        modelPosition(world, anchor.shipId, Vector3d(anchor.pos.x + 0.5, anchor.pos.y + 0.5, anchor.pos.z + 0.5))

    private fun modelPosition(world: ClientLevel, shipId: Long?, point: Vector3dc): Vector3dc? {
        if (shipId == null) return point
        val ship = world.shipObjectWorld?.loadedShips?.getById(shipId) ?: return null
        return ship.renderTransform.shipToWorld.transformPosition(point, Vector3d())
    }

    @JvmStatic
    fun tick() {
        if (!enabled()) { reset(); return }
        val mc = Minecraft.getInstance()
        val world = mc.level ?: return
        if (world !== level) { reset(); return }
        val player = mc.player ?: return
        val now = System.nanoTime()
        val view = player.lookAngle
        val sight = AeronautShipReadout.TerrainSight(world, player.eyePosition, AeronautShipReadout.CLIENT_RAYS)
        var selected = findTarget(world, sight, view)
        val previous = target
        visibleReadout = readout?.takeIf { now - receivedAt < 1_000_000_000L }?.let { packet ->
            AeronautShipReadout.filterVisible(packet.snapshot, sight, view, packet.shipId) { body, point ->
                modelPosition(world, body, point)
            }
        }
        if (selected != null) lastShipHitAt = now
        if (previous != null && selected?.id != previous.id && aimedAtReadout(world, previous, now, selected == null))
            selected = previous
        if (previous?.id != selected?.id) {
            readout = null
            visibleReadout = null
            requestAt = Long.MIN_VALUE
        }
        target = selected
        supportRim.update(visibleReadout?.assembly?.outline.orEmpty(), now) { from, to ->
            AeronautSupportRim.terrainClear(world, sight.eye, from, to)
        }
        val ship = selected ?: return
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
        level = null; target = null; readout = null; visibleReadout = null
        supportRim.clear()
        requestAt = Long.MIN_VALUE; capturedAt = 0L; lastShipHitAt = 0L
    }

    @JvmStatic
    fun render(graphics: GuiGraphics) {
        if (!enabled()) return
        val mc = Minecraft.getInstance()
        val width = mc.window.guiScaledWidth
        val height = mc.window.guiScaledHeight
        val tooltip = ArrayList<Component>()
        val magnetic = MagneticFieldView.tooltip()
        var joint: AeronautAssemblyReadout.Joint? = null
        val ship = target
        val snapshot = visibleReadout
        if (ship != null && snapshot != null && System.nanoTime() - capturedAt <= 250_000_000L) {
            fun worldPoint(position: Vector3dc) = AeronautHudMath.project(Vector3d(position).sub(cameraPosition), matrix, width, height)
            fun point(position: Vector3dc) = worldPoint(ship.renderTransform.shipToWorld.transformPosition(position, Vector3d()))
            fun hovered(p: AeronautHudMath.Point) = abs(p.x - width / 2f) <= 9f && abs(p.y - height / 2f) <= 9f
            fun row(key: String, value: String, color: ChatFormatting = ChatFormatting.GRAY) {
                CreateLang.builder().add(text(key, value)).style(color).forGoggles(tooltip, 1)
            }
            graphics.pose().pushPose()
            try {
                graphics.pose().translate(0.0, 0.0, 200.0)
                val center = snapshot.massCenter.takeIf { snapshot.massVisible }
                    ?.let { ship.renderTransform.shipToWorld.transformPosition(it, Vector3d()) }
                val projection = center.takeIf { snapshot.visibleGuides and AeronautShipReadout.PROJECTION != 0 }
                joint = renderAssembly(graphics, snapshot.assembly, projection, width, height, tooltip, ::worldPoint)
                center?.let(::worldPoint)?.let { p ->
                    marker(graphics, p, GOLD, false, hovered(p))
                    if (hovered(p)) {
                        val bodies = snapshot.assembly.bodyCount
                        val label = if (bodies > 1) text("assembly_mass_center") else text("mass_center")
                        CreateLang.builder().add(label).style(ChatFormatting.GOLD).forGoggles(tooltip)
                        row("mass", if (snapshot.mass >= 1000) format(snapshot.mass / 1000) + " t" else format(snapshot.mass) + " kg")
                        row("speed", format(ship.velocity.length()) + " m/s")
                        if (mc.player?.isShiftKeyDown == true)
                            row("velocity", "${format(ship.velocity.x())}, ${format(ship.velocity.y())}, ${format(ship.velocity.z())} m/s")
                    }
                }
                fun force(center: AeronautForceCenter?, color: Int, key: String, potential: Boolean, guide: Int = 0) {
                    center ?: return
                    val p = point(center.position) ?: return
                    marker(graphics, p, color, potential, hovered(p))
                    if (snapshot.visibleGuides and guide != 0 && center.force.lengthSquared() > 1e-8) {
                        point(Vector3d(center.position).add(Vector3d(center.force).normalize().mul(2.0)))?.let { arrow(graphics, p, it, color) }
                    }
                    if (hovered(p)) {
                        if (tooltip.isNotEmpty()) tooltip.add(Component.empty())
                        CreateLang.builder().add(text(key)).style(if (potential) ChatFormatting.GRAY else ChatFormatting.AQUA).forGoggles(tooltip)
                        row("force", format(center.force.length() / 1000.0) + " kN")
                        if (potential) CreateLang.builder().add(text("design_reference")).style(ChatFormatting.DARK_GRAY).forGoggles(tooltip, 1)
                    }
                }
                force(snapshot.potentialThrust, CYAN, "potential_thrust", true)
                force(snapshot.potentialLift, GREEN, "potential_lift", true)
                force(snapshot.liveThrust, CYAN, "live_thrust", false, AeronautShipReadout.THRUST_ARROW)
                force(snapshot.liveLift, GREEN, "live_lift", false, AeronautShipReadout.LIFT_ARROW)
            } finally { graphics.pose().popPose() }
        }
        if (magnetic.isNotEmpty()) renderTooltip(graphics, magnetic)
        else if (tooltip.isNotEmpty() && GoggleOverlayRenderer.hoverTicks == 0) renderTooltip(graphics, tooltip)
        joint?.let { renderTooltip(graphics, jointTooltip(it), jointInspection = true) }
    }

    private fun renderAssembly(graphics: GuiGraphics, assembly: AeronautAssemblyReadout.Detail, center: Vector3dc?,
                               width: Int, height: Int, tooltip: MutableList<Component>,
                               project: (Vector3dc) -> AeronautHudMath.Point?): AeronautAssemblyReadout.Joint? {
        val mc = Minecraft.getInstance()
        val world = mc.level ?: return null
        fun locate(anchor: AeronautAssemblyReadout.Anchor) = anchorPosition(world, anchor)?.let(project)
        fun distance(point: AeronautHudMath.Point): Float =
            maxOf(abs(point.x - width / 2f), abs(point.y - height / 2f))
        fun lineBetween(a: AeronautHudMath.Point?, b: AeronautHudMath.Point?, color: Int) {
            if (a != null && b != null) line(graphics, a.x.roundToInt(), a.y.roundToInt(), b.x.roundToInt(), b.y.roundToInt(), color)
        }

        val outline = assembly.outline
        if (outline.isNotEmpty()) {
            if (outline.size == 1) project(outline[0])?.let { marker(graphics, it, SUPPORT, false, false) }
            else supportRim.render(graphics, System.nanoTime(), project)
            if (center != null) {
                val projection = Vector3d(center.x(), outline[0].y(), center.z())
                project(projection)?.let { point ->
                    val inside = AeronautSupportGeometry.contains(outline, projection)
                    val color = if (outline.size < 3) GOLD else if (inside) SUPPORT else OUTSIDE
                    val x = point.x.roundToInt()
                    val y = point.y.roundToInt()
                    lineBetween(project(center), point, 0x80FFCC71.toInt())
                    line(graphics, x - 4, y, x + 4, y, color)
                    line(graphics, x, y - 4, x, y + 4, color)
                    if (distance(point) <= 7f) {
                        CreateLang.builder().add(text("mass_projection")).style(ChatFormatting.GOLD).forGoggles(tooltip)
                        if (outline.size >= 3) CreateLang.builder().add(text(if (inside) "inside_support" else "outside_support"))
                            .style(if (inside) ChatFormatting.GREEN else ChatFormatting.RED).forGoggles(tooltip, 1)
                    }
                }
            }
        }

        var load: Double? = null
        var loadLabel = "loaded_foot"
        var loadDistance = 8f
        for (surface in assembly.contactFaces) {
            val point = modelPosition(world, surface.shipId, surface.center)?.let(project) ?: continue
            val distance = distance(point)
            marker(graphics, point, SUPPORT, true, distance <= 8f, small = true)
            if (distance <= loadDistance) {
                load = surface.load
                loadLabel = "loaded_surface"
                loadDistance = distance
            }
        }
        for (loaded in assembly.feet) {
            val point = locate(loaded.anchor) ?: continue
            val distance = distance(point)
            marker(graphics, point, SUPPORT, false, distance <= 8f)
            if (distance <= loadDistance) {
                load = loaded.load
                loadLabel = "loaded_foot"
                loadDistance = distance
            }
        }
        load?.let {
            CreateLang.builder().add(text(loadLabel)).style(ChatFormatting.GREEN).forGoggles(tooltip)
            CreateLang.builder().add(text("foot_load", formatLoad(it))).style(ChatFormatting.GRAY).forGoggles(tooltip, 1)
        }

        val hit = (mc.hitResult as? BlockHitResult)?.takeIf { it.type == HitResult.Type.BLOCK }?.blockPos
        var selected = assembly.joints.firstOrNull { it.anchor.pos == hit }
        var nearest = if (selected == null) 8f else -1f
        for (reading in assembly.joints) {
            val point = locate(reading.anchor) ?: continue
            val distance = distance(point)
            if (distance <= nearest) {
                selected = reading
                nearest = distance
            }
            marker(graphics, point, JOINT, true, distance <= 8f || reading.anchor.pos == hit, small = true)
        }
        return selected
    }

    private fun jointTooltip(joint: AeronautAssemblyReadout.Joint): List<Component> {
        fun value(number: Double) = format(if (abs(number) < 0.05) 0.0 else number) + if (joint.kind.angular) "°" else " m"
        return buildList {
            add(Component.translatable("block.vs_clockwork.${joint.kind.blockId}").withStyle(ChatFormatting.LIGHT_PURPLE))
            if (joint.target != null) add(text("joint_comparison", value(joint.measured), value(joint.target)))
            else add(text("joint_measured", value(joint.measured)))
            if (joint.lower != null && joint.upper != null) add(text("joint_limits", value(joint.lower), value(joint.upper)))
            joint.error?.let { add(text("joint_error", value(it))) }
        }
    }

    private fun formatLoad(newtons: Double) = if (newtons >= 1000.0) "${format(newtons / 1000.0)} kN" else "${format(newtons)} N"

    private fun renderTooltip(graphics: GuiGraphics, tooltip: List<Component>, jointInspection: Boolean = false) {
        val mc = Minecraft.getInstance()
        val width = mc.window.guiScaledWidth
        val height = mc.window.guiScaledHeight
        val config = AllConfigs.client()
        val textWidth = tooltip.maxOf { mc.font.width(it) }
        val anchorX = if (jointInspection) 12 else width / 2 + config.overlayOffsetX.get()
        val anchorY = if (jointInspection) height / 2 else height / 2 + config.overlayOffsetY.get()
        val x = anchorX.coerceAtMost(width - textWidth - 20).coerceAtLeast(4)
        val y = anchorY.coerceAtMost(height - tooltip.size * 10 - 20).coerceAtLeast(20)
        val custom = config.overlayCustomColor.get()
        val background = if (custom) Color(config.overlayBackgroundColor.get()) else BoxElement.COLOR_VANILLA_BACKGROUND.copy().scaleAlpha(0.75f)
        val top = if (custom) Color(config.overlayBorderColorTop.get()) else BoxElement.COLOR_VANILLA_BORDER.getFirst().copy()
        val bottom = if (custom) Color(config.overlayBorderColorBot.get()) else BoxElement.COLOR_VANILLA_BORDER.getSecond().copy()
        graphics.pose().pushPose()
        try {
            if (!jointInspection) GuiGameElement.of(ClockworkItems.AERONAUT_GOGGLES.asStack())
                .at<GuiGameElement.GuiRenderBuilder>((x + 10).toFloat(), (y - 16).toFloat(), 450f).render(graphics)
            RemovedGuiUtils.drawHoveringText(graphics, tooltip, x, y, width, height, -1,
                background.getRGB(), top.getRGB(), bottom.getRGB(), mc.font)
        } finally { graphics.pose().popPose() }
    }

    private fun marker(graphics: GuiGraphics, point: AeronautHudMath.Point, color: Int, hollow: Boolean, hovered: Boolean, small: Boolean = false) {
        val x = point.x.roundToInt(); val y = point.y.roundToInt()
        val radius = if (hollow && !small) 7 else if (hovered) 5 else 3
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
