package org.valkyrienskies.clockwork.content.logistics.gas.filter

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.gui.AbstractSimiScreen
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.widget.IconButton
import com.simibubi.create.foundation.gui.widget.ScrollInput
import com.simibubi.create.foundation.utility.Components
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TextComponent
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.gui.ScrollingFrame

class FilterScreen(private val nodeA: DuctNodePos, private val nodeB: DuctNodePos, private val filter: HashSet<GasType>, private val blacklist: Boolean) : AbstractSimiScreen()  {

    private val tab: ClockworkGuiTextures = ClockworkGuiTextures.GAS_FILTER_TAB;
    private val frame: ClockworkGuiTextures = ClockworkGuiTextures.GAS_FILTER_FRAME;

    val scrollingElements: MutableList<ScrollingFrame.ScrollingElement> = mutableListOf()
    lateinit var scrollingFrame: FilterScrolling


    fun updateIcon(button: IconButton, gasType: GasType, Added: Boolean?=null) {
        if (Added==true || gasType in filter) {
            button.setIcon(AllIcons.I_CONFIRM)
            button.setToolTip(Components.translatable("Added"))
        }
        else {
            button.setIcon(AllIcons.I_NONE)
            button.setToolTip(Components.translatable("Removed"))
        }
    }

    override fun init() {
        setWindowSize(tab.width, tab.height)
        super.init()

        scrollingFrame = FilterScrolling(guiLeft+3, guiTop+20)
        for (type in GasType.entries) {

            val button = IconButton(0,0, AllIcons.I_ADD)
            button.withCallback<IconButton> { _, _ ->
                if (type in filter) {
                    filter.remove(type)
                    updateIcon(button, type, false)
                }
                else {
                    filter.add(type)
                    updateIcon(button, type, true)
                }
            }
            updateIcon(button, type)

            scrollingElements.add(FilterScrolling.FilterScrollingElement(type, font, button))
        }

        scrollingFrame.scrollingElements = scrollingElements
        addRenderableWidget(scrollingFrame)
//
//        temperatureInput = ScrollInput(guiLeft + 82,guiTop + 89, 51, 18)
//        temperatureInput.withRange(0,4500)
//        temperatureInput.calling { state: Int -> be.temperature = state.toDouble() }
//        addRenderableWidget(temperatureInput)
    }


    override fun renderWindowBackground(ms: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        frame.render(ms,guiLeft, guiTop+20)

    }

    override fun renderWindow(ms: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) { }

    override fun renderWindowForeground(ms: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        tab.render(ms, guiLeft, guiTop)

    }

    override fun onClose() {
        ClockworkPackets.sendToServer(FilterClosePacket(nodeA, nodeB, filter, blacklist))
        super.onClose()
    }
}