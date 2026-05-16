## CC: Tweaked peripheral documentation

Documentation on the lua peripherals and their functions implemented as CC: Tweaked compat.

---

## Generic gas node peripheral

Available on most blocks that have an internal inventory of gas 
(some exceptions apply, which are gas-storing blocks with their own custom peripheral)

---

```lua
getGasDetails(gasName: string) -> table
```

Returns a table representing the gas properties, in the following format: 

```lua
{
    name = <string>
    density = <number>
    viscosity = <number>
    specificHeatCapacity = <number>
    thermalConductivity = <number>
    sutherlandConstant = <number>
    adiabaticIndex = <number>
}
```

Throws if the `gasName` (resource location) is not found in the gas registry

---

```lua
getGasMass() -> table<string, number>
```
Returns a table of each gas name to its mass in the node.
Will contain every gas name, even if the node does not contain that gas.

---

```lua
getHeatEnergy() -> number
```

Returns the total heat energy in the node, in Joules

---

```lua
getPressure() -> number
```

Returns the pressure of the node, in Pascals

---

```lua
getTemperature() -> number
```

Returns the temperature of the node, in Kelvin

---

```lua
pushGas(toName: string, gasName: string, amount: number | nil)
```

Push the gas of type `gasName` from this node to the node with the peripheral name `toName`. 
If `amount` is not specified, it will move the entire amount of `gasName` in the node.

Throws if:
- `toName` is not found as a peripheral
- `toName` is not a gas node peripheral
- `gasName` is not found in the gas registry
- `amount` is `<=0`
- `amount` is larger than the amount of `gasName` in the node
- `cheatKelvinPeripheral` is not enabled in the server config (disabled by default)

---

```lua
pullGas(fromName: string, gasName: string, amount: number | nil)
```

The same as `pushGas`, documented above, except the gas is being moved from the `fromName` _into_ this node.

---

```lua
pushTemperature(toName: string, amount: number | nil)
```

Push `amount` of Kelvin from this node to the node peripheral named `toName`. 
If amount is not specific, it will transfer all heat from this node into `toName`.

Throws if:
- `toName` is not found as a peripheral
- `toName` is not a gas node peripheral
- `amount` is `<=0`
- `amount` is larger than the temperature in the node
- `cheatKelvinPeripheral` is not enabled in the server config (disabled by default)

---

```lua
pullTemperature(fromName: string, amount: number | nil)
```

The same as `pushTemperature`, documented above, except the temperature is being moved from the `fromName` _into_ this node.

---

## Physics Bearing peripheral

Peripheral type: `cw_phys_bearing`

---

```lua
assemble()
```

Sets the physics bearing to assemble on the next tick

---

```lua
disassemble()
```

Sets the physics to disassemble as soon as possible (not necessarily on the same tick)

---

```lua
setFollowAngleMode()
```

Sets the bearing mode to `FOLLOW_ANGLE`

---

```lua
setUnlockedMode()
```

Sets the bearing mode to `UNLOCKED`

---

```lua
setAngle(angle: number)
```

Sets the current angle of the physics bearing

---

```lua
isBeingDisassembled() -> bool
```

Whether the physics bearing is currently trying to disassemble (aka trying to align to the grid)

---

```lua
isActive() -> bool
```

Is the physics bearing assembled

---

```lua
isInFollowAngleMode() -> bool
```

Is the physics bearing currently in `FOLLOW_ANGLE` mode

---

```lua
getConnectedToShip() -> number
```

Returns the numeric ship id this bearing is controlling.
Will be -1 if the bearing is not currently assembled / has no ship id.

---

```lua
getTargetAngle() -> number
```

The current target angle the bearing is trying to get to

---

```lua
getActualAngle() -> number
```

The current angle the bearing is actually at

---

```lua
getTargetAngleChangeSpeed() -> number
```

The current speed the bearing is moving at to get to the target angle

---

```lua
getRPM() -> number
```

The current Create RPM being given to the bearing

---

```lua
getFacingDirection() -> string
```

The name of the direction the bearing block is rotated. 
Possible values: `up`, `down`, `north`, `south`, `east`, `west`

---

## Gas Nozzle peripheral

Peripheral type: `cw_gas_nozzle`

---

```lua
setPointer(value: number)
```

Sets the gas nozzle "power" (the dial on the front). Equivalent to using create rotation on the side of the nozzle.
`value` should be between `0.0` and `1.0`, and represent a percentage of max power.

---

```lua
getPointer() -> number
```

Returns a number between `0.0` and `1.0` of the gas nozzles current "power"

---

```lua
getPointerSpeed() -> number
```

Returns the current speed the gas nozzle power dial is moving at because of Create rotation.

---

```lua
hasBalloon() -> bool
```

Whether the gas nozzle currently has a balloon air pocket attached/detected.

---

```lua
getPocketTemperature() -> number
```

Gets the total temperature of the attached air pocket, in Kelvin. If no air pocket is attached, will return `0.0`

---

```lua
getTargetTemperature() -> number
```

Gets the current target temperature the gas nozzle is trying to achieve. 
Affected by both the gas nozzles current gas temperature, and the "power" dial.

---

```lua
getDuctTemperature() -> number
```

Gets the current internal temperature of the gas nozzle (not the pocket!) in Kelvin.

---

```lua
getBalloonVolume() -> number
```

The total volume (in metres cubed) of the attached air pocket. If no air pocket is attached, will return `0.0`.

---

```lua
getLeaks() -> number
```

Returns the number of leaks found in the air pocket. If no air pocket is attached, will return `0.0`.

---

## Flap Bearing peripheral

Peripheral type: `cw_flap_bearing`

---

```lua
isSmart() -> bool
```

Whether the flap bearing is a smart flap bearing

---

```lua
getAngle() -> number
```

The current angle the bearing is at. Will be within the range `-22.5 .. 22.5`

---

```lua
setAngle(angle: number, locked: bool | nil)
```

Sets the angle the bearing is currently at. `angle` must be in the range `-22.5 .. 22.5`.

If `locked` is not specified, the bearing will be "locked" 
and no longer respond to any redstone input (only computer input) until `setLocked` is used. 
If `locked` is specified as `false`, the bearing will not be locked, and will respond to redstone input, but will also reset every tick.

Throws if:
- The peripheral is not a smart flap bearing
- The flap bearing is not being provided rotational power (enable `cheatFlapBearingPeripheral` in the server config to bypass this)
- Flap bearing is not assembled

---

```lua
isLocked() -> bool
```

Whether the flap bearing is currently locked, and will only respond to computer input (not redstone).

---

```lua
setLocked(locked: bool)
```

Set the flap bearing to `locked` true/false. If true, the flap bearing will only respond to computer angle input.
If false, the computer can accept redstone _or_ computer input, but will reset the computer input each tick.

---

```lua
isRunning() -> bool
```

Is the flap bearing currently assembled

---

```lua
assemble() -> bool
```

Assembles the flap bearing. 
Returns `true`, unless the bearing was already assembled, in which case it will return `false`

---

```lua
disassemble() -> bool
```

Disassembles the flap bearing. 
Returns `true`, unless the bearing was already disassembled, in which case it will return `false`

---

## Propeller Bearing peripheral

Peripheral type: `cw_prop_bearing`

---

```lua
isBrass() -> bool
```

Whether the propeller bearing is a brass propeller bearing (false if it is a jury rigged bearing)

---

```lua
getAngle() -> number
```

The current angle the bearing is at. Will be within the range `-360 .. 360`

---

```lua
setBladeAngle(angle: number, locked: bool | nil)
```

Sets the blade angle of any blade controllers on the bearing. `angle` must be in the range `-180 .. 180`.

If `locked` is not specified, the bearing will be "locked"
and no longer respond to any redstone input (only computer input) until `setLocked` is used.
If `locked` is specified as `false`, the bearing will not be locked, and will respond to redstone input, but will also reset every tick.

Throws if:
- The peripheral is not a brass propeller bearing
- The propeller bearing is not assembled

---

```lua
isLocked() -> bool
```

Whether the propeller bearing is currently locked, and will only respond to computer input (not redstone).

---

```lua
setLocked(locked: bool)
```

Set the propeller bearing `locked` to true/false. If true, the propeller bearing will only respond to computer blade angle input.
If false, the propeller bearing can accept redstone _or_ computer input, but will reset to the redstone input each tick.

---

```lua
isRunning() -> bool
```

Is the propeller bearing currently assembled

---

```lua
assemble() -> bool
```

Assembles the propeller bearing.
Returns `true`, unless the bearing was already assembled, in which case it will return `false`

---

```lua
disassemble() -> bool
```

Disassembles the propeller bearing.
Returns `true`, unless the bearing was already disassembled, in which case it will return `false`

---

## Altimeter peripheral

Peripheral type: `cw_alt_meter`

---

```lua
getHeight() -> number
```

Returns the current height the altimeter is at. 
If the altimeter is on a ship, the height is automatically transformed from the shipyard into worldspace.

---

```lua
getOutput() -> number
```

Gets the current redstone signal strength the altimeter is outputting, in the range `0..15`.

---

```lua
getTargetHeight() -> number
```

Returns the target height the altimeter is currently set to.

---

```lua
setTargetHeight(height: number)
```

Sets the target height of the altimeter. If `height` has decimals, they will be truncated.

---

```lua
getSensitivity() -> number
```

Returns the sensitivity the altimeter is currently set to.

---

```lua
setSensitivity(sensitivity: number)
```

Sets the sensitivity of the altimeter. If `sensitivity` has decimals, they will be truncated.

---

```lua
getDirection() -> string
```

Returns the current direction mode of the altimeter. Will be one of: `UP`, `DOWN` or `BOTH`.

---

```lua
setDirection(direction: string)
```

Sets the current direction mode of the altimeter. Not uppercase sensitive.

Throws if `direction` is not one of: `UP`, `DOWN` or `BOTH`. 

---

This page is still a work in progress. 

Spot an error, or want to add extra information?
Then feel free to contribute to this page with a pull request