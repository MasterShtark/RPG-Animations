# Changelog

## Minecraft 1.21.1 support

RPG Animations 1.0.0 is also available for NeoForge 21.1.259 and Java 21. The 26.3 build is preserved. See the [1.21.1 feature list and port changes](versions/1.21.1/CHANGELOG.md).

## RPG Animations 1.0.0

### Waking and sleeping
- First-world wake-up sequence with low camera movement, textured eyelids, two blinks and a dominant-hand eye rub.
- Full-body get-up animation visible to nearby players.
- Bed entry with hand bracing, sequential leg movement, sitting and lying down.
- Sleep-settling movements, supported bed exit and an occasional reluctant wake-up variation.
- Wake-up replay, skipping and reduced-motion settings.

### Combat, tools and everyday interactions
- Weapon swings, axe chopping, pickaxe mining, digging and tilling.
- Eating animation.
- Shield raising/holding, lowering and blocked-hit recoil.
- Bow draw/release and crossbow loading, aiming and release.
- Separate door, trapdoor, fence-gate, chest, barrel, shulker-box and generic container gestures.
- Button pressing, lever pulling, block placement and book-page turning.

### Movement and riding
- Walking and sprinting poses with transitions.
- Soft and harder landing reactions.
- Ladder climbing, swimming push-off, swimming strokes and water-exit recovery.
- Mounting profiles for horses, donkeys, camels, llamas, pigs, striders and other mounts.
- Dismount profiles for horse-sized, camel-sized and smaller mounts.
- Elytra takeoff, flight and landing.
- Optional seat entry/exit gestures and backpack-equipping gestures.

### Items, inspections and fishing
- Item-drop release, pickup reach and occasional pickup fumble/catch.
- General item/block inspection.
- Separate sword, axe, pickaxe, shovel and hoe inspections.
- Special sword/tool inspection, ordinary and special equipment inspection, and treasure inspection.
- Two-handed support for selected inspections, including special gear and treasure.
- Remembered item-type discoveries so automatic inspections do not repeat for the same recorded item.
- Fishing cast, waiting adjustment, bite reaction and reel-in.

### Environmental and roleplaying details
- Brushing off after waking, sunlight shielding and two-arm rain shielding.
- Ledge balancing, looking over an edge and careful crouching near a ledge.
- Hand contact near walls and protecting a held item in narrow passages.
- Shaking off water and wiping the face after surfacing.
- Checking pockets, warming hands at a nearby campfire and voluntary campfire rest.
- Flinching at nearby projectile impacts.
- Patting at fire, rubbing arms when freezing, wiping sweat in the Nether and reacting to End cold.
- Catching breath after sprinting and guarded resting at low health.
- Small, medium and large idle stretches, plus sleepy idle presentation.

### Equipment, workstations and social gestures
- Individual helmet, chestplate, leggings and boots adjustment gestures.
- Crafting, anvil, enchanting and brewing gestures after relevant successful operations.
- Wave, nod, bow, beckon, point and thanks.
- Gesture menu and commands, without required animation keybinds.
- Continuous typing hand movement and inventory-browsing presentation.

### Multiplayer, previews and configuration
- Synchronised animation timelines for the actor and nearby observers, including late tracking.
- First-person hand/camera presentation for applicable actions and observer body poses.
- Opt-in sharing of inventory, container, book-page and supported backpack-screen previews using the sender's rendered textures.
- Typing, browsing and idle activity indicators with sharing/display controls.
- Individual animation switches, reduced motion, server controls and per-action ownership overrides.
- Punchy and Better Combat handoff rules that leave uncovered RPG actions available.
- Optional First-person Model handling and Sodium rendering bridge.
- Sophisticated Backpacks and Traveler's Backpack equip/preview adapters.
- Optional ragdoll ownership, Carry On, Corpse and seat handling to avoid conflicting player presentation.
- Preview bandwidth limits, texture reuse, activity batching and per-frame compatibility caching.

Compatibility depends on the installed versions and settings. The next-update animation batch is not included in 1.0.0.
