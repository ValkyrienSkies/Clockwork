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
