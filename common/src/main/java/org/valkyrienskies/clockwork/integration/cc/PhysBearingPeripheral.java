package org.valkyrienskies.clockwork.integration.cc;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity;

public class PhysBearingPeripheral implements IPeripheral {
    private final PhysBearingBlockEntity phys;

    public PhysBearingPeripheral(PhysBearingBlockEntity be) {
        this.phys = be;
    }

    @NotNull
    @Override
    public String getType() {
        return "phys_bearing";
    }

    @Override
    public boolean equals(@Nullable IPeripheral iPeripheral) {
        return iPeripheral instanceof PhysBearingPeripheral;
    }

    @LuaFunction
    public final void setAngle(double angle) {
        this.phys.setAngle((float) angle);
    }

    @LuaFunction
    public final double getAngularSpeed() {
        return this.phys.getAngularSpeed();
    }

    @LuaFunction
    public final void assemble() {
        this.phys.assemble();
    }

    @LuaFunction
    public final void disassemble() {
        this.phys.disassemble();
    }

    @LuaFunction
    public final boolean isRunning() {
        return this.phys.isRunning();
    }

    @LuaFunction
    public final void release() {
        this.phys.destroy();
    }
}
