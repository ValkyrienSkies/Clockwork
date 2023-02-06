package org.valkyrienskies.clockwork.mixin.compat;

import com.simibubi.create.content.contraptions.components.deployer.DeployerHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import javax.annotation.Nullable;

@Mixin(DeployerHandler.class)
public interface IMixinDeployerHandler {
    @Invoker("shouldActivate")
    static boolean invokeShouldActivate(ItemStack held, Level world, BlockPos targetPos, @Nullable Direction facing){
        throw  new AssertionError();
    }
}
