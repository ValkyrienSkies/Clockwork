package org.valkyrienskies.clockwork

import com.simibubi.create.foundation.gui.UIRenderHelper
import com.simibubi.create.foundation.gui.element.ScreenElement
import com.simibubi.create.foundation.utility.Color
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation


enum class ClockworkGuiTextures(
    namespace: String,
    location: String,
    var startX: Int,
    var startY: Int,
    var width: Int,
    var height: Int
) :
    ScreenElement {
    COMMAND_SEAT("command_seat", 173, 159);

    val location: ResourceLocation

    constructor(location: String, width: Int, height: Int) : this(location, 0, 0, width, height)
    constructor(startX: Int, startY: Int) : this("icons", startX * 16, startY * 16, 16, 16)
    constructor(location: String, startX: Int, startY: Int, width: Int, height: Int) : this(
        ClockworkMod.MOD_ID,
        location,
        startX,
        startY,
        width,
        height
    )

    init {
        this.location = ResourceLocation(namespace, "textures/gui/$location.png")
    }

    @Environment(EnvType.CLIENT)
    override fun render(gui: GuiGraphics, x: Int, y: Int) {
        gui.blit(
            location, x, y, 0, startX.toFloat(), startY.toFloat(),
            width,
            height, 256, 256
        )
    }

    @Environment(EnvType.CLIENT)
    fun render(guiGraphics: GuiGraphics?, x: Int, y: Int, c: Color?) {
        UIRenderHelper.drawColoredTexture(guiGraphics, c, x, y, startX, startY, width, height)
    }

    companion object {
        const val FONT_COLOR = 0x575F7A
    }
}