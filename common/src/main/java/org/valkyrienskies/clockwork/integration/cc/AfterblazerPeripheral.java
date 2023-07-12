package org.valkyrienskies.clockwork.integration.cc;

import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.propulsion.afterblazer.AfterblazerBlockEntity;

public class AfterblazerPeripheral implements IPeripheral {

    private final Level level;
    private final BlockPos pos;
    private final AfterblazerBlockEntity ballooner;

    public AfterblazerPeripheral(AfterblazerBlockEntity be) {
        this.ballooner = be;
        this.level = be.getLevel();
        this.pos = be.getBlockPos();
    }

    @NotNull
    @Override
    public String getType() {
        return "afterblazer";
    }

    @Override
    public boolean equals(@Nullable IPeripheral iPeripheral) {
        return level != null && level.getBlockEntity(pos) instanceof AfterblazerBlockEntity;
    }
}
