package org.valkyrienskies.clockwork.content.logistics.solid.delivery

import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3
import org.joml.Vector3dc
import org.valkyrienskies.clockwork.content.logistics.solid.delivery.chute.DeliveryChuteBlockEntity
import org.valkyrienskies.core.util.x
import org.valkyrienskies.core.util.y
import org.valkyrienskies.core.util.z
import org.valkyrienskies.mod.common.util.toJOMLD

object ActiveChutes {
     val actives: HashMap<BlockPos, DeliveryChuteBlockEntity> = HashMap()
     val unloaded: HashMap<BlockPos, DeliveryChuteBlockEntity> = HashMap()

    val chutesToRemove: ArrayList<BlockPos> = ArrayList()

    fun addChute(pos: BlockPos, chute: DeliveryChuteBlockEntity) {
        actives[pos] = chute
    }

    fun removeChute(pos: BlockPos) {
        chutesToRemove.add(pos)
    }

    private fun unloadChute(pos: BlockPos) {
        unloaded[pos] = actives[pos]!!
        actives.remove(pos)
    }

    private fun loadChute(pos: BlockPos) {
        actives[pos] = unloaded[pos]!!
        unloaded.remove(pos)
    }

    fun getChutes(): HashMap<BlockPos, DeliveryChuteBlockEntity> {
        return actives
    }

    fun getNearestChute(pos: BlockPos, maxDistance: Double): BlockPos? {
        var closest: BlockPos? = null
        var closestDistance: Double = Double.MAX_VALUE
        for (chute in actives.keys) {
            val realPos = actives[chute]!!.getRealPos()
            val realBlockPos = BlockPos(realPos.x().toInt(), realPos.y().toInt(), realPos.z().toInt())
            if (realBlockPos.closerThan(pos, maxDistance)) {
                if (realPos.distance(pos.toJOMLD()) < closestDistance) {
                    closest = chute
                    closestDistance = chute.toJOMLD().distance(pos.toJOMLD())
                }
            }
        }
        return closest
    }

    fun getNearestChuteWithFrequency(pos: BlockPos, maxDistance: Double, frequency: Frequency): BlockPos? {
        var closest: BlockPos? = null
        var closestDistance: Double = Double.MAX_VALUE
        for (chute in actives.keys) {
            val realPos = actives[chute]!!.getRealPos()
            val realBlockPos = BlockPos(realPos.x().toInt(), realPos.y().toInt(), realPos.z().toInt())
            if (realBlockPos.closerThan(pos, maxDistance)) {
                if (realPos.distance(pos.toJOMLD()) < closestDistance) {
                    //if (actives[chute]!=null) continue
                    if (actives[chute]!!.frequencySlotBehaviour.frequency == frequency) {
                        closest = chute
                        closestDistance = chute.toJOMLD().distance(pos.toJOMLD())
                    }
                }
            }
        }
        return closest
    }



    fun hasChute(pos: BlockPos): Boolean {
        return actives.containsKey(pos) || unloaded.containsKey(pos)
    }

    fun getChuteRealPos(pos: BlockPos): Vec3? {
        if (actives.containsKey(pos)) {
            val temp = actives[pos]!!.getRealPos()
            return Vec3(temp.x,temp.y,temp.z)
        } else if (unloaded.containsKey(pos)) {
            val temp = unloaded[pos]!!.getRealPos()
            return Vec3(temp.x,temp.y,temp.z)
        }
        return null
    }

    fun tick(level: ServerLevel) {

        for (pos in chutesToRemove) {
            actives.remove(pos)
            unloaded
        }
        chutesToRemove.clear()

//        val toUnload: ArrayList<BlockPos> = ArrayList()
//        for (pos in actives.keys) {
//            if (!level.isLoaded(pos)) {
//                toUnload.add(pos)
//            }
//        }
//        for (pos in toUnload) {
//            unloadChute(pos)
//        }
//
//        val toLoad: ArrayList<BlockPos> = ArrayList()
//        for (pos in unloaded.keys) {
//            if (level.isLoaded(pos)) {
//                toLoad.add(pos)
//            }
//        }
//        for (pos in toLoad) {
//            loadChute(pos)
//        }
    }
}
