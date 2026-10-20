package org.valkyrienskies.clockwork.content.curiosities.aeronaut

import org.joml.Quaterniond
import org.joml.Vector3d
import org.valkyrienskies.clockwork.ClockworkConfig
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerBearingBlockEntity
import org.valkyrienskies.clockwork.content.contraptions.propeller.PropellerTransform
import org.valkyrienskies.clockwork.content.contraptions.propeller.data.PropData
import org.valkyrienskies.clockwork.content.forces.PropellerAerodynamics
import org.valkyrienskies.core.api.ships.LoadedServerShip

object AeronautPropellerReadout {
    fun hub(prop: PropData): Vector3d {
        val axis = Vector3d(prop.bearingAxis).normalize()
        val tilt = prop.bearingTiltQuat ?: Quaterniond().rotationTo(axis, prop.bearingAxisRot ?: axis)
        return Vector3d(prop.position).add(0.5, 0.5, 0.5).add(axis).add(PropellerTransform.hubDisplacement(axis, tilt))
    }

    fun potential(bearing: PropellerBearingBlockEntity, ship: LoadedServerShip, result: AeronautForceAccumulator) {
        if (!bearing.running) return
        val prop = bearing.newCreateData().fromCreateData()
        prop.updateData(bearing.newUpdateData())
        prop.active = true
        // Keep the configured handedness even when the rotor is stopped.
        prop.bearingSpeed = AeronautForceReadout.REFERENCE_RPM * 0.3 *
            (if (bearing.speed < 0) -1.0 else 1.0) * (if (prop.inverted) -1.0 else 1.0)
        val flow = PropellerAerodynamics.Flow(ship.transform.shipToWorld, ship.transform.positionInShip,
            Vector3d(), Vector3d(), Vector3d()) { 1.225 }
        val multiplier = ClockworkConfig.SERVER.forceMulPerSailInPropeller *
            if (prop.brass) ClockworkConfig.SERVER.sailPropellerForceMultiplier else 1.0
        val force = PropellerAerodynamics.prepare(prop, flow, 0.0, multiplier,
            ClockworkConfig.SERVER.propellerMaxForce, ClockworkConfig.SERVER.propellerMaxTorque).force
        result.add(hub(prop), ship.transform.shipToWorldRotation.transformInverse(force, Vector3d()))
    }
}
