package org.valkyrienskies.clockwork.integration.cc;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.contraptions.combustion_engine.CombustionEngineBlockEntity;

public class CombustionEnginePeripheral implements IPeripheral {
    private final CombustionEngineBlockEntity engine;

    public CombustionEnginePeripheral(CombustionEngineBlockEntity engine) {
        this.engine = engine;
    }

    @NotNull
    @Override
    public String getType() {
        return "engine";
    }

    @Override
    public boolean equals(@Nullable IPeripheral iPeripheral) {
        return iPeripheral instanceof CombustionEnginePeripheral;
    }

    @LuaFunction
    public final String getFuelQuality() {
        return this.engine.getFuelQuality().name();
    }

    @LuaFunction
    public final long getFuelAmount() {
        return this.engine.tank.getPrimaryHandler().getCurrentAmount();
    }

    @LuaFunction
    public final long getFuelCapacity() {
        return this.engine.tank.getPrimaryHandler().getTotalCapacity();
    }

    @LuaFunction
    public final String getFuelBooster() {
        return this.engine.getFuelBooster().name();
    }

    @LuaFunction
    public final boolean isActive() {
        return this.engine.active;
    }

    @LuaFunction
    public final double getGeneratedSpeed() {
        return this.engine.getGeneratedSpeed();
    }

    @LuaFunction
    public final double getAddedStressCapacity() {
        return this.engine.calculateAddedStressCapacity();
    }

    @LuaFunction
    public final double getFuelConsumptionRate() {
        return this.engine.getDrainRate();
    }
}
