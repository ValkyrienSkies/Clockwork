package org.valkyrienskies.clockwork.content.logistics.gas.redstone

import com.mojang.blaze3d.systems.RenderSystem
import com.simibubi.create.foundation.gui.AllGuiTextures
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.ModularGuiLine
import com.simibubi.create.foundation.gui.ModularGuiLineBuilder
import com.simibubi.create.foundation.gui.widget.IconButton
import com.simibubi.create.foundation.gui.widget.Label
import com.simibubi.create.foundation.gui.widget.ScrollInput
import com.simibubi.create.foundation.gui.widget.SelectionScrollInput
import net.createmod.catnip.gui.AbstractSimiScreen
import net.createmod.catnip.gui.UIRenderHelper
import net.createmod.catnip.gui.widget.AbstractSimiWidget
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.components.ImageWidget
import net.minecraft.client.gui.components.events.GuiEventListener
import net.minecraft.network.chat.Component
import net.minecraft.util.Mth
import org.valkyrienskies.clockwork.content.logistics.gas.redstone.RedstoneDuctConditional.ConditionalType
import org.valkyrienskies.kelvin.api.GasType
import org.valkyrienskies.kelvin.impl.registry.GasTypeRegistry
import kotlin.math.ceil
import kotlin.math.max

class RedstoneDuctScreen(val be: RedstoneDuctBlockEntity) : AbstractSimiScreen(Component.translatable("block.clockwork.redstone_duct")) {

    // State
    private var type: ConditionalType = ConditionalType.NONE
    private var moreThan: Boolean = true
    private var comparisonValue: Double = 0.0
    private var filter: MutableList<GasType> = mutableListOf()
    private var filterBlacklist: Boolean = true

    // UI Helpers
    private lateinit var conditionalTypeScrollInput: SelectionScrollInput
    private lateinit var comparisonTypeScrollInput: SelectionScrollInput
    private lateinit var comparisonValueScrollInput: ScrollInput

    private lateinit var conditionalLabel: Label
    private lateinit var comparisonTypeLabel: Label
    private lateinit var comparisonValueLabel: Label

    private var filterWidget: GasFilterWidget? = null
    private var filterOpen = false


    init {
        // Load initial state from BE
        val conditional = be.conditional
        if (conditional != null) {
            this.type = conditional.type
            this.moreThan = conditional.moreThan
            this.comparisonValue = conditional.comparisonValue
            this.filter = ArrayList(conditional.filter)
            this.filterBlacklist = conditional.filterBlacklist
        }

    }

    override fun init() {
        // Setup Window
        val bg = AllGuiTextures.SCHEDULE
        setWindowSize(bg.width, bg.height)



        super.init()

        val x = guiLeft
        val y = guiTop



        conditionalTypeScrollInput = SelectionScrollInput(x + 20, y + 20, 80, 18)
        conditionalLabel = Label(x + 20, y + 29, Component.literal("Blank"))
        conditionalTypeScrollInput.forOptions(ConditionalType.entries.map { Component.literal(it.name) })
            .writingTo(conditionalLabel).setState(type.ordinal)

        comparisonTypeScrollInput = SelectionScrollInput(x + 100, y + 20, 40, 18)
        comparisonTypeLabel = Label(x + 100, y + 29, Component.literal("Blank"))
        comparisonTypeScrollInput.forOptions(listOf(Component.literal("Less Than"), Component.literal("More Than")))
            .writingTo(comparisonTypeLabel).setState(if (moreThan) 1 else 0)

        comparisonValueScrollInput = ScrollInput(x + 165, y + 20, 30, 18)
        comparisonValueLabel = Label(x + 165, y + 29, Component.literal("Blank"))
        comparisonValueScrollInput.withRange(0, 10000).writingTo(comparisonValueLabel).setState(comparisonValue.toInt())
            .withStepFunction {if (it.control) 100 else if (it.shift) 10 else 1}

        addRenderableOnly(conditionalLabel)
        addRenderableOnly(comparisonTypeLabel)
        addRenderableOnly(comparisonValueLabel)

        addRenderableWidget(conditionalTypeScrollInput)
        addRenderableWidget(comparisonTypeScrollInput)
        addRenderableWidget(comparisonValueScrollInput)


        val filterBtnX = x + 30 + 195
        val filterBtnY = y + 24
        val filterBtn = IconButton(filterBtnX, filterBtnY, AllIcons.I_ADD)
        filterBtn.withCallback<IconButton> {
            filterOpen = !filterOpen
            if (filterOpen) {
                // Initialize widget if opening
                filterWidget = GasFilterWidget(x, y + 85, 240, 100)
                addRenderableWidget(filterWidget!!)
            } else {
                removeWidget(filterWidget!!)
                filterWidget = null
            }
        }
        filterBtn.setToolTip(Component.literal("Configure Gas Filter"))
        addRenderableWidget(filterBtn)

    }

    override fun renderWindow(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {
        val bg = AllGuiTextures.SCHEDULE
        bg.render(graphics, guiLeft, guiTop)

        // Render Header
        val titleText = Component.literal("Redstone Duct Config")
        graphics.drawString(font, titleText, guiLeft + (bg.width - font.width(titleText)) / 2, guiTop + 5, 0x505050, false)
    }

    override fun removed() {
        super.removed()
        // Save logic: Send Packet to Server
        // Note: You must implement RedstoneDuctEditPacket
        val newConditional = RedstoneDuctConditional(type, moreThan, comparisonValue, filter, filterBlacklist)

        //AllPackets.getChannel().sendToServer(RedstoneDuctEditPacket(be.blockPos, newConditional))
    }

    // ==========================================
    //              Gas Filter Widget
    // ==========================================

    inner class GasFilterWidget(x: Int, y: Int, w: Int, h: Int) : AbstractSimiWidget(x,y,w,h), GuiEventListener {

        private var scroll = 0.0
        private val allGases = GasTypeRegistry.GAS_TYPES.values.toList()
        private val iconSize = 18
        private val cols = 9
        private val rows = ceil(allGases.size.toDouble() / cols).toInt()
        private val contentHeight = rows * iconSize + 20

        // Mode Toggle Button
        private val modeButton: IconButton = IconButton(x + w - 20, y - 20, if (filterBlacklist) AllIcons.I_BLACKLIST else AllIcons.I_WHITELIST)

        init {
            modeButton.withCallback<IconButton> {
                filterBlacklist = !filterBlacklist
                modeButton.setIcon (if (filterBlacklist) AllIcons.I_BLACKLIST else AllIcons.I_WHITELIST)
                modeButton.setToolTip (Component.literal(if (filterBlacklist) "Mode: Blacklist" else "Mode: Whitelist"))
            }
            modeButton.setToolTip(Component.literal(if (filterBlacklist) "Mode: Blacklist" else "Mode: Whitelist"))
        }

        override fun render(graphics: GuiGraphics, mouseX: Int, mouseY: Int, partialTicks: Float) {

            UIRenderHelper.drawStretched(graphics, x, y, width, height, 100, AllGuiTextures.SCHEDULE_CARD_DARK)

            val pose = graphics.pose()
            pose.pushPose()
            pose.translate(0f, 0f, 300f) // Render above everything

            // Background


            // Header
            graphics.drawString(font, "Gas Filter", x + 5, y - 12, 0xFFFFFF)
            modeButton.x = x + width - 20
            modeButton.y = y - 20
            modeButton.render(graphics, mouseX, mouseY, partialTicks)

            // Scrollable Content
            graphics.enableScissor(x + 5, y + 5, x + width - 5, y + height - 5)

            val startX = x + 10
            val startY = y + 10 - scroll.toInt()

            for ((index, gas) in allGases.withIndex()) {
                val col = index % cols
                val row = index / cols
                val px = startX + col * 20
                val py = startY + row * 20

                // Skip if out of view
                if (py < y - 20 || py > y + height) continue

                val isSelected = filter.contains(gas)

                // Draw selection highlight
                if (isSelected) {
                    graphics.fill(px - 1, py - 1, px + 17, py + 17, 0x80FFFFFF.toInt())
                }

                // Draw Icon
                // Assuming gas.iconLocation is a texture path.
                // We render it as a sprite 16x16
                RenderSystem.setShaderTexture(0, gas.iconLocation)
                graphics.blit(gas.iconLocation, px, py, 0f, 0f, 16, 16, 16, 16)

                // Tooltip
                if (mouseX in px..(px + 16) && mouseY in py..(py + 16)) {
                    val tooltip = mutableListOf<Component>(Component.literal(gas.name).withStyle(ChatFormatting.GOLD))
                    if (isSelected) tooltip.add(Component.literal("Selected").withStyle(ChatFormatting.GREEN))
                    graphics.disableScissor()
                    graphics.renderTooltip(font, tooltip, java.util.Optional.empty(), mouseX, mouseY)
                    graphics.enableScissor(x + 5, y + 5, x + width - 5, y + height - 5)
                }
            }

            graphics.disableScissor()
            pose.popPose()
        }

        override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
            if (modeButton.isMouseOver(mouseX, mouseY)) {
                return modeButton.mouseClicked(mouseX, mouseY, button)
            }

            if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + height) return false

            val relX = mouseX - (x + 10)
            val relY = mouseY - (y + 10) + scroll

            if (relX < 0 || relY < 0) return false

            val col = (relX / 20).toInt()
            val row = (relY / 20).toInt()
            val index = row * cols + col

            if (col < cols && index < allGases.size) {
                val gas = allGases[index]
                if (filter.contains(gas)) {
                    filter.remove(gas)
                } else {
                    filter.add(gas)
                }
                // Play click sound
                //net.createmod.catnip.gui.UIRenderHelper.s(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value())
                return true
            }
            return false
        }

        override fun mouseScrolled(mouseX: Double, mouseY: Double, delta: Double): Boolean {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                scroll -= delta * 10
                scroll = Mth.clamp(scroll, 0.0, max(0.0, (contentHeight - (height - 10)).toDouble()))
                return true
            }
            return false
        }

        override fun setFocused(focused: Boolean) {}
        override fun isFocused(): Boolean = false
    }

}