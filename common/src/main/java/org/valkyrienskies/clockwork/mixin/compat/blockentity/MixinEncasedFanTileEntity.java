package org.valkyrienskies.clockwork.mixin.compat.blockentity;

import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.fan.EncasedFanTileEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.valkyrienskies.clockwork.mixinduck.IExtendedAirCurrentSource;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

@Mixin(EncasedFanTileEntity.class)
public abstract class MixinEncasedFanTileEntity extends KineticTileEntity implements IExtendedAirCurrentSource {
    @Unique
    private Ship ship = null;

    public MixinEncasedFanTileEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    @Override
    public void setLevel(@NotNull Level level) {
        super.setLevel(level);
        ship = VSGameUtilsKt.getShipManagingPos(level, getBlockPos());
    }

    @Override
    public Ship getShip() {
        return ship;
    }
}
