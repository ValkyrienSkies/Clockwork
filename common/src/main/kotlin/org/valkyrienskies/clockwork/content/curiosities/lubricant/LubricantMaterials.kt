package org.valkyrienskies.clockwork.content.curiosities.lubricant

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.LevelChunkSection
import org.joml.Vector3ic
import org.valkyrienskies.core.api.physics.blockstates.FluidBoundarySolidState
import org.valkyrienskies.core.internal.physics.blockstates.VsiBlockState
import org.valkyrienskies.core.internal.world.chunks.VsiBlockType
import org.valkyrienskies.core.internal.world.chunks.VsiTerrainUpdate
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.fluid.FractionalFluidSources
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore

object LubricantMaterials {
    const val FRICTION = 0.02
    private val variants = HashMap<VsiBlockState, VsiBlockState>()
    private val types = HashMap<VsiBlockType, VsiBlockType>()

    @JvmStatic
    fun register(states: Collection<VsiBlockState>): Collection<VsiBlockState> {
        val result = LinkedHashSet(states)
        for (state in states.toSet()) {
            val solid = state.solidState ?: continue
            if (solid.friction <= FRICTION) continue
            val variant = variants.getOrPut(state) {
                VsiBlockState(vsCore.newSolidStateBuilder()
                    .shape(solid.shape).elasticity(solid.elasticity).hardness(solid.hardness)
                    .friction(minOf(solid.friction, FRICTION))
                    .fluidBoundary((solid as? FluidBoundarySolidState)?.fluidBoundary).build(),
                    state.liquidState, state.displacementState)
            }
            result.add(variant)
        }
        return result
    }

    fun lubricated(type: VsiBlockType): VsiBlockType = types.getOrPut(type) {
        val state = vsCore.blockTypes.getState(type)
        variants[state]?.let { vsCore.blockTypes.getType(it) } ?: type
    }

    @JvmStatic
    fun at(level: Level, x: Int, y: Int, z: Int, state: BlockState,
           info: Pair<Double, VsiBlockType>?): Pair<Double, VsiBlockType>? {
        if (info == null || level !is ServerLevel) return info
        return if (LubricantData.get(level).contains(BlockPos.asLong(x, y, z), state.block))
            info.first to lubricated(info.second) else info
    }

    fun refresh(level: ServerLevel, pos: BlockPos, coated: Boolean) {
        val state = level.getBlockState(pos)
        val (mass, normal) = BlockStateInfo.get(state) ?: return
        val slippery = lubricated(normal)
        if (normal == slippery) return
        val fluid = FractionalFluidSources.fluidName(state)
        val amount = fluid?.let { FractionalFluidSources.forLevel(level)?.amount(level.dimensionId, pos.x, pos.y, pos.z, it)?.toInt() }
        val old = if (coated) normal else slippery
        val new = if (coated) slippery else normal
        if (amount == null) level.shipObjectWorld.onSetBlock(pos.x, pos.y, pos.z, level.dimensionId, old, new, mass, mass)
        else level.shipObjectWorld.onSetBlock(pos.x, pos.y, pos.z, level.dimensionId, old, new, mass, mass, amount)
    }

    @JvmStatic
    fun terrain(section: LevelChunkSection, chunk: Vector3ic, level: Level?): VsiTerrainUpdate? {
        if (level !is ServerLevel) return null
        val data = LubricantData.get(level)
        if (!data.hasSection(chunk.x(), chunk.y(), chunk.z())) return null
        val update = vsCore.newDenseTerrainUpdateBuilder(chunk.x(), chunk.y(), chunk.z())
        val sources = FractionalFluidSources.forLevel(level)?.takeIf {
            it.hasChunk(level.dimensionId, chunk.x(), chunk.y(), chunk.z())
        }
        for (x in 0..15) for (y in 0..15) for (z in 0..15) {
            val state = section.getBlockState(x, y, z)
            val wx = (chunk.x() shl 4) + x
            val wy = (chunk.y() shl 4) + y
            val wz = (chunk.z() shl 4) + z
            val type = at(level, wx, wy, wz, state, BlockStateInfo.get(state))?.second ?: vsCore.blockTypes.air
            val fluid = FractionalFluidSources.fluidName(state)
            if (fluid == null || sources == null) update.addBlock(x, y, z, type)
            else update.addBlock(x, y, z, type, sources.sourceFillAmount(level.dimensionId, wx, wy, wz, fluid))
        }
        return update.build()
    }
}
