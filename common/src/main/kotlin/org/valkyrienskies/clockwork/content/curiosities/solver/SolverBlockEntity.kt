package org.valkyrienskies.clockwork.content.curiosities.solver

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import org.valkyrienskies.clockwork.ClockworkDamageTypes
import org.valkyrienskies.clockwork.ClockworkModClient
import org.valkyrienskies.clockwork.content.curiosities.IArcConnector
import org.valkyrienskies.clockwork.util.kelvin.KNodeBlockEntity
import org.valkyrienskies.clockwork.util.arc.ArcBias
import org.valkyrienskies.clockwork.util.arc.LightningBolt
import org.valkyrienskies.clockwork.util.arc.LightningManager
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.kelvin.api.DuctNodePos
import org.valkyrienskies.kelvin.util.KelvinExtensions.toDuctNodePos
import org.valkyrienskies.mod.common.getLoadedShipManagingPos
import org.valkyrienskies.mod.common.util.toDoubles
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft
import kotlin.math.abs
import kotlin.math.max

class SolverBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : KNodeBlockEntity(type, pos,
    state
), IArcConnector {
    val connectedArcNodes = ArrayList<IArcConnector>()

    val beamSeed = blockPos.asLong().toULong() * 0x9E3779B97F4A7C15uL
    var ticksTillLightning = 0

    var lastHitPosition: Vec3? = null
    var previousHitPosition: Vec3? = null

    var lastHitBlock: BlockPos? = null
    var previousHitBlock: BlockPos? = null

    var destroyProgress: Float = 0f

    var clientTime = 0f

    override fun initialize() {
        super.initialize()
        if (this.level?.isClientSide == true) {
            ClockworkModClient.LIGHTNING_NODES[worldPosition] = (this)
        }
    }

    override fun remove() {
        if (this.level?.isClientSide == true) {
            ClockworkModClient.LIGHTNING_NODES.remove(worldPosition)
        }
        super.remove()
    }

    override fun getConnectedArcs(): MutableList<IArcConnector> {
        return connectedArcNodes
    }

    override fun addBehaviours(behaviours: List<BlockEntityBehaviour>) {
    }

    override fun getMaxRange(): Double {
        return 16.0
    }

    override fun tick() {
        super.tick()
        val hitResult = raycastBeam(
            level!!,
            getWorldPos(),
            getWorldPos().add(getWorldFacing().scale(64.0))
        )
        lastHitPosition = hitResult?.location ?: getWorldPos().add(getWorldFacing().scale(64.0))
        if (hitResult != null) {
            lastHitBlock = hitResult.blockPos
        } else {
            lastHitBlock = null
        }
        if (level?.isClientSide == true) {
            clientTime += 1f / 20f
            if (ticksTillLightning > 0) {
                ticksTillLightning--
            } else {
                val len = getWorldPos().distanceTo(lastHitPosition!!)
                val turns = len * 4.25
                LightningManager.spawn(
                    LightningBolt(
                        startProvider = { getWorldPos()},
                        endProvider = { this.lastHitPosition ?: getWorldPos().add(getWorldFacing().scale(64.0)) },
                        seed = beamSeed.toLong(),
                        birthGameTime = level!!.gameTime,
                        arcBias = ArcBias.DoubleHelix (
                            turns = turns,
                            phase = level!!.gameTime.toDouble() * 0.4
                        ),
                        branchScale = 0.01,
                        branchTaper = true,
                        lifeTicks = 12
                    )
                )
                ticksTillLightning = 3
            }
            return
        }

        if (hitResult != null && level is ServerLevel) {
            val sLevel = level as ServerLevel
            val hitBlock = sLevel.getBlockState(hitResult.blockPos)
            if (lastHitBlock != previousHitBlock) destroyProgress = 0f
            if (true) {
                val targetResistance = max(hitBlock.getBlock().getExplosionResistance(), 0.01f)
                destroyProgress += Mth.clamp(0.5f * abs(4.0f * Mth.clamp(1.0f / targetResistance, 0.1f, 1f)), 0f, 2f)
                destroyProgress = Mth.clamp(destroyProgress, 0f, 1f);
                sLevel.sendParticles(ParticleTypes.DRAGON_BREATH, hitResult.blockPos.getX().toDouble()+.5, hitResult.blockPos.getY().toDouble()+.5, hitResult.blockPos.getZ().toDouble()+.5, 10, Math.random(), Math.random(), Math.random(), 1.0);

                //make this something achievable: attacking multiple blocks by hitting one
                sLevel.destroyBlockProgress(0, hitResult.blockPos, (destroyProgress * 10f).toInt())
                for (dx in -1..1) {
                    for (dy in -1..1) {
                        for (dz in -1..1) {
                            val neighborPos = hitResult.blockPos.offset(dx, dy, dz)
                            if (neighborPos == hitResult.blockPos) continue
                            val neighborBlock = sLevel.getBlockState(neighborPos)
                            val neighborResistance = max(neighborBlock.getBlock().getExplosionResistance(), 0.01f)
                            val neighborProgress = Mth.clamp(destroyProgress - 0.2f * abs(4.0f * Mth.clamp(1.0f / neighborResistance, 0.1f, 1f)), 0f, 1f);
                            sLevel.destroyBlockProgress(0, neighborPos, (neighborProgress * 10f).toInt())
                            if (neighborProgress >= 1) {
                                sLevel.destroyBlock(neighborPos, true)
                            }
                        }
                    }
                }
                if (destroyProgress >= 1) {
                    sLevel.destroyBlock(hitResult.blockPos, true)
                    destroyProgress = 0f
                    //lastHitPosition = null
                }
            } else {
                destroyProgress = 0f
            }

            val entityCastResult = sLevel.clip(
                ClipContext(
                    getWorldPos(),
                    lastHitPosition!!,
                    ClipContext.Block.COLLIDER,
                    ClipContext.Fluid.NONE,
                    null
                )
            )
            if (entityCastResult.type == HitResult.Type.ENTITY) {
                val entity = (entityCastResult as EntityHitResult).entity
                entity.hurt(
                    ClockworkDamageTypes.solver(sLevel.registryAccess(), entity),
                    4.0f
                )
            }
        } else {
            destroyProgress = 0f
        }


        previousHitPosition = lastHitPosition
        previousHitBlock = lastHitBlock
    }

    override fun getMaxConnections(): Int {
        return 4
    }

    override fun canConnect(other: IArcConnector): Boolean {
        return super.canConnect(other)
    }

    override fun getWorldPos(): Vec3 {
        val ship = level.getLoadedShipManagingPos(worldPosition) ?: return blockState.getValue(BlockStateProperties.FACING).normal.toDoubles()
        val transform = if (ship is ClientShip) {
            ship.renderTransform
        } else {
            ship.transform
        }
        return transform.shipToWorld.transformPosition(Vec3.atCenterOf(worldPosition).toJOML()).toMinecraft().add(getWorldFacing().scale(1.5))
    }

    fun getWorldFacing(): Vec3 {
        val ship = level.getLoadedShipManagingPos(worldPosition) ?: return blockState.getValue(BlockStateProperties.FACING).normal.toDoubles()
        val transform = if (ship is ClientShip) {
            ship.renderTransform
        } else {
            ship.transform
        }
        return transform.shipToWorldRotation.transform(blockState.getValue(BlockStateProperties.FACING).normal.toJOMLD()).toMinecraft()
    }

    override fun getDuctNodePosition(): DuctNodePos {
        return worldPosition.toDuctNodePos(this.level!!.dimension().location())
    }


    fun raycastBeam(level: Level, start: Vec3, desiredEnd: Vec3, entity: Entity? = null): BlockHitResult? {
        val ctx = ClipContext(
            start,
            desiredEnd,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            entity
        )
        val r = level.clip(ctx)
        return if (r.type == HitResult.Type.BLOCK) r as BlockHitResult else null
    }
}
