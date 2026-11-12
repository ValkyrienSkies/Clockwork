package org.valkyrienskies.clockwork.content.schematics

import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.widget.IconButton
import net.minecraft.Util
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.pow

class VdexBlueprintScreen : Screen(Component.translatable("gui.vs_clockwork.blueprint.title")) {
    private var files = emptyList<Path>()
    private var offset = 0
    private var selected: Path? = null
    private var draggingScroll = false
    private var draggingPreview = false
    private val coordinates = ArrayList<EditBox>()
    private val rows = ArrayList<Button>()
    private val preview = VdexSketchRenderer()
    private var apply: IconButton? = null
    private var paste: IconButton? = null
    private val panelWidth get() = (width - 28).coerceIn(308, 480)
    private val panelHeight get() = (height - 20).coerceIn(221, 340)
    private val left get() = (width - panelWidth) / 2
    private val top get() = (height - panelHeight) / 2
    private val rowCount get() = (panelHeight - 85) / 20
    private val previewX get() = left + 121
    private val previewY get() = top + 29
    private val previewWidth get() = panelWidth - 127
    private val previewHeight get() = panelHeight - 65
    private fun label(key: String) = Component.translatable("gui.vs_clockwork.blueprint.$key")
    private fun currentId() = minecraft?.player?.mainHandItem?.tag?.getString("VdexFile").orEmpty()

    override fun init() {
        val oldValues = coordinates.map { it.value }
        coordinates.clear(); rows.clear()
        refreshFiles()
        fun icon(x: Int, y: Int, image: AllIcons, text: Component, action: () -> Unit): IconButton {
            val button = IconButton(left + x, top + y, image)
            button.setTooltip(Tooltip.create(text))
            button.withCallback<IconButton>(Runnable(action))
            return addRenderableWidget(button)
        }
        icon(6, 21, AllIcons.I_OPEN_FOLDER, label("folder")) { Util.getPlatform().openFile(VdexBlueprintClient.directory().toFile()) }
        icon(28, 21, AllIcons.I_REFRESH, label("refresh")) { refreshFiles(); updateRows() }
        paste = if (minecraft?.player?.isCreative == true) icon(52, 21, AllIcons.I_SCHEMATIC, label("paste")) {
            if (deploy()) ClockworkPackets.sendToServer(VdexBlueprintActionPacket(VdexBlueprintActionPacket.Action.PASTE, currentId()))
        } else null
        repeat(rowCount) { index ->
            val row = object : Button(left + 14, top + 49 + index * 20, 101, 18, Component.empty(), {
                files.getOrNull(offset + index)?.let { selected = it; preview.reset(); VdexBlueprintClient.import(it); updateRows() }
            }, { narration -> narration.get() }) {
                override fun renderWidget(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                    skin(g, x, y, 101, 18, 309, 0, 101, 18)
                    if (files.getOrNull(offset + index) == selected) g.fill(x + 1, y + 1, x + 3, y + 17, 0xff8ed9ee.toInt())
                    if (isHoveredOrFocused) g.fill(x + 3, y + 2, x + 99, y + 16, 0x224ac5e6)
                    g.drawString(font, font.plainSubstrByWidth(message.string, 91), x + 6, y + 5, 0xfff3e6ce.toInt(), false)
                }
            }
            rows.add(addRenderableWidget(row))
        }
        updateRows()
        val tag = minecraft?.player?.mainHandItem?.tag
        val pos = minecraft?.player?.position() ?: Vec3.ZERO
        val values = if (tag?.getBoolean("Deployed") == true) listOf(tag.getDouble("VdexX"), tag.getDouble("VdexY"), tag.getDouble("VdexZ"), tag.getFloat("VdexYaw").toDouble()) else listOf(pos.x, pos.y, pos.z, 0.0)
        val cell = (panelWidth - 42) / 4
        values.forEachIndexed { index, value ->
            coordinates.add(addRenderableWidget(EditBox(font, left + 17 + index * cell, top + panelHeight - 23, cell - 18, 16, Component.literal(listOf("X", "Y", "Z", "Yaw")[index])).also {
                it.setMaxLength(16)
                it.value = oldValues.getOrNull(index) ?: "%.2f".format(Locale.ROOT, value)
                it.setBordered(false); it.setTextColor(0xff392619.toInt())
                it.setResponder { text -> it.setTextColor(if (text.toDoubleOrNull()?.isFinite() == true) 0xff392619.toInt() else 0xffa32929.toInt()) }
            }))
        }
        apply = icon(panelWidth - 24, panelHeight - 24, AllIcons.I_CONFIRM, label("place")) { if (deploy()) onClose() }
    }

    private fun refreshFiles() {
        try {
            files = Files.list(VdexBlueprintClient.directory()).use { paths -> paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".vdex", true) }.sorted().toList() }
        } catch (e: Exception) { VdexBlueprintClient.notice(Component.literal(e.message ?: "Could not read blueprint folder")) }
        offset = offset.coerceIn(0, (files.size - rowCount).coerceAtLeast(0))
    }

    private fun updateRows() {
        rows.forEachIndexed { index, button ->
            val file = files.getOrNull(offset + index)
            button.visible = file != null
            button.message = Component.literal(file?.fileName?.toString()?.substringBeforeLast('.') ?: "")
            button.setTooltip(file?.let { Tooltip.create(Component.literal(it.fileName.toString())) })
        }
    }

    override fun tick() {
        super.tick()
        val valid = VdexBlueprintData.validId(currentId()) && coordinates.all { it.value.toDoubleOrNull()?.isFinite() == true }
        apply?.active = valid; paste?.active = valid
    }

    private fun deploy(): Boolean {
        val values = coordinates.map { it.value.toDoubleOrNull() }
        if (values.any { it == null || !it.isFinite() } || !VdexBlueprintData.validId(currentId())) return false
        ClockworkPackets.sendToServer(VdexBlueprintActionPacket(VdexBlueprintActionPacket.Action.DEPLOY, currentId(),
            Vec3(values[0]!!, values[1]!!, values[2]!!), values[3]!!.toFloat()))
        return true
    }

    override fun render(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(g)
        nine(g, left, top, panelWidth, panelHeight - 31, 0, 0, 308, 190, 120, 48, 5, 4)
        nine(g, left, top + panelHeight - 31, panelWidth - 30, 31, 0, 190, 278, 31, 3, 3, 1, 2)
        skin(g, left + panelWidth - 30, top + panelHeight - 31, 38, 31, 278, 190, 38, 31)
        g.drawString(font, title, left + 7, top + 4, 0xfff3e6ce.toInt(), false)
        g.drawString(font, font.plainSubstrByWidth(VdexBlueprintClient.status.string, 101), left + 14, top + 40, 0xffe8dcc6.toInt(), false)
        val thumb = if (files.size <= rowCount) 0 else ((panelHeight - 94) * offset.toFloat() / (files.size - rowCount)).roundToInt()
        skin(g, left + 3, top + 49 + thumb, 8, 9, 410, 0, 8, 9)
        val name = minecraft?.player?.mainHandItem?.tag?.getString("VdexName").orEmpty()
        val data = VdexBlueprintClient.get(currentId())
        g.enableScissor(previewX, previewY, previewX + previewWidth, previewY + previewHeight)
        if (data != null) {
            val progress = preview.render(g, data, previewX + 5, previewY + 12, previewWidth - 10, previewHeight - 25)
            if (progress < 1f) g.drawCenteredString(font, label("drawing"), previewX + previewWidth / 2, previewY + previewHeight / 2, 0x526875)
            g.drawString(font, font.plainSubstrByWidth(name, previewWidth - 14), previewX + 7, previewY + 5, 0xff394c57.toInt(), false)
        } else g.drawCenteredString(font, label(if (currentId().isEmpty()) "choose" else "loading"), previewX + previewWidth / 2, previewY + previewHeight / 2, 0x526875)
        g.disableScissor()
        val cell = (panelWidth - 42) / 4
        listOf("X", "Y", "Z", "°").forEachIndexed { index, text ->
            val x = left + 7 + index * cell
            g.drawString(font, text, x, top + panelHeight - 23, 0xff392619.toInt(), false)
            g.hLine(x + 10, x + cell - 9, top + panelHeight - 10, 0xff8c8c87.toInt())
        }
        super.render(g, mouseX, mouseY, partialTick)
        if (overPreview(mouseX.toDouble(), mouseY.toDouble()) && !draggingPreview)
            g.renderTooltip(font, label("view_controls"), mouseX, mouseY)
        if (mouseX in left + 5..left + 116 && mouseY in top + 40..top + 47 && VdexBlueprintClient.status.string.isNotEmpty())
            g.renderTooltip(font, VdexBlueprintClient.status, mouseX, mouseY)
    }

    private fun overPreview(x: Double, y: Double) = x >= previewX && x < previewX + previewWidth && y >= previewY && y < previewY + previewHeight

    override fun mouseClicked(x: Double, y: Double, button: Int): Boolean {
        if (overPreview(x, y)) {
            if (button == 0) draggingPreview = true
            if (button == 1) preview.reset()
            return true
        }
        if (button == 0 && x in (left + 3).toDouble()..(left + 11).toDouble() && y >= top + 48 && y < top + panelHeight - 34) {
            draggingScroll = true; scrollTo(y); return true
        }
        return super.mouseClicked(x, y, button)
    }

    private fun scrollTo(y: Double) {
        offset = (((y - top - 53) / (panelHeight - 94)) * (files.size - rowCount).coerceAtLeast(0)).roundToInt().coerceIn(0, (files.size - rowCount).coerceAtLeast(0))
        updateRows()
    }

    override fun mouseDragged(x: Double, y: Double, button: Int, dx: Double, dy: Double): Boolean {
        if (draggingScroll) { scrollTo(y); return true }
        if (draggingPreview) {
            preview.yaw = (preview.yaw + dx.toFloat() * .7f) % 360f
            preview.pitch = (preview.pitch + dy.toFloat() * .7f).coerceIn(-85f, 85f)
            return true
        }
        return super.mouseDragged(x, y, button, dx, dy)
    }

    override fun mouseReleased(x: Double, y: Double, button: Int): Boolean {
        draggingScroll = false; draggingPreview = false
        return super.mouseReleased(x, y, button)
    }

    override fun mouseScrolled(x: Double, y: Double, amount: Double): Boolean {
        if (overPreview(x, y)) { preview.zoom = (preview.zoom * 1.12.pow(amount).toFloat()).coerceIn(.5f, 4f); return true }
        if (x >= left && x < left + 117 && y >= top + 48 && y < top + panelHeight - 34) {
            offset = (offset - amount.toInt()).coerceIn(0, (files.size - rowCount).coerceAtLeast(0)); updateRows(); return true
        }
        return super.mouseScrolled(x, y, amount)
    }

    override fun removed() { preview.close(); super.removed() }
    override fun isPauseScreen() = false

    companion object {
        private val TEXTURE = ClockworkMod.asResource("textures/gui/vdex.png")
        private fun skin(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, u: Int, v: Int, sw: Int, sh: Int) =
            g.blit(TEXTURE, x, y, w, h, u.toFloat(), v.toFloat(), sw, sh, 512, 256)
        private fun nine(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int, u: Int, v: Int, sw: Int, sh: Int, l: Int, t: Int, r: Int, b: Int) {
            val xs = intArrayOf(0, l, sw - r, sw); val ys = intArrayOf(0, t, sh - b, sh)
            val dx = intArrayOf(0, l, w - r, w); val dy = intArrayOf(0, t, h - b, h)
            for (i in 0..2) for (j in 0..2) skin(g, x + dx[i], y + dy[j], dx[i + 1] - dx[i], dy[j + 1] - dy[j], u + xs[i], v + ys[j], xs[i + 1] - xs[i], ys[j + 1] - ys[j])
        }
    }
}
