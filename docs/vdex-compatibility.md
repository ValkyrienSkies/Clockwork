# VDex compatibility

Clockwork uses VDex's block/entity NBT and joint remapping, plus `clockwork_links.nbt` for data that belongs to the world or a ship attachment. This implementation targets the VS 1.20.1 playtest API (`2.4.13+172e6d5460`).

## Persistent data

| Feature | Save/load path |
| --- | --- |
| Physical ropes, winches, springs, magical binds and attachments | Clockwork link records; new link and hitbox identities; translated endpoints; rebuilt joints. Winch and spring settings remain attached to their copied endpoints. |
| Hinges | Clockwork hinge records and block NBT; translated endpoints, limits, attachment state, and rebuilt joints. |
| Physical bearings, gimbals, spinoff bearings | Copyable-block callbacks remap partner positions, ship IDs and native joint IDs. Gimbal gyroscopic reference rotation follows the pasted host orientation. |
| Physical pistons and gantries | Copyable-block callbacks translate shaft/rail origins and ship IDs; the mechanism recreates its drive. |
| Universal shafts | Connected-ship discovery includes kinetic-only links; callbacks remap both endpoints. |
| Hose ports and extendons | Discovery includes connected endpoints; callbacks remap positions and joint IDs. Missing native joints are recreated by the connection restore code. |
| Propeller, copter and flap bearings | Bearing NBT plus explicit capture of their controlled contraption entities, including rotors outside the ship's block bounds. Assembly anchors move to the new ship and native segment IDs are regenerated. |
| Lubricant | Coated blocks and area memberships are copied relative to ship origins. The pasted coating gets new area identities and refreshes the physics material. |
| Balloons | Regions, gas composition, energy, volume and leaking state are copied. Geometry and enclosure validity are recalculated after loading. |
| Frozen ships | VDex preserves the static flag; Clockwork restores the frozen visual state and translated effect anchor. |
| Gas machinery and storage | Kelvin node NBT preserves contents and thermal data. Gas tank controller positions are translated; duct filter and smart-control settings follow their remapped endpoints. |
| Delivery cannons and chutes | Separate item-stack NBT preserves inventories. Cannon destinations within the schematic are translated; an in-flight stack without a copied destination can be queued again. |
| Infusers | Inventory and wand selections on copied ships are preserved and translated. An in-progress activation is stopped in the copy. |
| Sugar rockets | Fuel and burn progress remain; burning rockets register thrust on the pasted ship without replaying ignition. |
| Reaction wheels | Settings remain; the force-applier registration is recreated on the copied ship. |
| Grip pads and slickers | Block settings remain; contact constraints are rediscovered at the new location. |
| Wings, flaps, balloons' structural blocks, material blocks and decorations | Blockstates and ordinary block-entity NBT; ship material and aerodynamic data are regenerated. |
| Seats, redstone controls, meters, generators, engines, burners, crafters and other utility blocks | Their ordinary blockstates and block-entity NBT, including configured behaviours and inventories. |

## Boundaries

- Connections to world blocks or ships outside the schematic detach. A pasted connection must not point back into the original build.
- VDex's entity-loading option must be enabled for assembled propellers, copters and flaps. Disabling it deliberately omits their moving block payloads.
- Player grabs, rope riding, tool operations in progress, contact constraints, and infuser advancement ownership are not duplicated as schematic state.
- The infuser's inserted wand receives selection remapping. Tool stacks in arbitrary third-party containers retain the container's ordinary item-NBT behaviour.
- Schematics made before these fixes cannot recover data that was never saved, such as omitted rotor entities, lubricant, or balloon contents.

## Regression coverage

`VdexFeatureGameTests` exercises actual VDex save/paste, including rotated pastes, for registered Clockwork blocks and block entities, settings and inventories, assembled bearings and actuators, universal shafts, gas connections and multiblock tanks, configured duct filters and smart controls, controlled contraptions, burning rockets, lubricant, frozen ships, and balloon contents. The registry test excludes the development `asteroid_block`, whose placement spawns a separate meteor.

`WandLinkTransferGameTests`, `HingeGameTests`, and `CopyableBlockGameTests` cover link kinds, winch settings, hinge assembly, joint counts, legacy callbacks, and precise anchors. `ClockworkBlockReferencesTest` covers translation, 64-bit ship IDs, omitted endpoints, and runtime-state cleanup.

The registry sweep checks block and block-entity survival, not every possible configuration of every block. Specialized mechanism tests provide the deeper connection and contents checks.

Verified on the pinned VS playtest dependency: 261 unit tests and all 89 Forge game tests passed. The game tests include 16 feature-wide VDex tests alongside the existing link, hinge, and legacy callback suites.
