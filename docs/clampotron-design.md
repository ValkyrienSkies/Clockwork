# Clampotron

Design and prototype recorded 2026-11-12. Updated to playtest `13a618ce`, on branch `1.20.1/clampotron`.

## Behavior

The Clampotron is a mounted, shaft-powered gravitron gripper. Redstone enables acquisition of an assembled physics object along its forward beam. It holds one object at an adjustable distance, retaining its orientation relative to the carrier. Pistons, gantries, bearings, and winches move the mount and its cargo.

Removing redstone, losing shaft speed, or overstressing the kinetic network releases the grip. Network overstress also clears the physics command immediately in the kinetic speed-change callback. The gripper cannot continue holding from theoretical RPM while Create reports zero effective speed. A dropped load has a two-second retry cooldown after kinetic failure or sustained field overload.

The shaft enters the bottom of the supplied model, opposite the emitter. Its rotating half-shaft is rendered separately. The block supports all six facing directions.

## Initial tuning

- Wrench-adjustable reach: 1–32 blocks, default 3. Hold a wrench, aim at the center of any side face, and scroll; emitter and shaft faces have no distance control. A connected Thinkable reach input overrides this setting.
- Stress impact: `1 + held mass / 64000` SU/RPM, multiplied by absolute RPM by Create.
- Maximum force: `160000 * absolute RPM` newtons. Maximum torque is twice that value in newton-metres.
- At 64 RPM, a 256,000 kg load costs 320 SU. Available force is 10.24 MN, enough to support roughly 1,040,000 kg against ordinary gravity before allowing for movement or leverage.
- Both mass-to-stress and force-per-RPM settings are server configurable. Values are provisional playtest tuning.
- Demand above 120% of field capacity releases after 45 server ticks (2.25 seconds), with overload accumulation recovering twice as fast below that threshold. Displacement beyond 24 blocks breaks the hold immediately. Kinetic network overstress still releases immediately.

Stress currently counts the directly held ship's mass. Forces exerted through connected machinery still contribute to position error and overload, but joint-connected cargo needs further load-accounting work before its full mass contributes to SU.

## Thinkable ports

The four lateral faces carry Thinkable ports; the front is reserved for the field and the back for the shaft. For horizontal emitters the reach side is up; for vertical emitters it is north. The opposite side reports holding as 0/15. Enable is east except for east/west-facing emitters, where it is north. The opposite enable face reports position error in blocks, unavailable while empty.

Connected inputs override standalone redstone or the distance setting. Unavailable connected input releases the grip. Removing the connection restores standalone control. Thinkables supply commands, not power.

## Visuals and audio

Use the supplied `clampotron.json` and `holder.png`. Four stronger Wanderlite strands originate at the four prong tips. A translucent cone widens from the central plate toward the held object, with faint expanding contours traveling along its surface. Each prong beam ends at its own cached ship-local surface contact around the grip point. The held object uses the gravitron surface-ripple renderer. Strain increases strand motion.

Dedicated Clampotron sounds cover power on, power off, grab, and ordinary release. Forced release from field overload, excessive separation, or kinetic overstress chooses between the two supplied snap variants. Kinetic power loss also plays the power-off sound. These one-shots preserve their full duration and original pitch; all six are mono 48 kHz Vorbis.

The supplied holding and overload WAV files are also converted to mono 48 kHz Vorbis. A 120 ms circular crossfade smooths each loop boundary. The overload loop uses the sustained 0.75–13.5 second section, removing its quiet lead-in and long fade-out. `scripts/prepare-clampotron-audio.py` reproduces the conversion without modifying the source files; `--one-shots <directory>` includes the six additional WAVs.

Overload sound uses the existing rope/spring stress-warning thresholds and smoothed volume. Holding audio fades out on release, power loss, or unload and follows the moving mount in world space.

## Physics and lifecycle

Use the gravitron's stabilized controller calculation with carrier-relative targets and velocities, gravity compensation, finite force and torque, and reaction forces on the carrier. Reject the carrier itself, static ships, VDex construction, and objects already held by another gripper or handheld gravitron.

The acquisition raycast returns a ship-local hit point. Both physics and VFX transform that point to world space when using it. Each acquisition resets the controller's previous strain and position error.

Active grips are transient. Removing/unloading the block clears its command; a short command lease prevents stale physics after ticking stops. Reloading reacquires from the forward beam rather than restoring a potentially invalid target.

## Verification

Both power unit tests passed. Both Clampotron Forge GameTests passed. The real-ship test now uses a creative motor connected to the shaft, verifies a ship-local grab point, holds the object for 40 ticks, checks that position error settles below 0.2 blocks, and then verifies immediate release on kinetic overstress. The initial complete Forge run passed 115 of 118 tests; `proptestnegative`, `proptestpositive`, and `sailpropatlowrpm` failed their travel-distance assertions.

Installable Forge and Fabric jars built successfully with version `0.5.7-clampotron-playtest.2`, including the grab-anchor fix; production mixin mappings and power tests passed verification. The strengthened game-test run passed 116 of 118 tests, with `proptestpositive` and `sailpropatlowrpm` failing their travel-distance assertions. Outputs are in each loader's `build/libs` directory. Packaging used Java 17 and an isolated Minecraft cache because another test run held the shared cache open.

Visual appearance, subjective loop quality, carrier stability under load, and final balance require in-game playtesting.

Updated to playtest `13a618ce` with dedicated grab, release, power and snap sounds in version `0.5.7-clampotron-playtest.3`. Both loader production builds, mixin mapping verification, and both power tests passed on this base. All eight sound assets and both snap variants were verified in the packaged jars. Full GameTests were not rerun for this audio update. Log: `build/clampotron-latest-package.log`.

Version `0.5.7-clampotron-playtest.4`: Forge/Fabric production builds and mixin mapping verification passed; packaged resources contain the 1-32 block range. Client visuals have not been checked in-game. Log: `build/clampotron-cone-package.log`.

## Clampotron central field visibility fix

The vanilla position/color/texture fragment shader discards combined alpha below 0.1, which removed the entire cone and nearly all ring pixels. The central field now uses the existing emissive translucent shader and vertex adapter, whose cutoff tests texture alpha before applying vertex opacity. Beam rendering remains unchanged. Increased cone opacity and ring width/opacity for readability. Separate batches keep the vertex formats isolated.

Field opacity tuning: cone peak 30%, rising to 40% under strain, with an animated fade toward the target; expanding ring peak 55%.

Version `0.5.7-clampotron-playtest.5`: both production builds and mixin mapping verification passed. Packaged client classes verified for the soft-energy render path and higher opacity constants. No in-game visual verification performed. Log: `build/clampotron-field-visibility.log`.

## Grip failure feedback and tolerance

Beams, cone, and rings shift from lavender to red between 50% and 100% of the snap-strain threshold. A snapped grip emits red-to-lavender dust and electric sparks at the emitter and held anchor, plus a scattered trail along the severed field. The server emits these once before clearing the held ship, including kinetic-overstress snaps; ordinary releases do not trigger them. Positions are converted to world coordinates for mounted and moving ships.

Version `0.5.7-clampotron-playtest.6`: four unit tests passed (kinetic shutdown, power scaling, overload recovery/failure, warning thresholds). Both production builds and mixin mapping verification passed; packaged snap and warning code verified. Client effects still require in-game visual verification. Log: `build/clampotron-snap-package.log`.

## Clampotron warning color smoothing

Warning color now eases 20% toward its target each client tick, with partial-tick interpolation between frames. A full color change reaches roughly 90% in half a second. New grips reset the color history. This affects beam, cone, and ring colors without changing overload timing.

Version `0.5.7-clampotron-playtest.7`: both production builds and mixin verification passed; packaged color-history state verified. In-game appearance remains unverified. Log: `build/clampotron-color-smoothing.log`.
