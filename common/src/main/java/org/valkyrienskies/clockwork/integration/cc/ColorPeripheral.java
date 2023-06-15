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
import org.valkyrienskies.clockwork.content.materials.solids.colorblock.ColorBlockEntity;
import org.valkyrienskies.clockwork.util.blocktype.ConnectedWingAlike;

public class ColorPeripheral implements IPeripheral {
    private final ColorBlockEntity color;

    public ColorPeripheral(ColorBlockEntity color) {
        this.color = color;
    }

    @NotNull
    @Override
    public String getType() {
        String id = this.color.getBlockState().getBlock().getDescriptionId()
                .replace("block.vs_clockwork.", "");
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

        this.color.setColor(Color.ofRGB(red, green, blue).getColor());
    }

    @LuaFunction
    public void setHexColor(int rgb) throws LuaException {
        if (rgb > 0xFFFFFF || rgb < 0)
            throw new LuaException("value out of bounds, 0-0xFFFFFF");
        this.color.setColor(rgb);
    }

    @LuaFunction
    public Integer getColor() {
        int color = this.color.getColor();
        return color < 0 ? null : color;
    }

    @LuaFunction
    public void clearColor() {
        this.color.clearColor();
    }
}
