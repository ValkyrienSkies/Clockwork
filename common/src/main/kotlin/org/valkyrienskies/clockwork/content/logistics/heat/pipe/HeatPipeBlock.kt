package org.valkyrienskies.clockwork.content.logistics.heat.pipe


import com.simibubi.create.content.contraptions.ITransformableBlock
import com.simibubi.create.content.contraptions.StructureTransform
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour
import com.simibubi.create.content.equipment.wrench.IWrenchable
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockRotation
import com.simibubi.create.content.fluids.pipes.VanillaFluidTargets
import com.simibubi.create.foundation.block.IBE
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.Iterate
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionResult
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.PipeBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SimpleWaterloggedBlock
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.ticks.TickPriority
import org.valkyrienskies.clockwork.ClockworkBlockEntities
import java.util.*

class HeatPipeBlock
