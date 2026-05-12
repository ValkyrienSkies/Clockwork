package org.valkyrienskies.clockwork.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.gui.recipes.RecipeGuiLogic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.valkyrienskies.clockwork.compat.jei.categories.GasReactionCategory;

import java.util.Optional;
import java.util.function.Supplier;

@Mixin(RecipeGuiLogic.class)
public class MixinRecipeGuiLogic {
    /**
     * This forces JEI to check the height of our recipe every time if its a GasReaction instead of using a previously cached height.
     */
    @WrapOperation(
            method = "getVisibleRecipeLayoutsWithButtons",
            at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseGet(Ljava/util/function/Supplier;)Ljava/lang/Object;")
    )
    private static <T> T thingy(Optional instance, Supplier<? extends Integer> supplier, Operation<Integer> original, @Local IRecipeCategory<?> recipeCategory) {
        if (recipeCategory instanceof GasReactionCategory) {
            return (T) supplier.get();
        }
        return (T) original.call(instance, supplier);
    }

}
