package org.valkyrienskies.clockwork.integration.cc.api

import dan200.computercraft.api.lua.ILuaAPI
import dan200.computercraft.api.lua.LuaFunction
import dan200.computercraft.core.apis.IAPIEnvironment
import net.minecraft.core.Direction
import org.valkyrienskies.clockwork.content.forces.DragController
import org.valkyrienskies.core.util.pollUntilEmpty

class DragAPI(private val environment: IAPIEnvironment, private val controller: DragController): ILuaAPI {
    override fun getNames() = arrayOf("drag")

    override fun update() {
        val dragDataQueue = mutableListOf<LuaDragData>()
        this.controller.dragDataQueue.pollUntilEmpty {
            dragDataQueue.add(dragDataQueue.size, LuaDragData(it))
        }
        this.environment.queueEvent("drag_queued", dragDataQueue.toList())

        super.update()
    }

    @LuaFunction
    fun getSurfaceAreaOfDirection(direction: String) =
        this.controller.surfaceAreaByDirection.clone().getValue(Direction.valueOf(direction))

    @LuaFunction
    fun getMaxHeight() = this.controller.max_height

    data class LuaDragData(val dragData: DragController.DragData) {
        @LuaFunction
        fun getDrag() = mapOf(
            Pair("x", this.dragData.drag.x()),
            Pair("y", this.dragData.drag.y()),
            Pair("z", this.dragData.drag.z())
        )

        @LuaFunction
        fun getDragPos() = mapOf(
            Pair("x", this.dragData.dragPos.x()),
            Pair("y", this.dragData.dragPos.y()),
            Pair("z", this.dragData.dragPos.z())
        )

        @LuaFunction
        fun getRotDrag() = mapOf(
            Pair("x", this.dragData.rotDrag.x()),
            Pair("y", this.dragData.rotDrag.y()),
            Pair("z", this.dragData.rotDrag.z())
        )
    }
}