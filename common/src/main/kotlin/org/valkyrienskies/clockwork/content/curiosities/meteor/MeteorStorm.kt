package org.valkyrienskies.clockwork.content.curiosities.meteor

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import org.joml.Vector3d
import org.joml.Vector3i
import org.valkyrienskies.core.api.util.GameTickOnly
import org.valkyrienskies.kelvin.util.KelvinExtensions.toMinecraft
import org.valkyrienskies.mod.api.dimensionId
import org.valkyrienskies.mod.api.vsApi
import org.valkyrienskies.mod.common.assembly.ShipAssembler
import org.valkyrienskies.mod.common.shipObjectWorld
import org.valkyrienskies.mod.common.vsCore

object MeteorStorm {

    @OptIn(GameTickOnly::class)
    fun assembleMeteor(level: ServerLevel, at: Vector3i) {
        val ship = level.shipObjectWorld.createNewShipAtBlock(at, false, 1.0, level.dimensionId)
        val center = ship.kinematics.positionInModel.toMinecraft()
        ship.isStatic = true
        println("1 ${ship}")
        MeteorGenerator.generate(level, center, 0.035, 10, 3)
        println("2 ${ship}")
        ship.isStatic = false

    }
}
