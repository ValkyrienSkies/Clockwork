# Combobulator design

Recorded 2026-11-11. The single-function prototype is implemented. Saved node routines continue to run until the player applies a replacement configuration in the new screen.

## Role

Each Combobulator performs one selected function with a small set of inputs and outputs. Players compose routines by connecting multiple Combobulators in the world. The block arrangement and connections express the program.

The screen shows the selected function, its input bindings, editable constants, output bindings, and live values. Explore a single compact screen using the supplied GUI shell and icon buttons for binding types and faces. Start with Multiply, Compare, and Latch layouts to check how calculations, conditions, and memory fit.

## Input precision

| Input | Value |
| --- | --- |
| Wireless Redstone Link | Integer redstone strength, 0–15 |
| Constant | Signed fractional numeric value entered in the GUI |
| Thinkable | Signed fractional value from a compatible device port |
| Direct face connection to another Combobulator or compatible device | Signed fractional value through the device interface |
| Ordinary redstone on a face, if retained | Integer redstone strength, 0–15 |

Direct face connections mean adjacent compatible ports communicating through the device interface. A face carrying ordinary redstone retains redstone precision. Constants, Thinkables, and direct device connections use the same numeric precision; "precise" does not imply arbitrary-precision arithmetic.

Thinkable and direct device outputs preserve signed fractional values. Wireless Link and ordinary redstone outputs convert to 0–15 strength. Constants do not require separate blocks.

## Functions to explore

- Arithmetic: add, subtract, multiply, divide.
- Comparison: greater than, less than, equal, greater/equal, less/equal, not equal.
- Logic: AND, OR, NOT, XOR, NAND, NOR, XNOR.
- Timing: delay, pulse, repeating timer.
- Memory: latch, counter.
- Selection: choose between two values using a condition.

Timing and memory support routines such as setting a latch to move a leg, resetting it when a comparison detects the target angle, and sending a pulse to start the next leg. A timer can provide a timeout path.

Proposed defaults from the concept are nonzero inputs counting as true and comparisons/gates producing configurable false/true values, initially 0/15. Final timing, reset, startup, feedback-loop, and numeric-comparison semantics still need to be designed.

## Device interface

Use a shared device contract, tentatively `IThinkable`, for Combobulators and other compatible devices. Both Thinkable cables and direct adjacency should access this contract. Device support should not require adding device-specific branches to the cable or controller code.

The contract needs to describe which faces expose ports, whether a port can provide or accept a value, and whether a reading is currently available. It should accommodate devices that only provide readings, devices that accept control values, and Combobulators that do both. Unsupported or unavailable ports must remain distinguishable from a valid zero value.

`IThinkable` now exposes `providesThinkable`, `acceptsThinkable`, `thinkableValue`, `receiveThinkable`, and `onThinkablePulse`. The Alt Meter and Combobulator implement it. `ThinkableConnections` validates and connects generic endpoints, and unavailable readings are represented by null. Receivers can pull using `ThinkableConnections.read`; devices accepting pushed values receive `receiveThinkable` callbacks while loaded and a null value on disconnect. Incoming cables take precedence over adjacency on an adjacent-device input.

### Device ports

Holding a Thinkable and aiming at a device shows its available faces, directions, and units. `IThinkable.thinkablePortName` supplies those labels. Ports use the same interface for cables and adjacent devices.

| Device | Input | Output | Faces |
| --- | --- | --- | --- |
| Alt Meter | — | World altitude, blocks | All faces |
| Thinkable Relay | One precise signal | The same signal on the other five faces | Wrench the desired input face; placement initially reads from the support face |
| Andesite / Smart Flap Bearing | Target angle, −22.5° to +22.5° | Actual angle, degrees; unavailable when disassembled | Positive redstone control side is input; opposite is output |
| Physics Bearing | — | Actual signed angle, degrees; unavailable without attached ship | Sides perpendicular to bearing axis |
| Physical Piston / Gantry | — | Actual displacement along travel axis, blocks; unavailable when disassembled or attached ship unloaded | Sides perpendicular to facing axis |
| Rope Winch | Target paid-out rope length, blocks | Current paid-out rope length, blocks; unavailable without rope | Top input; horizontal faces perpendicular to shaft output |
| Redstone Resistor | Direct speed multiplier, 0–1 | Effective speed multiplier, 0–1 | Top input / bottom output; north input / south output for vertical shafts |
| Gas Valve | Target opening, 0–1 | Actual opening, 0–1 | Unused axis between duct and shaft: positive side input, negative side output |
| Redstone Duct | — | Selected raw measurement: temperature K, heat energy J, filtered pressure Pa, or filtered mass kg | Sides perpendicular to duct axis; select measurement/filter in existing screen |

Flaps and valves retain their kinetic movement rate; winches approach the target using the magnitude of the supplied RPM, with the existing spool speed and maximum-length limits. Thinkables do not provide mechanical power. Winch redstone release and ComputerCraft flap locking retain priority. Resistor Thinkable values directly set the multiplier without redstone quantization or the redstone direction setting.

These receivers pull through `ThinkableConnections.input` each server tick. A connected cable takes priority over adjacency, including when its source is unavailable. Missing cable data holds flap/winch position and closes valves/stops resistor transmission. Removing the cable restores an adjacent source or normal control. Live command overrides are not saved as standalone setpoints; saved cables restore the input through the existing WandLinks persistence and VDex paths.

The Thinkable Relay forwards values on demand without quantization or an additional tick of storage. Relay chains are traversed iteratively; loops and missing sources return unavailable. Its chosen input is saved in the block's six-way facing property, including rotation/mirroring and VDex copying. The model uses the Combobulator's base, frame, and connecting extensions with no decorative panels, and joins either relays or Combobulators. Holding a wrench also shows its face labels. Wrenching changes the input face without moving existing cable attachments; cables on faces whose role changes stop carrying data until correctly reconnected.

## Feedback and presentation

Show live values beside ports and report faults such as division by zero through the status lamp and a readable explanation. Expose the selected function through goggles and eventually a visible symbol on the block model.

Retain the supplied Thinkable textures, activity pulses and sounds, rope connection behavior, persistence, and VDex support. Activity pulses are visual/audio feedback; they do not currently delay data reads.

## Prototype behavior

- All functions listed above are available. The supplied shell has I/O and Function tabs: I/O contains aligned port cards and binding controls, while Function contains the function selector and its settings. A block preview sits beside Apply. Port values remain visible; the redundant live-readings footer and function name beside the lamp are removed.
- Precise Combobulator connections read the previous tick's output. Feedback between controllers does not depend on their block-entity tick order. Device implementers should expose a stable tick snapshot if they compute outputs during their own tick.
- Nonzero values, including negative fractions, count as true. Boolean outputs default to 0/15 and are editable. Equality and inequality use an editable absolute tolerance, initially 0.000001; ordering comparisons remain strict.
- Delay queues each numeric value for the chosen number of ticks. Pulse starts or restarts on a rising edge. Timer emits one tick after each complete interval while enabled; disabling resets its phase.
- Latch and Counter give Reset priority. Counter counts rising edges. Switch selects A for zero and B for nonzero.
- Memory, edge history, timer/pulse progress, and the delay queue save with the block. Configuration changes reset this state. Unavailable inputs clear queued timing activity, report a fault, and propagate unavailable precise output or zero redstone output. Latch/counter memory remains held until valid inputs return or reset is applied.
- The model joins adjacent Combobulators on all six sides independently of port configuration. The shared core uses extensions and infill, frame pieces follow exposed convex edges and corners, and exposed sides have position-stable weighted decorative panels or no panel (60% empty, 10% for each of four panels). The item uses a complete standalone frame.
- Model generation is reproducible with `node scripts/generate-combobulator-models.cjs`. The generator validates all 64 neighbor combinations. Imported emissive elements use Forge's face-light metadata on 1.20.1; Fabric renders their ordinary textured geometry.

## Further design decisions

- Final function set and screen layouts.
- Port selection, face assignment, and binding controls, including whether direct device and ordinary redstone face modes are explicit choices.
- Output conversion and comparison tolerance.
- Timing, memory, and deterministic behavior in chains and feedback loops.
- Additional devices implementing `IThinkable`, including control-value receivers.

The current function screen and binding icons are still a prototype for playtesting.
