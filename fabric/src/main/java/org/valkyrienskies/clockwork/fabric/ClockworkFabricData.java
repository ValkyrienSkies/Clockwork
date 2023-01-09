package org.valkyrienskies.clockwork.fabric;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ClockworkFabricData implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        ExistingFileHelper helper = ExistingFileHelper.standard();
        ClockWorkModFabric.REGISTRATE.setupDatagen(generator, helper);
        ClockWorkModFabric.gatherData(generator, helper);
    }
}
