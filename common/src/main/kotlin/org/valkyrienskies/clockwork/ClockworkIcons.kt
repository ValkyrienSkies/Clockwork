package org.valkyrienskies.clockwork

import com.simibubi.create.foundation.gui.element.ScreenElement
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation

class ClockworkIcons(x: Int, y: Int) : ScreenElement {

    companion object {
        private val ICON_ATLAS: ResourceLocation = ClockworkMod.asResource("textures/gui/icons.png")
        private const val ICON_ATLAS_SIZE: Int = 256

        private var x = 0
        private var y = -1

        @JvmField
        val GRAB: ClockworkIcons = newRow()

        @JvmField
        val ASSEMBLE: ClockworkIcons = next()

        @JvmField
        val GRABSSEMBLE: ClockworkIcons = next()

        @JvmField
        val DESTROY: ClockworkIcons = next()

        private fun next(): ClockworkIcons {
            return ClockworkIcons(++x, y)
        }

        private fun newRow(): ClockworkIcons {
            return ClockworkIcons(x = 0, y = ++y)
        }
    }

    private var iconX = x * 16
    private var iconY = y * 16

    override fun render(graphics: GuiGraphics?, x: Int, y: Int) {
        graphics?.blit(ICON_ATLAS, x, y, 0, iconX.toFloat(), iconY.toFloat(), 16, 16, ICON_ATLAS_SIZE, ICON_ATLAS_SIZE)
    }
}