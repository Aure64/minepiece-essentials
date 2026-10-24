# Minepiece Essentials

A client-side Fabric mod for the **MinePiece** Minecraft server. It reads what the game already shows you (tooltips, server menus, boss bar, chat) and turns it into clear HUDs and tooltips. No macros, no auto-farming. The mod only activates on MinePiece.

## Features

### 🕒 Boss Timers

- Respawn timers for every boss on every island, in one HUD
- Fetched automatically from `/boss` a few seconds after you join, plus one ⟳ button to refresh
- Sound alert when a boss is about to respawn
- Click an island header to collapse it, click a boss to copy its coordinates

### 🐾 Pets & Minions

- **Stat quality**: each pet stat shows its roll as a % of the maximum for its rarity and level
- **Minion calculator**: resources left to the next prestige and to max prestige (P10), counted in stacks, in the minion tooltip
- **Active pets panel**: your active pets and the total combat stats they give, filled automatically on join

### 📜 Quests

- **Scrolls**: objectives of the quest scrolls in your inventory, shown as a HUD
- **Daily quests**: pass quests and their progress, fetched on join and after the midnight reset, with completion detected live from chat

### 🍎 Progression

- **Ascension panel**: your fruits and weapons with level, XP to next level, and a marker when an ascension is available
- **Job HUD**: level, XP and % of your current job while you harvest
- **Haki timer**: remaining haki cooldown after activation

### 💎 Rarity & Auction House

- **Rarity emblems** on items in chests, your inventory and the hotbar
- **Rarity filter**: highlight the rarities you pick, dim the rest
- **Chest sorter**: sort buttons on chests and shulker boxes, by rarity or by item
- **Auction house prices**: price per unit on stacks, and a colour band comparing the listing to the average price (yellow = cheap, green = fair, red = expensive)

### 🎛 HUD Editor

- Press `K` to move, resize or hide any HUD, choose panel backgrounds and switch each feature on or off
- Press `H` for the in-game guide (also shown on first join)

### 🌍 French & English

The mod's interface follows your Minecraft language, and it works with the server game language set to French or English.

## Requirements

- Minecraft 26.2 (the version MinePiece runs on)
- Fabric Loader ≥ 0.19.5
- Fabric API
- Java 25

## Install

Drop the `.jar` into your `.minecraft/mods/` folder, along with Fabric API. Keep only one version of the mod in the folder.

## Configuration

Everything is set in game with `K`. Files are stored in `.minecraft/config/minepiece-essentials/`:

- `config.json` — feature switches, alert settings
- `layouts.json` — HUD positions and sizes
- `bosses/` — saved boss timers
- `minion-resource-xp.json` — resource XP values learned by the minion calculator
- `telemetry.json` — your random anonymous ID (see below)

## What the mod sends

**To the MinePiece server**

- To fill its HUDs, the mod sends `/boss`, `/pets` and `/pass quests` (commands every player has) in the background, reads the menu the server sends back, and closes it. It never clicks in those menus.
- The **chest sorter** moves items inside the open chest when you press a sort button, like any inventory-sorting mod. It is the only feature that acts on the game, it never runs by itself, and it can be switched off in `K`.
- The optional **donate button** sends a `/pay` command to the mod's author, only after a confirmation screen showing the exact command.

**Anonymous statistics (can be turned off)**

The mod sends a few anonymous statistics so the author knows how many players use it and which features are worth maintaining.

- Sent: a random ID generated on first launch, mod / Minecraft / Fabric Loader / Java versions, OS, client language, and the names of the features used
- Never sent: your username, Minecraft UUID, IP address, chat, coordinates, inventory or progression
- Turn it off: `K` → "Anonymous stats". Hosted by PostHog, EU region.

**Update check**

On launch the mod asks GitHub whether a newer release exists and tells you in chat if so.

## Source

Source code: https://github.com/Aure64/minepiece-essentials
