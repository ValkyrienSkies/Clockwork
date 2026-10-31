package org.valkyrienskies.clockwork.content.logistics.gas.smart

import com.simibubi.create.AllSoundEvents
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.widget.IconButton
import com.simibubi.create.foundation.gui.widget.ScrollInput
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput
import net.createmod.catnip.gui.AbstractSimiScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.resources.sounds.SimpleSoundInstance
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.util.gui.DuctTextUtil
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.SmartEdge.FilterType
import kotlin.math.sign


class SmartDuctScreen(
    private val nodeA: DuctNodePos, private val nodeB: DuctNodePos, private val settings: SmartDuctSettings,
) : AbstractSimiScreen(Component.translatable("vs_clockwork.gui.smart_duct.title")) {
    private val types = listOf(FilterType.PRESSURE, FilterType.TEMPERATURE, FilterType.HEAT_ENERGY, FilterType.MASS)

    override fun init() {
        setWindowSize(214, 104)
        super.init()

        val typeInput = SelectionScrollInput(guiLeft + 8, guiTop + 23, 100, 18)
        typeInput.forOptions(types.map { typeName(it) })
            .setState(types.indexOf(settings.filter).coerceAtLeast(0))
            .titled(Component.translatable("vs_clockwork.gui.smart_duct.measurement"))
            .calling {
                val oldStep = baseStep(settings.filter)
                settings.filter = types[it]
                settings.comparisonValue *= baseStep(settings.filter) / oldStep
            }
        addRenderableWidget(typeInput)

        val comparisonInput = SelectionScrollInput(guiLeft + 112, guiTop + 23, 94, 18)
        comparisonInput.forOptions(listOf(comparisonName(true), comparisonName(false)))
            .setState(if (settings.moreThan) 0 else 1)
            .titled(Component.translatable("vs_clockwork.gui.smart_duct.comparison"))
            .calling { settings.moreThan = it == 0 }
        addRenderableWidget(comparisonInput)

        // Keep the actual Double instead of quantizing it through ScrollInput's integer state.
        val valueInput = object : ScrollInput(guiLeft + 8, guiTop + 45, 160, 18) {
            override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
                if (delta == 0.0) return false
                val step = baseStep(settings.filter)
                val multiplier = if (hasControlDown()) 100.0 else if (hasShiftDown()) 10.0 else 1.0
                val value = (settings.comparisonValue + sign(delta) * step * multiplier).coerceAtLeast(0.0)
                if (!value.isFinite() || value == settings.comparisonValue) return false
                settings.comparisonValue = value
                minecraft?.soundManager?.play(SimpleSoundInstance.forUI(AllSoundEvents.SCROLL_VALUE.mainEvent, 1.5f))
                return true
            }
        }
        valueInput.titled(Component.translatable("vs_clockwork.gui.smart_duct.threshold"))
            .addHint(Component.translatable("vs_clockwork.gui.smart_duct.scroll_hint"))
        addRenderableWidget(valueInput)

        val filterButton = IconButton(guiLeft + 180, guiTop + 45, AllIcons.I_WHITELIST)
        filterButton.setToolTip(Component.translatable("vs_clockwork.gui.smart_duct.gas_filter"))
        filterButton.withCallback<IconButton> {
            minecraft?.setScreen(SmartGasFilterScreen(this, settings))
        }
        addRenderableWidget(filterButton)

        val confirm = IconButton(guiLeft + 181, guiTop + 80, AllIcons.I_CONFIRM)
        confirm.setToolTip(Component.translatable("gui.done"))
        confirm.withCallback<IconButton> { onClose() }
        addRenderableWidget(confirm)
    }

    override fun renderWindow(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        renderPanel(graphics, guiLeft, guiTop)
        graphics.drawCenteredString(font, title, guiLeft + 103, guiTop + 6, 0xFFFFFF)
        field(graphics, 8, 23, 100)
        field(graphics, 112, 23, 88)
        field(graphics, 8, 45, 160)
        graphics.drawString(font, typeName(settings.filter), guiLeft + 12, guiTop + 28, 0xFFFFFF)
        graphics.drawString(font, comparisonName(settings.moreThan), guiLeft + 116, guiTop + 28, 0xFFFFFF)
        graphics.drawString(font, formattedValue(), guiLeft + 12, guiTop + 50, 0xFFFFFF)
    }

    private fun field(graphics: GuiGraphics, x: Int, y: Int, width: Int) {
        graphics.fill(guiLeft + x, guiTop + y, guiLeft + x + width, guiTop + y + 18, 0xFF49443C.toInt())
    }

    private fun formattedValue(): Component {
        val builder = ClockworkLang.builder()
        val value = settings.comparisonValue
        return when (settings.filter) {
            FilterType.PRESSURE -> DuctTextUtil.translatePressure(builder, value, true)
            FilterType.TEMPERATURE -> DuctTextUtil.translateTemperature(builder, value, true)
            FilterType.HEAT_ENERGY -> DuctTextUtil.translateEnergy(builder, value, true)
            FilterType.MASS -> DuctTextUtil.translateMass(builder, value, true)
            FilterType.NONE -> builder
        }.component()
    }

    override fun onClose() {
        ClockworkPackets.sendToServer(SmartScreenClosePacket(nodeA, nodeB, settings))
        super.onClose()
    }

    companion object {
        fun renderPanel(graphics: GuiGraphics, x: Int, y: Int) {
            ClockworkGuiTextures.GAS_FILTER_FRAME.render(graphics, x, y + 20)
            ClockworkGuiTextures.GAS_FILTER_TAB.render(graphics, x, y)
        }

        private fun typeName(type: FilterType) =
            Component.translatable("vs_clockwork.gui.smart_duct." + type.name.lowercase(java.util.Locale.ROOT))

        private fun comparisonName(moreThan: Boolean) =
            Component.translatable("vs_clockwork.gui.smart_duct." + if (moreThan) "more_than" else "less_than")

        private fun baseStep(type: FilterType) = when (type) {
            FilterType.PRESSURE, FilterType.HEAT_ENERGY -> 1000.0
            FilterType.MASS -> 0.1
            else -> 1.0
        }
    }
}
