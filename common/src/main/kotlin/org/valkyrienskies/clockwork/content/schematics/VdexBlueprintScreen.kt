package org.valkyrienskies.clockwork.content.schematics

import net.minecraft.Util
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkPackets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

class VdexBlueprintScreen : Screen(Component.translatable("gui.vs_clockwork.blueprint.title")) {
    private var files = emptyList<Path>()
    private var page = 0
    private val coordinates = ArrayList<EditBox>()
    private val column get() = ((width - 50) / 2).coerceAtMost(200)
    private val left get() = width / 2 - column - 10
    private val right get() = width / 2 + 10
    private val rows get() = ((height - 122) / 22).coerceIn(3, 8)
    private val previewHeight get() = (height - 192).coerceIn(42, 110)
    private val fieldsTop get() = 58 + previewHeight
    private val actionsTop get() = fieldsTop + 49
    private fun label(key: String) = Component.translatable("gui.vs_clockwork.blueprint.$key")

    override fun init() {
        coordinates.clear()
        files = Files.list(VdexBlueprintClient.directory()).use { paths -> paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".vdex") }.sorted().toList() }
        fun button(x: Int, y: Int, w: Int, text: Component, action: () -> Unit) = addRenderableWidget(Button.builder(text) { action() }.bounds(x, y, w, 20).build())
        button(left, 32, column - 65, label("folder")) { Util.getPlatform().openFile(VdexBlueprintClient.directory().toFile()) }
        button(left + column - 60, 32, 60, label("refresh")) { rebuildWidgets() }
        repeat(rows) { row ->
            val file = files.getOrNull(page * rows + row)
            button(left, 57 + row * 22, column, Component.literal(file?.fileName?.toString()?.removeSuffix(".vdex") ?: "")) {
                file?.let(VdexBlueprintClient::import)
            }.active = file != null
        }
        button(left, 61 + rows * 22, column / 2 - 4, Component.literal("<")) { if (page > 0) { page--; rebuildWidgets() } }
        button(left + column / 2 + 4, 61 + rows * 22, column / 2 - 4, Component.literal(">")) { if ((page + 1) * rows < files.size) { page++; rebuildWidgets() } }
        val stack = minecraft?.player?.mainHandItem
        val tag = stack?.tag
        val pos = minecraft?.player?.position() ?: Vec3.ZERO
        val values = if (tag?.getBoolean("Deployed") == true) listOf(tag.getDouble("VdexX"), tag.getDouble("VdexY"), tag.getDouble("VdexZ"), tag.getFloat("VdexYaw").toDouble()) else listOf(pos.x, pos.y, pos.z, 0.0)
        values.forEachIndexed { index, value ->
            coordinates.add(addRenderableWidget(EditBox(font, right + (index % 2) * (column / 2) + 14, fieldsTop + (index / 2) * 23, column / 2 - 19, 18, Component.literal(listOf("X", "Y", "Z", "Yaw")[index])).also {
                it.value = "%.2f".format(Locale.ROOT, value); it.setMaxLength(16)
            }))
        }
        button(right, actionsTop, column / 2 - 4, label("place")) { deploy() }
        button(right + column / 2 + 4, actionsTop, column / 2 - 4, label("paste")) {
            if (deploy()) send(VdexBlueprintActionPacket.Action.PASTE)
        }.active = minecraft?.player?.isCreative == true
        button(width / 2 - 50, height - 30, 100, Component.translatable("gui.done")) { onClose() }
    }

    private fun send(action: VdexBlueprintActionPacket.Action) {
        val id = minecraft?.player?.mainHandItem?.tag?.getString("VdexFile").orEmpty()
        ClockworkPackets.sendToServer(VdexBlueprintActionPacket(action, id))
    }

    private fun deploy(): Boolean {
        val values = coordinates.map { it.value.toDoubleOrNull() }
        if (values.any { it == null || !it.isFinite() }) return false
        val id = minecraft?.player?.mainHandItem?.tag?.getString("VdexFile").orEmpty()
        if (!VdexBlueprintData.validId(id)) return false
        ClockworkPackets.sendToServer(VdexBlueprintActionPacket(VdexBlueprintActionPacket.Action.DEPLOY, id,
            Vec3(values[0]!!, values[1]!!, values[2]!!), values[3]!!.toFloat()))
        return true
    }

    override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(graphics)
        graphics.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF)
        val stack = minecraft?.player?.mainHandItem
        graphics.drawString(font, stack?.tag?.getString("VdexName") ?: "", right, 34, 0xA8E4F5)
        VdexBlueprintPreview.drawScreen(graphics, stack, right, 52, column, previewHeight)
        listOf("X", "Y", "Z", "°").forEachIndexed { i, s -> graphics.drawString(font, s, right + (i % 2) * (column / 2), fieldsTop + (i / 2) * 23 + 5, 0xFFFFFF) }
        graphics.drawCenteredString(font, VdexBlueprintClient.status, width / 2, height - 46, 0xBBDDEA)
        super.render(graphics, mouseX, mouseY, partialTick)
    }

    override fun isPauseScreen() = false
}
