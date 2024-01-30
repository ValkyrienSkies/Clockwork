package org.valkyrienskies.clockwork.fabric.content.contraptions.sticker;

import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;

public interface ISticker {
    void vs_clockwork$removeConstraint(@Nullable ServerLevel level, boolean removeTags);
}
