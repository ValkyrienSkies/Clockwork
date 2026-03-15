package org.valkyrienskies.clockwork.content.logistics.gas.crafter

import com.simibubi.create.content.processing.basin.BasinBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.simple.DeferralBehaviour
import com.simibubi.create.foundation.recipe.RecipeFinder
import net.createmod.catnip.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.Container
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkRecipes
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import java.util.*
import java.util.function.Predicate
import kotlin.math.max

class GasCrafterBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state)  {

    var basinChecker: DeferralBehaviour? = null

    var processingTicks = 0
    var currentRecipe: Recipe<*>? = null

    var clientProcessingTicks = 0f
    val glow: LerpedFloat = LerpedFloat.linear()

    init {
        glow.chase(0.0,0.5, LerpedFloat.Chaser.EXP)
    }

    val isRunning: Boolean get() {
        return processingTicks > 0
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        basinChecker = DeferralBehaviour(this, ::updateBasin )
        behaviours.add(basinChecker!!)

    }

    override fun tick() {
        super.tick()

        glow.tickChaser()
        //println("${level?.isClientSide} ${glow.value}")
        if (level!!.isClientSide && !isVirtual) return


        processingTicks = max(processingTicks-1,0)
        if (processingTicks == 0 && currentRecipe != null) {
            glow.setValue(2.0)
            GasCraftingRecipe.apply(this, currentRecipe!!)
            currentRecipe = null
            sendData()
        }
    }

    override fun lazyTick() {
        basinChecker?.scheduleUpdate()
        super.lazyTick()
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
        processingTicks = max((currentRecipe as GasCraftingRecipe).processingDuration, 20)
        sendData()
        return true
    }

    fun getMatchingRecipes(): MutableList<Recipe<*>?> {
        val basin = getBasin()
        if (basin.isEmpty()) return ArrayList<Recipe<*>?>()

        val list: MutableList<Recipe<*>?> = ArrayList<Recipe<*>?>()


        for (r in RecipeFinder.get(GAS_CRAFTER_RECIPES_KEY, level)
            { recipe: Recipe<*> -> recipe.type == ClockworkRecipes.ClockworkRecipeTypes.GAS_CRAFTING.getType() })
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
        val basinBE = level!!.getBlockEntity(worldPosition.below(1))
        if (basinBE !is BasinBlockEntity) return Optional.empty<BasinBlockEntity>()
        return Optional.of<BasinBlockEntity>(basinBE)
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        tag.putInt("processingTicks", processingTicks)
        tag.putFloat("glow", glow.value)
        super.write(tag, clientPacket)
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        processingTicks = tag.getInt("processingTicks")
        glow.setValue(tag.getFloat("glow").toDouble())

        if (clientPacket) clientProcessingTicks = processingTicks.toFloat()
    }


    companion object {
        val GAS_CRAFTER_RECIPES_KEY = Any()
    }
}
