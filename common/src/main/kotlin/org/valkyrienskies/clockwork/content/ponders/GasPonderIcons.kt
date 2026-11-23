package org.valkyrienskies.clockwork.content.ponders

import net.createmod.ponder.api.element.InputElementBuilder
import org.valkyrienskies.kelvin.api.GasType

fun InputElementBuilder.withGas(gas: GasType): InputElementBuilder = showing { graphics, x, y ->
    graphics.blit(gas.iconLocation, x, y, 0f, 0f, 16, 16, 16, 16)
}
