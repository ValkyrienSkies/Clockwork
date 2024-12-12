package org.valkyrienskies.clockwork.content.contraptions.propeller.blades

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour
import com.simibubi.create.foundation.utility.animation.LerpedFloat
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState

class BladeControllerBlockEntity(type: BlockEntityType<*>, pos: BlockPos, state: BlockState) : SmartBlockEntity(type, pos,
    state
) {

    var previousBladeCount = 0

    var blades = mutableListOf<ItemStack>()
    var clientBladeAngle = LerpedFloat.linear()
        .chase(0.0, 0.5, LerpedFloat.Chaser.EXP)
    var clientBladeLength = LerpedFloat.linear()
        .chase(1.0, 0.5, LerpedFloat.Chaser.EXP)

    var bladeCooldown = 0

    var clientBladeRotation = HashMap<Int, LerpedFloat>().withDefault { LerpedFloat.linear().chase(0.0, 0.5, LerpedFloat.Chaser.EXP) }

    lateinit var bladeControlBehaviour: BladeControlBehaviour

    override fun addBehaviours(behaviours: MutableList<BlockEntityBehaviour>) {
        bladeControlBehaviour = BladeControlBehaviour(this)
        behaviours.add(bladeControlBehaviour)
    }

    override fun tick() {
        super.tick()
        if (bladeCooldown > 0) bladeCooldown--
        if (level?.isClientSide == true) {
            val angleBetweenBlades = 360.0 / blades.size.toDouble()

            if (previousBladeCount != blades.size) {
                for (i in blades.indices) {
                    clientBladeRotation[i]?.chase(angleBetweenBlades * i.toDouble(), 0.5, LerpedFloat.Chaser.EXP) ?: run {
                        clientBladeRotation[i] = LerpedFloat.linear().chase(angleBetweenBlades * i.toDouble(), 0.5, LerpedFloat.Chaser.EXP)
                    }
                }
            }

            clientBladeAngle.tickChaser()
            clientBladeLength.tickChaser()
            for (i in blades.indices) {
                clientBladeRotation[i]?.tickChaser()
            }
        }

        if (level?.isClientSide() == false) {
            val sLevel = level as ServerLevel
        }
    }

    override fun remove() {
        super.remove()
    }

    override fun destroy() {
        super.destroy()
        dropBlades()
    }

    override fun invalidate() {
        super.invalidate()
    }

    fun dropBlades() {
        if (level != null && level is ServerLevel) {
            blades.forEach {
                val itemEntity = ItemEntity(
                    level!!,
                    worldPosition.x.toDouble() + 0.5,
                    worldPosition.y.toDouble() + 0.5,
                    worldPosition.z.toDouble() + 0.5,
                    it
                )
                itemEntity.deltaMovement.add(0.0, 0.5, 0.0)
                    .scale((level!!.random.nextFloat() * .3f).toDouble())
                level!!.addFreshEntity(itemEntity)

            }
        }
        blades.clear()
    }

    override fun read(tag: CompoundTag, clientPacket: Boolean) {
        super.read(tag, clientPacket)
        blades.clear()
        val bladesTag = tag.getCompound("Blades")
        val bladeCount = tag.getInt("BladeCount")
        for (i in 1 .. bladeCount) {
            blades.add(ItemStack.of(bladesTag.getCompound("Blade$i")))
        }
        bladeCooldown = tag.getInt("BladeCooldown")
    }

    override fun write(tag: CompoundTag, clientPacket: Boolean) {
        val bladesTag = CompoundTag()
        var counter = 0
        blades.forEach {
            counter++
            bladesTag.put("Blade$counter", it.save(CompoundTag()))
        }
        tag.put("Blades", bladesTag)
        tag.putInt("BladeCount", counter)
        tag.putInt("BladeCooldown", bladeCooldown)
        super.write(tag, clientPacket)
    }

    fun insertBlade(blade: ItemStack): Boolean {
        if (blades.size < 8) {
            blades.add(blade)
            bladeCooldown = 10
            sendData()
            return true
        } else return false
    }

    fun removeBlade(index: Int): ItemStack {
        return blades.removeAt(index)
    }

    fun removeBlade(): ItemStack {
        if (blades.isEmpty()) return ItemStack.EMPTY
        val blade = blades.removeAt(blades.size - 1)
        bladeCooldown = 10
        sendData()
        return blade
    }
}