package org.valkyrienskies.clockwork.forge.mixin.platform;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityProvider;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.valkyrienskies.clockwork.platform.SmartFluidTankBlockEntity;
import org.valkyrienskies.clockwork.platform.forge.ForgeCWFluidTankBehaviour;

@Mixin(BlockEntity.class)
public class MixinBlockEntity extends CapabilityProvider<BlockEntity> {

    protected MixinBlockEntity(Class<BlockEntity> baseClass) {
        super(baseClass);
    }


    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {

        if (cap == ForgeCapabilities.FLUID_HANDLER && this instanceof SmartFluidTankBlockEntity te) {
            return ((ForgeCWFluidTankBehaviour) te.getFluidTankBehaviour()).getCapability().cast();
        }

        return super.getCapability(cap, side);
    }
}
