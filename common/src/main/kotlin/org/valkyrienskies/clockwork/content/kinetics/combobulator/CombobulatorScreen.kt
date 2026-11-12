package org.valkyrienskies.clockwork.content.kinetics.combobulator

import com.simibubi.create.AllBlocks
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.widget.IconButton
import com.simibubi.create.foundation.gui.widget.ScrollInput
import net.createmod.catnip.gui.AbstractSimiScreen
import net.createmod.catnip.gui.element.GuiGameElement
import net.createmod.catnip.gui.element.ScreenElement
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.ClockworkItems
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import java.util.Locale

class CombobulatorScreen(private val be: CombobulatorBlockEntity) :
    AbstractSimiScreen(Component.translatable("block.vs_clockwork.combobulator")) {
    private var draft = if (be.program.operation == null) CombobulatorProgram.controller() else be.program
    private val revision = be.revision
    private var selected = 0
    private var functionTab = false
    private var ghost = -1
    private var rebuildPending = false
    private val invalid = mutableSetOf<String>()
    private val labels = mutableListOf<(GuiGraphics) -> Unit>()
    private val operation get() = draft.operation!!
    private val port get() = draft.channels.firstOrNull { it.id == selected }
    private var apply: IconButton? = null

    override fun init() {
        setWindowSize(316, 221)
        setWindowOffset(-20, 0)
        super.init()
        rebuild()
    }

    override fun tick() {
        super.tick()
        if (rebuildPending) { rebuildPending = false; rebuild() }
        apply?.active = invalid.isEmpty() && revision == be.revision
    }

    private fun rebuild() {
        clearWidgets()
        setFocused(null)
        labels.clear()
        invalid.clear()
        tab(2, tr("io"), false)
        tab(69, tr("function"), true)
        if (functionTab) {
            functionSelector()
            settings()
        } else {
            portCards()
            bindings()
        }
        apply = icon(284, 197, AllIcons.I_CONFIRM, tr("apply")) {
            if (invalid.isEmpty() && revision == be.revision) {
                ClockworkPackets.sendToServer(ConfigureCombobulatorPacket(be.blockPos, revision, draft))
                onClose()
            }
        }
        apply?.active = invalid.isEmpty() && revision == be.revision
    }

    private fun tab(left: Int, name: Component, function: Boolean) {
        addRenderableWidget(object : Button(guiLeft + left, guiTop + 20, 67, 16, name,
            { functionTab = function; ghost = -1; rebuild() }, DEFAULT_NARRATION) {
            override fun renderWidget(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                val active = functionTab == function
                if (active) skin(g, left, 20, 309, 0, 67, 16)
                draw(g, left + (67 - font.width(name)) / 2, 23, name, if (active) LIGHT else INK)
                if (isHoveredOrFocused) g.fill(x + 4, y + 12, x + width - 4, y + 13, 0xffdfbd77.toInt())
            }
        })
    }

    private fun functionSelector() {
        label(14, 52, tr("function"))
        val selector = ScrollInput(guiLeft + 14, guiTop + 64, 101, 18)
            .withRange(0, CombobulatorFunction.entries.size).titled(tr("function"))
            .setState(operation.function.ordinal).calling { index ->
                val next = operation.copy(function = CombobulatorFunction.entries[index])
                draft = draft.copy(operation = next, channels = CombobulatorProgram.controllerChannels(next, draft.channels))
                if (draft.channels.none { it.id == selected }) selected = draft.channels.first().id
                ghost = -1
                rebuildPending = true
            }
        addRenderableWidget(selector)
        labels += { field(it, 14, 64, 101, 18); draw(it, 18, 69, fitted(functionName(), 93), INK) }
    }

    private fun portCards() {
        draft.channels.forEachIndexed { row, channel ->
            val y = 51 + row * 22
            val name = portName(channel.id)
            val card = object : Button(guiLeft + 14, guiTop + y, 101, 18, name,
                { selected = channel.id; ghost = -1; rebuild() }, DEFAULT_NARRATION) {
                override fun renderWidget(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
                    skin(g, 14, y, 308, 19, 101, 18)
                    if (selected == channel.id || isHovered) g.fill(x, this.y, x + 2, this.y + 18, 0xffdfbd77.toInt())
                    val textY = y + (18 - font.lineHeight) / 2
                    draw(g, 19, textY, fitted(name, 43), INK)
                    val current = if (draft.write() == be.program.write()) be.value(channel.id) else channel.constant
                    val value = fitted(Component.literal(current?.let(::format) ?: "—"), 44)
                    draw(g, 110 - font.width(value), textY, value, INK)
                }
            }
            addRenderableWidget(card)
        }
    }

    private fun settings() {
        label(126, 53, functionName())
        val f = operation.function
        if (f == CombobulatorFunction.COMPARE) {
            button(126, 69, 169, tr("compare.${operation.comparison.name.lowercase()}")) {
                draft = draft.copy(operation = operation.copy(comparison = CombobulatorComparison.entries[
                    (operation.comparison.ordinal + 1) % CombobulatorComparison.entries.size]))
                rebuild()
            }
            label(126, 93, tr("tolerance"))
            numeric(126, 104, 169, "tolerance", operation.tolerance, positive = true) {
                draft = draft.copy(operation = operation.copy(tolerance = it))
            }
        }
        if (f.timed) {
            label(126, 75, tr("ticks"))
            val ticks = ScrollInput(guiLeft + 126, guiTop + 89, 169, 18).withRange(1, CombobulatorProgram.MAX_TICKS + 1)
                .titled(tr("ticks")).setState(operation.ticks).calling { draft = draft.copy(operation = operation.copy(ticks = it)) }
            addRenderableWidget(ticks)
            labels += { field(it, 126, 89, 169, 18); draw(it, 131, 94, Component.literal("${ticks.state} ticks"), INK) }
        }
        if (f.booleanOutput) {
            label(126, 132, tr("false_value"))
            label(214, 132, tr("true_value"))
            numeric(126, 144, 81, "low", operation.low) { draft = draft.copy(operation = operation.copy(low = it)) }
            numeric(214, 144, 81, "high", operation.high) { draft = draft.copy(operation = operation.copy(high = it)) }
        }
        labels += { g -> g.drawWordWrap(font, tr("function_hint.${f.name.lowercase()}"), guiLeft + 126, guiTop + 165, 169, LIGHT) }
    }

    private fun bindings() {
        val p = port ?: return
        label(126, 53, portName(p.id))
        val modes = if (p.input) listOf("constant", "link", "thinkable", "device", "redstone")
            else listOf("link", "thinkable", "device", "redstone")
        modes.forEachIndexed { index, mode ->
            val stack = when (mode) {
                "constant" -> ItemStack(Items.PAPER)
                "link" -> AllBlocks.REDSTONE_LINK.asStack()
                "thinkable" -> ClockworkItems.THINKABLE.asStack()
                "device" -> ClockworkBlocks.COMBOBULATOR.asStack()
                else -> ItemStack(Items.REDSTONE)
            }
            icon(126 + index * 34, 69, ScreenElement { g, x, y -> g.renderItem(stack, x, y) }, tr("binding.$mode"), mode(p) == mode) {
                update { it.copy(constant = if (mode == "constant") 0.0 else null,
                    wireless = mode == "link", thinkable = mode == "thinkable", device = mode == "device") }
                ghost = -1
                rebuild()
            }
        }
        label(126, 93, tr("binding.${mode(p)}"))
        if (p.constant != null) {
            numeric(126, 112, 169, "constant", p.constant) { value -> update { it.copy(constant = value) } }
            label(126, 145, tr("precise_hint"))
        } else if (p.wireless) {
            label(126, 111, tr(if (ghost < 0) "choose_slot" else "choose_item"))
        } else {
            val glyphs = listOf("●", "×", "←", "→", "↑", "↓")
            CombobulatorFace.entries.forEachIndexed { index, face ->
                val taken = draft.channels.any { it.id != selected && !it.wireless && it.constant == null && it.face == face }
                val image = ScreenElement { g, x, y -> g.drawCenteredString(font, glyphs[index], x + 8, y + 4, INK) }
                val button = icon(126 + index * 28, 112, image, tr("face.${face.name.lowercase()}"), p.face == face) {
                    update { it.copy(face = face) }; rebuild()
                }
                button.active = !taken
            }
            label(126, 138, p.face?.let { tr("face.${it.name.lowercase()}") } ?: tr("unbound"))
            labels += { g -> g.drawWordWrap(font, tr("binding_hint.${mode(p)}"), guiLeft + 126, guiTop + 155, 169, LIGHT) }
        }
    }

    private fun mode(p: CombobulatorChannel) = when {
        p.constant != null -> "constant"
        p.wireless -> "link"
        p.thinkable -> "thinkable"
        p.device -> "device"
        else -> "redstone"
    }

    private fun update(change: (CombobulatorChannel) -> CombobulatorChannel) {
        draft = draft.copy(channels = draft.channels.map { if (it.id == selected) change(it) else it })
    }

    private fun numeric(x: Int, y: Int, width: Int, key: String, value: Double, positive: Boolean = false, change: (Double) -> Unit) {
        val edit = EditBox(font, guiLeft + x, guiTop + y, width, 18, tr(key))
        edit.setMaxLength(32)
        edit.value = value.toString()
        edit.setResponder { text ->
            val number = text.toDoubleOrNull()?.takeIf { it.isFinite() && (!positive || it >= 0) }
            if (number == null) { invalid.add(key); edit.setTextColor(0xff7777) }
            else { invalid.remove(key); edit.setTextColor(0xffffff); change(number) }
            apply?.active = invalid.isEmpty() && revision == be.revision
        }
        addRenderableWidget(edit)
    }

    private fun icon(x: Int, y: Int, image: ScreenElement, tooltip: Component, selected: Boolean = false, callback: () -> Unit): IconButton {
        val button = IconButton(guiLeft + x, guiTop + y, image)
        button.green = selected
        button.setToolTip(tooltip)
        button.withCallback<IconButton>(Runnable(callback))
        addRenderableWidget(button)
        return button
    }

    private fun button(x: Int, y: Int, width: Int, text: Component, callback: () -> Unit) {
        addRenderableWidget(Button.builder(fitted(text, width - 8)) { callback() }.bounds(guiLeft + x, guiTop + y, width, 18).build())
    }

    private fun label(x: Int, y: Int, text: Component) { labels += { draw(it, x, y, text) } }
    private fun functionName() = tr("function.${operation.function.name.lowercase()}")
    private fun portName(id: Int): Component {
        if (id == 3) return tr("output")
        val names = when (operation.function) {
            CombobulatorFunction.LATCH -> listOf("set", "reset")
            CombobulatorFunction.COUNTER -> listOf("count", "reset")
            CombobulatorFunction.TIMER -> listOf("enable")
            CombobulatorFunction.SWITCH -> listOf("a", "b", "select")
            else -> listOf("a", "b", "c")
        }
        return tr("port.${names[id]}")
    }

    override fun renderWindow(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        skin(g, 0, 0, 0, 0, 308, 221)
        skin(g, 308, 190, 308, 190, 8, 31)
        draw(g, 7, 4, title)
        val lamp = when { be.fault != null -> 376; be.value(3) != 0.0 -> 414; else -> 395 }
        skin(g, 145, 19, lamp, 0, 19, 19)
        val status = if (revision != be.revision) tr("stale") else be.fault?.let { tr("fault.${it.name.lowercase()}") }
        status?.let { draw(g, 168, 24, fitted(it, 132), INK) }
        labels.forEach { it(g) }
        val p = port
        if (!functionTab && p?.wireless == true) {
            slot(g, 252, 91, p.first, ghost == 0)
            slot(g, 270, 91, p.second, ghost == 1)
            for (i in 0 until 36) slot(g, 126 + i % 9 * 18, 120 + i / 9 * 16, inventory(i), false)
        }
        val footer = when {
            invalid.isNotEmpty() -> tr("invalid_number")
            be.program.operation == null -> tr("replace_legacy")
            draft.write() != be.program.write() -> tr("unsaved")
            else -> null
        }
        footer?.let { draw(g, 8, 201, fitted(it, 265), INK) }
        GuiGameElement.of(ClockworkBlocks.COMBOBULATOR.asStack())
            .at<GuiGameElement.GuiRenderBuilder>((guiLeft + 322).toFloat(), (guiTop + 165).toFloat(), -200f)
            .scale(5.0)
            .render(g)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val x = mouseX - guiLeft
        val y = mouseY - guiTop
        if (!functionTab && port?.wireless == true) {
            for (i in 0..1) if (x >= 252 + i * 18 && x < 268 + i * 18 && y >= 91 && y < 107 && button in 0..1) {
                if (button == 0) ghost = i else frequency(i, ItemStack.EMPTY)
                rebuild()
                return true
            }
            if (ghost >= 0 && button == 0 && x >= 126 && x < 288 && y >= 120 && y < 184) {
                val i = ((y - 120).toInt() / 16) * 9 + (x - 126).toInt() / 18
                frequency(ghost, inventory(i)); ghost = -1; rebuild(); return true
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun renderWindowForeground(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTick: Float) {
        val x = mouseX - guiLeft
        val y = mouseY - guiTop
        if (!functionTab && x in 14 until 115 && y >= 51 && (y - 51) % 22 < 18) {
            val p = draft.channels.getOrNull((y - 51) / 22) ?: return
            val current = if (draft.write() == be.program.write()) be.value(p.id) else p.constant
            g.renderTooltip(font, Component.literal(current?.let(::format) ?: "—"), mouseX, mouseY)
        }
    }

    private fun frequency(index: Int, item: ItemStack) = update {
        if (index == 0) it.copy(first = item.copyWithCount(1)) else it.copy(second = item.copyWithCount(1))
    }
    private fun inventory(index: Int) = minecraft?.player?.inventory?.getItem(if (index < 27) index + 9 else index - 27) ?: ItemStack.EMPTY
    private fun slot(g: GuiGraphics, x: Int, y: Int, stack: ItemStack, selected: Boolean) {
        field(g, x, y, 16, 16)
        if (selected) g.fill(guiLeft + x, guiTop + y, guiLeft + x + 16, guiTop + y + 16, 0xff7ea665.toInt())
        g.renderItem(stack, guiLeft + x, guiTop + y)
    }
    private fun field(g: GuiGraphics, x: Int, y: Int, w: Int, h: Int) {
        g.fill(guiLeft + x, guiTop + y, guiLeft + x + w, guiTop + y + h, 0xff755a43.toInt())
        g.fill(guiLeft + x + 1, guiTop + y + 1, guiLeft + x + w - 1, guiTop + y + h - 1, 0xfffaf0d9.toInt())
    }
    private fun draw(g: GuiGraphics, x: Int, y: Int, text: Component, color: Int = LIGHT) = g.drawString(font, text, guiLeft + x, guiTop + y, color, false)
    private fun fitted(text: Component, width: Int) = if (font.width(text) <= width) text else Component.literal(font.plainSubstrByWidth(text.string, width - 7) + "…")
    private fun skin(g: GuiGraphics, x: Int, y: Int, u: Int, v: Int, w: Int, h: Int) = g.blit(TEXTURE, guiLeft + x, guiTop + y, u.toFloat(), v.toFloat(), w, h, 512, 256)
    override fun isPauseScreen() = false

    companion object {
        private const val LIGHT = 0xfff3e6ce.toInt()
        private const val INK = 0xff392619.toInt()
        private val TEXTURE = ClockworkMod.asResource("textures/gui/combobulator.png")
        private fun tr(key: String) = Component.translatable("gui.vs_clockwork.combobulator.$key")
        private fun format(value: Double) = if (!value.isFinite()) "Unavailable" else String.format(Locale.ROOT, "%.6g", value)
    }
}
