# Minepiece Essentials

A lightweight Fabric mod for the MinePiece server with two passive QoL features:

- **Boss timers** — auto-tracks boss respawns per island, plays a sound alert when one is close.
- **Parchment reader** — scans quest parchments in your inventory and shows their objectives in a HUD.

Both HUDs are draggable. Press `K` to enter edit mode, drag with the mouse, click outside or press `Esc` to save.

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

## Configuration

Config files live at `.minecraft/config/minepiece-essentials/`:

- `config.json` — main settings (refresh intervals, alert thresholds)
- `layouts.json` — HUD positions
- `bosses/` — saved boss data per island
- `waypoints/` — manual waypoints

## License

All rights reserved.
