package org.valkyrienskies.clockwork.integration.cc;

import com.simibubi.create.foundation.tileEntity.behaviour.scrollvalue.ScrollOptionBehaviour;
import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.contraptions.phys.bearing.PhysBearingBlockEntity;
import org.valkyrienskies.clockwork.platform.api.ContraptionController;

import java.util.Optional;

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

    @LuaFunction
    public final double getAngle() {
        return this.phys.getAngle();
    }

    @LuaFunction
    public final void lock(IArguments args) throws LuaException {
        ScrollOptionBehaviour<ContraptionController.LockedMode> mode = this.phys.getMovementMode();
        boolean locked = args.optBoolean(0, !mode.get().equals(ContraptionController.LockedMode.LOCKED));

        mode.setValue(locked ? 1 : 0);

        this.phys.setMovementMode(mode);
    }
}
