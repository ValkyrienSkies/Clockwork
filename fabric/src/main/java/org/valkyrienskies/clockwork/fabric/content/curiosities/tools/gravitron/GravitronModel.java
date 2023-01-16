package org.valkyrienskies.clockwork.fabric.content.curiosities.tools.gravitron;


import com.simibubi.create.foundation.item.render.CreateCustomRenderedItemModel;
import net.minecraft.client.resources.model.BakedModel;
import org.valkyrienskies.clockwork.fabric.render.ClockworkCustomRenderedItemModel;

public class GravitronModel extends ClockworkCustomRenderedItemModel {

    public GravitronModel(BakedModel template) {
        super(template, "gravitron");
        addPartials("dialhand", "prongleftone", "pronglefttwo", "prongrightone", "prongrighttwo", "prongtopone", "prongtoptwo", "prongleftthree", "prongrightthree", "prongtopthree");
    }

}
