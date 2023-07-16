package org.valkyrienskies.clockwork.integration.cc;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.propulsion.ballooner.BalloonerBlockEntity;

public class BalloonerPeripheral implements IPeripheral {
    private final Level level;
    private final BlockPos pos;
    private final BalloonerBlockEntity ballooner;

    public BalloonerPeripheral(BalloonerBlockEntity be) {
        this.ballooner = be;
        this.level = be.getLevel();
        this.pos = be.getBlockPos();
    }

    @NotNull
    @Override
    public String getType() {
        return "ballooner";
    }

    @Override
    public boolean equals(@Nullable IPeripheral iPeripheral) {
        return level != null && level.getBlockEntity(pos) instanceof BalloonerBlockEntity;
    }

    @LuaFunction
    public final double getTemperature() {
        return this.ballooner.getTemp();
    }

    @LuaFunction
    public final String getFuelQuality() {
        return this.ballooner.getFuelQuality().name();
    }

    @LuaFunction
    public final boolean isLeaking() {
        return !this.ballooner.checkForRepair();
    }

    @LuaFunction
    public final boolean isSealed() {
        return this.ballooner.getVolume().size() > 0;
    }

    @LuaFunction
    public final double getFuelConsumptionRate() {
        return this.ballooner.getDrainRate();
    }
}
