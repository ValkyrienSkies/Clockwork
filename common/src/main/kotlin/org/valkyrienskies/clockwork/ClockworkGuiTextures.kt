package org.valkyrienskies.clockwork

import com.simibubi.create.foundation.gui.element.ScreenElement
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

enum class ClockworkGuiTextures(
    val location: ResourceLocation,
    val width: Int,
    val height: Int,
    val startX: Int,
    val startY: Int
) : ScreenElement {

    GRAVITRON_SELECTED("widgets", 0, 0, 22, 22),
    GYRO("gyro", 200, 212),
    ALT_METER("alt_meter", 173, 73),
    COMMAND_SEAT("command_seat", 173, 159),
    AURIC_TOOL_BACKGROUND("overlay", 0, 0, 16, 16);

    constructor(location: String, width: Int, height: Int) : this(
        ResourceLocation(
            ClockworkMod.MOD_ID,
            "textures/gui/$location.png"
        ), width, height, 0, 0
    )

    constructor(startX: Int, startY: Int) : this("icons", startX * 16, startY * 16, 16, 16)

    constructor(location: String, startX: Int, startY: Int, width: Int, height: Int) : this(
        ResourceLocation(
            ClockworkMod.MOD_ID,
            "textures/gui/$location.png"
        ), width, height, startX, startY
    )

    constructor(namespace: String, location: String, startX: Int, startY: Int, width: Int, height: Int) : this(
        ResourceLocation(namespace, "textures/gui/$location.png"),
        width,
        height,
        startX,
        startY
    )

    override fun render(graphics: GuiGraphics, x: Int, y: Int) {
        graphics.blit(location, x, y, startX, startY, width, height)
    }
}
