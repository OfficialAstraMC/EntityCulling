# EntityCulling

Hides entities and tiles(mainly chests) that players are not able to see due to blocks in the way(occlusion culling),
and then blocks packets for these entities. This results in the following:

- Massive client fps improvements in big bases with tons of chests and farms due to the client not correctly culling
  them clientside(and therefor leaving a lot of performance on the table). Seen up to 500%+ more fps! (from 52fps->
  270fps at the same location without anything else changed)
- Minimap mods/Hackclients are unable to see mobs/items/chests etc through walls
- Reduces sent packets since the client gets not sent entity movement of hidden entities

## Requires

- Paper 1.21.4+
- packetevents

## Credits

[RoboTricker](https://github.com/robotricker/) created the original server side async raytracing occlusion culling
implementation for [Transport-Pipes](https://github.com/RoboTricker/Transport-Pipes). I took it and optimized it to a
point where it's able to do multiple thousands of traces in a second over a predefined sized area(100x100x100 currently
with the player in the center of it). The
original [EntityCulling plugin](https://github.com/tr7zw/EntityCulling_LegacySpigot/tree/main) without it this updated
version would not have been possible.
