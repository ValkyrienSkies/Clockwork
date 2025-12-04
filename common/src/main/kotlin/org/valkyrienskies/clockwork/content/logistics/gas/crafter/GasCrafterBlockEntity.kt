package org.valkyrienskies.clockwork.content.logistics.gas.crafter

import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity
import com.simibubi.create.content.processing.basin.BasinBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.blockEntity.behaviour.simple.DeferralBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.util.KNodeBlockEntity
import java.util.*
import java.util.function.Predicate

class GasCrafterBlockEntity(type: BlockEntityType<*>?, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos, state),  {

    var basinChecker: DeferralBehaviour? = null

    var processingTicks = 0
    var currentRecipe = null

    val isRunning: Boolean get() {
        return processingTicks > 0
    }

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        basinChecker = DeferralBehaviour(this, ::updateBasin )
        behaviours.add(basinChecker!!)

    }

    fun updateBasin(): Boolean {
        if (isRunning) return true
        if (level == null || level!!.isClientSide) return true
        val basin = getBasin()
        if (!basin.filter(Predicate { obj: BasinBlockEntity? -> obj!!.canContinueProcessing() })
                .isPresent()
        ) return true

//        val recipes: MutableList<Recipe<*>?> = getMatchingRecipes()
//        if (recipes.isEmpty()) return true
//        currentRecipe = recipes.get(0)
//        startProcessingBasin()
        sendData()
        return true
    }

    fun getBasin(): Optional<BasinBlockEntity> {
        if (level == null) return Optional.empty<BasinBlockEntity>()
        val basinBE = level!!.getBlockEntity(worldPosition.below(2))
        if (basinBE !is BasinBlockEntity) return Optional.empty<BasinBlockEntity>()
        return Optional.of<BasinBlockEntity>(basinBE)
    }

    init {

    }
}