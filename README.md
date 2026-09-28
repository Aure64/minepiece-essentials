# Minepiece Essentials

A lightweight Fabric mod for the MinePiece server with passive quality-of-life features:

- **Boss timers** — respawn timers for every island, fetched automatically from `/boss` on join (no screen shown, no click sent), with a sound alert when one is close. One refresh button in the header; click an island header (in edit mode) to collapse/expand it, click a boss to copy its coordinates.
- **Parchment reader** — scans quest parchments in your inventory and shows their objectives in a HUD.
- **Pet stat quality** — shows the roll quality (`%` of the max for the rarity & level) next to each pet stat in the `/pets` tooltip.
- **Minion calculator** — shows the resources left to reach the next prestige and the max prestige (P10), in stacks, in the minion tooltip. Resource XP values are learned automatically from the feeding screen.
- **Active pets panel** — a HUD listing your active pets (rarity-coloured names + level) and the total combat stats they grant, filled automatically on join.
- **Daily quests** — the pass daily quests and their progress as a HUD, fetched automatically on join and after the midnight reset; completion is detected live from chat.
- **Ascension panel** — a HUD listing your fruits and weapons with their level, XP to the next level, and a marker when an ascension is available.
- **Haki timer** — shows the remaining haki cooldown after activation (display only).
- **In-game help** — a guide popup shown on first join and reopenable with `H`.
- **Update notifier** — tells you in chat when a newer version is available on GitHub.

All HUDs are draggable. Press `K` to enter edit mode, drag with the mouse, scroll to resize, press `Esc` to save.

## Requirements

- Minecraft 26.2 (for 1.21.11, use version 1.8.0)
- Fabric Loader ≥ 0.19.5
- Fabric API
- Java 25
- *(optional)* Xaero's Minimap — boss coordinates are auto-synced as waypoints when installed.

## Build

Requires JDK 25 (`JAVA_HOME=/path/to/jdk-25`).

```bash
./gradlew build
```

The jar lands in `build/libs/minepiece-essentials-<version>.jar`.

## Upgrading to a newer Minecraft version

See [UPGRADE.md](UPGRADE.md).

## Keybinds

| Default | Action |
|--|--|
| `K` | Open HUD edit screen |
| `H` | Open the in-game help guide |

## Configuration

Config files live at `.minecraft/config/minepiece-essentials/`:

- `config.json` — main settings (refresh intervals, alert thresholds)
- `layouts.json` — HUD positions
- `bosses/` — saved boss data per island
- `waypoints/` — manual waypoints

## License

All rights reserved.
