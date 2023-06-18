package org.valkyrienskies.clockwork.util.render;

import net.minecraft.client.resources.model.BakedModel;

public class WingModel extends ClockworkCustomRenderedItemModel {
    public WingModel(BakedModel template) {
        super(template, "wing");
        addPartials("wing_sail");
    }
}
