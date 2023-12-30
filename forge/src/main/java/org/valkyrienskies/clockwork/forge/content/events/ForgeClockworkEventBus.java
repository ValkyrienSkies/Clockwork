package org.valkyrienskies.clockwork.forge.content.events;

import com.simibubi.create.foundation.ModFilePackResources;
import com.simibubi.create.foundation.utility.Components;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import org.valkyrienskies.clockwork.ClockworkMod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class ForgeClockworkEventBus {

    @SubscribeEvent
    public static void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            IModFileInfo modFileInfo = ModList.get().getModFileById(ClockworkMod.MOD_ID);
            if (modFileInfo == null) {
                return;
            }
            IModFile modFile = modFileInfo.getFile();

            event.addRepositorySource(consumer -> {
                Pack pack = Pack.readMetaAndCreate(
                        ClockworkMod.asResource("legacy_copper").toString(),
                        Components.literal("Clockwork: Gearwork"),
                        false,
                        id -> new ModFilePackResources(id, modFile, "resourcepacks/gearwork"),
                        PackType.CLIENT_RESOURCES,
                        Pack.Position.TOP, PackSource.BUILT_IN
                );
                if (pack != null) {
                    consumer.accept(pack);
                }
            });
        }
    }
}
