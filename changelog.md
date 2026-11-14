# 0.5.7
Holy bug fixes batman

## IMPORTANT
Kelvin is no longer included in the Clockwork Jar. You must download it separately.
It is still approving on Modrinth (and has been for 3 weeks), for now get it from Curseforge:
https://www.curseforge.com/minecraft/mc-mods/kelvin-vs

---

Rest of the changelog:

### Updated Kelvin:
- New, very performant default solver (JacobiSeidel)
- Prettier gas particles
- Replaced the wall heat system with node heatCapacity
- Added a consistent way to get flow rate for an edge
- More usable by other mods with the new Connectable system
- More usable by other mods with it being published as its own mod instead of included in the clockwork jar
- Fixed a few bugs (thanks G_Mungus)
- Added JEI compat for viewing gas reactions and gas properties

### Additions:
- Added a CC: Tweaked peripheral to the gas valve
- Added a clockwork ponder group
- Added JEI compat for gas crafting and gas-producing blocks
- Added lazy linking to flap bearings (e.g. click the side of a bearing with a linked controller to copy its frequency). Has compat for tweaked controllers.
- Added the ability to change the tilt of the blade controller while moving by powering the side of the brass propeller bearing with redstone
- Added the gimbal bearing (like a cross between a physics bearing and a copter bearing)
- Added the sterling engine
- Added polish translations (by SuperKotz)
- Added bind, attach, and weld mode to the Wanderwand
- Added new shiny visuals to the Wanderwand and Gravitrons
- Added custom recipes for everything that activate when TFG is present, to better fit into its progression
- Added a config to enable propellers generating torque (off by default)

### Changes:
- Changed the gas engine to use flowrate 
- Changed the delivery cannon to maintain its previous angle instead of resetting after firing
- Changed the model for the creative gas generator, gas back tank, and the redstone duct
- Added VMod compatibility for all the bearings
- Rewrote all the propeller math, making sails and blade controllers more balanced (and hopefully better to use)
- Reworked phys bearing
- Made phys bearing count as glued to other blocks, making nesting phys bearings easier
- Allowed the universal shaft to be waterlogged
- Added more gas related blocks to the wrenchable tag
- Added new visuals to the physics assembler
- Reworked the smart duct completely
- Rewrote how the balloons calculate buoyancy, making them much more fun to use
- Made wanderglass have backface culling and connected textures

### Bugfixes:
- Fixed aeronaut goggles not rendering on armor stands or other players
- Fixed balloon casing not being able to be placed on shafts
- Fixed balloon casing not showing up in the inventory on fabric
- Fixed balloon casing not dropping an item when broken
- Fixed blade controller not updating its blade angle properly
- Fixed composters not generating bog
- Fixed delivery cannon silently failing when the destination chute was full
- Fixed delivery chute missing translation strings
- Fixed gas crafter JEI compat
- Fixed gas crafting not consuming energy or gas on forge
- Fixed gas crafting not outputting fluid
- Fixed gas heater not working on boilers
- Fixed gyroscope and reaction wheel rendering an extra shaft
- Fixed reaction wheel applying torque in the wrong direction
- Fixed spinoff bearings not copying/pasting in VMod schematics properly
- Fixed phys bearings not copying/pasting in VMod schematics properly
- Fixed spinoff bearings not serialising, causing a jolt on world reload
- Fixed phys bearings not serialising, causing a jolt on world reload
- Fixed the item model of the wanderlite matrix
- Fixed balloons sometimes not being detected
- Fixed universal shaft not dropping compressible shaft item when broken
- Fixed universal shafts sometimes having reversed rotation
- Fixed universal shafts looking glitched out when one end is unloaded
- Fixed universal shafts not pasting linked in create schematics
- Fixed Z-Fighting on the physics bearing model
- Fixed Creative Gravitron ring spinning at different speeds depending on FPS
- Fixed gas tanks having all sorts of bugs
- Fixed gas valve and pump not working with connections
- Fixed oneway connection visual not matching 
- Fixed physics bearing not taking into account the mass of connected ships
- Fixed extendon tweaking out
