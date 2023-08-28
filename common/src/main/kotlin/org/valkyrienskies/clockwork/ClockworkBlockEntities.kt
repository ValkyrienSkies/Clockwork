package org.valkyrienskies.clockwork

import com.jozufozu.flywheel.api.MaterialManager
import com.simibubi.create.content.contraptions.bearing.BearingRenderer
import com.simibubi.create.content.kinetics.base.KineticBlockEntity
import com.simibubi.create.content.kinetics.base.ShaftInstance
import com.simibubi.create.content.kinetics.base.ShaftRenderer
import com.tterrag.registrate.util.entry.BlockEntityEntry
import com.tterrag.registrate.util.nullness.NonNullFunction
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.valkyrienskies.clockwork.ClockworkMod.REGISTRATE
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.flap.FlapBearingRenderer
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserRenderer
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingRenderer


object ClockworkBlockEntities {
    // Kinetics
    val PROPELLOR_BEARING: BlockEntityEntry<PropellerBearingBlockEntity> = REGISTRATE
        .blockEntity(java.lang.String("propellor_bearing"), ::PropellerBearingBlockEntity) //            .instance(() -> BearingInstance::new)
        .validBlocks(ClockworkBlocks.PROPELLOR_BEARING)
        .renderer {
            return@renderer NonNullFunction { context: BlockEntityRendererProvider.Context ->
                return@NonNullFunction PropellerBearingRenderer(context)
            }
        }
        .register()

    val PHYS_BEARING: BlockEntityEntry<PhysBearingBlockEntity> = REGISTRATE
        .blockEntity("phys_bearing") { PhysBearingBlockEntity() } //            .instance(() -> BearingInstance::new)
        .validBlocks(ClockworkBlocks.PHYS_BEARING)
        .renderer { { PhysBearingRenderer() } }
        .register()
    val SOLVER: BlockEntityEntry<SolverBlockEntity> = REGISTRATE
        .blockEntity("solver") { SolverBlockEntity() }
        .validBlocks(ClockworkBlocks.SOLVER)
        .renderer { { SolverRenderer() } }
        .register()
    val AFTERBLAZER: BlockEntityEntry<AfterblazerEngineBlockEntity> = REGISTRATE
        .blockEntity("afterblazer") { AfterblazerEngineBlockEntity() }
        .validBlocks(ClockworkBlocks.AFTERBLAZER)
        .renderer { { AfterblazerRenderer() } }
        .register()

    val PHYSICS_INFUSER: BlockEntityEntry<PhysicsInfuserBlockEntity> = REGISTRATE
        .blockEntity(java.lang.String("physics_infuser"), ::PhysicsInfuserBlockEntity)
        .validBlocks(ClockworkBlocks.PHYSICS_INFUSER)
        .renderer {
            return@renderer NonNullFunction { context: BlockEntityRendererProvider.Context ->
                return@NonNullFunction PhysicsInfuserRenderer(context)
            }
        }
        .register()

    /////// Sequenced Seat ////////
    val COMMAND_SEAT: BlockEntityEntry<SequencedSeatBlockEntity> = REGISTRATE
        .blockEntity("sequenced_seat") { SequencedSeatBlockEntity() }
        .validBlocks(ClockworkBlocks.COMMAND_SEAT)
        .renderer { { SequencedSeatRenderer() } }
        .register()

    // FALP
    val FLAP_BEARING: BlockEntityEntry<FlapBearingBlockEntity> = REGISTRATE
        .blockEntity(java.lang.String("flap_bearing"), ::FlapBearingBlockEntity)
        .validBlocks(ClockworkBlocks.FLAP_BEARING)
        .renderer {
            return@renderer NonNullFunction { context: BlockEntityRendererProvider.Context ->
                return@NonNullFunction FlapBearingRenderer(context)
            }
        }
        .register()

    // Intake
    val INTAKE: BlockEntityEntry<IntakeBlockEntity> = REGISTRATE
        .blockEntity("intake") { IntakeBlockEntity() }
        .validBlocks(ClockworkBlocks.INTAKE)
        .renderer { { IntakeRenderer() } }
        .register()

    //BALLOONER
    val BALLOONER: BlockEntityEntry<BalloonerBlockEntity> = REGISTRATE
        .blockEntity("ballooner") { BalloonerBlockEntity() }
        .validBlocks(ClockworkBlocks.BALLOONER)
        .renderer { { BalloonerRenderer() } }
        .register()

    //REDSTONE RESISTOR todo: add instance
    val REDSTONE_RESISTOR: BlockEntityEntry<RedstoneResistorBlockEntity> = REGISTRATE
        .blockEntity("redstone_resistor") { RedstoneResistorBlockEntity() }
        .validBlocks(ClockworkBlocks.REDSTONE_RESISTOR)
        .renderer { { RedstoneResistorRenderer() } }
        .register()

    //REACTION WHEEL
    val REACTIONWHEEL: BlockEntityEntry<ReactionWheelBlockEntity> = REGISTRATE
        .blockEntity("reaction_wheel") { ReactionWheelBlockEntity() }
        .validBlocks(ClockworkBlocks.REACTIONWHEEL)
        .renderer { { ReactionWheelRenderer() } }
        .register()
    val EXTENDED_ENCASED_SHAFT: BlockEntityEntry<KineticBlockEntity> = REGISTRATE
        .blockEntity("extended_encased_shaft") { typeIn: BlockEntityType<*>?, pos: BlockPos?, state: BlockState? ->
            KineticBlockEntity(
                typeIn,
                pos,
                state
            )
        }
        .instance({
            { materialManager: MaterialManager?, blockEntity: T? ->
                ShaftInstance(
                    materialManager,
                    blockEntity
                )
            }
        }, false)
        .validBlocks(ClockworkBlocks.BALLOON_ENCASED_SHAFT)
        .renderer {
            { context: BlockEntityRendererProvider.Context? ->
                ShaftRenderer(
                    context
                )
            }
        }
        .register()

    //    public static final BlockEntityEntry<UniversalJointBlockEntity> UNIVERSAL_JOINT = REGISTRATE
    //            .blockEntity("universal_joint", UniversalJointBlockEntity::new)
    //            .validBlocks(ClockworkBlocks.UNIVERSAL_JOINT)
    //            .renderer(() -> UniversalJointRenderer::new)
    //            .register();
    // COMBUSTION ENGINE
    val COMBUSTION_ENGINE: BlockEntityEntry<CombustionEngineBlockEntity> = REGISTRATE
        .blockEntity("combustion_engine") { CombustionEngineBlockEntity() }
        .validBlocks(ClockworkBlocks.COMBUSTION_ENGINE)
        .renderer { { CombustionEngineRenderer() } }
        .register()

    // WINX
    val COLOR_BLOCK_ENTITY: BlockEntityEntry<ColorBlockEntity> = REGISTRATE
        .blockEntity("color_block_entity") { ColorBlockEntity() }
        .validBlocks(ClockworkBlocks.WING, ClockworkBlocks.FLAP)
        .renderer { { WingBlockEntityRenderer() } }
        .register()
    val HEAT_PIPE: BlockEntityEntry<HeatPipeBlockEntity> = REGISTRATE
        .blockEntity("heat_pipe") { HeatPipeBlockEntity() }
        .validBlocks(ClockworkBlocks.HEAT_PIPE)
        .renderer { { HeatPipeRenderer() } }
        .register()

    fun register() {}
}
