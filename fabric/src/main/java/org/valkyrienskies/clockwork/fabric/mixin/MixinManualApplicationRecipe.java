package org.valkyrienskies.clockwork.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.kinetics.deployer.ManualApplicationRecipe;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.crafting.Recipe;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.ClockworkMod;

import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(value = ManualApplicationRecipe.class, remap = false)
public class MixinManualApplicationRecipe {
    @Unique
    private static final ResourceLocation CLOCKWORK_GAS_ENGINE_APPLICATION =
        new ResourceLocation(ClockworkMod.MOD_ID, "item_application/gas_engine");

    @WrapOperation(method = "manualApplicationRecipesApplyInWorld",
        at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;filter(Ljava/util/function/Predicate;)Ljava/util/stream/Stream;"))
    private static Stream<Recipe<?>> clockwork$skipGasEngineDuringSecondaryUse(
        Stream<Recipe<?>> recipes, Predicate<Recipe<?>> matches, Operation<Stream<Recipe<?>>> original,
        Player player
    ) {
        if (player.isSecondaryUseActive()) {
            return original.call(recipes, matches.and(recipe -> !CLOCKWORK_GAS_ENGINE_APPLICATION.equals(recipe.getId())));
        }
        return original.call(recipes, matches);
    }
}
