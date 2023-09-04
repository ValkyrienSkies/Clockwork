package org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator

import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3ic
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlockEntity
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.core.impl.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.util.toJOML
import java.util.*


class AreaDesignatorItem(properties: Properties) : CWItem(properties) {
    var selectedAreas: HashMap<AABBic, String> = HashMap()
    var selectionClusters: MutableSet<Set<AABBic>> = HashSet()
    private val toBeStored = ArrayList<AABBic>()
    private val toBeRemoved = ArrayList<Set<AABBic>>()
    var toStopRendering = ArrayList<Set<AABBic>>()
    private var wasSelected = false
    var firstPos: Vector3ic? = null
    var secondPos: Vector3ic? = null
    var shouldRenderOutlines = false
    private val soundRandom = Random()
    private var soundTickCounter = 0f

    //ANIMATION
    var animationType = Animation.IDLE
    var drawProgress = 0f
    var successProgress = 0f
    var dumpProgress = 0f
    var idleProgress = 0f
    var hasBeenLoaded = false
    var loadCooldown = 100
    private var nextKey = 0
    override fun verifyTagAfterLoad(compoundTag: CompoundTag) {
        reloadClusters(compoundTag)
        nextKey = compoundTag.getInt("nextKey")
        hasBeenLoaded = true
        super.verifyTagAfterLoad(compoundTag)
    }

    private fun clusterNewArea(initial: AABBic) {
        val newCluster: MutableSet<AABBic> = HashSet()
        newCluster.add(initial)
        val makeNewCluster = true
        //        for (AABBic area : selectedAreas.keySet()) {
//            if (initial.containsAABB(area)) {
//                boolean existingCluster = false;
//                Set<AABBic> foundCluster = new HashSet<>();
//                for (Set<AABBic> cluster : selectionClusters) {
//                    if (cluster.contains(area) && !existingCluster) {
//                        cluster.add(initial);
//                        foundCluster = new HashSet<>(cluster);
//                        existingCluster = true;
//                        makeNewCluster = false;
//                    } else if (cluster.contains(area)) {
//                        foundCluster.addAll(cluster);
//                        selectionClusters.remove(cluster);
//                        makeNewCluster = true;
//                        newCluster.addAll(foundCluster);
//                    }
//                }
//            }
//        }
        if (makeNewCluster) {
            toBeStored.add(initial)
            selectionClusters.add(newCluster)
            mergeClusters(initial)
        }
    }

    private fun massClusterAreas(areas: Set<AABBic>) {
        for (box in areas) {
            mergeClusters(box)
        }
    }

    private fun reloadClusters(tag: CompoundTag) {
        var keyToCheck = pointDataSaveKey + "0"
        var increment = 0
        var highestKey = 0
        val refreshedTag = CompoundTag()
        val toReload: MutableSet<AABBic> = HashSet()
        while (increment <= nextKey) {
            if (tag.contains(keyToCheck)) {
                val pointData = tag.getIntArray(keyToCheck)
                val loaded: AABBic = AABBi(
                    pointData[0],
                    pointData[1], pointData[2], pointData[3], pointData[4], pointData[5]
                )
                //clusterNewArea(loaded);
                toReload.add(loaded)
                selectedAreas[loaded] = keyToCheck
                highestKey++
                refreshedTag.putIntArray(pointDataSaveKey + highestKey, pointData)
            }
            increment++
            keyToCheck = pointDataSaveKey + increment
        }

//        if (highestKey < nextKey-1) {
//            nextKey = highestKey+1;
//            tag = refreshedTag;
//        }
        massClusterAreas(toReload)
    }

    private fun mergeClusters(starter: AABBic) {
//        Set<Set<AABBic>> clustersTemp = new HashSet<>(selectionClusters);
//        for (Set<AABBic> cluster : clustersTemp) {
//            Set<AABBic> newCluster = new HashSet<>(cluster);
//            for (AABBic area : cluster) {
//                for (Set<AABBic> cluster2 : clustersTemp) {
//                    for (AABBic area2 : cluster2) {
//                        if (area.containsAABB(area2) && !area.equals(area2)) {
//                            newCluster.addAll(cluster2);
//                            dumpClusterDirty(cluster2);
//                            break;
//                        }
//                    }
//                }
//            }
//            selectionClusters.add(newCluster);
//        }
        val newCluster: MutableSet<AABBic> = HashSet()

        //get direct neighbors
        for (area in selectedAreas.keys) {
            if (starter.intersectsAABB(area)) {
                newCluster.add(area)
            }
        }
        //spiral out of control
        val toCheck = ArrayList(newCluster)
        while (!toCheck.isEmpty()) {
            val check = toCheck[0]
            for (area in selectedAreas.keys) {
                if (check!!.intersectsAABB(area) && !newCluster.contains(area) && !toCheck.contains(area)) {
                    newCluster.add(area)
                    toCheck.add(area)
                }
            }
            toCheck.removeAt(0)
        }
        //finish off the insanity by adding back the initial
        newCluster.add(starter)

        //now check to dump all clusters that were merged
        for (check in newCluster) {
            val oldCluster = getClusterContainingAABB(check)
            oldCluster?.let { dumpClusterDirty(it) }
        }
        selectionClusters.add(newCluster)
    }

    override fun canAttackBlock(state: BlockState, level: Level, pos: BlockPos, player: Player): Boolean {
        return super.canAttackBlock(state, level, pos, player)
    }

    fun onAttack(player: Player) {
        val hitResult = getPlayerPOVHitResult(player.level, player, ClipContext.Fluid.NONE)
        val pos: Vector3ic = hitResult.blockPos.toJOML()
        val hitCluster = getClusterContaining(pos)
        if (hitCluster != null) {
            val pitch = Mth.randomBetween(soundRandom, 0.8f, 1.2f)
            dumpCluster(hitCluster)
            player.level.playSound(
                null,
                player,
                ClockworkSounds.DESIGNATOR_DUMP_CLUSTER.mainEvent!!,
                player.soundSource,
                0.5f,
                pitch
            )
            animationType = Animation.DUMP
        }
    }

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        super.inventoryTick(stack, level, entity, slotId, isSelected)
        if (loadCooldown > 0) {
            loadCooldown--
        } else {
            if (!hasBeenLoaded) {
                val tag = stack.getOrCreateTag()
                if (tag.contains("nextKey")) {
                    nextKey = tag.getInt("nextKey")
                }
                reloadClusters(tag)
                hasBeenLoaded = true
            } else {
                while (!toBeStored.isEmpty()) {
                    if (stack.hasTag()) {
                        val nbt = stack.getOrCreateTag()
                        nextKey = nbt.getInt("nextKey")
                        val toStore = toBeStored[0]
                        val pointData = intArrayOf(
                            toStore.minX(),
                            toStore.minY(),
                            toStore.minZ(),
                            toStore.maxX(),
                            toStore.maxY(),
                            toStore.maxZ()
                        )
                        nbt.putIntArray(pointDataSaveKey + nextKey, pointData)
                        selectedAreas[toStore] = pointDataSaveKey + nextKey
                        nextKey++
                        nbt.putInt("nextKey", nextKey)
                        toBeStored.removeAt(0)
                    }
                }
                while (!toBeRemoved.isEmpty()) {
                    if (stack.hasTag()) {
                        val nbt = stack.getOrCreateTag()
                        val set = toBeRemoved.removeAt(0)
                        for (box in set) {
                            val key = selectedAreas[box]
                            nbt.remove(key)
                            selectedAreas.remove(box)
                        }
                    }
                }
            }
        }
        if (level.isClientSide) {
            return
        }
        if (isSelected && !wasSelected) {
            shouldRenderOutlines = true
            animationType = Animation.DRAW
            val pitch = Mth.randomBetween(soundRandom, 0.8f, 1.3f)
            level.playSound(
                null,
                entity,
                ClockworkSounds.DESIGNATOR_ACTIVATE.mainEvent!!,
                entity.soundSource,
                0.5f,
                pitch
            )
        } else if (!isSelected && wasSelected) {
            shouldRenderOutlines = false
        }
        wasSelected = isSelected
        idleProgress += (0.01f * Math.PI).toFloat()
        if (idleProgress >= 2f * Math.PI) {
            idleProgress = 0f
        }
        if (isSelected) {
            if (entity is Player) {
                val player = entity
            }
            if (animationType == Animation.IDLE) {
                soundTickCounter += Mth.randomBetween(soundRandom, 0.1f, 0.3f)
                if (soundTickCounter >= 10) {
                    soundTickCounter = 0f
                    val pitch = Mth.randomBetween(soundRandom, 0.8f, 1.2f)
                    level.playSound(
                        null,
                        entity,
                        ClockworkSounds.DESIGNATOR_IDLE.mainEvent!!,
                        entity.soundSource,
                        0.5f,
                        pitch
                    )
                }
            } else if (animationType == Animation.DRAW) {
                drawProgress++
                if (drawProgress >= 60) {
                    drawProgress = 0f
                    animationType = Animation.IDLE
                }
            } else if (animationType == Animation.SUCCESS) {
                successProgress++
                if (successProgress >= 40) {
                    successProgress = 0f
                    animationType = Animation.IDLE
                }
            } else if (animationType == Animation.DUMP) {
                dumpProgress++
                if (dumpProgress >= 40) {
                    dumpProgress = 0f
                    animationType = Animation.IDLE
                }
            }
        } else {
            firstPos = null
            secondPos = null
        }
    }

    override fun useOn(context: UseOnContext): InteractionResult {
        val player = context.player ?: return InteractionResult.FAIL
        val world = context.level
        if (world.isClientSide) {
            return InteractionResult.PASS
        }
        if (world.getBlockEntity(context.clickedPos) is PhysicsInfuserBlockEntity) {
            return InteractionResult.PASS
        }
        val hand = context.hand
        val stack = player.getItemInHand(hand)
        val pos: Vector3ic = context.clickedPos.toJOML()
        if (!stack.`is`(this)) {
            return super.useOn(context)
        }
        val pitch = Mth.randomBetween(soundRandom, 0.8f, 1.2f)
        if (firstPos == null) {
            firstPos = pos
            player.displayClientMessage(
                TextComponent("First Position Selected!").withStyle(
                    Style.EMPTY.withColor(
                        ChatFormatting.DARK_PURPLE
                    )
                ), true
            )
            world.playSound(
                null,
                player,
                ClockworkSounds.DESIGNATOR_SELECT_START.mainEvent!!,
                player.soundSource,
                0.5f,
                pitch
            )
            player.cooldowns.addCooldown(this, 10)
            ClockworkPackets.sendToClientsTrackingAndSelf(AreaDesignatorSelectionPacket(this), player as ServerPlayer)
            return InteractionResult.SUCCESS
        } else if (secondPos == null && firstPos != null) {
            secondPos = pos
            val area: AABBic = AABBi(
                Math.min(firstPos!!.x(), secondPos!!.x()), Math.min(
                    firstPos!!.y(), secondPos!!.y()
                ), Math.min(firstPos!!.z(), secondPos!!.z()), Math.max(
                    firstPos!!.x(), secondPos!!.x()
                ), Math.max(firstPos!!.y(), secondPos!!.y()), Math.max(
                    firstPos!!.z(), secondPos!!.z()
                )
            )
            firstPos = null
            secondPos = null
            if (selectedAreas.containsKey(area)) {
                player.displayClientMessage(
                    TextComponent("Area Already Exists.").withStyle(
                        Style.EMPTY.withColor(
                            ChatFormatting.DARK_PURPLE
                        )
                    ), true
                )
                world.playSound(
                    null,
                    player,
                    ClockworkSounds.PHYSICS_INFUSER_LIGHTNING.mainEvent!!,
                    player.soundSource,
                    0.5f,
                    pitch
                )
                animationType = Animation.DUMP
                player.cooldowns.addCooldown(this, 10)
                return InteractionResult.SUCCESS
            }
            clusterNewArea(area)
            player.displayClientMessage(
                TextComponent("Area Designated!").withStyle(Style.EMPTY.withColor(ChatFormatting.DARK_PURPLE)),
                true
            )
            world.playSound(
                null,
                player,
                ClockworkSounds.DESIGNATOR_SELECT_END.mainEvent!!,
                player.soundSource,
                0.5f,
                pitch
            )
            stack.damageValue = stack.damageValue - 1
            animationType = Animation.SUCCESS
            player.cooldowns.addCooldown(this, 10)
            return InteractionResult.SUCCESS
        }
        return super.useOn(context)
    }

    fun getClosestCluster(pos: Vector3ic): Set<AABBic?> {
        var returnCluster: Set<AABBic?> = HashSet()
        var closestDistance = Double.MAX_VALUE
        for (cluster in selectionClusters) {
            for (area in cluster) {
                if (area!!.containsPoint(pos)) {
                    return cluster
                } else {
                    val center: Vector3dc = area.center(Vector3d())
                    val distance = center.distance(pos.x().toDouble(), pos.y().toDouble(), pos.z().toDouble())
                    if (distance < closestDistance) {
                        closestDistance = distance
                        returnCluster = cluster
                    }
                }
            }
        }
        return returnCluster
    }

    fun getClusterContaining(pos: Vector3ic?): Set<AABBic>? {
        for (cluster in selectionClusters) {
            for (area in cluster) {
                if (area!!.containsPoint(pos)) {
                    return cluster
                }
            }
        }
        return null
    }

    fun getClusterContainingAABB(box: AABBic?): Set<AABBic>? {
        for (cluster in selectionClusters) {
            if (cluster.contains(box)) {
                return cluster
            }
        }
        return null
    }

    fun denseBlocksFromCluster(cluster: Set<AABBic>): DenseBlockPosSet {
        val set = DenseBlockPosSet()
        for (area in cluster) {
            for (x in area.minX()..area.maxX()) {
                for (y in area.minY()..area.maxY()) {
                    for (z in area.minZ()..area.maxZ()) {
                        set.add(x, y, z)
                    }
                }
            }
        }
        return set
    }

    fun blocksFromCluster(cluster: Set<AABBic>): Set<BlockPos> {
        val set: MutableSet<BlockPos> = HashSet()
        for (area in cluster) {
            for (x in area.minX()..area.maxX()) {
                for (y in area.minY()..area.maxY()) {
                    for (z in area.minZ()..area.maxZ()) {
                        set.add(BlockPos(x, y, z))
                    }
                }
            }
        }
        return set
    }

    fun entitiesFromCluster(cluster: Set<AABBic>, level: ServerLevel): Set<Entity> {
        val set: MutableSet<Entity> = HashSet()
        for (area in cluster) {
            val box = AABB(
                area.maxX().toDouble(),
                area.maxY().toDouble(),
                area.maxZ().toDouble(),
                area.minX().toDouble(),
                area.minY().toDouble(),
                area.minZ().toDouble()
            )
            set.addAll(level.getEntities(null, box))
        }
        return set
    }

    fun getAABBFromPos(pos: Vector3ic): AABBic? {
        for (area in selectedAreas.keys) {
            if (area!!.containsPoint(pos)) {
                return area
            }
        }
        return null
    }

    fun dumpCluster(cluster: Set<AABBic>) {
        selectionClusters.remove(cluster)
        toBeRemoved.add(cluster)
        toStopRendering.add(cluster)
    }

    fun dumpClusterDirty(cluster: Set<AABBic>) {
        selectionClusters.remove(cluster)
        toStopRendering.add(cluster)
    }

    enum class Animation {
        DRAW,
        IDLE,
        SUCCESS,
        DUMP
    }

    companion object {
        private const val pointDataSaveKey = "pointData_"
    }
}