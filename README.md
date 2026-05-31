# Minepiece Essentials

A lightweight Fabric mod for the MinePiece server with passive quality-of-life features:

- **Boss timers** — auto-tracks boss respawns per island, plays a sound alert when one is close. Click the refresh button to update timers (don't move during a "Refresh All").
- **Parchment reader** — scans quest parchments in your inventory and shows their objectives in a HUD.
- **Pet stat quality** — shows the roll quality (`%` of the max for the rarity & level) next to each pet stat in the `/pets` tooltip.
- **Minion calculator** — shows the resources left to reach the next prestige and the max prestige (P10), in stacks, in the minion tooltip. Resource XP values are learned automatically from the feeding screen.
- **Active pets panel** — a HUD listing your active pets and the total combat stats they grant.
- **In-game help** — a guide popup shown on first join and reopenable with `H`.

All HUDs are draggable. Press `K` to enter edit mode, drag with the mouse, scroll to resize, press `Esc` to save.

## Requirements

- Minecraft 1.21.11
- Fabric Loader ≥ 0.19.2
- Fabric API
- Java 21
- *(optional)* Xaero's Minimap — boss coordinates are auto-synced as waypoints when installed.

## Build

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
| `B` | Place a manual waypoint at your current position |
| `H` | Open the in-game help guide |

## Configuration

Config files live at `.minecraft/config/minepiece-essentials/`:

- `config.json` — main settings (refresh intervals, alert thresholds)
- `layouts.json` — HUD positions
- `bosses/` — saved boss data per island
- `waypoints/` — manual waypoints

## License

All rights reserved.
