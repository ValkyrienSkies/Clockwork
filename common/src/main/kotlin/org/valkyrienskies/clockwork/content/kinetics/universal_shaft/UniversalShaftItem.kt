package org.valkyrienskies.clockwork.content.kinetics.universal_shaft

import net.minecraft.world.level.block.entity.BlockEntity
import org.valkyrienskies.clockwork.util.universal_joint.UniversalJointItem

class UniversalShaftItem(properties: Properties) : UniversalJointItem<UniversalShaftBlockEntity>(properties) {

    override fun isJoint(be: BlockEntity): Boolean {
        return be is UniversalShaftBlockEntity
    }
}