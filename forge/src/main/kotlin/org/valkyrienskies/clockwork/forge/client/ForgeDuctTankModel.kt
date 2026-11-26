package org.valkyrienskies.clockwork.forge.client

import com.simibubi.create.api.connectivity.ConnectivityHandler
import com.simibubi.create.foundation.block.connected.CTModel
import net.createmod.catnip.data.Iterate
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.block.model.BakedQuad
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.client.model.data.ModelData
import net.minecraftforge.client.model.data.ModelProperty
import org.valkyrienskies.clockwork.ClockworkSpriteShifts
import org.valkyrienskies.clockwork.content.logistics.gas.storage.tank.DuctTankBlockEntity
import org.valkyrienskies.clockwork.content.logistics.gas.storage.tank.DuctTankCTBehaviour

class ForgeDuctTankModel(originalModel: BakedModel) : CTModel(
    originalModel,
    DuctTankCTBehaviour(ClockworkSpriteShifts.DUCT_TANK, ClockworkSpriteShifts.DUCT_TANK_TOP, ClockworkSpriteShifts.DUCT_TANK_TOP)
) {

    override fun gatherModelData(
        builder: ModelData.Builder,
        world: BlockAndTintGetter,
        pos: BlockPos,
        state: BlockState,
        blockEntityData: ModelData
    ): ModelData.Builder {
        super.gatherModelData(builder, world, pos, state, blockEntityData)
        val culled = BooleanArray(4)
        for (d in Iterate.horizontalDirections)
            culled[d.get2DDataValue()] = ConnectivityHandler.isConnected<DuctTankBlockEntity>(world, pos, pos.relative(d))
        return builder.with(CULL_PROPERTY, CullData(culled))
    }

    override fun getQuads(
        state: BlockState?,
        side: Direction?,
        rand: RandomSource,
        extraData: ModelData,
        renderType: RenderType?
    ): List<BakedQuad> {
        if (side != null) return emptyList()

        val cullData = if (extraData.has(CULL_PROPERTY)) extraData.get(CULL_PROPERTY) else null
        val quads = ArrayList<BakedQuad>()
        for (d in Iterate.directions) {
            if (cullData != null && cullData.isCulled(d)) continue
            quads.addAll(super.getQuads(state, d, rand, extraData, renderType))
        }
        quads.addAll(super.getQuads(state, null, rand, extraData, renderType))
        return quads
    }

    private class CullData(private val culledFaces: BooleanArray) {
        fun isCulled(face: Direction): Boolean = !face.axis.isVertical && culledFaces[face.get2DDataValue()]
    }

    companion object {
        private val CULL_PROPERTY = ModelProperty<CullData>()
    }
}
