package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.gui.AbstractSimiScreen
import com.simibubi.create.foundation.gui.widget.AbstractSimiWidget
import com.simibubi.create.foundation.gui.widget.ScrollInput
import net.minecraft.client.gui.components.AbstractWidget
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.content.contraptions.phys.altmeter.AltMeterBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.clockwork.util.gui.ScrollingFrame

class CreativeGeneratorScreen(private val be: CreativeGeneratorBlockEntity) : AbstractSimiScreen()  {

    private val background: ClockworkGuiTextures = ClockworkGuiTextures.CREATIVE_GAS_GENERATOR;
    private val frame: ClockworkGuiTextures = ClockworkGuiTextures.CREATIVE_GAS_GENERATOR_FRAME;

    val scrollingElements: MutableList<ScrollingFrame.ScrollingElement> = mutableListOf()
    lateinit var scrollingFrame: CreativeGeneratorScrolling


    override fun init() {
        setWindowSize(background.width, background.height)
        super.init()

        scrollingFrame = CreativeGeneratorScrolling(guiLeft+3, guiTop+16)

        for (type in GasType.values()) {

            val input = ScrollInput(0,0,159, 13)
            input.calling { state: Int -> stateChange(type, state) }
            input.withRange(0,1000)

            scrollingElements.add(CreativeGeneratorScrolling.CreativeGeneratorScrollingElement(input))
        }

        scrollingFrame.maxScroll = 1000.0
        scrollingFrame.minScroll = -1000.0
        scrollingFrame.scrollingElements = scrollingElements



        addRenderableWidget(scrollingFrame)
    }

    fun stateChange(gas: GasType, state: Int) {
        print(gas.name)
        print(" ")
        println(state)
    }

    override fun renderWindowBackground(ms: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) {

        frame.render(ms,guiLeft, guiTop)

    }

    override fun renderWindow(ms: PoseStack?, mouseX: Int, mouseY: Int, partialTicks: Float) { }

    override fun renderWindowForeground(ms: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        background.render(ms, guiLeft, guiTop)
    }


}