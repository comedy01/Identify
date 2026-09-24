# Identify

Shows what you are looking at. Point at a block or an entity and a panel at the top of the screen shows its icon and name, plus useful extras. Item tooltips get durability, food, and fuel info.

Identify is a small client-side mod for Minecraft on Fabric or NeoForge, compatible with Mod Menu (Fabric) / the built-in config screen (NeoForge).

Supported versions: 1.21 to 26.3.

## Features

- **Block and entity panel** - the icon, name, and owning mod of whatever is under your crosshair, out to a configurable range.
- **Mob intel** - health with armor, baby grow-up and breeding timers, a pet's owner, a villager's profession and level, a horse's speed and jump height, active effects, and the item a mob is holding. The panel shows at most four detail lines, so it stays compact.
- **Block details** - crop growth, redstone power, the best tool (and tier) to mine the block, what a spawner spawns, a beacon's tier, range and effects, and a beehive's honey level.
- **Item tooltips** - remaining durability with a percentage, hunger and saturation for food, and how many items a fuel smelts.
- **Equipment comparison** - hold Shift over armor or a weapon to see how it compares with what you have equipped, such as `+2 Armor, +2 Toughness`.
- **Placement** - two sliders move the panel anywhere on screen, from the top-left corner to the bottom-right corner, at any GUI scale.
- **Configurable** - toggle blocks, entities, icon, details, mod name, and item comparison, and adjust range and position.
- **Client-side only** - there is nothing to install on a server.

Some information depends on where you play:

- Exact grow-up and breeding timers, and fuel values on 26.3, come from the integrated server, so they are available in singleplayer and on a LAN host. On other servers a baby animal is shown as just "Baby".
- Servers do not send other mobs' active effects to clients, so the effects line only appears in singleplayer (and for yourself).
- A pet's owner is looked up in the player list, so the name only shows while the owner is online. Otherwise the panel shows "Tamed".

## Install

**Fabric**

1. Install [Fabric Loader](https://fabricmc.net/use/) for your version of Minecraft.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) and the Fabric Identify jar for your version in your `mods` folder.
3. Start the game.

Optional: add [Mod Menu](https://modrinth.com/mod/modmenu) to get a settings screen (*Mods > Identify > Configure*).

**NeoForge**

1. Install [NeoForge](https://neoforged.net/) for your version of Minecraft.
2. Put the NeoForge Identify jar for your version in your `mods` folder.
3. Start the game. The settings screen is available from the mod list (*Mods > Identify > Config*).

## Building

`./gradlew build -Pminecraft_version=26.3` builds the Fabric jar for one version; add `-Ploader=neoforge` for NeoForge. `./gradlew runClientGameTest -Pminecraft_version=26.3` runs the in-game test on Fabric (1.21.4 and newer), and `./gradlew runClient -Ploader=neoforge -Pminecraft_version=1.21.1` starts NeoForge with a load self-test that prints its result and closes the game.
