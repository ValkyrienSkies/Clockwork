package org.valkyrienskies.clockwork.content.logistics.gas.pockets.nozzle

import com.jozufozu.flywheel.api.Instancer
import com.jozufozu.flywheel.api.MaterialManager
import com.simibubi.create.content.kinetics.base.KineticBlockEntityInstance
import com.simibubi.create.content.kinetics.base.SingleRotatingInstance
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData
import org.valkyrienskies.clockwork.ClockworkPartials

class GasNozzleInstance(materialManager: MaterialManager?, blockEntity: GasNozzleBlockEntity?) : SingleRotatingInstance<GasNozzleBlockEntity>(
    materialManager, blockEntity
) {

    override fun getModel(): Instancer<RotatingData> {
        return rotatingMaterial.getModel(ClockworkPartials.NOZZLE_AXIS)
    }
}