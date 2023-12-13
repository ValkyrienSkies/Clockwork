package org.valkyrienskies.clockwork.content.kinetics.resistor;

import com.jozufozu.flywheel.api.Material
import com.jozufozu.flywheel.api.MaterialManager
import com.simibubi.create.AllPartialModels
import com.simibubi.create.content.kinetics.base.IRotate
import com.simibubi.create.content.kinetics.base.KineticBlockEntityInstance
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity
import com.simibubi.create.foundation.utility.Iterate
import net.minecraft.world.level.block.Block
import org.valkyrienskies.clockwork.ClockworkPartials
import java.util.function.Consumer

class ResistorInstance(modelManager: MaterialManager, blockEntity: SplitShaftBlockEntity) : KineticBlockEntityInstance<SplitShaftBlockEntity>(modelManager, blockEntity) {
    protected var keys: ArrayList<RotatingData>? = null

    init{
        keys = ArrayList(2)

        val speed = blockEntity.speed

        val rotatingMaterial: Material<RotatingData> = getRotatingMaterial()

        for (dir in Iterate.directionsInAxis(getRotationAxis())) {
            val half = rotatingMaterial.getModel(AllPartialModels.SHAFT_HALF, blockState, dir)
            val red = rotatingMaterial.getModel(ClockworkPartials.RESISTOR_INDICATOR, blockState, dir)

            val splitSpeed = speed * blockEntity.getRotationSpeedModifier(dir)

            keys!!.add(setup(half.createInstance(), splitSpeed))
            keys!!.add(setup(red.createInstance()))
        }
    }

    override fun update() {
        val block: Block = blockState.getBlock()
        val boxAxis = (block as IRotate).getRotationAxis(blockState)

        val directions = Iterate.directionsInAxis(boxAxis)

        for (i in Iterate.zeroAndOne) {
            updateRotation(keys!![i], blockEntity.getSpeed() * blockEntity.getRotationSpeedModifier(directions[i]))
        }
    }

    override fun updateLight() {
        relight<RotatingData>(pos, keys!!.stream())
    }

    override fun remove() {
        keys!!.forEach(Consumer { obj: RotatingData -> obj.delete() })
        keys!!.clear()
    }
}
