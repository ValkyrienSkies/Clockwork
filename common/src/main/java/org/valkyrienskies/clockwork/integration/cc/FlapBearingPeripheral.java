package org.valkyrienskies.clockwork.integration.cc;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity;

public class FlapBearingPeripheral implements IPeripheral {
    private final Level level;
    private final BlockPos pos;
    private final FlapBearingBlockEntity flap;

    public FlapBearingPeripheral(FlapBearingBlockEntity be) {
        this.flap = be;
        this.level = be.getLevel();
        this.pos = be.getBlockPos();
    }

    @NotNull
    @Override
    public String getType() {
        return "flap_bearing";
    }

    @Override
    public boolean equals(@Nullable IPeripheral iPeripheral) {
        return level != null && level.getBlockEntity(pos) instanceof FlapBearingBlockEntity;
    }

    @LuaFunction
    public final void setAngle(float angle) {
        this.flap.setAngle(angle);
    }

    @LuaFunction
    public final double getFlapSpeed() {
        return this.flap.getFlapSpeed();
    }

    @LuaFunction
    public final double getAngularSpeed() {
        return this.flap.getAngularSpeed();
    }

    @LuaFunction
    public final boolean isValid() {
        return this.flap.isValid();
    }
}
