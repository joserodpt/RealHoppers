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
* [Tiers](#tiers)
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
| `TELEPORT`       | Sends a player who steps onto the hopper to the hopper it is linked to.                  |
| `ITEM_TRANSF`     | Pushes its contents, one stack at a time, into the hopper it is linked to.              |
| `SUCTION`        | Pulls in dropped items from the blocks around it.                                       |
| `BLOCK_BREAKING` | Breaks the block directly above it and takes the drop.                                  |
| `KILL_MOB`       | Damages living entities standing on it and claims their drops.                          |
| `AUTO_SELL`      | Anything the hopper cannot fit is sold instead, into a balance the owner collects later. |
| `AUTO_SMELT`     | Smelts what the hopper takes in — cobblestone lands as stone, ore as ingots.             |
| `VOID`           | Destroys whatever will not fit, instead of dropping it on the floor.                     |
| `MOB_PULL`       | Drags living things nearby onto the hopper itself. Pair it with `KILL_MOB`.              |
| `HARVEST`        | Cuts fully grown crops around it and replants them.                                      |
| `GROW`           | Nudges crops around it along, the way bonemeal would.                                    |
| `AUTO_COMPACT`   | Squashes its contents — nine ingots become a block, nine nuggets an ingot.               |
| `XP_COLLECT`     | Soaks up experience orbs nearby and keeps them until somebody takes them.                |
| `FILTER`         | Decides what the hopper is allowed to keep.                                              |

`TELEPORT` and `ITEM_TRANSF` are *linked* traits: they follow the hopper's link. A hopper has **one** link, and both
traits use it — so a linked pair can be a teleporter and an item pipe at the same time, and re-pointing the hopper
moves both at once. Either trait can be switched on before a link exists; it simply sits idle until there is one.

`AUTO_SELL` is what gives a hopper a balance. Open the hopper's GUI and click the hopper icon to collect it —
left-click takes half, shift-left-click takes all.

`MOB_PULL` gathers onto the hopper's own top, so one hopper carrying both `MOB_PULL` and `KILL_MOB` is a complete
grinder. With something built directly over it there is nowhere up there to stand, so mobs go beside it instead.

`XP_COLLECT` keeps experience the same way, in a separate store, with its own button in the panel — that one is not
money and is handed straight back as levels rather than paid through Vault.

`AUTO_COMPACT` reads the server's crafting recipes for anything that is nine of one thing filling the grid, so
datapack recipes work as well. Its result goes back through the hopper, which means a compacting hopper that also
sells prices the block rather than the ingots.

`FILTER` governs what the hopper may *keep*, and nothing else — so what it turns away still meets the rest of the
hopper. Beside `AUTO_SELL` that means keep what is listed and sell the rest; beside `VOID`, keep what is listed and
destroy the rest. An empty list keeps everything. Edit it by shift-clicking the trait in the Traits screen, then
holding an item and clicking to add it, or by browsing every material in the picker.

`AUTO_SMELT` uses the server's own furnace recipes, so anything a player could smelt by hand — including recipes added
by a datapack — smelts in the hopper. On a hopper that also has `AUTO_SELL`, the smelted material is the one that gets
priced.

Everything entering a hopper meets its traits, whether RealHoppers put it there — suction, block breaking, harvesting,
a mob drop — or vanilla did, from a chest above or a hopper feeding in. The order is always the same: smelt it, store
it if it fits, sell it if it does not, void it if it still has nowhere to go, and otherwise drop it on the floor when
`Drop-Items-If-Full` is on.

`HARVEST` and `GROW` read a square around the hopper, so their cost grows with the square of the radius. Both run on a
slow cycle for that reason, and a high tier over a large field is the expensive combination to watch.

## Tiers

Most traits sit at a **tier**. A tier has a **power**, which multiplies what the trait is configured to do, and a
**price**, which is what upgrading to it costs the player. Both are per trait, in `config.yml`:

```yaml
    SUCTION:
      # What the trait is worth at power 1.
      Radius: 2
      Tiers:
        '1': { Power: 1.0, Price: 0 }
        '2': { Power: 2.0, Price: 500 }
        '3': { Power: 3.0, Price: 2000 }
```

A trait has as many tiers as are listed for it, so they need not all have the same number — suction ships with five,
mob killing with three. The powers need not be linear either: item transfer ships `1, 4, 16, 64`, so the top tier
moves a full stack a cycle.

| Trait            | What the power multiplies                         | Config key                   |
|------------------|---------------------------------------------------|------------------------------|
| `SUCTION`        | How many blocks around it it pulls items from.    | `SUCTION.Radius`             |
| `BLOCK_BREAKING` | How far up the column it breaks, stopping at air. | `BLOCK_BREAKING.Blocks`      |
| `KILL_MOB`       | Damage per cycle.                                 | `KILL_MOB.Damage`            |
| `ITEM_TRANSF`     | Items handed over per cycle.                      | `ITEM_TRANSF.Items`           |
| `AUTO_SELL`      | What each sale pays, on top of the item's price.  | `AUTO_SELL.Price-Multiplier` |
| `MOB_PULL`       | How far it drags living things in from.           | `MOB_PULL.Radius`            |
| `HARVEST`        | How far it reaps.                                 | `HARVEST.Radius`             |
| `GROW`           | How far it grows.                                 | `GROW.Radius`                |
| `AUTO_COMPACT`   | Sweeps over the contents per cycle.               | `AUTO_COMPACT.Passes`        |
| `XP_COLLECT`     | How far it soaks orbs up from.                    | `XP_COLLECT.Radius`          |

`TELEPORT`, `AUTO_SMELT`, `VOID` and `FILTER` have no tiers — there is one destination to send a player to, and an item either
has a furnace recipe or it does not, is either destroyed or it is not, and is either on the list or it is not. All
four stay at tier 1.

A trait starts at tier 1, so tier 1's price is never charged. Players upgrade by right-clicking the trait in the
Traits screen, which charges the next tier's price through Vault and only ever goes up. `/rh settrait <trait> <tier>`
sets a tier outright without charging, for admins.

## Requirements## Requirements

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

Right-click it to open its panel. One screen holds everything: the hopper's own five slots on the second row, which
you can move items in and out of exactly as you would the normal hopper screen, its balance and experience beside
them, and every trait on the two rows below.

To give it a trait, click it — clicking one it already has takes it off again. Right-click a trait to buy its next
tier, and shift-click `FILTER` to choose what the hopper keeps. The same thing from the command line, looking at the
hopper:

```
/rh settrait SUCTION
```

To link a hopper to another, hold a stick, right-click it and then the hopper it should point at. Anything else in
between calls it off — dropping the stick, clicking another block, clicking a mob, or opening a hopper's panel without
the stick in hand. Linking a hopper that already points somewhere just re-points it.

## Commands and Permissions

| Command                | Permission          | Description                                   |
|------------------------|---------------------|-----------------------------------------------|
| `/realhoppers`, `/rh`  | —                   | Plugin info.                                  |
| `/rh reload`, `/rh rl` | `realhoppers.admin` | Reloads the config and language files.        |
| `/rh settrait <trait> [tier]` | `realhoppers.admin` | Gives the hopper you are looking at a trait, at an optional tier. |

## Configuration

`config.yml` holds the prefix, the sound and particle toggles, the teleport cooldown, how often hoppers.yml is
written, whether a full hopper drops what it cannot take, what each trait does at tier 1, and the sell price of each
material:

```yaml
RealHoppers:
  Prefix: "&fReal&6Hoppers &7>"
  Effects:
    Sounds: true
    Particles: true
  Teleportation-Cooldown: 20   # ticks, so 20 is one second
  Save-Interval-Seconds: 60
  Drop-Items-If-Full: true
  Traits:
    SUCTION:
      Radius: 2
      Tiers:
        '1': { Power: 1.0, Price: 0 }
        # ... one block per trait, see Tiers above
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
