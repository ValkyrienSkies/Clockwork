package org.valkyrienskies.clockwork.util;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3ic;
import org.valkyrienskies.clockwork.content.curiosities.tools.gravitron.SelectedAreaToolkit;

import java.util.Optional;

public class AreaDataSerializer {
    public static final EntityDataAccessor<SelectedAreaToolkit> AREA_TOOLKIT = SynchedEntityData.defineId(Player.class, CWEntityDataSerializers.AREA_TOOLKIT_SERIALIZER);

    public static final EntityDataAccessor<Optional<Vector3ic>> FIRST_POS = SynchedEntityData.defineId(Player.class, CWEntityDataSerializers.VEC3I);
    public static final EntityDataAccessor<Optional<Vector3ic>> SECOND_POS = SynchedEntityData.defineId(Player.class, CWEntityDataSerializers.VEC3I);
}
