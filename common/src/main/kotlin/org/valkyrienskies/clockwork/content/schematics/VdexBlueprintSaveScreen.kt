package org.valkyrienskies.clockwork.content.schematics

import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.widget.IconButton
import net.minecraft.Util
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.MultiLineEditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkShaders
import kotlin.math.pow

class VdexBlueprintSaveScreen : Screen(Component.translatable("gui.vs_clockwork.blueprint.save_title")) {
    private val preview = VdexSketchRenderer()
    private val transition = VdexSketchTransition()
    private var draftId: String? = null
    val captureId get() = draftId
    private var sound: VdexSketchSound? = null
    private var draggingPreview = false
    private var saving = false
    private var committed = false
    private lateinit var name: EditBox
    private lateinit var author: EditBox
    private lateinit var description: MultiLineEditBox
    private lateinit var confirm: IconButton
    private val left get() = (width - 308) / 2
    private val top get() = (height - 221) / 2
    private val previewX get() = left + 6
    private val previewY get() = top + 30
    private fun label(key: String) = Component.translatable("gui.vs_clockwork.blueprint.$key")

    override fun init() {
        val previousName = if (::name.isInitialized) name.value else ""
        val previousAuthor = if (::author.isInitialized) author.value else minecraft?.player?.scoreboardName.orEmpty()
        val previousDescription = if (::description.isInitialized) description.value else ""
        name = addRenderableWidget(VdexCoordinateBox(font, left + 8, top + 201, 112, 12, label("name")).also {
            it.setBordered(false)
            it.setTextColor(0xfff3e6ce.toInt())
            it.setMaxLength(VdexBlueprintAuthoring.NAME_LIMIT)
            it.value = previousName
            it.setHint(label("name"))
            it.setTooltip(Tooltip.create(label("name")))
        })
        author = addRenderableWidget(VdexCoordinateBox(font, left + 197, top + 67, 102, 16, label("author")).also {
            it.setMaxLength(VdexBlueprintAuthoring.AUTHOR_LIMIT)
            it.setTextColor(0xfff3e6ce.toInt())
            it.value = previousAuthor
        })
        description = addRenderableWidget(MultiLineEditBox(font, left + 197, top + 102, 102, 71,
            label("description_hint"), label("description")).also {
            it.setCharacterLimit(VdexBlueprintAuthoring.DESCRIPTION_LIMIT)
            it.value = previousDescription
        })
        confirm = addRenderableWidget(IconButton(left + 284, top + 197, AllIcons.I_CONFIRM).also {
            it.setTooltip(Tooltip.create(label("save")))
            it.withCallback<IconButton>(Runnable { save() })
            it.active = false
        })
        setInitialFocus(name)
    }

    fun capture(packet: VdexBlueprintCapturePacket) {
        draftId = packet.id
        if (name.value.isBlank()) name.value = packet.name
        if (author.value.isBlank()) author.value = packet.author
    }

    fun saved(packet: VdexBlueprintSaveResultPacket) {
        if (packet.id != draftId) return
        saving = false
        if (packet.success) { committed = true; onClose() }
    }

    private fun save() {
        val id = draftId ?: return
        if (!confirm.active || saving) return
        saving = true
        confirm.active = false
        ClockworkPackets.sendToServer(VdexBlueprintSavePacket(id, true,
            VdexBlueprintAuthoring(name.value, author.value, description.value)))
    }

    override fun tick() {
        super.tick()
        name.tick(); author.tick(); description.tick()
        confirm.active = !saving && transition.phase == VdexSketchTransition.Phase.READY &&
            name.value.isNotBlank() && author.value.isNotBlank() && minecraft?.player?.mainHandItem?.item is EmptyBlueprintItem
        name.setEditable(!saving); author.setEditable(!saving); description.active = !saving
    }

    override fun render(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        renderBackground(g)
        g.blit(TEXTURE, left, top, 316, 221, 0f, 0f, 316, 221, 512, 256)
        g.drawString(font, title, left + 7, top + 4, 0xfff3e6ce.toInt(), false)
        g.drawString(font, label("authoring"), left + 197, top + 33, 0xff392619.toInt(), false)
        g.drawString(font, label("author"), left + 197, top + 54, 0xffe8dcc6.toInt(), false)
        g.drawString(font, label("description"), left + 197, top + 90, 0xffe8dcc6.toInt(), false)
        val now = Util.getMillis()
        val data = draftId?.let { VdexBlueprintClient.captured(it) }
        val ready = data != null && VdexBlueprintPreview.mesh(data).buffer != null &&
            ClockworkShaders.blueprintGeometry() != null && ClockworkShaders.blueprintSketch() != null && ClockworkShaders.blueprintInk() != null
        val previousPhase = transition.phase
        transition.update(draftId, ready, now)
        if (previousPhase != transition.phase) updateSound()
        g.enableScissor(previewX, previewY, previewX + 182, previewY + 154)
        if (data != null && transition.current != null) {
            preview.render(g, data, previewX + 3, previewY + 3, 176, 148, transition.draw(now))
        } else {
            val loading = label(if (draftId == null) "capturing" else "drawing")
            g.drawString(font, loading, previewX + (182 - font.width(loading)) / 2, previewY + 73, 0xff526875.toInt(), false)
        }
        g.disableScissor()
        val status = if (saving) label("saving") else VdexBlueprintClient.status
        g.drawString(font, font.plainSubstrByWidth(status.string, 145), left + 130, top + 202, 0xff392619.toInt(), false)
        super.render(g, mouseX, mouseY, partialTick)
        if (overPreview(mouseX.toDouble(), mouseY.toDouble()) && !draggingPreview)
            g.renderTooltip(font, label("view_controls"), mouseX, mouseY)
        if (mouseX in left + 130..left + 278 && mouseY in top + 197..top + 216 && status.string.isNotEmpty())
            g.renderTooltip(font, status, mouseX, mouseY)
    }

    private fun updateSound() {
        sound?.let { minecraft?.soundManager?.stop(it) }
        sound = null
        if (transition.phase != VdexSketchTransition.Phase.DRAWING) return
        sound = VdexSketchSound(true) {
            if (minecraft?.screen !== this || transition.phase != VdexSketchTransition.Phase.DRAWING) null else {
                val progress = transition.draw(Util.getMillis())
                minOf(progress / .06f, (1f - progress) / .12f, 1f).coerceAtLeast(0f)
            }
        }.also { minecraft?.soundManager?.play(it) }
    }

    private fun overPreview(x: Double, y: Double) = x >= previewX && x < previewX + 182 && y >= previewY && y < previewY + 154

    override fun mouseClicked(x: Double, y: Double, button: Int): Boolean {
        if (overPreview(x, y)) {
            if (button == 0) draggingPreview = true
            if (button == 1) preview.reset()
            return true
        }
        return super.mouseClicked(x, y, button)
    }

    override fun mouseDragged(x: Double, y: Double, button: Int, dx: Double, dy: Double): Boolean {
        if (draggingPreview) {
            preview.yaw = (preview.yaw + dx.toFloat() * .7f) % 360f
            preview.pitch = (preview.pitch + dy.toFloat() * .7f).coerceIn(-85f, 85f)
            return true
        }
        return super.mouseDragged(x, y, button, dx, dy)
    }

    override fun mouseReleased(x: Double, y: Double, button: Int): Boolean {
        draggingPreview = false
        return super.mouseReleased(x, y, button)
    }

    override fun mouseScrolled(x: Double, y: Double, amount: Double): Boolean {
        if (overPreview(x, y)) {
            preview.zoom = (preview.zoom * 1.12.pow(amount).toFloat()).coerceIn(.5f, 4f)
            return true
        }
        return super.mouseScrolled(x, y, amount)
    }

    override fun removed() {
        draftId?.let {
            if (!committed) ClockworkPackets.sendToServer(VdexBlueprintSavePacket(it, false))
            VdexBlueprintClient.discardCapture(it)
        }
        sound?.let { minecraft?.soundManager?.stop(it) }
        sound = null
        preview.close()
        super.removed()
    }

    override fun isPauseScreen() = false

    companion object {
        private val TEXTURE = ClockworkMod.asResource("textures/gui/vdex_save_gui.png")
    }
}
