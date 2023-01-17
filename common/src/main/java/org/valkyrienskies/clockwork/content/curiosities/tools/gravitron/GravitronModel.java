package org.valkyrienskies.clockwork.content.curiosities.tools.gravitron;

import net.minecraft.client.resources.model.BakedModel;
import org.valkyrienskies.clockwork.util.render.ClockworkCustomRenderedItemModel;

public class GravitronModel extends ClockworkCustomRenderedItemModel {

    public GravitronModel(BakedModel template) {
        super(template, "gravitron");
        addPartials("dialhand", "prongleftone", "pronglefttwo", "prongrightone", "prongrighttwo", "prongtopone", "prongtoptwo", "prongleftthree", "prongrightthree", "prongtopthree");
    }

}
