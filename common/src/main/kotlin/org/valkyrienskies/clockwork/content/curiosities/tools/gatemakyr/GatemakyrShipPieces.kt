package org.valkyrienskies.clockwork.content.curiosities.tools.gatemakyr

import net.minecraft.world.level.Level
import net.minecraft.world.entity.Entity
import org.joml.Quaterniond
import org.valkyrienskies.core.api.ships.ClientShip
import org.valkyrienskies.mod.common.getShipMountedToData
import org.valkyrienskies.mod.common.util.toMinecraft
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4dc
import org.joml.Vector3d
import org.joml.Vector4dc
import org.valkyrienskies.core.api.ships.QueryableShipData
import org.valkyrienskies.mod.common.allShips

internal object GatemakyrShipPieces {
    private val split = QueryableShipData::class.java.methods.firstOrNull { it.name == "getSplitPieces" }
    private class Access(val type: Class<*>) {
        val transform = type.getMethod("getShipToWorld")
        val planes = type.getMethod("getClippingPlanes")
        val rendered by lazy { Class.forName("org.valkyrienskies.mod.common.render.batched.ShipPieceRenderFrames")
            .getMethod("rendered", type) }
    }
    private val access by lazy { Access(Class.forName("org.valkyrienskies.core.api.ships.ShipPhysicalPiece")) }

    data class Seat(val feet: Vec3, val eye: Vec3, val canonicalEye: Vec3, val rotation: Quaterniond)

    @JvmStatic fun seat(rider: Entity, render: Boolean, pt: Float): Seat? {
        if (!rider.isPassenger || split == null) return null
        val mount = getShipMountedToData(rider, if (render) pt else null) ?: return null
        val ship = mount.shipMountedTo
        val canonical = if (render && ship is ClientShip) ship.renderTransform.shipToWorld else ship.shipToWorld
        val local = mount.mountPosInShip
        val piece = transform(rider.level(), ship.id, local.toMinecraft(), canonical, render) ?: return null
        if (piece === canonical) return null
        val feet = piece.transformPosition(local, Vector3d())
        val canonicalUp = canonical.transformDirection(0.0, 1.0, 0.0, Vector3d()).normalize().mul(rider.eyeHeight.toDouble())
        val up = piece.transformDirection(0.0, 1.0, 0.0, Vector3d()).normalize().mul(rider.eyeHeight.toDouble())
        val rotation = piece.getUnnormalizedRotation(Quaterniond()).mul(canonical.getUnnormalizedRotation(Quaterniond()).conjugate())
        return Seat(feet.toMinecraft(), up.add(feet).toMinecraft(),
            canonical.transformPosition(local, Vector3d()).add(canonicalUp).toMinecraft(), rotation)
    }

    fun transform(level: Level, shipId: Long, position: Vec3, canonical: Matrix4dc, render: Boolean): Matrix4dc? {
        val pieces = split?.invoke(level.allShips, shipId) as? List<*> ?: return canonical
        if (pieces.isEmpty()) return canonical
        val anchor = Vector3d(position.x, position.y, position.z)
        val piece = pieces.singleOrNull {
            val point = (access.transform.invoke(it) as Matrix4dc).transformPosition(anchor, Vector3d())
            (access.planes.invoke(it) as List<*>).all { plane ->
                plane as Vector4dc
                plane.x() * point.x + plane.y() * point.y + plane.z() * point.z + plane.w() >= -1e-8
            }
        } ?: return null
        return access.transform.invoke(if (render) access.rendered.invoke(null, piece) else piece) as Matrix4dc
    }
}
