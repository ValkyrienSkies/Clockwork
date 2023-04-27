package org.valkyrienskies.clockwork.integration.cc;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerBlockEntity;
import org.valkyrienskies.clockwork.content.contraptions.ballooner.BalloonerBlockEntity;

import java.util.Map;

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
    public final String getFuel() {
        return this.ballooner.tank.getPrimaryHandler().asSmartFluidTank().getFluid().getDisplayName().getString();
    }

    @LuaFunction
    public final int getRemainingFuel() {
        return this.ballooner.getRemainingFuel();
    }

    @LuaFunction
    public final boolean isLeaking() {
        return !this.ballooner.checkForRepair();
    }

    @LuaFunction
    public final boolean isSealed() {
        return this.ballooner.getVolume().size() > 0;
    }
}
