package org.valkyrienskies.clockwork.content.logistics.gas.crafter

import com.simibubi.create.AllRecipeTypes
import com.simibubi.create.content.processing.basin.BasinBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.simple.DeferralBehaviour
import com.simibubi.create.foundation.recipe.RecipeFinder
import net.minecraft.core.BlockPos
import net.minecraft.world.Container
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.util.KNodeBlockEntity
import org.valkyrienskies.core.impl.shadow.cu
import java.util.*
import java.util.function.Predicate
import kotlin.math.max

class GasCrafterBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state)  {

    var basinChecker: DeferralBehaviour? = null

    var processingTicks = 0
    var currentRecipe: Recipe<*>? = null

    val isRunning: Boolean get() {
        return processingTicks > 0
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        basinChecker = DeferralBehaviour(this, ::updateBasin )
        behaviours.add(basinChecker!!)

    }

    override fun tick() {
        super.tick()

        if (level!!.isClientSide && !isVirtual) return

        processingTicks = max(processingTicks,0)
        if (processingTicks == 0 && currentRecipe != null) {
            GasCraftingRecipe.apply(this, currentRecipe!!)
            currentRecipe = null
        }
    }

    override fun lazyTick() {
        basinChecker?.scheduleUpdate()
    }

    fun updateBasin(): Boolean {
        if (isRunning) return true
        if (level == null || level!!.isClientSide) return true
        val basin = getBasin()
        if (!basin.filter(Predicate { obj: BasinBlockEntity? -> obj!!.canContinueProcessing() })
                .isPresent()
        ) return true
        val recipes: MutableList<Recipe<*>?> = getMatchingRecipes()
        if (recipes.isEmpty() || currentRecipe != null) return true
        currentRecipe = recipes[0]
        processingTicks = (currentRecipe as GasCraftingRecipe).processingDuration
        sendData()
        return true
    }

    fun getMatchingRecipes(): MutableList<Recipe<*>?> {
        val basin = getBasin()
        if (basin.isEmpty()) return ArrayList<Recipe<*>?>()

        val list: MutableList<Recipe<*>?> = ArrayList<Recipe<*>?>()


        for (r in RecipeFinder.get(GAS_CRAFTER_RECIPES_KEY, level)
            { recipe: Recipe<*> -> recipe.type == AllRecipeTypes.BASIN.getType() })
            if (matchRecipe(r)) list.add(r)

        if (list.size > 1)
            list.sortWith(Comparator { r1: Recipe<*>, r2: Recipe<*> -> r2.ingredients.size - r1.ingredients.size })

        return list
    }

    fun <C : Container> matchRecipe(recipe: Recipe<C>?): Boolean {
        if (recipe == null) return false

        return GasCraftingRecipe.match(this, recipe)
    }

    fun getBasin(): Optional<BasinBlockEntity> {
        if (level == null) return Optional.empty<BasinBlockEntity>()
        val basinBE = level!!.getBlockEntity(worldPosition.below(2))
        if (basinBE !is BasinBlockEntity) return Optional.empty<BasinBlockEntity>()
        return Optional.of<BasinBlockEntity>(basinBE)
    }

    companion object {
        val GAS_CRAFTER_RECIPES_KEY = Any()
    }
}