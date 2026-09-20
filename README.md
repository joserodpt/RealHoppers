<div align="center">

## RealHoppers

### Hoppers with traits — teleporters, item pipes, auto-sellers and mob grinders.

[![Build](https://img.shields.io/github/actions/workflow/status/joserodpt/RealHoppers/maven.yml?branch=main)](https://github.com/joserodpt/RealHoppers/actions)
![Issues](https://img.shields.io/github/issues-raw/joserodpt/RealHoppers)
[![Stars](https://img.shields.io/github/stars/joserodpt/RealHoppers)](https://github.com/joserodpt/RealHoppers/stargazers)

<a href="/#"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/v2/assets/compact/supported/spigot_46h.png" height="35"></a>
<a href="/#"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/v2/assets/compact/supported/paper_46h.png" height="35"></a>
<a href="/#"><img src="https://raw.githubusercontent.com/intergrav/devins-badges/v2/assets/compact/supported/purpur_46h.png" height="35"></a>

</div>

**RealHoppers** turns an ordinary hopper into something that does a job. Place a hopper, right-click it, and give it a
*trait*: move items to another hopper across the map, teleport whoever walks over it, break the block above it, kill
what stands on it, or sell everything it swallows straight into the player's balance.

Every hopper is stored by its block position in `plugins/RealHoppers/hoppers.yml`, so traits and balances survive a
restart.

----

## Table of Contents

* [Traits](#traits)
* [Requirements](#requirements)
* [Installation](#installation)
* [Getting Started](#getting-started)
* [Commands and Permissions](#commands-and-permissions)
* [Configuration](#configuration)
* [Placeholders](#placeholders)
* [Building](#building)

## Traits

| Trait            | What it does                                                                           |
|------------------|----------------------------------------------------------------------------------------|
| `TELEPORT`       | Sends a player who steps onto the hopper to the hopper it is linked to.                 |
| `ITEM_TRANS`     | Pushes its contents, one stack at a time, into the hopper it is linked to.              |
| `SUCTION`        | Pulls in dropped items from the blocks around it.                                       |
| `BLOCK_BREAKING` | Breaks the block directly above it and takes the drop.                                  |
| `KILL_MOB`       | Damages living entities standing on it and claims their drops.                          |
| `AUTO_SELL`      | Anything the hopper cannot fit is sold instead, into a balance the owner collects later. |
| `AUTO_SMELT`     | Smelts what the hopper takes in — cobblestone lands as stone, ore as ingots.             |

`TELEPORT` and `ITEM_TRANS` are *linked* traits: they need a second hopper to point at. The rest act on their own.

`AUTO_SELL` is what gives a hopper a balance. Open the hopper's GUI and click the hopper icon to collect it —
left-click takes half, shift-left-click takes all.

`AUTO_SMELT` uses the server's own furnace recipes, so anything a player could smelt by hand — including recipes added
by a datapack — smelts in the hopper. It applies to what RealHoppers' own traits feed the hopper: what suction pulls
in, what block breaking mines, what a mob drops. Items pushed in by vanilla hopper behaviour are untouched, the same
as with `AUTO_SELL`. On a hopper that also has `AUTO_SELL`, the smelted material is the one that gets priced.

## Requirements

* Spigot, Paper or Purpur, 1.14 or newer
* Java 16 or newer
* [Vault](https://www.spigotmc.org/resources/34315/) and an economy plugin — only needed for `AUTO_SELL`
* Optional: [PlaceholderAPI](https://www.spigotmc.org/resources/6245/) and
  [RealPermissions](https://github.com/joserodpt/RealPermissions), both picked up automatically when present

## Installation

1. Drop the jar into `plugins/`.
2. Restart the server.
3. Edit `plugins/RealHoppers/config.yml` and run `/rh reload`.

## Getting Started

Place a hopper. Every hopper you place is registered with RealHoppers, with no traits on it yet.

Right-click it to open its panel: the traits it carries, its balance if it has one, and a shortcut into the hopper's
own inventory.

To give it a trait, click **Traits** in the panel and pick one — clicking a trait it already has
takes it off again. The same thing from the command line, looking at the hopper:

```
/rh settrait SUCTION
```

To link two hoppers, hold a stick, right-click the source hopper and then the destination. Anything else in between
calls it off — dropping the stick, clicking another block, clicking a mob, or opening a hopper's panel without the
stick in hand.

## Commands and Permissions

| Command                | Permission          | Description                                   |
|------------------------|---------------------|-----------------------------------------------|
| `/realhoppers`, `/rh`  | —                   | Plugin info.                                  |
| `/rh reload`, `/rh rl` | `realhoppers.admin` | Reloads the config and language files.        |
| `/rh settrait <trait>` | `realhoppers.admin` | Gives the hopper you are looking at a trait.  |

## Configuration

`config.yml` holds the prefix, the sound and particle toggles, the teleport cooldown, how often hoppers.yml is
written, whether a full hopper drops what it cannot take, and the sell price of each material:

```yaml
RealHoppers:
  Prefix: "&fReal&6Hoppers &7>"
  Effects:
    Sounds: true
    Particles: true
  Teleportation-Cooldown: 20
  Save-Interval-Seconds: 60
  Drop-Items-If-Full: true
  Material-Values:
    COBBLESTONE: 5
    SAND: 2
```

`language.yml` holds every message the plugin sends, including the trait names shown in the GUI.

## Placeholders

With PlaceholderAPI installed:

| Placeholder                 | Value                                            |
|-----------------------------|--------------------------------------------------|
| `%realhoppers_hoppers%`     | How many hoppers are registered.                 |
| `%realhoppers_balance%`     | The total banked across every hopper.            |
| `%realhoppers_trait_<T>%`   | How many hoppers carry trait `<T>`, e.g. `SUCTION`. |
| `%realhoppers_version%`     | The plugin version.                              |

## Building

```bash
mvn clean package
```

The plugin jar lands in `realhoppers-plugin/target/`. `./compile.sh` does the same and copies it straight into the
local dev server.
