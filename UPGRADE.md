# Upgrading to a new Minecraft version

This mod is designed to be upgraded with minimal effort. In most cases, a version bump only requires editing **one file** (`gradle.properties`) and running a build. Mixins are kept minimal and use Fabric API events where possible, which makes them more resilient across versions.

## Quick upgrade (most cases)

When a new Minecraft version is released, follow these steps:

### 1. Look up new version numbers

Go to https://fabricmc.net/develop/ and note the **stable** versions for the target Minecraft release:

- Minecraft version (e.g. `1.21.12`)
- Yarn mappings (e.g. `1.21.12+build.3`)
- Fabric Loader (e.g. `0.20.0`)
- Fabric API (e.g. `0.142.0+1.21.12`)

### 2. Update `gradle.properties`

Edit only this file. Update the four version lines:

```properties
minecraft_version=1.21.12
yarn_mappings=1.21.12+build.3
loader_version=0.20.0
fabric_version=0.142.0+1.21.12
```

Also bump `mod_version` for the new release (e.g. `1.0.0` → `1.1.0`).

### 3. Update `fabric.mod.json` dependency range

In `src/client/resources/fabric.mod.json`, update:

```json
"depends": {
    "fabricloader": ">=0.20.0",
    "minecraft": "~1.21.12",
    ...
}
```

### 4. Build

```bash
./gradlew build
```

If it compiles, you're done. The jar is in `build/libs/`.

## When the build breaks

If compilation fails, it usually means a Minecraft internal name changed. The mod uses **three mixins** that touch MC internals — these are the only places likely to break:

### `BossBarHudMixin`

Targets: `BossBarHud.render()` and shadows the field `bossBars` (`Map<UUID, ClientBossBar>`).

If broken:
- Open `net.minecraft.client.gui.hud.BossBarHud` in the new yarn mappings (use `./gradlew genSources` then browse in your IDE).
- Find the new method/field name and update the mixin.

### `ClientPlayNetworkHandlerMixin`

Targets the methods `onOpenScreen`, `onInventory`, `onScreenHandlerSlotUpdate` on `ClientPlayNetworkHandler`, plus packet classes `OpenScreenS2CPacket`, `InventoryS2CPacket`, `ScreenHandlerSlotUpdateS2CPacket`.

If a method or packet was renamed, find the new name in yarn and update the `@Inject(method = ...)` annotation accordingly.

### `MinecraftClientMixin`

Targets `MinecraftClient.tick()` and shadows `currentScreen`. Very stable but worth checking.

## Philosophy

The mod follows three rules to make upgrades easy:

1. **Single source of truth for versions** — `gradle.properties` is the only place version numbers live.
2. **Use Fabric API events over mixins** — Fabric API abstracts most version differences. Mixins are reserved for places where no event exists (bossbar text intercept, GUI silent refresh).
3. **Minimal mixin surface** — three small mixins, each doing one thing. Less code to fix when MC changes.

## After a successful upgrade

1. Test in-game on the MinePiece server (boss timer detection, parchment reading, HUD edit).
2. Commit `gradle.properties`, `fabric.mod.json`, and any mixin fixes.
3. Tag the release: `git tag v1.1.0 && git push --tags`.
