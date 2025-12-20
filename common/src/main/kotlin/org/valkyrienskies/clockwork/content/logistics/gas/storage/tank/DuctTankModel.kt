package org.valkyrienskies.clockwork.content.logistics.gas.storage.tank

import com.simibubi.create.AllSpriteShifts
import com.simibubi.create.api.connectivity.ConnectivityHandler
import com.simibubi.create.content.fluids.tank.FluidTankCTBehaviour
import com.simibubi.create.foundation.block.connected.CTModel
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry
import net.createmod.catnip.data.Iterate
import net.fabricmc.fabric.api.renderer.v1.mesh.MutableQuadView
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkSpriteShifts
import java.util.*
import java.util.function.Supplier

class DuctTankModel private constructor(originalModel: BakedModel, side: CTSpriteShiftEntry, top: CTSpriteShiftEntry, inner: CTSpriteShiftEntry) :
    CTModel(originalModel, DuctTankCTBehaviour(side, top, inner)) {

    constructor(originalModel: BakedModel) : this(originalModel, ClockworkSpriteShifts.DUCT_TANK, ClockworkSpriteShifts.DUCT_TANK_TOP, AllSpriteShifts.FLUID_TANK_INNER)

    override fun emitBlockQuads(
        blockView: BlockAndTintGetter,
        state: BlockState,
        pos: BlockPos,
        randomSupplier: Supplier<RandomSource>,
        context: RenderContext
    ) {
        val cullData = CullData()
        for (d in Iterate.horizontalDirections) cullData.setCulled(d, ConnectivityHandler.isConnected<DuctTankBlockEntity>(blockView, pos, pos.relative(d)))

        context.pushTransform { quad: MutableQuadView ->
            val cullFace = quad.cullFace()
            if (cullFace != null && cullData.isCulled(cullFace)) {
                return@pushTransform false
            }
            quad.cullFace(null)
            true
        }
        super.emitBlockQuads(blockView, state, pos, randomSupplier, context)
        context.popTransform()
    }

    private class CullData {
        var culledFaces: BooleanArray = BooleanArray(4)

        init {
            Arrays.fill(culledFaces, false)
        }

        fun setCulled(face: Direction, cull: Boolean) {
            if (face.axis
                    .isVertical
            ) return
            culledFaces[face.get2DDataValue()] = cull
        }

        fun isCulled(face: Direction): Boolean {
            if (face.axis
                    .isVertical
            ) return false
            return culledFaces[face.get2DDataValue()]
        }
    }

}
