package org.valkyrienskies.clockwork

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.gui.AllGuiTextures
import com.simibubi.create.foundation.gui.UIRenderHelper
import com.simibubi.create.foundation.gui.element.ScreenElement
import com.simibubi.create.foundation.utility.Color
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.GuiComponent
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
    COMMAND_SEAT("command_seat", 173, 159);

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

    @Environment(EnvType.CLIENT)
    fun bind() {
        RenderSystem.setShaderTexture(0, location)
    }

    @Environment(EnvType.CLIENT)
    override fun render(ms: PoseStack, x: Int, y: Int) {
        bind()
        GuiComponent.blit(ms, x, y, 0, startX.toFloat(), startY.toFloat(), width, height, 256, 256)
    }

    @Environment(EnvType.CLIENT)
    fun render(ms: PoseStack, x: Int, y: Int, component: GuiComponent) {
        bind()
        component.blit(ms, x, y, startX, startY, width, height)
    }

    @Environment(EnvType.CLIENT)
    fun render(ms: PoseStack?, x: Int, y: Int, c: Color?) {
        bind()
        UIRenderHelper.drawColoredTexture(ms, c, x, y, startX, startY, width, height)
    }
}
