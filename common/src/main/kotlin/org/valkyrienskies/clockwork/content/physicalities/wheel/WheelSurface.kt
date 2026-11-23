package org.valkyrienskies.clockwork.content.physicalities.wheel

import net.minecraft.core.BlockPos
import net.minecraft.core.registries.Registries
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.tags.FluidTags
import net.minecraft.tags.BlockTags
import net.minecraft.tags.TagKey
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.SnowLayerBlock
import net.minecraft.world.level.block.MudBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.core.api.physics.blockstates.TireSurfaceProperties
import org.valkyrienskies.mod.api.vsApi
import kotlin.math.abs

enum class WheelSurface(val rubber: Boolean, val loose: Boolean = false) {
    HARD(true), GRASS(false, true), DIRT(false, true), SAND(false, true), GRAVEL(false, true),
    SNOW(false, true), ICE(false), WOOD(true), METAL(true), WET(false, true);

    private val tag = TagKey.create(Registries.BLOCK, ResourceLocation(ClockworkMod.MOD_ID, "tire_surfaces/" + name.lowercase()))

    fun rollingTracks(state: BlockState) = this == SAND || this == SNOW || (this == WET && state.`is`(tag))

    companion object {
        private val roadTag = TagKey.create(Registries.BLOCK, ResourceLocation(ClockworkMod.MOD_ID, "tire_surfaces/road"))
        private val hardPhysics = TireSurfaceProperties(rollingResistance = .015)
        private val roadPhysics = TireSurfaceProperties(rollingResistance = .005, gripMultiplier = 1.08)
        private val physics = mapOf(
            GRASS to TireSurfaceProperties(rollingResistance = .045),
            DIRT to TireSurfaceProperties(rollingResistance = .035),
            GRAVEL to TireSurfaceProperties(rollingResistance = .05),
            SAND to TireSurfaceProperties(.06, .12, 3_000_000.0, 300_000.0, .3),
            SNOW to TireSurfaceProperties(.05, .16, 1_500_000.0, 200_000.0, .25),
            WET to TireSurfaceProperties(.07, .2, 1_000_000.0, 250_000.0, .4, 40_000.0, 4_000.0),
            WOOD to TireSurfaceProperties(rollingResistance = .012),
            METAL to TireSurfaceProperties(rollingResistance = .008),
            ICE to TireSurfaceProperties(rollingResistance = .006),
        )

        fun physics(state: BlockState): TireSurfaceProperties {
            if (state.`is`(roadTag)) return roadPhysics
            val surface = entries.firstOrNull { state.`is`(it.tag) }
            val profile = physics[surface] ?: hardPhysics
            if (surface == SNOW && state.block is SnowLayerBlock) {
                return profile.copy(maximumSinkage = minOf(profile.maximumSinkage,
                    (state.getValue(SnowLayerBlock.LAYERS) - 1) / 16.0))
            }
            return profile
        }

        fun registerPhysics() {
            vsApi.registerBlockStateEvent.on { event ->
                for (block in BuiltInRegistries.BLOCK) for (state in block.stateDefinition.possibleStates) {
                    if (!state.isAir) event.tireSurface(state, physics(state))
                }
            }
        }

        fun scrapeSurface(state: BlockState): WheelSurface? = when (of(state)) {
            METAL -> METAL
            HARD -> HARD.takeIf { state.`is`(roadTag) || state.`is`(BlockTags.MINEABLE_WITH_PICKAXE) ||
                state.soundType == SoundType.STONE || state.soundType == SoundType.GLASS }
            else -> null
        }

        fun of(state: BlockState): WheelSurface {
            if (state.fluidState.`is`(FluidTags.WATER)) return WET
            entries.firstOrNull { state.`is`(it.tag) }?.let { return it }
            return when (state.soundType) {
                SoundType.METAL, SoundType.ANVIL, SoundType.CHAIN, SoundType.COPPER, SoundType.NETHERITE_BLOCK -> METAL
                SoundType.WOOD, SoundType.BAMBOO_WOOD -> WOOD
                SoundType.GRAVEL -> GRAVEL
                SoundType.SAND -> SAND
                SoundType.GRASS, SoundType.MOSS -> GRASS
                SoundType.SNOW, SoundType.POWDER_SNOW -> SNOW
                SoundType.MUD -> WET
                else -> HARD
            }
        }

        fun contact(level: Level, point: Vector3d, normal: Vector3d): Pair<BlockPos, BlockState>? {
            if (normal.y > .92) {
                val pos = BlockPos.containing(point.x, point.y + .08, point.z)
                if (level.hasChunkAt(pos)) {
                    val state = level.getBlockState(pos)
                    if (state.block is SnowLayerBlock &&
                        abs(point.y - pos.y - (state.getValue(SnowLayerBlock.LAYERS) - 1) / 8.0) < .08) return pos to state
                }
            }
            val from = BlockPos.containing(point.x - .08, point.y - .08, point.z - .08)
            val to = BlockPos.containing(point.x + .08, point.y + .08, point.z + .08)
            var best: Pair<BlockPos, BlockState>? = null
            var distance = .08 * .08
            val inside = Vector3d(point).fma(-.015, normal)
            for (pos in BlockPos.betweenClosed(from, to)) {
                if (!level.hasChunkAt(pos)) continue
                val state = level.getBlockState(pos)
                val shape = if (state.block is MudBlock) state.getShape(level, pos) else state.getCollisionShape(level, pos)
                for (box in shape.toAabbs()) {
                    val x = inside.x.coerceIn(box.minX + pos.x, box.maxX + pos.x)
                    val y = inside.y.coerceIn(box.minY + pos.y, box.maxY + pos.y)
                    val z = inside.z.coerceIn(box.minZ + pos.z, box.maxZ + pos.z)
                    val d = inside.distanceSquared(x, y, z)
                    if (d < distance) { distance = d; best = pos.immutable() to state }
                }
            }
            return best
        }
    }
}
