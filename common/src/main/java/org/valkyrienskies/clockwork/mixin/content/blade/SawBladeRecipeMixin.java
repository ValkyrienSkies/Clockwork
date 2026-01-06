package org.valkyrienskies.clockwork.mixin.content.blade;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.simibubi.create.content.kinetics.saw.CuttingRecipe;
import com.simibubi.create.content.kinetics.saw.SawBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.valkyrienskies.clockwork.ClockworkRecipes;
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item.BladeItem;
import org.valkyrienskies.clockwork.content.contraptions.propeller.blades.item.CuttingBladeRecipe;

import java.util.List;

@Mixin(value = SawBlockEntity.class)
public abstract class SawBladeRecipeMixin extends BlockEntity {
    @Shadow(remap = false)
    public ProcessingInventory inventory;

    public SawBladeRecipeMixin(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    @Inject(method = "getRecipes", at = @At("HEAD"), cancellable = true, remap = false)
    private void vs_clockwork$addBladeRecipe(CallbackInfoReturnable<List<? extends Recipe<?>>> cir) {
        if (inventory.getStackInSlot(0).getItem() instanceof BladeItem) {
            cir.setReturnValue(level.getRecipeManager().getAllRecipesFor(
                    ClockworkRecipes.ClockworkRecipeTypes.BLADE_CUTTING.getType()
            ));
        }
    }

    @WrapOperation(method = "applyRecipe", at = @At(value = "INVOKE", target = "Lcom/simibubi/create/content/kinetics/saw/CuttingRecipe;rollResults()Ljava/util/List;"), remap = false)
    private List<ItemStack> vs_clockwork$applyBladeRecipe(CuttingRecipe recipe, Operation<List<ItemStack>> original, @Local(name = "input") ItemStack input) {
        if (recipe instanceof CuttingBladeRecipe) {
            return List.of(CuttingBladeRecipe.Companion.cutBlade(input.copyWithCount(1)));
        } else {
            return original.call(recipe);
        }
    }
}
