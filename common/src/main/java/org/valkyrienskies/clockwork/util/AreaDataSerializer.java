package org.valkyrienskies.clockwork.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.SelectedAreaToolkit;

public class AreaDataSerializer {
    public static final EntityDataAccessor<SelectedAreaToolkit> AREA_TOOLKIT = SynchedEntityData.defineId(Player.class, CWEntityDataSerializers.AREA_TOOLKIT_SERIALIZER);
}
