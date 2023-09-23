package org.valkyrienskies.clockwork.content.contraptions.phys.infuser

import com.simibubi.create.content.contraptions.AbstractContraptionEntity
import com.simibubi.create.content.contraptions.AssemblyException
import com.simibubi.create.content.contraptions.actors.seat.SeatEntity
import com.simibubi.create.content.contraptions.glue.SuperGlueEntity
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.client.Minecraft
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.Mth
import net.minecraft.world.ContainerHelper
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkMod
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.client.render.scanner.ScannerRenderer
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserRenderer.Companion.SCAN_GROWTH_DURATION
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.AreaDesignatorItem
import org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator.SelectedAreaToolkit
import org.valkyrienskies.clockwork.util.EaseHelper.easeInBounce
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.core.api.ships.Ship
import org.valkyrienskies.core.impl.datastructures.DenseBlockPosSet
import org.valkyrienskies.mod.common.assembly.createNewShipWithBlocks
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD
import org.valkyrienskies.mod.common.util.toMinecraft
import java.util.*
import java.util.function.Consumer

class PhysicsInfuserBlockEntity(type: BlockEntityType<*>?, pos: BlockPos?, state: BlockState?) :
    SmartBlockEntity(type, pos, state), WorldlyContainer {
    private val thisposition = worldPosition.toJOMLD().toMinecraft()
    var isAssembled = false
    var assembling = false
    var disassembling = false
    var animationType: Animation? = Animation.IDLE
    var assemblyProgress = LerpedFloat.linear()
    var disassemblyProgress = LerpedFloat.linear()
    var idleProgress = LerpedFloat.linear()
    protected var lastException: AssemblyException? = null
    var skippingAssembly = false
    var coreAngle = 0f
    var previousCoreAngle = 0f
    var useCooldown = 0
    var onCooldown = false
    var initPlayed = false
    var connectedShip: Ship? = null
        private set
    private val createdShips: MutableSet<Ship> = HashSet()
    private var sendAnimationUpdate = false
    private val toDump: MutableSet<Set<AABBic>> = HashSet()
    var shouldEjectDesignator = false
    var inventory = NonNullList.withSize(1, ItemStack.EMPTY)

    var storedClusters: SelectedAreaToolkit = SelectedAreaToolkit()

    override fun getSlotsForFace(side: Direction): IntArray {
        return IntArray(0)
    }

    override fun canPlaceItemThroughFace(index: Int, itemStack: ItemStack, direction: Direction?): Boolean {
        return itemStack.item is AreaDesignatorItem
    }

    override fun canTakeItemThroughFace(index: Int, stack: ItemStack, direction: Direction): Boolean {
        return true
    }

    override fun getContainerSize(): Int {
        return 1
    }

    override fun isEmpty(): Boolean {
        return inventory[0].isEmpty
    }

    override fun getItem(slot: Int): ItemStack {
        return inventory[0]
    }

    override fun removeItem(slot: Int, amount: Int): ItemStack {
        return inventory.removeAt(0)
    }

    override fun removeItemNoUpdate(slot: Int): ItemStack {
        return inventory.removeAt(0)
    }

    override fun setItem(slot: Int, stack: ItemStack) {
        inventory[0] = stack
    }

    override fun setChanged() {
        ClockworkPackets.sendToNear(getLevel(), worldPosition, 64, PhysicsInfuserSyncPacket(this))
        super.setChanged()
    }

    override fun stillValid(player: Player): Boolean {
        return true
    }

    override fun clearContent() {
        inventory[0] = ItemStack.EMPTY
    }

    fun initialize(center: Vec3?, scanRadius: Float, scanComputeDuration: Int) {}
    override fun tick() {
        super.tick()
        if (level == null) return
        if (shouldEjectDesignator) {
            shouldEjectDesignator = false
            if (inventory[0].isEmpty) return
            var launchForce = 0
            for (cluster in toDump) {
                val adi: AreaDesignatorItem = inventory[0].item as AreaDesignatorItem
                adi.selectedArea.dumpCluster(cluster)
                launchForce++
            }
            toDump.clear()
            val ejected = ItemEntity(
                level, blockPos.x.toDouble(), (blockPos.y + 1).toDouble(), blockPos.z.toDouble(),
                inventory[0]
            )
            inventory[0] = ItemStack.EMPTY
            ejected.deltaMovement = Vec3(0.0, launchForce.toDouble(), 0.0)
            level!!.addFreshEntity(ejected)
        }
        if (level is ServerLevel) {
            connectedShip = level.getShipObjectManagingPos(worldPosition)
        }
        if (useCooldown > 0) {
            onCooldown = true
            useCooldown--
        }
        if (useCooldown == 0) {
            onCooldown = false
        }
        if (animationType == null) {
            animationType = Animation.IDLE
        }
        if (animationType == Animation.IDLE) {
            startAnimation(Animation.IDLE)
        }
        if (assembling) {
            assemblyProgress.setValue((assemblyProgress.value + 1).toDouble())
        }
        //        if (disassembling) {
//            disassemblyProgress.setValue(disassemblyProgress.getValue() + 1);
//        }
        val rand = level!!.getRandom()
        //client sounds
        if (assembling) {
            if (assemblyProgress.value == 0f) {
                playInitializeSound(level!!, thisposition)
            }
            if (assemblyProgress.value == 100f) {
                playWindupSound(level!!, thisposition)
            }
            if (assemblyProgress.value == 160f || assemblyProgress.value == 220f || assemblyProgress.value == 240f || assemblyProgress.value == 300f || assemblyProgress.value == 320f || assemblyProgress.value == 360f || assemblyProgress.value == 400f || assemblyProgress.value == 410f || assemblyProgress.value == 420f) {
                playZapSound(level!!, thisposition, rand)
            }
            if (assemblyProgress.value == 455f) {
                assemble()
            }
            if (assemblyProgress.value == 460f) {
                playFinishSound(level!!, thisposition)
                if (level!!.isClientSide) {
                    for (cship in createdShips) {
                        ScannerRenderer.INSTANCE.ping(cship as ClientShip, thisposition, this)
                        createdShips.remove(cship)
                    }
                }
            }
            if (assemblyProgress.value == 500f) {
                resetAfterAssemble()
            }
        }
    }

    private fun resetAfterAssemble() {
//        isAssembled = true;
        assembling = false
        skippingAssembly = false
        initPlayed = false
        animationType = Animation.IDLE
        startAnimation(Animation.IDLE)
        assemblyProgress.startWithValue(0.0)
        useCooldown = 400
        shouldEjectDesignator = true
    }

    //Ship Assembly Handlers
    fun startAssembly() {
        assembling = true
        animationType = Animation.ASSEMBLY
        startAnimation(Animation.ASSEMBLY)
    }

    fun skipAssembly() {
        skippingAssembly = true
    }

    fun startDisassembly() {
        disassembling = true
        animationType = Animation.DISASSEMBLY
        startAnimation(Animation.DISASSEMBLY)
    }

    val pulseRange: Double
        get() = if (connectedShip != null) {
            val shipAABB = connectedShip!!.shipAABB
            val max = Vector3d(
                shipAABB!!.maxX().toDouble(),
                shipAABB.maxY().toDouble(),
                shipAABB.maxZ().toDouble()
            )
            val min = Vector3d(
                shipAABB.minX().toDouble(),
                shipAABB.minY().toDouble(),
                shipAABB.minZ().toDouble()
            )
            max.distance(min)
        } else {
            Minecraft.getInstance().gameRenderer.renderDistance.toDouble()
        }
    val scanGrowthDuration: Int
        //Animation Jargon
        get() {
            if (connectedShip != null) {
                val range = pulseRange
                return SCAN_GROWTH_DURATION * range.toInt() / 12
            }
            return SCAN_GROWTH_DURATION * Minecraft.getInstance().options.renderDistance / 12
        }

    fun computeRadius(start: Long, duration: Float): Float {
        // Scan wave speeds up exponentially. To avoid the initial speed being
        // near zero due to that we offset the time and adjust the remaining
        // parameters accordingly. Base equation is:
        //   r = a + (t + b)^2 * c
        // with r := 0 and target radius and t := 0 and target time this yields:
        //   c = r1/((t1 + b)^2 - b*b)
        //   a = -r1*b*b/((t1 + b)^2 - b*b)
        val r1 = pulseRange.toFloat()
        val b = 200f
        val n = 1f / ((duration + b) * (duration + b) - b * b)
        val a = -r1 * b * b * n
        val c = r1 * n
        val t = (System.currentTimeMillis() - start).toFloat()
        return 10 + a + (t + b) * (t + b) * c
    }

    fun assemble() {
        if (getLevel()!!.isClientSide()) return
        if (inventory[0].item !is AreaDesignatorItem) return
        val item: AreaDesignatorItem = inventory[0].item as AreaDesignatorItem
        item.selectedArea.selectionClusters.forEach { cluster ->
            val selection: DenseBlockPosSet
            val caughtEntities: Set<Entity>
            if (level is ServerLevel) {
                selection = SelectedAreaToolkit.denseBlocksFromCluster(cluster)
                caughtEntities = SelectedAreaToolkit.entitiesFromCluster(cluster, (level as ServerLevel))
                if (selection == null) return@forEach
                connectedShip = createNewShipWithBlocks(worldPosition, selection, level as ServerLevel)
                // TODO: relocate entities properly cause it barely works
                if (caughtEntities != null) {
                    caughtEntities.forEach(Consumer { entity: Entity ->
                        if (entity is AbstractContraptionEntity || entity is SuperGlueEntity || entity is SeatEntity) {
                            if (entity !is SuperGlueEntity) {
                                val oldPos: Vector3dc = entity.position().toJOML()
                                val newPos: Vector3dc =
                                    connectedShip!!.transform.worldToShip.transformPosition(oldPos, Vector3d())
                                entity.moveTo(newPos.toMinecraft())
                            } else {
                                val glueEntity =
                                    entity
                                val oldBounds = glueEntity.boundingBox
                                val oldMax: Vector3dc =
                                    Vector3d(oldBounds.maxX, oldBounds.maxY, oldBounds.maxZ)
                                val oldMin: Vector3dc =
                                    Vector3d(oldBounds.minX, oldBounds.minY, oldBounds.minZ)
                                val newMax: Vector3dc =
                                    connectedShip!!.transform.worldToShip.transformPosition(oldMax, Vector3d())
                                val newMin: Vector3dc =
                                    connectedShip!!.transform.worldToShip.transformPosition(oldMin, Vector3d())
                                val newBounds = AABB(
                                    newMin.x(),
                                    newMin.y(),
                                    newMin.z(),
                                    newMax.x(),
                                    newMax.y(),
                                    newMax.z()
                                )
                                glueEntity.boundingBox = newBounds
                                glueEntity.resetPositionToBB()
                            }
                        }
                    })
                }
                createdShips.add(connectedShip!!)
            }
            toDump.add(cluster)
        }
    }

    fun disassemble() {}
    fun startAnimation(animation: Animation) {
        animationType = animation
        if (animation == Animation.ASSEMBLY) {
            assemblyProgress.startWithValue(0.0)
        } else if (animation == Animation.DISASSEMBLY) {
            disassemblyProgress.startWithValue(0.0)
        } else if (animation == Animation.IDLE) {
            idleProgress.startWithValue(0.0)
        }
        sendAnimationUpdate = true
        sendData()
    }

    fun getInterpolatedCoreAngle(partialTicks: Float): Float {
        previousCoreAngle = coreAngle
        coreAngle++
        if (coreAngle == 360f) {
            coreAngle = 0f
        }
        return if (isVirtual) Mth.lerp(partialTicks + .5f, previousCoreAngle, coreAngle) else Mth.lerp(
            partialTicks,
            coreAngle,
            coreAngle + 4f
        )
    }

    fun getCoreOffset(partialTicks: Float): Float {
        if (animationType == Animation.IDLE) {
            return 0f
        } else if (animationType == Animation.ASSEMBLY) {
            val runningTicks = Math.abs(assemblyProgress.value).toInt()
            val prevRunningTicks = Math.abs(assemblyProgress.value - 1).toInt()
            val ticks = Mth.lerp(partialTicks, prevRunningTicks.toFloat(), runningTicks.toFloat())
            return if (runningTicks < ASSEMBLY_TIME * 3 / 4) {
                Mth.clamp(Math.pow((ticks / ASSEMBLY_TIME * 3).toDouble(), 4.0), 0.0, 1.0).toFloat()
            } else easeInBounce(Mth.clamp((ASSEMBLY_TIME - ticks) / ASSEMBLY_TIME * 8, 0f, 1f))
        } else if (animationType == Animation.DISASSEMBLY) {
            return disassemblyProgress.getValue(partialTicks)
        }
        return 0f
    }

    public override fun write(compound: CompoundTag, clientPacket: Boolean) {
        compound.putString("animationState", animationType.toString())
        compound.putFloat("assemblyProgress", assemblyProgress.value)
        compound.putFloat("disassemblyProgress", disassemblyProgress.value)
        compound.putFloat("idleProgress", idleProgress.value)
        compound.putBoolean("isAssembled", isAssembled)
        compound.putBoolean("assembling", assembling)
        compound.putBoolean("disassembling", disassembling)
        ContainerHelper.saveAllItems(compound, inventory)
        super.write(compound, clientPacket)
    }

    //NBT stuff
    override fun read(compound: CompoundTag, clientPacket: Boolean) {
        animationType =
            if (compound.getString("animationState") == "ASSEMBLY") Animation.ASSEMBLY else if (compound.getString("animationState") == "DISASSEMBLY") Animation.DISASSEMBLY else Animation.IDLE
        assemblyProgress.setValueNoUpdate(compound.getFloat("assemblyProgress").toDouble())
        disassemblyProgress.setValueNoUpdate(compound.getFloat("disassemblyProgress").toDouble())
        idleProgress.setValueNoUpdate(compound.getFloat("idleProgress").toDouble())
        isAssembled = compound.getBoolean("isAssembled")
        assembling = compound.getBoolean("assembling")
        disassembling = compound.getBoolean("disassembling")
        inventory = NonNullList.withSize(this.containerSize, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(compound, inventory)
        super.read(compound, clientPacket)
    }

    override fun addBehaviours(behaviours: List<BlockEntityBehaviour>) {}

    //Create Behaviors
    enum class Animation {
        ASSEMBLY,
        DISASSEMBLY,
        IDLE
    }

    companion object {
        const val ASSEMBLY_TIME = 500
        const val DISASSEMBLY_TIME = 1000
        fun playInitializeSound(world: Level, location: Vec3) {
            ClockworkSounds.PHYSICS_INFUSER_INITIALIZE.playAt(world, location, 1f, 1f, false)
        }

        fun playWindupSound(world: Level, location: Vec3) {
            ClockworkSounds.PHYSICS_INFUSER_WINDUP.playAt(world, location, 1f, 1f, false)
        }

        fun playZapSound(world: Level, location: Vec3, rand: Random) {
            val pitch = 0.6f + rand.nextFloat() * 0.4f
            ClockworkSounds.PHYSICS_INFUSER_LIGHTNING.playAt(world, location, 1f, 1f, false)
        }

        fun playFinishSound(world: Level, location: Vec3) {
            ClockworkSounds.PHYSICS_INFUSER_FINISH.playAt(world, location, 1f, 1f, false)
        }

        fun spawnParticlesAssembly(world: Level, pos: Vec3, rand: Random) {
            val degrees = rand.nextDouble() * 360
            val angle = Math.toRadians(degrees)
            val radius = 2.0
            val x = radius * Math.cos(angle)
            val y = 0.5
            val z = radius * Math.sin(angle)
        }
    }
}
