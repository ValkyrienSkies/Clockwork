Thinkable assets

- Supplied end and side textures are unchanged 10 × 10 PNGs. Clockwork spring textures are 12 × 12. Both use hard pixel edges and full-face UVs; the side tiles along the cable and the end covers each cap.
- Supplied item texture is an unchanged 16 × 16 PNG using Minecraft's generated item model.
- Thinkables use a 10/16 block width and 10/16 block texture repeat. Pulses add an emissive pass over the supplied texture.
- Attachment, data, snap, and overload WAVs were converted with FFmpeg to mono Vorbis OGG at quality 5. Data variants and receiver variants have separate sound events.
- Automated checks cover persistence, assembly, VDex copying, pulse timing, and data precision. Client appearance and audio balance still need a visual/listening check.

Combobulator model assets

- Imported the supplied Blockbench base, upward extension, corner, edge, and four north-facing decorative models. The shared PNG atlas is 64 × 64 and is copied without resizing. Texture references and particle references resolve to `vs_clockwork:block/combobulator`.
- Axis rotations preserve frame UV orientation. Generated infill pieces close gaps where extensions intersect. Exposed faces use weighted decoration choices including an empty model.
- Forge light metadata preserves exported emissive elements. Fabric uses ordinary shading for those elements.
- `scripts/generate-combobulator-models.cjs` rebuilds the generated models and multipart state file and validates all 64 neighbor combinations.

Combobulator UI and pulse adjustments

- Restored the supplied tab artwork for I/O and Function, moved function settings into their tab, aligned Apply with the texture, and added the Create-style block preview beside it.
- Port cards use one vertically centered row with labels on the left and values on the right. Removed the live-readings footer and the function name beside the status lamp.
- Empty exposed faces now have weight 12 against weight 2 for each decorative panel (60% empty).
- Thinkable glow uses a continuous per-vertex falloff, dim neighboring sections, and up to 8% outward expansion. The receiving cap uses the same falloff.
- Validation: all 64 model layouts passed generation checks; Fabric Kotlin compilation and Forge remapping passed. Packaged resources and classes were checked. Client appearance still needs an in-game check.

Thinkable device ports

- Added precise inputs and feedback outputs to both flap bearings, rope winches, redstone resistors, and gas valves. Added measured outputs to physics bearings, physical pistons/gantries, and redstone ducts. Existing Alt Meter support remains.
- Shared input resolution prioritizes cables over adjacent devices and distinguishes a missing connection from unavailable data. Inputs are read live, so removed/unloaded sources cannot leave a saved command active. Movement retains existing kinetic power and limits.
- Added interface-provided face labels and units shown when holding a Thinkable. The cable/controller code does not hardcode supported device classes. Documented ports and fallback behavior in `docs/combobulator-design.md`.
- Validation: 273 unit tests passed. All five new device GameTests passed, as did the piston/gantry tests with actual-displacement checks and the 17 VDex tests. The full GameTest run retained the three known propulsion failures (`proptestpositive`, `sailpropreversedrotationkeepsthrust`, `sailpropatlowrpm`). Client port-label appearance has not been visually checked.
- Final Fabric compilation and Forge remapping passed; packaged device classes and face labels were verified in `clockwork-thinkable-devices-forge-1.20.1.jar`.

Thinkable Relay

- Added a relay with one input and five outputs. Wrenching a face selects the input; placement initially selects the support face. The input accepts a cable or an adjacent compatible device, with cable priority.
- Relay chains forward precise values without added storage delay. Iterative traversal detects loops and returns unavailable rather than recursing or retaining stale values.
- Reused the Combobulator base/frame/extension models with no decorative panels. Relays and Combobulators connect their casings to each other. Added item, crafting recipe, loot, and mining tags.
- Face labels appear while holding a wrench or Thinkable. The input face is blockstate data, so it saves and rotates with the block; cables use existing WandLinks persistence and VDex transfer.
- Fixed Alt Meter removal leaving a readable block entity behind, and rejected removed/mismatched block entities in shared Thinkable lookup.
- Validation: 273 unit tests passed. All relay tests, five device tests, and 18 VDex tests passed on the final GameTest run (103/106 overall; the three existing propulsion failures remained). An initial physical-bearing VDex failure did not recur. Connected model generation passed all 64 layouts; relay models contain no decorative faces. Client appearance has not been visually checked.

## Clampotron implementation

- Workspace: `build/clampotron-playtest`; branch `1.20.1/clampotron`; base `8ad15b43` from `origin/1.20.1/playtest`.
- Route: existing Clockwork common Kotlin source, Registrate registration, Create kinetics, and Valkyrien Skies physics listeners. Both Forge and Fabric share the implementation.
- Supplied model: `C:/Users/thepl/Desktop/Art/Clockwork/clampotron.json`; texture: adjacent `holder.png`.
- Supplied sounds: `C:/Users/thepl/Downloads/clampotron_hold_loop.wav` and `clampotron_overload.wav`. Original assets remain unchanged.
- Runtime ID: `vs_clockwork:clampotron`; implementation: `content/curiosities/clampotron`.
- Design and controls: `docs/clampotron-design.md`.
- First compile found `SmartBlockEntity.setRemoved` is final; cleanup moved to `invalidate`, which also handles chunk unload.
- Initial Gradle setup remapped cached dependencies for the fresh worktree. Source remapping emits existing dependency warnings.
- Real-ship GameTest caught a missing VS attachment registration. Registered `ClampotronController` with the transient serializer; both Clampotron GameTests then passed.
- Complete Forge GameTest run: 115/118 passed. Failures: `proptestnegative`, `proptestpositive`, `sailpropatlowrpm` (travel-distance assertions). Log: `build/clampotron-gametest.log`.
- Forge and Fabric Kotlin compilation and both Clampotron power unit tests passed. Supplied model geometry and texture match the originals; all six facing variants and four prong emitters were checked.
- Final Forge/Fabric remap builds, repeated power tests, and production mixin mapping checks passed. Version: `0.5.7-clampotron-playtest`. Packaging log: `build/clampotron-package.log`. Both final jars were checked for the Clampotron classes and assets.
- Fixed repeated grab/release after user playtesting: `clipIncludeShips` had returned a world-space hit, which was incorrectly stored as a ship-local anchor and transformed a second time by physics and VFX. Acquisition now explicitly requests an untransformed ship-local hit and clears stale controller error/strain.
- Strengthened the real-ship regression with a connected creative motor, ship-local anchor bounds, and 40 ticks of holding before asserting less than 0.2 blocks of position error and testing overstress release. The strengthened Clampotron tests passed. Log: `build/clampotron-hold-regression.log`.
- Replacement version `0.5.7-clampotron-playtest.2`: Forge/Fabric production jars, mixin mappings, and power tests passed. GameTest total: 116/118, with two propeller travel-distance failures. Packaging log: `build/clampotron-hold-fix-package.log`.

## Clampotron sound update and playtest sync

- Updated the Clampotron branch to playtest `13a618ce`, preserving the latest VDex and Wanderlite changes.
- Converted the six supplied one-shot WAVs to mono 48 kHz Vorbis. Added dedicated power, grab, and release events, with two random snap variants for broken grips. Kinetic overstress still stops the grip immediately.
- Version `0.5.7-clampotron-playtest.3`: both loader production builds, mixin mapping checks, and two power tests passed. Verified all eight sound assets and both snap variants in each packaged jar. Full GameTests and client audiovisual checks were not rerun for this update.

## Clampotron projection and distance control

- Replaced the ring-only projection with a translucent cone that widens toward the held object. Faint rings expand along the cone.
- Four prong beams use separate ship-local surface contacts, sampled once per grip and transformed with the held ship.
- Centered the wrench scroll control on each side face and expanded both manual and Thinkable holding distance to 1-32 blocks (default 3).

Version `0.5.7-clampotron-playtest.4`: Forge/Fabric production builds and mixin mapping verification passed; packaged resources contain the 1-32 block range. Client visuals have not been checked in-game. Log: `build/clampotron-cone-package.log`.

## Clampotron central field visibility fix

The vanilla position/color/texture fragment shader discards combined alpha below 0.1, which removed the entire cone and nearly all ring pixels. The central field now uses the existing emissive translucent shader and vertex adapter, whose cutoff tests texture alpha before applying vertex opacity. Beam rendering remains unchanged. Increased cone opacity and ring width/opacity for readability. Separate batches keep the vertex formats isolated.

Field opacity tuning: cone peak 30%, rising to 40% under strain, with an animated fade toward the target; expanding ring peak 55%.

Version `0.5.7-clampotron-playtest.5`: both production builds and mixin mapping verification passed. Packaged client classes verified for the soft-energy render path and higher opacity constants. No in-game visual verification performed. Log: `build/clampotron-field-visibility.log`.

## Clampotron grip tolerance and snap feedback

- Increased field snap strain from 1.05 to 1.2, overload duration from 30 to 45 ticks, and maximum displacement from 16 to 24 blocks. Kinetic network overstress remains an immediate release.
- Added server-synchronized electric sparks and red-to-lavender dust at both ends of a snapped grip, plus particles along the broken field. Normal release does not trigger the burst.
- Beams, cone and rings transition toward red after 50% of the snap-strain threshold.

Version `0.5.7-clampotron-playtest.6`: four unit tests passed (kinetic shutdown, power scaling, overload recovery/failure, warning thresholds). Both production builds and mixin mapping verification passed; packaged snap and warning code verified. Client effects still require in-game visual verification. Log: `build/clampotron-snap-package.log`.

## Clampotron warning color smoothing

Warning color now eases 20% toward its target each client tick, with partial-tick interpolation between frames. A full color change reaches roughly 90% in half a second. New grips reset the color history. This affects beam, cone, and ring colors without changing overload timing.

Version `0.5.7-clampotron-playtest.7`: both production builds and mixin verification passed; packaged color-history state verified. In-game appearance remains unverified. Log: `build/clampotron-color-smoothing.log`.
