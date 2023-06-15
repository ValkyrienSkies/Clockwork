package org.valkyrienskies.clockwork.integration.cc;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import me.shedaniel.math.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.valkyrienskies.clockwork.util.blocktype.ConnectedWingAlike;

public class ColorPeripheral implements IPeripheral {
    private final BlockState state;
    private final Level level;
    private final BlockPos pos;

    public ColorPeripheral(Level level, BlockPos pos, BlockState state) {
        this.level = level;
        this.pos = pos;
        this.state = state;
    }

    @NotNull
    @Override
    public String getType() {
        String id = state.getBlock().getDescriptionId().replace("block.vs_clockwork.", "");
        return id + "_color";
    }

    @Override
    public boolean equals(@Nullable IPeripheral iPeripheral) {
        return iPeripheral instanceof ColorPeripheral;
    }

    @LuaFunction
    public void setRGBColor(int red, int green, int blue) throws LuaException {
        if (red > 255 || red < 0)
            throw new LuaException("red is out of bounds, 0-255");
        if (green > 255 || green < 0)
            throw new LuaException("blue is out of bounds, 0-255");
        if (blue > 255 || blue < 0)
            throw new LuaException("red is out of bounds, 0-255");

        this.state.setValue(ConnectedWingAlike.COLOR, Color.ofRGB(red, green, blue).getColor());
        this.level.setBlockAndUpdate(this.pos, this.state);
    }

    @LuaFunction
    public void setHexColor(int rgb) throws LuaException {
        if (rgb > 0xFFFFFF || rgb < 0)
            throw new LuaException("value out of bounds, 0-0xFFFFFF");
        this.state.setValue(ConnectedWingAlike.COLOR, rgb);
        this.level.setBlockAndUpdate(this.pos, this.state);
    }

    @LuaFunction
    public Integer getColor() {
        int color = this.state.getValue(ConnectedWingAlike.COLOR);
        return color > 0xFFFFFF ? null : color;
    }

    @LuaFunction
    public void clearColor() {
        this.state.setValue(ConnectedWingAlike.COLOR, 0x1000000);
        this.level.setBlockAndUpdate(this.pos, this.state);
    }
}
