package org.valkyrienskies.clockwork.content.curiosities.tools.wanderwand

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import org.valkyrienskies.clockwork.platform.CWItem

class WanderwandItem(properties: Properties) : CWItem(properties) {

    var idleProgress = 0.0f


    companion object {

        @JvmStatic
        fun select(sLevel: ServerLevel, sPlayer: ServerPlayer, firstPos: BlockPos, secondPos: BlockPos, isSecond: Boolean, deselect: Boolean, leftClick: Boolean) {


        }

        @JvmStatic
        fun startWeld(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos) {

        }
        @JvmStatic
        fun weld(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos) {

        }

        @JvmStatic
        fun attach(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos) {

        }

        @JvmStatic
        fun startBind(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos) {

        }
        @JvmStatic
        fun bind(sLevel: ServerLevel, sPlayer: ServerPlayer, clickedPos: BlockPos) {

        }
    }
}