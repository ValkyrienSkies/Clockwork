package org.valkyrienskies.clockwork.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.api.stress.BlockStressValues;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.valkyrienskies.clockwork.ClockworkStress;

import java.util.function.DoubleSupplier;

@Mixin(BlockStressValues.class)
public class MixinBlockStressValues {
    @WrapMethod(method = "getImpact")
    private static double injectCWImpact(Block block, Operation<Double> original) {
        double base = original.call(block);
        if (base == 0) {
            DoubleSupplier sup = ClockworkStress.Companion.getImpact(block);
            if (sup != null) return sup.getAsDouble();
        }
        return base;
    }

    @WrapMethod(method = "getCapacity")
    private static double injectCWCapacity(Block block, Operation<Double> original) {
        double base = original.call(block);
        if (base == 0) {
            DoubleSupplier sup = ClockworkStress.Companion.getCapacity(block);
            if (sup != null) return sup.getAsDouble();
        }
        return base;
    }
}
