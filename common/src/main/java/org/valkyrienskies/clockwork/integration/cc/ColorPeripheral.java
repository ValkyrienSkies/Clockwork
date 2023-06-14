package org.valkyrienskies.clockwork.integration.cc;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.materials.solids.colorblock.ColorBlockEntity;

import java.util.Optional;

public class ColorPeripheral implements IPeripheral {
    private final ColorBlockEntity color;

    public ColorPeripheral(ColorBlockEntity color) {
        this.color = color;
    }

    @NotNull
    @Override
    public String getType() {
        String id = this.color.getBlockState().getBlock().getDescriptionId()
                .replace("vs_clockwork:block.vs_clockwork.", "");

        return id + "_color";
    }

    @Override
    public boolean equals(@Nullable IPeripheral iPeripheral) {
        return iPeripheral instanceof ColorPeripheral;
    }

    @LuaFunction
    public void setColor(int rgb) {
        this.color.setColor(rgb);
    }

    @LuaFunction
    public void setColor(IArguments args) throws LuaException {
        String hex = args.optString(0, null);
        if (hex != null) {
            if (!hex.matches("0[xX][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]"))
                throw new LuaException("does not match standard hexidecimal");
            this.color.setColor(Integer.parseInt(hex));
            return;
        }
        Optional<Integer> r = args.optInt(0);
        Optional<Integer> g = args.optInt(1, null);
        Optional<Integer> b = args.optInt(2, null);
        if ()
    }

    @LuaFunction
    public int getColor() {
        return this.color.getColor();
    }

    @LuaFunction
    public void clearColor() {
        this.color.clearColor();
    }
}
