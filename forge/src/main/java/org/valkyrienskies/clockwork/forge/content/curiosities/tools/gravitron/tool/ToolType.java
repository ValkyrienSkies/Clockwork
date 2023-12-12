package org.valkyrienskies.clockwork.forge.content.curiosities.tools.gravitron.tool;

import com.simibubi.create.AllKeys;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.valkyrienskies.clockwork.ClockworkIcons;
import org.valkyrienskies.clockwork.util.ClockworkLang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.simibubi.create.foundation.utility.Lang.translate;


public enum ToolType {
    GRAB(new GrabTool(), ClockworkIcons.GRAB),
    ASSEMBLE(new AssembleTool(), ClockworkIcons.ASSEMBLE),
    GRAB_AND_ASSEMBLE(new GrabssembleTool(), ClockworkIcons.GRABSSEMBLE),
    DESTROY(new DestroyTool(), ClockworkIcons.DESTROY);

    public final IGravitronTool tool;
    public final ClockworkIcons icon;

    ToolType(IGravitronTool tool, ClockworkIcons icon) {
        this.tool = tool;
        this.icon = icon;
    }

    public MutableComponent getDisplayName() {
        return ClockworkLang.translateDirect("gravitron.tool." + ClockworkLang.asId(name()));
    }

    public List<Component> getDescription() {//ClockworkLang.translateDirect(holdToFocus, keyName)
        return translatedOptions("gravitron.tool." + ClockworkLang.asId(name()) + ".description", "0", "1", "2", "3");
    }

    public static List<Component> translatedOptions(String prefix, String... keys) {
        List<Component> result = new ArrayList<>(keys.length);
        result.add(translate((prefix + ".") + "0").component());
        result.add(translate((prefix + ".") + "1", AllKeys.ACTIVATE_TOOL.getBoundKey()).component());
        result.add(translate((prefix + ".") + "2").component());
        result.add(translate((prefix + ".") + "3").component());

        return result;
    }

    public static List<ToolType> getTools() {
        List<ToolType> tools = new ArrayList<>();
        Collections.addAll(tools, GRAB, ASSEMBLE, GRAB_AND_ASSEMBLE, DESTROY);
        return tools;
    }
}