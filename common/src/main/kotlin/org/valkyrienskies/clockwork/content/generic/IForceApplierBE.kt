package org.valkyrienskies.clockwork.content.generic

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import org.valkyrienskies.clockwork.content.forces.MultiInstanceForceApplier
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierCreateData
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierData
import org.valkyrienskies.clockwork.content.forces.data.ForceApplierUpdateData
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import kotlin.reflect.typeOf

interface IForceApplierBE<U: ForceApplierUpdateData, D: ForceApplierData<U>, C: ForceApplierCreateData<D>, A: MultiInstanceForceApplier<U, D, C>> {
    var physID: Int

    fun tickData(attachment: A, shouldUpdate: Boolean) {
        if (physID < 0) {
            val createData = newCreateData()
            physID = attachment.createApplier(createData)
        }
        if (physID >= 0) {
            if (shouldUpdate) {
                attachment.updateApplier(physID, newUpdateData())
            }
        }
    }

    fun newCreateData(): C

    fun newUpdateData(): U

    fun removeApplier(clazz: Class<A>, level: Level?, pos: BlockPos) {
        if (level == null || level.isClientSide) return
        assert(level is ServerLevel)

        val ship = (level as ServerLevel).getShipObjectManagingPos(pos) ?: return
        val attachment = ship.getAttachment(clazz) ?: return
        attachment.removeApplier(physID)
        physID = -1
    }
}