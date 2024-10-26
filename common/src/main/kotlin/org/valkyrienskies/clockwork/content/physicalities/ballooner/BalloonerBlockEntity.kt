package org.valkyrienskies.clockwork.content.physicalities.ballooner

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkAugmentations
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.content.logistics.gas.IHeatableBlockEntity
import org.valkyrienskies.clockwork.kelvin.api.DuctNodePos
import org.valkyrienskies.clockwork.kelvin.api.GasType
import org.valkyrienskies.core.api.world.connectivity.ConnectionStatus
import org.valkyrienskies.core.impl.shadow.DW
import org.valkyrienskies.mod.common.dimensionId
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.util.toJOMLD

class BalloonerBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?): SmartBlockEntity(type, pos, state),
    IHeatableBlockEntity {


    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>?) {
        return
    }

    override fun tick() {
        super.tick()
        if (level == null || level!!.isClientSide) return

        val ship = level.getShipManagingPos(blockPos)?: return

        val nodePos = blockPos.toJOMLD()

        val node = ClockworkMod.getKelvin().getNodeAt(nodePos)?: return

        var tempKey = ClockworkAugmentations.getComponentAugmentation("temperature")
        val airKey = ClockworkAugmentations.getComponentAugmentation("gas_air")
        val helKey = ClockworkAugmentations.getComponentAugmentation("gas_helium")
        val phlogKey = ClockworkAugmentations.getComponentAugmentation("gas_phlogiston")




        if (level!!.server!!.shipObjectWorld.isIsolatedAir(blockPos.x,blockPos.y+1,blockPos.z,level!!.dimensionId)!= ConnectionStatus.DISCONNECTED) return


        level!!.server!!.shipObjectWorld.setAirComponentAugmentation(tempKey,blockPos.x,blockPos.y+1,blockPos.z,level!!.dimensionId,ClockworkMod.getKelvin().getTemperatureAt(nodePos))

        val volumes = ClockworkMod.getKelvin().getGasVolumesAt(nodePos)

        if (volumes[GasType.AIR]!=null) level!!.server!!.shipObjectWorld.setAirComponentAugmentation(airKey,blockPos.x,blockPos.y+1,blockPos.z,level!!.dimensionId,volumes[GasType.AIR]!!)
        if (volumes[GasType.PHLOGISTON]!=null) level!!.server!!.shipObjectWorld.setAirComponentAugmentation(phlogKey,blockPos.x,blockPos.y+1,blockPos.z,level!!.dimensionId,volumes[GasType.PHLOGISTON]!!)
        if (volumes[GasType.HELIUM]!=null) level!!.server!!.shipObjectWorld.setAirComponentAugmentation(helKey,blockPos.x,blockPos.y+1,blockPos.z,level!!.dimensionId,volumes[GasType.HELIUM]!!)


    }

    override fun getDuctNodePosition(): DuctNodePos {
        return blockPos.toJOMLD()
    }
}