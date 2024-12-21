package org.valkyrienskies.clockwork

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
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

    BRASS_SELECTED("widgets", 0, 0, 22, 22),
    WANDERLITE_SELECTED_1("widgets", 0, 22, 22, 22),
    WANDERLITE_SELECTED_2("widgets", 22, 22, 22, 22),
    GYRO("gyro", 200, 212),
    ALT_METER("alt_meter", 173, 73),

    CREATIVE_GAS_GENERATOR("creative_gas_generator", 173, 112),
    CREATIVE_GAS_GENERATOR_FRAME("creative_gas_generator_frame", 173, 112),
    CREATIVE_GAS_GENERATOR_ELEMENT("creative_gas_generator_tab", 128, 18),

    GAS_FILTER_TAB("gas_filter_menu", 214, 104),
    GAS_FILTER_FRAME("gas_filter_menu", 0,104,214, 49),
    GAS_FILTER_ELEMENT("gas_filter_menu", 0,153,75, 18),


    COMMAND_SEAT("command_seat", 173, 159),
    WANDER_TOOL_BACKGROUND("overlay", 0, 0, 16, 16);

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
