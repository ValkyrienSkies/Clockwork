package org.valkyrienskies.clockwork

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.gui.element.ScreenElement
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.GuiComponent
import net.minecraft.resources.ResourceLocation

class KelvinIcons(x: Int, y: Int) : ScreenElement {

    companion object {
        private val ICON_ATLAS = ResourceLocation("kelvin", "textures/gui/icons.png")
        private const val ICON_ATLAS_SIZE: Int = 256

        private var x = 0
        private var y = -1

        private fun next(): KelvinIcons {
            return KelvinIcons(++x, y)
        }

        private fun newRow(): KelvinIcons {
            return KelvinIcons(x = 0, ++y)
        }
    }

    private var iconX = x * 16
    private var iconY = y * 16

    @Environment(EnvType.CLIENT)
    fun bind() {
        RenderSystem.setShaderTexture(0, ICON_ATLAS)
    }

    @Environment(EnvType.CLIENT)
    override fun render(matrixStack: PoseStack, x: Int, y: Int) {
        bind()
        GuiComponent.blit(matrixStack, x, y, 0, iconX.toFloat(), iconY.toFloat(), 16, 16, 256, 256)
    }

}