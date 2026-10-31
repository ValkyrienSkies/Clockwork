package org.valkyrienskies.clockwork.content.logistics.gas.smart

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.widget.IconButton
import com.simibubi.create.foundation.gui.widget.ScrollInput
import net.createmod.catnip.gui.AbstractSimiScreen
import net.minecraft.client.gui.GuiGraphics
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.content.logistics.gas.filter.FilterClosePacket
import org.valkyrienskies.clockwork.util.gui.GuiUtil
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.api.edges.SmartEdge.FilterType
import kotlin.collections.set
import kotlin.math.roundToInt

class SmartDuctScreen(val nodeA: DuctNodePos, val nodeB: DuctNodePos, var filter: FilterType, var comparisonValue: Double, var moreThan: Boolean): AbstractSimiScreen() {

    private val bg: ClockworkGuiTextures = ClockworkGuiTextures.SMART_DUCT_BG;

    lateinit private var filterTypeButton: IconButton
    lateinit private var moreThanButton: IconButton
    lateinit private var scrollInput: ScrollInput

    fun updateFilterButton() {
        val icon = when (filter) {
            FilterType.NONE -> AllIcons.I_NONE
            FilterType.PRESSURE -> AllIcons.I_PLAY
            FilterType.TEMPERATURE -> AllIcons.I_CLEAR
        }
        filterTypeButton.setIcon(icon)
    }

    fun updateMoreThanButton() {
        val icon = when (moreThan) {
            true -> AllIcons.I_MTD_RIGHT
            false -> AllIcons.I_MTD_LEFT
        }

        moreThanButton.setIcon(icon)
    }

    override fun init() {
        setWindowSize(bg.width, bg.height)
        super.init()

        // #### Filter Button
        filterTypeButton = IconButton(guiLeft+120, guiTop+17, AllIcons.I_PLAY)
        updateFilterButton()
        filterTypeButton.withCallback<IconButton> { _,_ ->
            filter = when (filter) {
                FilterType.NONE -> FilterType.PRESSURE
                FilterType.PRESSURE -> FilterType.TEMPERATURE
                FilterType.TEMPERATURE -> FilterType.NONE
            }
            updateFilterButton()
        }
        addWidget(filterTypeButton)

        // #### More than Button
        moreThanButton = IconButton(guiLeft+140, guiTop+17, AllIcons.I_MTD_LEFT)
        updateMoreThanButton()
        moreThanButton.withCallback<IconButton> { _, _ ->
            moreThan = !moreThan
            updateMoreThanButton()
        }
        addWidget(moreThanButton)

        // #### Scroll Input
        scrollInput = ScrollInput(guiLeft+9,guiTop+19,106, 18)
        scrollInput.calling { state: Int -> comparisonValue = state.toDouble() }
        scrollInput.withRange(0,99999)
        scrollInput.withStepFunction{  c: ScrollValueBehaviour.StepContext ->
            if (filter == FilterType.NONE) 0 else if (c.control) 100 else if (c.shift) 10 else 1 }
        scrollInput.state = comparisonValue.toInt()
        addWidget(scrollInput)
    }

    override fun renderWindow(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        bg.render(graphics, guiLeft, guiTop)
        filterTypeButton.render(graphics, mouseX, mouseY, partialTicks)
        moreThanButton.render(graphics, mouseX, mouseY, partialTicks)

        val symbol = when(filter) {
            FilterType.NONE -> ""
            FilterType.PRESSURE -> " kPa"
            FilterType.TEMPERATURE -> " K"
        }

        val value = when (filter) {
            FilterType.NONE -> "-"
            else -> comparisonValue.toInt()
        }.toString()

        graphics.drawString(font, value + symbol, guiLeft+12, guiTop+22, 0xFFFFFF)
    }

    override fun onClose() {
        ClockworkPackets.sendToServer(SmartScreenClosePacket(nodeA, nodeB, filter, comparisonValue, moreThan))
        super.onClose()
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {


        return super.mouseClicked(mouseX, mouseY, button)
    }
}
