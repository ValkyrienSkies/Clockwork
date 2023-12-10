package org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron

import com.simibubi.create.content.schematics.client.tools.*
import com.simibubi.create.foundation.gui.AllIcons
import com.simibubi.create.foundation.utility.Lang
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.tool.*
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.tool.GrabTool
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.gravitron.tool.IGravitronTool
import java.util.*

enum class ToolType(val tool: IGravitronTool, val icon: AllIcons) {
    GRAB(GrabTool(), AllIcons.I_TOOL_DEPLOY),
    ASSEMBLE(AssembleTool(), AllIcons.I_TOOL_MOVE_XZ),
    GRAB_AND_ASSEMBLE(GrabssembleTool(), AllIcons.I_TOOL_MOVE_Y);



    val displayName: MutableComponent
        get() = Lang.translateDirect("schematic.tool." + Lang.asId(name))

    val description: List<Component>
        get() = Lang.translatedOptions("schematic.tool." + Lang.asId(name) + ".description", "0", "1", "2", "3")

    companion object {
        fun getTools(): List<ToolType> {
            val tools: MutableList<ToolType> = ArrayList()
            Collections.addAll(tools, GRAB, ASSEMBLE, GRAB_AND_ASSEMBLE)
            return tools
        }
    }
}