package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.world.item.ItemStack
import org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool.ToolType
import org.valkyrienskies.clockwork.util.ClockworkHotbarSlotOverlays

open class WanderwandHandler {
    var selectionScreen: WanderwandSelectionScreen? = null
    var active: Boolean = false
    var currentTool: ToolType? = null
    var activeHotbarSlot: Int = 0
    var activeSchematicItem: ItemStack? = null
    var overlay: ClockworkHotbarSlotOverlays? = null
    var isRegular = true
}