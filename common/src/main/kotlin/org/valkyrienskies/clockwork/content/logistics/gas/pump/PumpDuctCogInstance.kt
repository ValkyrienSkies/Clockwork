package org.valkyrienskies.clockwork.content.logistics.gas.pump

import com.jozufozu.flywheel.api.Instancer
import com.jozufozu.flywheel.api.MaterialManager
import com.jozufozu.flywheel.api.instance.DynamicInstance
import com.simibubi.create.content.kinetics.base.SingleRotatingInstance
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.clockwork.ClockworkPartials

class PumpDuctCogInstance(materialManager: MaterialManager?, blockEntity: PumpDuctBlockEntity?) :
    SingleRotatingInstance<PumpDuctBlockEntity?>(materialManager, blockEntity),
    DynamicInstance {
    override fun beginFrame() {}

    override fun getModel(): Instancer<RotatingData> {

        val referenceState = blockEntity!!.blockState
        val facing = referenceState.getValue(BlockStateProperties.FACING)
        return rotatingMaterial.getModel(ClockworkPartials.PUMP_COG, referenceState, facing)
    }


}
