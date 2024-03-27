package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand.tool

import com.simibubi.create.AllKeys
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import org.valkyrienskies.clockwork.ClockworkIcons
import org.valkyrienskies.clockwork.ClockworkLang
enum class ToolType(val tool: IWanderwandTool, val icon: ClockworkIcons) {
    SELECT(SelectTool(), ClockworkIcons.GRAB),
    DESELECT(DeselectTool(), ClockworkIcons.ASSEMBLE),
    ATTACH(AttachTool(), ClockworkIcons.GRABSSEMBLE),
    BIND(BindTool(), ClockworkIcons.DESTROY),
    WELD(WeldTool(), ClockworkIcons.WELD);

    fun getDisplayName(): MutableComponent {
        return ClockworkLang.translateDirect("gravitron.tool.${ClockworkLang.asId(name)}")
    }

    fun getDescription(): List<Component> {
        return translatedOptions("gravitron.tool.${ClockworkLang.asId(name)}.description")
    }

    companion object {
        @JvmStatic
        fun translatedOptions(prefix: String): List<Component> {
            val result: MutableList<Component> = ArrayList()
            result.add(ClockworkLang.translate("${prefix}.0").component())
            if (AllKeys.ACTIVATE_TOOL.keybind != null) {
                result.add(
                    ClockworkLang.translate("${prefix}.1", AllKeys.ACTIVATE_TOOL.boundKey).component()
                )
            } else {
                result.add(
                    ClockworkLang.translate("${prefix}.1", "LEFT CTRL").component()
                )
            }

            result.add(ClockworkLang.translate("${prefix}.2").component())
            result.add(ClockworkLang.translate("${prefix}.3").component())

            return result
        }

        @JvmStatic
        fun getTools(): List<ToolType> {
            val tools: MutableList<ToolType> = ArrayList()
            tools.addAll(listOf(SELECT, DESELECT, ATTACH, BIND, WELD))
            return tools
        }
    }
}