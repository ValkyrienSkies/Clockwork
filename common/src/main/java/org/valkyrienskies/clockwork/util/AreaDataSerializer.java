package org.valkyrienskies.clockwork.util;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.SelectedAreaToolkit;

import java.util.Optional;

public class AreaDataSerializer {
    public static final EntityDataAccessor<SelectedAreaToolkit> AREA_TOOLKIT = SynchedEntityData.defineId(Player.class, CWEntityDataSerializers.AREA_TOOLKIT_SERIALIZER);

    public static final EntityDataAccessor<Optional<BlockPos>> FIRST_POS = SynchedEntityData.defineId(Player.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    public static final EntityDataAccessor<Optional<BlockPos>> SECOND_POS = SynchedEntityData.defineId(Player.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
}
