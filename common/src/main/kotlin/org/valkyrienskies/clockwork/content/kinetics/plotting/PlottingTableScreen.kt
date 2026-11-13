package org.valkyrienskies.clockwork.content.kinetics.plotting

import com.simibubi.create.AllBlocks
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.widget.IconButton
import net.createmod.catnip.gui.AbstractSimiScreen
import net.createmod.catnip.gui.element.GuiGameElement
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.Minecraft
import net.minecraft.core.Direction
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.client.gui.components.Button
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.Tooltip
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.Util
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.state.BlockState
import org.lwjgl.glfw.GLFW
import org.valkyrienskies.clockwork.ClockworkBlocks
import org.valkyrienskies.clockwork.content.schematics.VdexSketchSound
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorBlock
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorComparison
import org.valkyrienskies.clockwork.content.kinetics.combobulator.CombobulatorFunction
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow

class PlottingTableScreen(private val be: PlottingTableBlockEntity) : AbstractSimiScreen(tr("title")) {
    private val files = PlottingPlanFiles(Minecraft.getInstance().gameDirectory.toPath().resolve("schematics/plotting"))
    private var fileMessage: Component? = null
    private val documents = loadDocuments()
    private var activePlan = 0
    private var plan: PlottingPlan
        get() = documents[activePlan].plan
        set(value) { documents[activePlan] = documents[activePlan].copy(plan = value) }
    private val simulation = PlottingSimulation().apply { reset(plan) }
    private val wireRenderer = PlottingWireRenderer()
    private val devices by lazy { minecraft?.level?.let(PlottingDevices::discover) ?: emptyList() }
    private var dropdown: PlottingDropdown? = null
    private var contextMenu: PlottingContextMenu? = null
    private var rightClick: Pair<Double, Double>? = null
    private val undo = ArrayDeque<PlottingPlan>()
    private val redo = ArrayDeque<PlottingPlan>()
    private var selected: Int? = null
    private var placing: PlottingKind? = null
    private var wire: Int? = null
    private var dragging: Int? = null
    private var panning = false
    private var panX = 16.0
    private var panY = 16.0
    private var zoom = 1.0
    private var playing = false
    private var speed = 1
    private var clock = 0
    private var ioTab = false
    private var selectedInput = 0
    private var rebuildPending = false
    private var closing = false
    private var inspectorScroll = 0
    private var buildingInspector = false
    private var sketchSound: VdexSketchSound? = null
    private val labels = mutableListOf<Triple<Int, Int, Component>>()
    private val invalid = mutableSetOf<String>()
    private var save: IconButton? = null
    private var saveWithoutClosing: IconButton? = null
    private val left get() = guiLeft + 32
    private val top get() = guiTop + 51
    private val right get() = guiLeft + windowWidth - 166
    private val bottom get() = guiTop + windowHeight - 25
    private val inspector get() = windowWidth - 158
    private val cell get() = 64.0 * zoom
    private val node get() = plan.nodes.firstOrNull { it.id == selected }

    override fun init() {
        contextMenu = null
        rightClick = null
        dragging = null
        panning = false
        setWindowSize((width - 12).coerceAtMost(1000), (height - 12).coerceAtMost(650))
        super.init()
        rebuild()
    }

    private fun rebuild() {
        dropdown = null
        clearWidgets()
        setFocused(null)
        labels.clear()
        invalid.clear()
        if (node == null) inspectorScroll = 0
        button(inspector, 55, 40, "plans", Component.literal("${activePlan + 1}/${documents.size}")) {
            activePlan = (activePlan + 1) % documents.size
            changePlan()
        }
        button(inspector + 44, 55, 26, "new", Component.literal("+")) {
            if (documents.size < PlottingPlanFiles.MAX_DOCUMENTS) {
                documents.add(PlottingDocument(PlottingPlan("Plan ${documents.size + 1}"))); activePlan = documents.lastIndex; changePlan()
            }
        }
        button(inspector + 74, 55, 32, "copy", Component.literal("++")) {
            if (documents.size < PlottingPlanFiles.MAX_DOCUMENTS) {
                documents.add(PlottingDocument(plan.copy(name = (plan.name + " copy").take(40)))); activePlan = documents.lastIndex; changePlan()
            }
        }
        val delete = addRenderableWidget(IconButton(guiLeft + inspector + 132, guiTop + 55, AllIcons.I_TRASH)
            .withCallback<IconButton> { deletePlan() })
        delete.setToolTip(tr("delete_plan"))
        textBox(112, 4, windowWidth - 142, plan.name) { plan = plan.copy(name = it) }
        save = addRenderableWidget(IconButton(guiLeft + windowWidth - 23, guiTop + windowHeight - 21, AllIcons.I_CONFIRM)
            .withCallback { savePlans(true) })
        save?.setToolTip(tr("save"))
        saveWithoutClosing = addRenderableWidget(IconButton(guiLeft + windowWidth - 45, guiTop + windowHeight - 21, AllIcons.I_CONFIG_SAVE)
            .withCallback { savePlans(false) })
        saveWithoutClosing?.setToolTip(tr("save_files"))
        toolbarButton(6, 44, if (playing) "pause" else "play") { playing = !playing; rebuildPending = true }
        toolbarButton(52, 40, "step") { playing = false; simulation.tick(plan); rebuildPending = true }
        toolbarButton(94, 42, "reset") { reset(); rebuildPending = true }
        toolbarButton(138, 34, "speed", Component.literal("${20 / speed}t/s")) {
            speed = when (speed) { 1 -> 4; 4 -> 20; else -> 1 }; rebuildPending = true
        }
        toolbarButton(174, 28, "home", Component.literal("⌂")) { panX = 16.0; panY = 16.0; zoom = 1.0 }
        toolbarButton(204, 25, "undo", Component.literal("<")) { history(undo, redo) }
        toolbarButton(231, 25, "redo", Component.literal(">")) { history(redo, undo) }
        PlottingKind.entries.forEachIndexed { i, kind ->
            button(6, 55 + i * 25, 22, "kind.${kind.name.lowercase()}", Component.literal(listOf("ƒ", "#", "I", "O")[i])) {
                placing = kind; wire = null; playing = false; rebuildPending = true
            }
        }
        val folder = IconButton(guiLeft + windowWidth - 25, guiTop + 4, AllIcons.I_OPEN_FOLDER)
        folder.withCallback<IconButton> {
            fileAction {
                java.nio.file.Files.createDirectories(files.directory)
                Util.getPlatform().openFile(files.directory.toFile())
            }
        }
        folder.setToolTip(tr("folder"))
        addRenderableWidget(folder)
        button(6, 155, 22, "reload", Component.literal("R")) {
            playing = false
            if (documents.any { it.dirty }) minecraft?.setScreen(ConfirmScreen({ reload ->
                if (reload) reloadDocuments()
                minecraft?.setScreen(this)
            }, tr("reload"), tr("reload_confirm"))) else reloadDocuments()
        }
        buildingInspector = true
        inspector()
        buildingInspector = false
    }

    private fun inspector() {
        val n = node
        if (n == null) {
            if (be.plans.isNotEmpty()) button(inspector, 78, 150, "import_table") {
                val imported = be.plans.filter { old -> documents.none { it.plan.write() == old.write() } }
                    .take(PlottingPlanFiles.MAX_DOCUMENTS - documents.size)
                documents.addAll(imported.map { PlottingDocument(it) })
                if (imported.isNotEmpty()) { activePlan = documents.size - imported.size; changePlan() }
                fileMessage = tr("imported", imported.size)
            }
            label(inspector, if (be.plans.isEmpty()) 82 else 105, Component.literal(documents[activePlan].file?.fileName?.toString() ?: tr("unsaved_file").string))
            return
        }
        label(inspector, 53, Component.literal("#${n.id} ").append(tr("kind.${n.kind.name.lowercase()}")))
        textBox(inspector, 67, 150, n.name) { update(n.id, reset = false) { old -> old.copy(name = it) } }
        button(inspector, 89, 72, "settings") { ioTab = false; rebuildPending = true }
        button(inspector + 76, 89, 74, "io") { ioTab = true; rebuildPending = true }
        if (ioTab && n.inputCount > 0) {
            selectedInput = selectedInput.coerceAtMost(n.inputCount - 1)
            repeat(n.inputCount) { index -> button(inspector + index * 50, 113, 47, "input", Component.literal("${'A' + index}")) {
                selectedInput = index; rebuildPending = true
            } }
            val binding = n.inputs[selectedInput]
            label(inspector, 137, binding.source?.let { tr("from", it) } ?: tr("constant"))
            numeric(inspector, 151, "binding", binding.constant) { value ->
                update(n.id) { it.copy(inputs = it.inputs.mapIndexed { i, input -> if (i == selectedInput) input.copy(constant = value) else input }) }
            }
            button(inspector, 176, 150, "connection.${binding.connection.name.lowercase()}") {
                val next = PlottingConnection.entries[(binding.connection.ordinal + 1) % PlottingConnection.entries.size]
                update(n.id) { it.copy(inputs = it.inputs.mapIndexed { i, input -> if (i == selectedInput) input.copy(connection = next) else input }) }
                rebuildPending = true
            }
            button(inspector, 199, 150, "disconnect") { bind(n.id, selectedInput, null) }
        } else if (n.kind == PlottingKind.FUNCTION) {
            button(inspector, 113, 150, "function", functionName(n)) {
                val f = CombobulatorFunction.entries[(n.operation.function.ordinal + 1) % CombobulatorFunction.entries.size]
                update(n.id) { it.copy(operation = it.operation.copy(function = f)) }; rebuildPending = true
            }
            when {
                n.operation.function == CombobulatorFunction.COMPARE -> {
                    button(inspector, 137, 150, "comparison", Component.translatable("gui.vs_clockwork.combobulator.compare.${n.operation.comparison.name.lowercase()}")) {
                        val comparison = CombobulatorComparison.entries[(n.operation.comparison.ordinal + 1) % CombobulatorComparison.entries.size]
                        update(n.id) { it.copy(operation = it.operation.copy(comparison = comparison)) }; rebuildPending = true
                    }
                    label(inspector, 161, tr("tolerance"))
                    numeric(inspector, 172, "tolerance", n.operation.tolerance, 0.0) { v -> update(n.id) { it.copy(operation = it.operation.copy(tolerance = v)) } }
                }
                n.operation.function.timed -> {
                    label(inspector, 140, tr("duration"))
                    numeric(inspector, 153, "ticks", n.operation.ticks.toDouble(), 1.0, 1200.0, true) { v ->
                        update(n.id) { it.copy(operation = it.operation.copy(ticks = v.toInt())) }
                    }
                }
            }
            if (n.operation.function.booleanOutput) {
                label(inspector, 198, tr("false_true"))
                numeric(inspector, 210, "low", n.operation.low, fieldWidth = 72) { v -> update(n.id) { it.copy(operation = it.operation.copy(low = v)) } }
                numeric(inspector + 78, 210, "high", n.operation.high, fieldWidth = 72) { v -> update(n.id) { it.copy(operation = it.operation.copy(high = v)) } }
            }
        } else {
            val device = devices.firstOrNull { it.id == n.device }
            if (n.kind != PlottingKind.CONSTANT) {
                val choices = listOf<PlottingDevice?>(null) + devices.filter { it.ports(n.kind).isNotEmpty() }
                dropdownButton(113, "device", deviceName(n), choices.map { PlottingChoice(it?.name ?: tr("device.generic"), it?.id ?: "") },
                    choices.indexOf(device).coerceAtLeast(0)) { index ->
                    val next = choices[index]
                    val port = next?.ports(n.kind)?.firstOrNull()
                    update(n.id) { it.copy(device = next?.id ?: "", deviceSide = port?.side ?: Direction.NORTH, reading = port?.reading ?: "") }
                    rebuildPending = true
                }
                val ports = device?.ports(n.kind).orEmpty()
                if (ports.isNotEmpty()) {
                    val selectedPort = ports.firstOrNull { it.side == n.deviceSide && it.reading == n.reading } ?: ports.first()
                    dropdownButton(137, "reading", selectedPort.label(), ports.map { PlottingChoice(it.label(), it.reading) },
                        ports.indexOf(selectedPort)) { index ->
                        val next = ports[index]
                        update(n.id) { it.copy(deviceSide = next.side, reading = next.reading) }
                        rebuildPending = true
                    }
                }
            }
            val offset = if (n.kind != PlottingKind.CONSTANT && device != null) 24 else 0
            if (n.hasOutput) {
                label(inspector, 140 + offset, tr(if (n.kind == PlottingKind.INPUT) "initial_value" else "value"))
                numeric(inspector, 153 + offset, "value", n.value) { v -> update(n.id, reset = false) { it.copy(value = v) } }
            }
            if (n.kind == PlottingKind.INPUT) {
                label(inspector, 179 + offset, tr("test_value"))
                numeric(inspector, 190 + offset, "test_value", simulation.inputValue(n)) { simulation.setInput(n.id, it) }
            }
        }
        val y = 239
        button(inspector, y, 72, "rotate") { update(n.id) { it.copy(rotation = (it.rotation + 1) % 4) }; rebuildPending = true }
        button(inspector + 76, y, 74, "delete") { removeSelected() }
    }

    private fun textBox(x: Int, y: Int, w: Int, initial: String, change: (String) -> Unit) {
        val field = EditBox(font, guiLeft + x, guiTop + shifted(y), w, 18, tr("value"))
        field.visible = visible(y)
        field.setMaxLength(40)
        field.value = initial
        field.setResponder(change)
        addRenderableWidget(field)
    }

    private fun numeric(x: Int, y: Int, key: String, value: Double, minimum: Double = -Double.MAX_VALUE,
                        maximum: Double = Double.MAX_VALUE, integer: Boolean = false, fieldWidth: Int = 150, change: (Double) -> Unit) {
        val field = EditBox(font, guiLeft + x, guiTop + shifted(y), fieldWidth, 18, tr(key))
        field.visible = visible(y)
        field.setMaxLength(24)
        field.value = format(value)
        field.setResponder { text ->
            val number = text.toDoubleOrNull()?.takeIf { it.isFinite() && it in minimum..maximum && (!integer || it == floor(it)) }
            if (number == null) { invalid.add(key); field.setTextColor(0xff7777) }
            else { invalid.remove(key); field.setTextColor(0xe0e0e0); change(number) }
        }
        addRenderableWidget(field)
    }

    private fun dropdownButton(y: Int, key: String, title: Component, options: List<PlottingChoice>, selected: Int, change: (Int) -> Unit) {
        val anchorY = guiTop + shifted(y) + 20
        button(inspector, y, 150, key, Component.literal(fit(title.string, 130) + " ▾")) {
            setFocused(null)
            val rows = minOf(7, max(1, (windowHeight - 80) / 20))
            val popupY = minOf(anchorY, bottom - 27 - rows * 20).coerceAtLeast(guiTop + 27)
            dropdown = PlottingDropdown(font, guiLeft + windowWidth - 218, popupY, 210, rows, options, selected,
                change, { dropdown = null })
        }
    }

    private fun toolbarButton(x: Int, w: Int, key: String, title: Component = tr(key), action: () -> Unit) {
        val scale = minOf(1.0, (windowWidth - 12) / 250.0)
        button(6 + ((x - 6) * scale).toInt(), 29, (w * scale).toInt(), key, title, action)
    }

    private fun button(x: Int, y: Int, w: Int, key: String, title: Component = tr(key), action: () -> Unit) {
        val tooltip = if (key == "function" && node?.kind == PlottingKind.FUNCTION)
            Component.translatable("gui.vs_clockwork.combobulator.function_hint.${node!!.operation.function.name.lowercase()}") else tr(key)
        addRenderableWidget(Button.builder(title) { action() }.bounds(guiLeft + x, guiTop + shifted(y), w, 18)
            .tooltip(Tooltip.create(tooltip)).build()).visible = visible(y)
    }

    private fun shifted(y: Int) = y + if (buildingInspector) (if (node != null) 26 else 0) - inspectorScroll else 0
    private fun visible(y: Int) = !buildingInspector || shifted(y) >= 78 && shifted(y) + 18 < windowHeight - 25
    private fun label(x: Int, y: Int, text: Component) { if (visible(y)) labels.add(Triple(x, shifted(y), text)) }
    private fun checkpoint() { undo.addLast(plan); while (undo.size > 50) undo.removeFirst(); redo.clear() }
    private fun update(id: Int, reset: Boolean = true, transform: (PlottingNode) -> PlottingNode) {
        checkpoint()
        plan = plan.copy(nodes = plan.nodes.map { if (it.id == id) transform(it) else it })
        if (reset) reset()
    }
    private fun reset() { playing = false; clock = 0; simulation.reset(plan) }
    private fun changePlan() {
        contextMenu = null; rightClick = null
        selected = null; wire = null; placing = null; undo.clear(); redo.clear(); panX = 16.0; panY = 16.0
        inspectorScroll = 0
        reset(); rebuildPending = true
    }
    private fun history(from: ArrayDeque<PlottingPlan>, to: ArrayDeque<PlottingPlan>) {
        if (from.isEmpty()) return
        contextMenu = null; rightClick = null
        to.addLast(plan); plan = from.removeLast(); selected = null; wire = null; reset(); rebuildPending = true
    }
    private fun bind(id: Int, port: Int, source: Int?) {
        if (plan.nodes.firstOrNull { it.id == id }?.inputs?.getOrNull(port)?.source == source) {
            wire = null
            return
        }
        update(id) { it.copy(inputs = it.inputs.mapIndexed { i, input -> if (i == port) input.copy(source = source) else input }) }
        editSound(source != null)
        wire = null; rebuildPending = true
    }
    private fun removeSelected() {
        val id = selected ?: return
        checkpoint()
        plan = plan.copy(nodes = plan.nodes.filter { it.id != id }.map { n ->
            n.copy(inputs = n.inputs.map { if (it.source == id) it.copy(source = null) else it })
        })
        editSound(false)
        selected = null; wire = null; reset(); rebuildPending = true
    }

    private fun freeCell(x: Int, y: Int) = x in -64..64 && y in -64..64 && plan.nodes.none { it.x == x && it.y == y }

    private fun addNode(x: Int, y: Int, kind: PlottingKind, copy: PlottingNode? = null) {
        if (plan.nodes.size >= PlottingPlan.MAX_NODES || !freeCell(x, y)) return
        val id = (1..PlottingPlan.MAX_NODES).first { candidate -> plan.nodes.none { it.id == candidate } }
        val added = copy?.copy(id = id, x = x, y = y, inputs = copy.inputs.map { it.copy(source = null) })
            ?: PlottingNode(id, x, y, kind)
        checkpoint()
        plan = plan.copy(nodes = plan.nodes + added)
        editSound(true)
        selected = id; placing = null; wire = null; inspectorScroll = 0; reset(); rebuildPending = true
    }

    private fun duplicateNode(n: PlottingNode) {
        if (plan.nodes.size >= PlottingPlan.MAX_NODES) return
        for (radius in 1..128) {
            for ((dx, dy) in listOf(radius to 0, 0 to radius, -radius to 0, 0 to -radius)) {
                if (freeCell(n.x + dx, n.y + dy)) { addNode(n.x + dx, n.y + dy, n.kind, n); return }
            }
            for (dx in -radius..radius) for (dy in intArrayOf(-radius, radius)) {
                if (freeCell(n.x + dx, n.y + dy)) { addNode(n.x + dx, n.y + dy, n.kind, n); return }
            }
            for (dy in -radius + 1 until radius) for (dx in intArrayOf(radius, -radius)) {
                if (freeCell(n.x + dx, n.y + dy)) { addNode(n.x + dx, n.y + dy, n.kind, n); return }
            }
        }
    }

    private fun disconnectNode(id: Int) {
        checkpoint()
        plan = plan.copy(nodes = plan.nodes.map { n -> n.copy(inputs = n.inputs.map {
            if (n.id == id || it.source == id) it.copy(source = null) else it
        }) })
        editSound(false)
        wire = null; reset(); rebuildPending = true
    }

    private fun openContextMenu(mouseX: Double, mouseY: Double) {
        val hit = portAt(mouseX, mouseY, true)
        val (x, y) = grid(mouseX, mouseY)
        val n = hit?.first ?: plan.nodes.lastOrNull { it.x == x && it.y == y }
        wire = null; placing = null; dragging = null; panning = false
        selected = n?.id; inspectorScroll = 0; rebuildPending = true
        val actions = mutableListOf<PlottingMenuAction>()
        if (n == null) {
            for (kind in PlottingKind.entries) actions.add(PlottingMenuAction(tr("context.add", tr("kind.${kind.name.lowercase()}")),
                plan.nodes.size < PlottingPlan.MAX_NODES && freeCell(x, y)) { addNode(x, y, kind) })
        } else {
            actions.add(PlottingMenuAction(tr("context.configure")) { ioTab = false; rebuildPending = true })
            actions.add(PlottingMenuAction(tr("context.duplicate"), plan.nodes.size < PlottingPlan.MAX_NODES) { duplicateNode(n) })
            actions.add(PlottingMenuAction(tr("rotate")) {
                update(n.id) { it.copy(rotation = (it.rotation + 1) % 4) }; editSound(true); rebuildPending = true
            })
            if (hit != null && hit.second >= 0) actions.add(PlottingMenuAction(tr("context.disconnect_input", "${'A' + hit.second}"),
                n.inputs[hit.second].source != null) { bind(n.id, hit.second, null) })
            val connected = n.inputs.any { it.source != null } || plan.nodes.any { sink -> sink.inputs.any { it.source == n.id } }
            actions.add(PlottingMenuAction(tr("context.disconnect_all"), connected) { disconnectNode(n.id) })
            actions.add(PlottingMenuAction(tr("delete"), destructive = true) { removeSelected() })
        }
        actions.add(PlottingMenuAction(tr("undo"), undo.isNotEmpty()) { history(undo, redo) })
        actions.add(PlottingMenuAction(tr("redo"), redo.isNotEmpty()) { history(redo, undo) })
        contextMenu = PlottingContextMenu(mouseX.toInt(), mouseY.toInt(), actions.maxOf { font.width(it.name) } + 16,
            width, height, actions) { contextMenu = null }
    }

    override fun tick() {
        super.tick()
        dropdown?.tick()
        if (playing && invalid.isEmpty() && ++clock >= speed) { clock = 0; simulation.tick(plan) }
        if (rebuildPending) { rebuildPending = false; rebuild() }
        save?.active = invalid.isEmpty()
        saveWithoutClosing?.active = invalid.isEmpty()
    }

    private fun position(n: PlottingNode) = (left + panX + n.x * cell) to (top + panY + n.y * cell)
    private fun port(n: PlottingNode, index: Int): Pair<Double, Double> {
        val (x, y) = position(n)
        val (dx, dy) = n.direction(index)
        return (x + (32 + dx * 32) * zoom) to (y + (32 + dy * 32) * zoom)
    }
    private fun canvas(x: Double, y: Double) = x >= left && x < right && y >= top && y < bottom
    private fun grid(x: Double, y: Double) = floor((x - left - panX) / cell).toInt() to floor((y - top - panY) / cell).toInt()
    private fun showPorts(n: PlottingNode, mouseX: Double, mouseY: Double): Boolean {
        if (dropdown != null || contextMenu != null) return false
        if (wire != null) return true
        if (!canvas(mouseX, mouseY)) return false
        val (x, y) = position(n)
        val margin = max(1.5, 5 * zoom)
        return mouseX in x - margin..x + cell + margin && mouseY in y - margin..y + cell + margin
    }
    private fun portAt(x: Double, y: Double, preferInput: Boolean = wire != null): Pair<PlottingNode, Int>? {
        val hits = mutableListOf<Pair<PlottingNode, Int>>()
        for (n in plan.nodes.asReversed()) for (index in -1 until n.inputCount) {
            if (!showPorts(n, x, y)) continue
            if (index == -1 && !n.hasOutput) continue
            val (px, py) = port(n, index)
            val radius = max(1.5, 5 * zoom)
            if (abs(x - px) <= radius && abs(y - py) <= radius) hits.add(n to index)
        }
        return hits.firstOrNull { (it.second >= 0) == preferInput } ?: hits.firstOrNull()
    }

    override fun renderWindow(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        renderBackgroundTexture(g)
        g.drawString(font, fit(title.string, 100), guiLeft + 7, guiTop + 9, 0xffe1d8be.toInt(), false)
        g.fill(left, top, right, bottom, 0xff1c292b.toInt())
        g.enableScissor(left, top, right, bottom)
        PlottingGridRenderer.draw(g, left, top, right, bottom, panX, panY, cell)
        val byId = plan.nodes.associateBy { it.id }
        val hovered = if (dropdown == null && contextMenu == null && canvas(mouseX.toDouble(), mouseY.toDouble())) {
            val (gx, gy) = grid(mouseX.toDouble(), mouseY.toDouble())
            plan.nodes.lastOrNull { it.x == gx && it.y == gy } ?: portAt(mouseX.toDouble(), mouseY.toDouble())?.first
        } else null
        val time = if (simulation.ticks == 0L && !playing) -1.0 else simulation.ticks + if (playing) (clock + partialTicks) / speed.toDouble() else 0.0
        wireRenderer.begin()
        for (n in plan.nodes) repeat(n.inputCount) { index ->
            val input = n.inputs[index]
            val source = byId[input.source] ?: return@repeat
            wireRenderer.add(port(source, -1), port(n, index), source.direction(-1), n.direction(index),
                input.connection, time, n.id * 3 + index, zoom, left, top, right, bottom, n.touches(source, index))
        }
        wire?.let { id -> byId[id]?.let { wireRenderer.add(port(it, -1), mouseX.toDouble() to mouseY.toDouble(),
            it.direction(-1), -1 to 0, PlottingConnection.THINKABLE, -1.0, -1, zoom, left, top, right, bottom) } }
        wireRenderer.draw(g, zoom)
        for (n in plan.nodes) {
            val (x, y) = position(n)
            if (x + cell < left || x > right || y + cell < top || y > bottom) continue
            val state = model(n)
            if (state != null) {
                GuiGameElement.of(state).rotateBlock(90.0, -90.0, -n.rotation * 90.0)
                    .at<GuiGameElement.GuiRenderBuilder>(x.toFloat(), (y + cell).toFloat(), 0f)
                    .scale(cell).render(g)
            } else if (n.kind == PlottingKind.CONSTANT) {
                renderConstant(g, x, y)
            } else {
                g.pose().pushPose()
                g.pose().translate(x, y, 0.0)
                g.pose().scale((4 * zoom).toFloat(), (4 * zoom).toFloat(), 1f)
                g.renderItem(Items.COMPARATOR.defaultInstance, 0, 0)
                g.pose().popPose()
            }
            g.pose().pushPose()
            g.pose().translate(0.0, 0.0, 220.0)
            val value = if (n.kind == PlottingKind.INPUT || n.kind == PlottingKind.CONSTANT) simulation.inputValue(n) else simulation.values[n.id] ?: 0.0
            if (selected == n.id) g.renderOutline(x.toInt(), y.toInt(), cell.toInt(), cell.toInt(), 0xffe6bd6b.toInt())
            if (zoom >= 0.6) {
                g.fill((x + 7 * zoom).toInt(), (y + 57 * zoom - 10).toInt(), (x + 57 * zoom).toInt(), (y + 57 * zoom).toInt(), 0xc0202828.toInt())
                g.drawString(font, fit(format(value), (48 * zoom).toInt()), (x + 8 * zoom).toInt(), (y + 57 * zoom - 9).toInt(), if (value.isFinite()) 0xfff0e4bd.toInt() else 0xffff7777.toInt(), false)
            }
            for (index in -1 until n.inputCount) {
                if (!showPorts(n, mouseX.toDouble(), mouseY.toDouble())) continue
                if (index == -1 && !n.hasOutput) continue
                val (px, py) = port(n, index)
                PlottingWireRenderer.socket(g, px to py, zoom)
                val wireless = if (index >= 0) n.inputs[index].connection == PlottingConnection.LINK && n.inputs[index].source != null
                    else plan.nodes.any { sink -> sink.inputs.take(sink.inputCount).any { it.source == n.id && it.connection == PlottingConnection.LINK } }
                if (wireless) {
                    g.pose().pushPose()
                    g.pose().translate(px - 8 * zoom, py - 8 * zoom, 0.0)
                    g.pose().scale(zoom.toFloat(), zoom.toFloat(), 1f)
                    g.renderItem(AllBlocks.REDSTONE_LINK.asStack(), 0, 0)
                    g.pose().popPose()
                }
                if (zoom >= 0.6 && (selected == n.id || hovered?.id == n.id || wire != null)) {
                    val (dx, dy) = n.direction(index)
                    g.drawString(font, if (index == -1) ">" else "${'A' + index}", (px - dx * 10 * zoom - 2).toInt(), (py - dy * 10 * zoom - 4).toInt(), 0xfff3e3bb.toInt(), true)
                }
            }
            g.pose().popPose()
        }
        hovered?.let { n ->
            val (x, y) = position(n)
            val name = n.name.ifBlank {
                if (n.kind == PlottingKind.FUNCTION) functionName(n).string
                else if (n.device.isNotEmpty()) deviceName(n).string else tr("kind.${n.kind.name.lowercase()}").string
            }
            val label = fit(name, right - left - 8)
            val labelX = (x + (cell - font.width(label)) / 2).toInt().coerceIn(left + 4, max(left + 4, right - font.width(label) - 4))
            g.pose().pushPose()
            g.pose().translate(0.0, 0.0, 230.0)
            g.drawString(font, label, labelX, max(top, y.toInt() - 11), 0xfff3e3bb.toInt(), true)
            g.pose().popPose()
        }
        if ((placing != null || dragging != null) && canvas(mouseX.toDouble(), mouseY.toDouble())) {
            val (cx, cy) = grid(mouseX.toDouble(), mouseY.toDouble())
            val px = (left + panX + cx * cell).toInt()
            val py = (top + panY + cy * cell).toInt()
            val size = (64 * zoom).toInt()
            val free = cx in -64..64 && cy in -64..64 && plan.nodes.none { it.x == cx && it.y == cy && it.id != dragging }
            val color = if (free) 0xffa5ce89.toInt() else 0xffe17d76.toInt()
            g.renderOutline(px, py, size, size, color)
        }
        g.disableScissor()
        labels.forEach { (x, y, text) -> g.drawString(font, fit(text.string, 150), guiLeft + x, guiTop + y, 0xffe1d8be.toInt(), false) }
        val status = when {
            invalid.isNotEmpty() -> tr("invalid")
            fileMessage != null -> fileMessage!!
            wire != null -> tr("wiring", wire!!)
            placing != null -> tr("place")
            else -> tr("status", simulation.ticks, if (playing) tr("play").string else tr("pause").string)
        }
        g.drawString(font, fit(status.string, windowWidth - 57), guiLeft + 6, guiTop + windowHeight - 16, 0xff404040.toInt(), false)
        if (dropdown == null && contextMenu == null && canvas(mouseX.toDouble(), mouseY.toDouble())) portAt(mouseX.toDouble(), mouseY.toDouble())?.let { (hover, index) ->
            g.renderTooltip(font, if (index == -1) tr("output_port", hover.id) else tr("input_port", hover.id, "${'A' + index}"), mouseX, mouseY)
        }
    }

    override fun renderWindowForeground(g: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        dropdown?.render(g, mouseX, mouseY, partialTicks)
        contextMenu?.render(g, font, mouseX, mouseY)
    }

    private fun renderConstant(g: GuiGraphics, x: Double, y: Double) {
        g.pose().pushPose()
        g.pose().translate(x, y, 0.0)
        g.pose().scale((cell / 12).toFloat(), (cell / 12).toFloat(), 1f)
        g.blit(ITEM_FRAME_BACK, 1, 1, 3f, 3f, 10, 10, 16, 16)
        g.blit(ITEM_FRAME_WOOD, 0, 0, 2f, 2f, 12, 1, 16, 16)
        g.blit(ITEM_FRAME_WOOD, 0, 11, 2f, 13f, 12, 1, 16, 16)
        g.blit(ITEM_FRAME_WOOD, 0, 1, 2f, 3f, 1, 10, 16, 16)
        g.blit(ITEM_FRAME_WOOD, 11, 1, 13f, 3f, 1, 10, 16, 16)
        g.pose().translate(3.0, 3.0, 1.0)
        g.pose().scale(0.375f, 0.375f, 1f)
        g.renderItem(Items.PAPER.defaultInstance, 0, 0)
        g.pose().popPose()
    }

    private fun renderBackgroundTexture(g: GuiGraphics) {
        val sourceX = intArrayOf(0, 32, 462, 628)
        val sourceY = intArrayOf(0, 51, 363, 388)
        val targetX = intArrayOf(0, 32, windowWidth - 166, windowWidth)
        val targetY = intArrayOf(0, 51, windowHeight - 25, windowHeight)
        for (column in 0..2) for (row in 0..2) {
            val tileWidth = sourceX[column + 1] - sourceX[column]
            val tileHeight = sourceY[row + 1] - sourceY[row]
            var y = targetY[row]
            while (y < targetY[row + 1]) {
                val h = minOf(tileHeight, targetY[row + 1] - y)
                var x = targetX[column]
                while (x < targetX[column + 1]) {
                    val w = minOf(tileWidth, targetX[column + 1] - x)
                    g.blit(BACKGROUND, guiLeft + x, guiTop + y, sourceX[column].toFloat(), sourceY[row].toFloat(), w, h, 628, 388)
                    x += w
                }
                y += h
            }
        }
    }

    private fun model(n: PlottingNode): BlockState? = if (n.kind == PlottingKind.FUNCTION)
        ClockworkBlocks.COMBOBULATOR.defaultState.setValue(CombobulatorBlock.FUNCTION_GROUP, n.operation.function.group)
    else ResourceLocation.tryParse(n.device)?.takeIf { BuiltInRegistries.BLOCK.containsKey(it) }
        ?.let { BuiltInRegistries.BLOCK.get(it).defaultBlockState() }

    private fun deviceName(n: PlottingNode): Component = if (n.device.isEmpty()) tr("device.generic")
        else ResourceLocation.tryParse(n.device)?.takeIf { BuiltInRegistries.BLOCK.containsKey(it) }
            ?.let { BuiltInRegistries.BLOCK.get(it).name } ?: Component.literal(n.device)

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        contextMenu?.let { return it.mouseClicked(mouseX, mouseY, button) }
        dropdown?.let { return it.mouseClicked(mouseX, mouseY, button) }
        if (!canvas(mouseX, mouseY)) return super.mouseClicked(mouseX, mouseY, button)
        setFocused(null)
        if (button == 1) { rightClick = mouseX to mouseY; return true }
        val hit = portAt(mouseX, mouseY)
        if (hit != null && button == 0 && placing == null) {
            val (n, index) = hit
            if (index == -1) { wire = n.id; playing = false }
            else if (wire != null) bind(n.id, index, wire)
            else { selected = n.id; selectedInput = max(0, index); ioTab = index >= 0; rebuildPending = true }
            return true
        }
        if (button == 2) { panning = true; wire = null; placing = null; return true }
        if (button != 0) return true
        val (x, y) = grid(mouseX, mouseY)
        if (placing != null) {
            addNode(x, y, placing!!)
            return true
        }
        selected = plan.nodes.lastOrNull { it.x == x && it.y == y }?.id
        inspectorScroll = 0
        dragging = selected
        rebuildPending = true
        return true
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, dragX: Double, dragY: Double): Boolean {
        if (dropdown != null || contextMenu != null) return true
        rightClick?.let { (x, y) ->
            if (button == 1 && (abs(mouseX - x) > 3 || abs(mouseY - y) > 3)) {
                rightClick = null; panning = true; wire = null; placing = null; dragging = null
                panX += mouseX - x; panY += mouseY - y
            }
            return true
        }
        if (panning) { panX += dragX; panY += dragY; return true }
        if (dragging != null) return true
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (dropdown != null || contextMenu != null) return true
        if (button == 1) rightClick?.let { (x, y) ->
            rightClick = null
            if (canvas(mouseX, mouseY)) openContextMenu(x, y)
            return true
        }
        if (panning) { panning = false; return true }
        val id = dragging
        dragging = null
        if (id != null && canvas(mouseX, mouseY)) {
            val (x, y) = grid(mouseX, mouseY)
            if (x in -64..64 && y in -64..64 && plan.nodes.none { it.id != id && it.x == x && it.y == y }) {
                val n = node
                if (n != null && (n.x != x || n.y != y)) {
                    update(id, reset = false) { it.copy(x = x, y = y) }
                    editSound(true)
                }
            }
            return true
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
        contextMenu?.let { return it.scroll(delta) }
        if (rightClick != null) return true
        dropdown?.let { return it.scroll(delta) }
        if (mouseX >= guiLeft + inspector && mouseX < guiLeft + windowWidth && mouseY >= top && mouseY < bottom) {
            inspectorScroll = (inspectorScroll - (delta * 20).toInt()).coerceIn(0, if (node == null) 0 else max(0, 314 - windowHeight))
            rebuildPending = true
            return true
        }
        if (!canvas(mouseX, mouseY)) return super.mouseScrolled(mouseX, mouseY, delta)
        val next = (zoom * 1.15.pow(delta)).coerceIn(0.1, 1.6)
        panX = mouseX - left - (mouseX - left - panX) * next / zoom
        panY = mouseY - top - (mouseY - top - panY) * next / zoom
        zoom = next
        return true
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        contextMenu?.let { return it.keyPressed(keyCode) }
        if (rightClick != null && keyCode == GLFW.GLFW_KEY_ESCAPE) { rightClick = null; return true }
        dropdown?.let { return it.keyPressed(keyCode, scanCode, modifiers) }
        if (focused !is EditBox) {
            if (keyCode == GLFW.GLFW_KEY_DELETE) { removeSelected(); return true }
            if (hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) { history(undo, redo); return true }
            if (hasControlDown() && keyCode == GLFW.GLFW_KEY_Y) { history(redo, undo); return true }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE && (wire != null || placing != null)) { wire = null; placing = null; return true }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    override fun charTyped(character: Char, modifiers: Int): Boolean = if (contextMenu != null) true else dropdown?.charTyped(character, modifiers)
        ?: super.charTyped(character, modifiers)

    override fun onClose() {
        if (!closing && documents.any { it.dirty }) {
            playing = false
            minecraft?.setScreen(ConfirmScreen({ discard ->
                if (discard) { closing = true; minecraft?.setScreen(null) } else minecraft?.setScreen(this)
            }, tr("discard"), tr("discard_hint")))
        } else super.onClose()
    }

    private fun loadDocuments(): MutableList<PlottingDocument> {
        return try {
            val library = files.load()
            fileMessage = library.errors.takeIf { it.isNotEmpty() }?.let { tr("load_errors", it.joinToString("; ")) }
            library.documents.toMutableList().apply { if (isEmpty()) add(PlottingDocument(PlottingPlan(), saved = PlottingPlan().write())) }
        } catch (e: Exception) {
            fileMessage = tr("file_error", e.message ?: e.javaClass.simpleName)
            mutableListOf(PlottingDocument(PlottingPlan(), saved = PlottingPlan().write()))
        }
    }

    private fun reloadDocuments() {
        val loaded = loadDocuments()
        documents.clear()
        documents.addAll(loaded)
        activePlan = 0
        changePlan()
    }

    private fun deletePlan() {
        playing = false
        minecraft?.setScreen(ConfirmScreen({ confirm ->
            if (confirm) {
                fileAction {
                    files.trash(documents[activePlan])
                    documents.removeAt(activePlan)
                    if (documents.isEmpty()) documents.add(PlottingDocument(PlottingPlan()))
                    activePlan = activePlan.coerceAtMost(documents.lastIndex)
                    changePlan()
                }
            }
            minecraft?.setScreen(this)
        }, tr("delete_plan"), tr("trash_confirm", plan.name)))
    }

    private fun savePlans(close: Boolean) {
        if (invalid.isNotEmpty()) return
        fileAction {
            documents.indices.forEach { index -> documents[index] = files.save(documents[index]) }
            if (close) {
                closing = true
                super.onClose()
            } else rebuildPending = true
        }
    }

    private fun fileAction(action: () -> Unit) {
        try { action(); fileMessage = null }
        catch (e: Exception) { fileMessage = tr("file_error", e.message ?: e.javaClass.simpleName) }
    }

    private fun editSound(drawing: Boolean) {
        sketchSound?.let { minecraft?.soundManager?.stop(it) }
        val started = Util.getMillis()
        val duration = if (drawing) 350L else 450L
        sketchSound = VdexSketchSound(drawing) {
            val elapsed = Util.getMillis() - started
            if (minecraft?.screen !== this || elapsed >= duration) null
            else minOf(elapsed / 40f, (duration - elapsed) / 100f, 1f).coerceAtLeast(0f)
        }.also { minecraft?.soundManager?.play(it) }
    }

    override fun removed() {
        sketchSound?.let { minecraft?.soundManager?.stop(it) }
        sketchSound = null
        super.removed()
    }

    private fun fit(text: String, width: Int) = font.plainSubstrByWidth(text, width.coerceAtLeast(0))
    private fun functionName(n: PlottingNode) = Component.translatable("gui.vs_clockwork.combobulator.function.${n.operation.function.name.lowercase()}")

    companion object {
        private val ITEM_FRAME_BACK = ResourceLocation("minecraft", "textures/block/item_frame.png")
        private val ITEM_FRAME_WOOD = ResourceLocation("minecraft", "textures/block/birch_planks.png")
        private val BACKGROUND = ResourceLocation("vs_clockwork", "textures/gui/plotting_table.png")
        private fun tr(key: String, vararg args: Any): Component = Component.translatable("gui.vs_clockwork.plotting.$key", *args)
        private fun format(value: Double): String = if (!value.isFinite()) "Unavailable" else
            java.math.BigDecimal.valueOf(value).stripTrailingZeros().toPlainString().let {
                if (it.length <= 10) it else String.format(Locale.ROOT, "%.4g", value)
            }
    }
}
