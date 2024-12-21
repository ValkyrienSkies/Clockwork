package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.gui.AbstractSimiScreen
import com.simibubi.create.foundation.gui.widget.ScrollInput
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.gui.ScrollingFrame
import org.valkyrienskies.kelvin.impl.GasTypeRegistry

class CreativeGeneratorScreen(private val be: CreativeGeneratorBlockEntity) : AbstractSimiScreen()  {

    private val background: ClockworkGuiTextures = ClockworkGuiTextures.CREATIVE_GAS_GENERATOR;
    private val frame: ClockworkGuiTextures = ClockworkGuiTextures.CREATIVE_GAS_GENERATOR_FRAME;

    val scrollingElements: MutableList<ScrollingFrame.ScrollingElement> = mutableListOf()
    lateinit var scrollingFrame: CreativeGeneratorScrolling

    lateinit var temperatureInput: ScrollInput

    override fun init() {
        setWindowSize(background.width, background.height)
        super.init()

        scrollingFrame = CreativeGeneratorScrolling(guiLeft+3, guiTop+16)
        for (type in GasTypeRegistry.GAS_TYPES.values) {

            val input = ScrollInput(0,0,51, 18)
            input.calling { state: Int -> be.gasValues[type] = state }
            input.withRange(0,1000)
            input.state = be.gasValues[type] ?: 0

            scrollingElements.add(CreativeGeneratorScrolling.CreativeGeneratorScrollingElement(type, font, input))
        }

        scrollingFrame.scrollingElements = scrollingElements
        addRenderableWidget(scrollingFrame)

        temperatureInput = ScrollInput(guiLeft + 82,guiTop + 89, 51, 18)
        temperatureInput.withRange(0,4500)
        temperatureInput.calling { state: Int -> be.temperature = state.toDouble() }
        addRenderableWidget(temperatureInput)
    }


    override fun renderWindowBackground(ms: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) {

        frame.render(ms,guiLeft, guiTop)

    }

    override fun renderWindow(ms: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) { }

    override fun renderWindowForeground(ms: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        background.render(ms, guiLeft, guiTop)

        drawString(ms, font, "Temperature", guiLeft+8, guiTop+93,0xFFFFFF)
        drawString(ms, font, be.temperature.toInt().toString()+" K", guiLeft+82, guiTop+93,0xFFFFFF)
    }

    override fun onClose() {
        ClockworkPackets.sendToServer(CreativeGeneratorPacket(be.gasValues, be.temperature, be.blockPos))
        super.onClose()
    }
}