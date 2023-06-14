package org.valkyrienskies.clockwork.integration.cc;

import dan200.computercraft.api.lua.IArguments;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import me.shedaniel.math.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.content.materials.solids.colorblock.ColorBlockEntity;

import java.awt.*;
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
        // Is First Argument Hex?
        String hex = args.optString(0, null);
        if (hex != null) {
            if (!hex.matches("0[xX][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F][0-9a-fA-F]"))
                throw new LuaException("does not match standard hexidecimal");
            this.color.setColor(Integer.parseInt(hex));
            return;
        }

        // Is RGB?
        Optional<Integer> r = args.optInt(0);
        int g = args.optInt(1, -1);
        int b = args.optInt(2, -1);
        if (r.isEmpty())
            throw new LuaException("missing number or hexidecimal");

        if (g != -1 && b != -1)
            this.color.setColor(Color.ofRGB(r.get(), g, b).getColor());
        else if (g != -1 || b != -1)
            throw new LuaException("missing green and/or blue");
        else {
            if (r.get() > 0xFFFFFF || r.get() < 0)
                throw new LuaException("value too large/small");
            this.color.setColor(r.get());
        }
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
