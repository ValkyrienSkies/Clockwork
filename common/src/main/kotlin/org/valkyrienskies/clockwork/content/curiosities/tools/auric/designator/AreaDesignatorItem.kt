package org.valkyrienskies.clockwork.content.curiosities.tools.auric.designator

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.TextComponent
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
import org.joml.Vector3ic
import org.joml.primitives.AABBi
import org.joml.primitives.AABBic
import org.valkyrienskies.clockwork.ClockworkPackets
import org.valkyrienskies.clockwork.ClockworkSounds
import org.valkyrienskies.clockwork.content.contraptions.phys.infuser.PhysicsInfuserBlockEntity
import org.valkyrienskies.clockwork.platform.CWItem
import org.valkyrienskies.core.impl.util.serialization.VSJacksonUtil
import org.valkyrienskies.mod.common.util.toJOML
import java.util.Random
import kotlin.math.max
import kotlin.math.min

class AreaDesignatorItem(properties: Properties) : CWItem(properties) {

    val selectedArea: SelectedAreaToolkit = SelectedAreaToolkit()
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

    override fun verifyTagAfterLoad(compoundTag: CompoundTag) {
        if (compoundTag.contains("selectedData")) {
            this.selectedArea.overwriteFrom(getMapper().readValue<SelectedAreaToolkit>(compoundTag.getByteArray("selectedData")))
        }
        super.verifyTagAfterLoad(compoundTag)
    }

    override fun canAttackBlock(state: BlockState, level: Level, pos: BlockPos, player: Player): Boolean {
        return super.canAttackBlock(state, level, pos, player)
    }

    override fun inventoryTick(stack: ItemStack, level: Level, entity: Entity, slotId: Int, isSelected: Boolean) {
        super.inventoryTick(stack, level, entity, slotId, isSelected)

        if (level.isClientSide) {
            return
        }

        if (isSelected && !this.wasSelected) {
            this.shouldRenderOutlines = true
            this.animationType = Animation.DRAW
            val pitch = Mth.randomBetween(soundRandom, 0.8f, 1.3f)
            level.playSound(
                null,
                entity,
                ClockworkSounds.DESIGNATOR_ACTIVATE.mainEvent!!,
                entity.soundSource,
                0.5f,
                pitch
            )
        } else if (!isSelected && this.wasSelected) {
            this.shouldRenderOutlines = false
        }
        this.wasSelected = isSelected
        this.idleProgress += (0.01f * Math.PI).toFloat()
        if (this.idleProgress >= 2f * Math.PI) {
            this.idleProgress = 0f
        }
        if (isSelected) {
            if (entity is Player) {
                val player = entity
            }
            if (this.animationType == Animation.IDLE) {
                this.soundTickCounter += Mth.randomBetween(soundRandom, 0.1f, 0.3f)
                if (this.soundTickCounter >= 40) {
                    this.soundTickCounter = 0f
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
            } else if (this.animationType == Animation.DRAW) {
                this.drawProgress++
                if (this.drawProgress >= 60) {
                    this.drawProgress = 0f
                    this.animationType = Animation.IDLE
                }
            } else if (this.animationType == Animation.SUCCESS) {
                this.successProgress++
                if (this.successProgress >= 40) {
                    this.successProgress = 0f
                    this.animationType = Animation.IDLE
                }
            } else if (this.animationType == Animation.DUMP) {
                this.dumpProgress++
                if (this.dumpProgress >= 40) {
                    this.dumpProgress = 0f
                    this.animationType = Animation.IDLE
                }
            }
        } else {
            this.firstPos = null
            this.secondPos = null
        }

        val compoundTag = stack.orCreateTag
        compoundTag.putByteArray("selectedData", getMapper().writeValueAsBytes(this.selectedArea))
        stack.tag = compoundTag
    }


    fun onAttack(player: Player) {
        val hitResult = getPlayerPOVHitResult(player.level, player, ClipContext.Fluid.NONE)
        val pos: Vector3ic = hitResult.blockPos.toJOML()
        val hitCluster: Set<AABBic> = this.selectedArea.getClusterContaining(pos) ?: return
        if (hitCluster != null) {
            val pitch = Mth.randomBetween(soundRandom, 0.8f, 1.2f)
            this.selectedArea.dumpCluster(hitCluster)
            player.level.playSound(
                null,
                player,
                ClockworkSounds.DESIGNATOR_DUMP_CLUSTER.mainEvent!!,
                player.soundSource,
                1.0f,
                pitch
            )
            animationType = Animation.DUMP
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
        if (this.firstPos == null) {
            this.firstPos = pos
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
        } else if (this.secondPos == null && this.firstPos != null) {
            this.secondPos = pos
            if (this.firstPos!!.distance(secondPos) > 500) {
                player.displayClientMessage(
                    TextComponent("Area Too Large!").withStyle(
                        Style.EMPTY.withColor(
                            ChatFormatting.DARK_PURPLE
                        )
                    ), true
                )
                this.firstPos = null
                this.secondPos = null
                return InteractionResult.SUCCESS
            }
            val area: AABBic = AABBi(
                min(this.firstPos!!.x(), this.secondPos!!.x()),
                min(this.firstPos!!.y(), this.secondPos!!.y()),
                min(this.firstPos!!.z(), this.secondPos!!.z()),
                max(this.firstPos!!.x(), this.secondPos!!.x()),
                max(this.firstPos!!.y(), this.secondPos!!.y()),
                max(this.firstPos!!.z(), this.secondPos!!.z())
            )
            this.firstPos = null
            this.secondPos = null
            if (this.selectedArea.containsAABB(area)) {
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
                this.animationType = Animation.DUMP
                player.cooldowns.addCooldown(this, 10)
                return InteractionResult.SUCCESS
            }

            if (this.selectedArea.selectedAreas.size >= 150) {
                player.displayClientMessage(
                    TextComponent("This Designator is at selection capacity.").withStyle(
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
                this.animationType = Animation.DUMP
                player.cooldowns.addCooldown(this, 10)
                return InteractionResult.SUCCESS
            }

            if (this.selectedArea.selectionClusters.size >= 20) {
                player.displayClientMessage(
                    TextComponent("This Designator is at cluster capacity.").withStyle(
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
                this.animationType = Animation.DUMP
                player.cooldowns.addCooldown(this, 10)
                return InteractionResult.SUCCESS
            }

            this.selectedArea.clusterNewArea(area)
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
            this.animationType = Animation.SUCCESS
            player.cooldowns.addCooldown(this, 10)
            return InteractionResult.SUCCESS
        }
        return super.useOn(context)
    }

    enum class Animation {
        DRAW,
        IDLE,
        SUCCESS,
        DUMP
    }

    companion object {
        private fun getMapper(): ObjectMapper {
            return VSJacksonUtil.defaultMapper
        }
    }
}
