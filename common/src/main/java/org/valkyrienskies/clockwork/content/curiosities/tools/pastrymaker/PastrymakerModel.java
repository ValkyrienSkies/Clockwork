package org.valkyrienskies.clockwork.content.curiosities.tools.pastrymaker;

import net.minecraft.client.resources.model.BakedModel;
import org.valkyrienskies.clockwork.util.render.ClockworkCustomRenderedItemModel;

public class PastrymakerModel extends ClockworkCustomRenderedItemModel {

    public PastrymakerModel(BakedModel template) {
        super(template, "pastrymaker");
        addPartials("leftcannon", "rightcannon", "pressuretank");
    }
}
