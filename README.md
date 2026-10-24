# Minepiece Essentials

A client-side Fabric mod for the MinePiece server. It reads what the game already shows you (tooltips, server menus, boss bar, chat) and turns it into HUDs and tooltips. No macros, no auto-farming. The mod only activates on MinePiece.

- **Boss timers** — respawn timers for every island, fetched automatically from `/boss` on join, then every 5 minutes and after each respawn (no screen shown, no click sent), with a sound alert when one is close. In edit mode (`K`): click an island header to collapse it, Shift+click a boss to hide it (HUD and alerts), click a boss to copy its coordinates; a compact mode lists every boss without island headers.
- **Parchment reader** — scans quest parchments in your inventory and shows their objectives in a HUD.
- **Pet stat quality** — shows the roll quality (`%` of the max for the rarity & level) next to each pet stat in the `/pets` tooltip.
- **Minion calculator** — shows the resources left to reach the next prestige and the max prestige (P10), in stacks, in the minion tooltip. Resource XP values are learned automatically from the feeding screen.
- **Active pets panel** — a HUD listing your active pets with their head, rarity-coloured name, stat icons and level, filled automatically on join.
- **Active armor panel** — the four pieces you wear, the set in progress and, when complete, its bonus in one line. Read locally from your armor slots.
- **Profile panel** — your global stats from `/profil` (level and % live from the boss bar, ascension, then each stat with the share granted by pets and by armor in brackets). Fetched silently on join, after an armor, pet or level change and every 10 minutes; recomputed locally in between.
- **Daily quests** — the pass daily quests and their progress as a HUD, fetched automatically on join and after the midnight reset; completion is detected live from chat.
- **Ascension panel** — a HUD listing your fruits and weapons with their level, XP to the next level, and a marker when an ascension is available; evolving tools with their progression bar.
- **Job HUD** — level, XP and `%` of your current job while you harvest.
- **Haki timer** — shows the remaining haki cooldown after activation (display only).
- **Rarity emblems** — rarity emblems on items in chests, your inventory and the hotbar, with a filter bar that highlights the rarities you pick and dims the rest.
- **Chest sorter** — sort buttons on chests and shulker boxes, by rarity or by item. It moves items only when you press a button, like any inventory-sorting mod; it is the only feature that acts on the game.
- **Auction house prices** — price per unit on stacks, and a colour band comparing the listing to the average price (yellow = cheap, green = fair, red = expensive).
- **In-game help** — a guide popup shown on first join and reopenable with `H`.
- **Update notifier** — tells you in chat when a newer version is available on GitHub.

All HUDs are draggable. Press `K` to enter edit mode, drag with the mouse, scroll to resize, press `Esc` to save. The same screen has a switch for each feature. The interface follows your Minecraft language (French or English).

## Requirements

- Minecraft 26.2
- Fabric Loader ≥ 0.19.5
- Fabric API
- Java 25

## Install

Download the jar from [Modrinth](https://modrinth.com/mod/minepiece-essentials) or the [GitHub releases](https://github.com/Aure64/minepiece-essentials/releases/latest) and drop it into `.minecraft/mods/`, along with Fabric API. Keep only one version of the mod in the folder.

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

- `config.json` — feature switches, alert settings
- `layouts.json` — HUD positions and sizes
- `bosses/` — saved boss timers
- `minion-resource-xp.json` — resource XP values learned by the minion calculator
- `telemetry.json` — your random anonymous ID (see below)

## What the mod sends

**To the MinePiece server**

- To fill its HUDs, the mod sends `/boss`, `/pets`, `/pass quests` and `/profil` in the background, reads the menu the server sends back, and closes it. It never clicks in those menus. See [docs/architecture-background-refresh.md](docs/architecture-background-refresh.md).
- The chest sorter moves items inside the open chest when you press a sort button. It never runs by itself and can be switched off in `K`.
- The optional donate button sends a `/pay` command to the mod's author, only after a confirmation screen showing the exact command.

**Anonymous statistics (can be turned off)**

The mod sends a few anonymous statistics so the author knows how many players use it and which features are worth maintaining.

- Sent: a random ID generated on first launch, mod / Minecraft / Fabric Loader / Java versions, OS, client language, and the names of the features used.
- Never sent: your username, Minecraft UUID, IP address, chat, coordinates, inventory or progression.
- Turn it off: `K` → "Anonymous stats". Hosted by PostHog, EU region.

**Update check**

On launch the mod asks GitHub whether a newer release exists and tells you in chat if so.

## License

All rights reserved.
