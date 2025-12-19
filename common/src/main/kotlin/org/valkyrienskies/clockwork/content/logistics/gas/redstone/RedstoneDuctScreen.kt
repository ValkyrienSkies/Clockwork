package org.valkyrienskies.clockwork.content.logistics.gas.redstone

import com.simibubi.create.foundation.gui.AllGuiTextures
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.widget.IconButton
import net.createmod.catnip.gui.AbstractSimiScreen
import net.createmod.catnip.gui.element.GuiGameElement
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.AbstractWidget
import net.minecraft.client.gui.narration.NarrationElementOutput
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import org.valkyrienskies.clockwork.ClockworkLang
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry

/**
 * Client-only configuration screen for [RedstoneDuctBlockEntity].
 */
class RedstoneDuctScreen(private val blockEntity: RedstoneDuctBlockEntity) :
    AbstractSimiScreen(Component.translatable("gui.clockwork.redstone_duct")) {


    private var conditional: RedstoneDuctConditional =
        blockEntity.conditional ?: RedstoneDuctConditional(
            RedstoneDuctConditional.ConditionalType.NONE,
            moreThan = true,
            comparisonValue = 0.0
        )

    private val gasFilter: MutableSet<GasType> = conditional.filter.toMutableSet()

    private lateinit var typeButton: IconButton
    private lateinit var compareModeButton: IconButton
    private lateinit var gasFilterButton: IconButton
    private lateinit var gasFilterWidget: GasFilterWidget

    override fun init() {
        super.init()

        clearWidgets()
        initHeader()
        initGasFilterWidget()
    }

    private fun initHeader() {
        typeButton = IconButton(leftPos + 20, topPos + 22, AllIcons.I_CLEAR)
            .withCallback { conditional.type = conditional.type.next() }

        compareModeButton = IconButton(leftPos + 150, topPos + 22, AllIcons.I_MTD_LEFT)
            .withCallback { conditional.moreThan = !conditional.moreThan }

        gasFilterButton = IconButton(leftPos + 20, topPos + 50, AllIcons.I_CONFIG_OPEN)
            .withCallback { gasFilterWidget.toggle() }

        addRenderableWidget(typeButton)
        addRenderableWidget(compareModeButton)
        addRenderableWidget(gasFilterButton)
    }

    private fun initGasFilterWidget() {
        gasFilterWidget = GasFilterWidget(
            leftPos + 10,
            topPos + 70,
            gasFilter
        )
        addRenderableWidget(gasFilterWidget)
    }

    override fun onClose() {
        blockEntity.conditional = conditional
        super.onClose()
    }

    override fun renderBackground(guiGraphics: GuiGraphics) {
        AllGuiTextures.SCHEDULE.render(guiGraphics, leftPos, topPos)
    }

    override fun render(guiGraphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        super.render(guiGraphics, mouseX, mouseY, partialTicks)

        val font = minecraft!!.font
        guiGraphics.drawString(font, title, leftPos + 8, topPos + 6, 0xFFFFFF, false)

        guiGraphics.drawString(
            font,
            ClockworkLang.translate("gui.clockwork.conditional_type", conditional.type.name.lowercase()).string(),
            leftPos + 20,
            topPos + 40,
            0xFFFFFF,
            false
        )
    }

    override fun renderWindow(
        graphics: GuiGraphics,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float
    ) {

    }
}

