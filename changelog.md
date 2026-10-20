## 0.5.7
Holy bug fixes batman

### Updated Kelvin:
- New and shiny default solver (JacobiSeidel)
- Prettier gas particles
- Removed the wall heat system
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

### Changes:
- Changed how the gas engine works
- Changed the delivery cannon to maintain its previous angle instead of resetting after firing
- Changed the model for the creative gas generator, gas back tank, and the redstone duct
- Rewrote all serialization code for the Physics and Spinoff bearings, making them much stabler on world reload
- Added VMod compatibility for all the bearings
- Rewrote all the propeller math, making sails and blade controllers more balanced (and hopefully better to use)
- Allowed the universal shaft to be waterlogged
- Added more gas related blocks to the wrenchable tag

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
- Fixed spinoff bearings not serialising, causing a jolt on world reload
- Fixed the item model of the wanderlite matrix
- Fixed balloons sometimes not being detected
- Fixed universal shaft not dropping compressible shaft item when broken
- Fixed universal shafts sometimes having reversed rotation
- Fixed universal shafts looking glitched out when one end is unloaded
- Fixed Z-Fighting on the physics bearing model
- Fixed Creative Gravitron ring spinning at different speeds depending on FPS