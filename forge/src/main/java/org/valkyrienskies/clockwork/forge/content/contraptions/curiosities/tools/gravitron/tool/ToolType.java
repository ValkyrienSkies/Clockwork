package org.valkyrienskies.clockwork.forge.content.contraptions.curiosities.tools.gravitron.tool;

import com.simibubi.create.content.schematics.client.tools.ISchematicTool;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.valkyrienskies.clockwork.ClockworkIcons;
import org.valkyrienskies.clockwork.ClockworkLang;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum ToolType {
    GRAB(new GrabTool(), ClockworkIcons.GRAB),
    ASSEMBLE(new AssembleTool(), ClockworkIcons.ASSEMBLE),
    GRAB_AND_ASSEMBLE(new GrabssembleTool(), ClockworkIcons.GRABSSEMBLE);

    public final IGravitronTool tool;
    public final ClockworkIcons icon;

    ToolType(IGravitronTool tool, ClockworkIcons icon) {
        this.tool = tool;
        this.icon = icon;
    }

    public MutableComponent getDisplayName() {
        return ClockworkLang.translateDirect("gravitron.tool." + ClockworkLang.asId(name()));
    }

    public List<Component> getDescription() {
        return ClockworkLang.translatedOptions("gravitron.tool." + ClockworkLang.asId(name()) + ".description", "0", "1", "2", "3");
    }

    public static List<ToolType> getTools() {
        List<ToolType> tools = new ArrayList<>();
        Collections.addAll(tools, GRAB, ASSEMBLE, GRAB_AND_ASSEMBLE);
        return tools;
    }
}