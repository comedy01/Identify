# Identify

Shows what you are looking at. Point at a block or an entity and a panel at the top of the screen shows its icon and name, plus useful extras. Item tooltips get durability, food, and fuel info.

Identify is a small client-side mod for Minecraft on Fabric or NeoForge, compatible with Mod Menu (Fabric) / the built-in config screen (NeoForge).

Supported versions: 26.1 to 26.3.

## Features

- **Block and entity panel** - the icon, name, and owning mod of whatever is under your crosshair, out to a configurable range.
- **Entity details** - health, and for baby animals the time left until they grow up or, for adults, until they can breed again.
- **Block details** - crop growth, redstone power, and the best tool (and tier) to mine the block.
- **Item tooltips** - remaining durability with a percentage, hunger and saturation for food, and how many items a fuel smelts.
- **Configurable** - toggle blocks, entities, icon, details, and mod name, and adjust range and vertical position.
- **Client-side only** - there is nothing to install on a server.

Exact grow-up and breeding timers, and fuel values on 26.3, come from the integrated server, so they are available in singleplayer and on a LAN host. On other servers a baby animal is shown as just "Baby".

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

`./gradlew build -Pminecraft_version=26.3` builds the Fabric jar for one version; add `-Ploader=neoforge` for NeoForge. `./gradlew runClientGameTest -Pminecraft_version=26.3` runs the in-game test on Fabric.
