package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import com.jozufozu.flywheel.api.Instancer
import com.jozufozu.flywheel.api.MaterialManager
import com.simibubi.create.content.kinetics.base.SingleRotatingInstance
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.Rotation
import org.valkyrienskies.clockwork.ClockworkPartials

class GasNozzleInstance(materialManager: MaterialManager?, blockEntity: GasNozzleBlockEntity?
) : SingleRotatingInstance<GasNozzleBlockEntity>(
    materialManager, blockEntity
) {

    override fun getModel(): Instancer<RotatingData> {
        val referenceState = blockState.rotate(Rotation.CLOCKWISE_180)
        return rotatingMaterial.getModel(ClockworkPartials.NOZZLE_AXIS, referenceState, blockState.getValue(HorizontalDirectionalBlock.FACING))
    }
}