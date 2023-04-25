package org.valkyrienskies.clockwork.integration.cc;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.LuaValues;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.contraptions.afterblazer.AfterblazerBlockEntity;

import java.util.Map;
import java.util.Optional;

public class AfterblazerPeripheral implements IPeripheral {
    private final Level level;
    private final BlockPos pos;
    private final AfterblazerBlockEntity afterblazer;

    public AfterblazerPeripheral(AfterblazerBlockEntity be) {
        this.afterblazer = be;
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

    @LuaFunction
    public final void setGimbal(IArguments arg) throws LuaException {
        Optional<Double> pitch = arg.optDouble(0);
        if (pitch.isEmpty()) throw LuaValues.badArgumentOf(0, "number", pitch);
        if (arg.count() == 1) this.afterblazer.setGimbal(pitch.get(), this.afterblazer.getGimbalYaw());

        Optional<Double> yaw = arg.optDouble(1);
        if (yaw.isEmpty()) throw LuaValues.badArgumentOf(1, "number", yaw);
        this.afterblazer.setGimbal(pitch.get(), yaw.get());
    }

    @LuaFunction
    public final double getGimbalPitch() {
        return this.afterblazer.getGimbalPitch();
    }

    @LuaFunction
    public final double getGimbalYaw() {
        return this.afterblazer.getGimbalYaw();
    }

    @LuaFunction
    public final String getFuelQuality() {
        return this.afterblazer.getFuelQuality().name();
    }

    @LuaFunction
    public final String getFuelType() {
        return this.afterblazer.tank.getPrimaryHandler().getFluidType().toString();
    }

    @LuaFunction
    public final int getRemainingFuel() {
        return this.afterblazer.getRemainingFuel();
    }

    @LuaFunction
    public final Map<String, Double> getGimbal() {
        return Map.of("pitch", this.afterblazer.getGimbalPitch(), "yaw", this.afterblazer.getGimbalYaw());
    }
}
