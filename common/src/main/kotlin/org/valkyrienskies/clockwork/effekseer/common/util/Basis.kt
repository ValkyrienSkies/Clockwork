package org.valkyrienskies.clockwork.effekseer.common.util

import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import kotlin.math.cos
import kotlin.math.sin


@JvmRecord
data class Basis(
    val row0: Vec3,
    val row1: Vec3,
    val row2: Vec3,
    val x: Vec3,
    val y: Vec3,
    val z: Vec3
) {
    fun toLocal(global: Vec3): Vec3 {
        return Vec3(global.dot(x), global.dot(y), global.dot(z))
    }

    fun toGlobal(local: Vec3): Vec3 {
        return Vec3(
            x.x * local.x + y.x * local.y + z.x * local.z,
            x.y * local.x + y.y * local.y + z.y * local.z,
            x.z * local.x + y.z * local.y + z.z * local.z
        )
    }

    fun tDotX(with: Vec3): Double {
        return row0.x * with.x + row1.x * with.y + row2.x * with.z
    }

    fun tDotY(with: Vec3): Double {
        return row0.y * with.x + row1.y * with.y + row2.y * with.z
    }

    fun tDotZ(with: Vec3): Double {
        return row0.z * with.x + row1.z * with.y + row2.z * with.z
    }

    companion object {
        fun factory(row0: Vec3, row1: Vec3, row2: Vec3): Basis {
            val x = Vec3(row0.x, row1.x, row2.x)
            val y = Vec3(row0.y, row1.y, row2.y)
            val z = Vec3(row0.z, row1.z, row2.z)
            return Basis(row0, row1, row2, x, y, z)
        }

        /**
         * Basis from x, y, z
         * Godot's basis constructor
         */
        fun fromVectors(x: Vec3, y: Vec3, z: Vec3): Basis {
            val row0 = Vec3(x.x, y.x, z.x)
            val row1 = Vec3(x.y, y.y, z.y)
            val row2 = Vec3(x.z, y.z, z.z)
            return Basis(row0, row1, row2, x, y, z)
        }

        fun fromBodyRotation(yRot: Float): Basis {
            val z = getBodyFront(yRot).reverse()
            return fromVectors(getBodyX(z), UP, z)
        }

        fun fromEntityBody(entity: Entity): Basis {
            return fromBodyRotation(entity.yRot)
        }

        fun fromEuler(euler: Vec3): Basis {
            var sin = sin(euler.x)
            var cos = cos(euler.x)
            val xmat = fromVectors(
                Vec3(1.0, 0.0, 0.0),
                Vec3(0.0, cos, sin),
                Vec3(0.0, -sin, cos)
            )

            sin = sin(euler.y)
            cos = cos(euler.y)
            val ymat = fromVectors(
                Vec3(cos, 0.0, -sin),
                Vec3(0.0, 1.0, 0.0),
                Vec3(sin, 0.0, cos)
            )

            sin = sin(euler.z)
            cos = cos(euler.z)
            val zmat = fromVectors(
                Vec3(cos, sin, 0.0),
                Vec3(-sin, cos, 0.0),
                Vec3(0.0, 0.0, 1.0)
            )
            return mul(mul(ymat, xmat), zmat)
        }

        fun mul(left: Basis, right: Basis): Basis {
            return factory(
                Vec3(right.tDotX(left.row0), right.tDotY(left.row0), right.tDotZ(left.row0)),
                Vec3(right.tDotX(left.row1), right.tDotY(left.row1), right.tDotZ(left.row1)),
                Vec3(right.tDotX(left.row2), right.tDotY(left.row2), right.tDotZ(left.row2))
            )
        }

        private val UP = Vec3(0.0, 1.0, 0.0)

        private fun getBodyFront(yRot: Float): Vec3 {
            return Vec3(
                -Mth.sin(
                    Math.toRadians(yRot.toDouble())
                        .toFloat()
                ).toDouble(), 0.0, Mth.cos(
                    Math.toRadians(yRot.toDouble())
                        .toFloat()
                ).toDouble()
            )
        }

        private fun getBodyX(z: Vec3): Vec3 {
            return Vec3(z.z, 0.0, -z.x)
        }
    }
}