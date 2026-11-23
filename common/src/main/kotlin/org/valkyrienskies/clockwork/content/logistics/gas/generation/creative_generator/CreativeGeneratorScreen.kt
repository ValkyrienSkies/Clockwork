package org.valkyrienskies.clockwork.content.logistics.gas.generation.creative_generator

import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.widget.IconButton
import net.createmod.catnip.gui.AbstractSimiScreen
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import org.valkyrienskies.clockwork.ClockworkGuiTextures
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry

class CreativeGeneratorScreen(private val be: CreativeGeneratorBlockEntity) : AbstractSimiScreen(Component.translatable("block.vs_clockwork.creative_gas_generator")) {
    private val gasValues = HashMap(GasTypeRegistry.GAS_TYPES.values.associateWith { (be.gasValues[it] ?: 0).coerceIn(0, MAX_GAS) })
    private var temperature = be.temperature.toInt().coerceIn(0, MAX_TEMPERATURE)
    private lateinit var gasList: CreativeGeneratorScrolling
    private lateinit var temperatureInput: CreativeGeneratorValueInput
    private lateinit var resetButton: IconButton

    override fun init() {
        val scroll = if (::gasList.isInitialized) gasList.scroll else 0.0
        setWindowSize(217, 157)
        super.init()

        gasList = CreativeGeneratorScrolling(guiLeft + 3, guiTop + 18, 204, 106, font, gasValues)
        gasList.restoreScroll(scroll)
        addRenderableWidget(gasList)

        temperatureInput = CreativeGeneratorValueInput(guiLeft + 98, guiTop + 133, MAX_TEMPERATURE, "K")
        temperatureInput.titled(tr("temperature"))
        temperatureInput.state = temperature
        temperatureInput.calling { temperature = it; updateResetButton() }
        addRenderableWidget(temperatureInput)

        resetButton = IconButton(guiLeft + 156, guiTop + 132, AllIcons.I_TRASH)
        resetButton.setToolTip(tr("reset"))
        resetButton.withCallback<IconButton> {
            gasList.reset()
            temperatureInput.state = 0
            temperatureInput.onChanged()
        }
        addRenderableWidget(resetButton)
        updateResetButton()

        val confirm = IconButton(guiLeft + 184, guiTop + 132, AllIcons.I_CONFIRM)
        confirm.setToolTip(Component.translatable("gui.done"))
        confirm.withCallback<IconButton> { onClose() }
        addRenderableWidget(confirm)
    }

    override fun tick() {
        super.tick()
        updateResetButton()
    }

    private fun updateResetButton() {
        resetButton.active = temperature != 0 || gasValues.values.any { it != 0 }
    }

    override fun renderWindow(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val background = ClockworkGuiTextures.CREATIVE_GAS_GENERATOR.location
        val frame = ClockworkGuiTextures.CREATIVE_GAS_GENERATOR_FRAME.location
        graphics.blitNineSliced(background, guiLeft, guiTop, 209, 15, 3, 165, 15, 0, 0)
        graphics.blitNineSliced(frame, guiLeft + 1, guiTop + 15, 207, 111, 2, 163, 66, 1, 15)
        for (dx in 0 until 203 step 158) {
            for (dy in 0 until 109 step 64) {
                graphics.blit(frame, guiLeft + 3 + dx, guiTop + 16 + dy, 3f, 16f,
                    minOf(158, 203 - dx), minOf(64, 109 - dy), 256, 256)
            }
        }
        graphics.blitNineSliced(background, guiLeft, guiTop + 127, 96, 29, 4, 80, 29, 0, 82)
        graphics.blit(background, guiLeft + 96, guiTop + 127, 80f, 82f, 54, 29, 256, 256)
        graphics.blit(background, guiLeft + 150, guiTop + 127, 134f, 82f, 28, 29, 256, 256)
        graphics.blit(background, guiLeft + 178, guiTop + 127, 134f, 82f, 39, 29, 256, 256)
        graphics.drawCenteredString(font, title, guiLeft + 104, guiTop + 4, 0xFFFFEE)
        graphics.drawString(font, tr("temperature"), guiLeft + 8, guiTop + 138, 0xFFFFEE)
    }

    override fun renderWindowForeground(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        gasList.renderHoverTooltip(graphics, mouseX, mouseY)
        if (temperatureInput.isMouseOver(mouseX.toDouble(), mouseY.toDouble())) {
            graphics.renderComponentTooltip(font, temperatureInput.valueTooltip(), mouseX, mouseY)
        }
    }

    override fun onClose() {
        ClockworkPackets.sendToServer(CreativeGeneratorPacket(HashMap(gasValues), temperature.toDouble(), be.blockPos))
        super.onClose()
    }

    companion object {
        const val MAX_GAS = 8000
        const val MAX_TEMPERATURE = 2000

        private fun tr(key: String, vararg args: Any) = Component.translatable("vs_clockwork.gui.creative_generator.$key", *args)
    }
}
