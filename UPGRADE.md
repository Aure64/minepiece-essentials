# Upgrading to a new Minecraft version

## Prerequisites since 26.x

- **JDK 25 is required** to build (Minecraft 26.2's manifest declares
  `javaVersion.majorVersion = 25`), and Gradle itself must run on it:
  `JAVA_HOME=/path/to/jdk-25 ./gradlew build`. No machine path is committed.
- **There are no mappings any more.** From 26.1 onwards Minecraft ships
  unobfuscated, Mojang no longer publishes a mappings file (the 26.2 manifest
  only has `client` and `server`), and `loom.officialMojangMappings()` fails.
  `build.gradle` declares no `mappings` line at all.
- The Loom plugin is `net.fabricmc.fabric-loom`, and mods are plain
  `implementation` dependencies — nothing gets remapped, so there is no
  `remapJar` step.

This mod uses **Mojang official mappings**. Yarn and Intermediary were discontinued after
1.21.11 (Mojang now ships Java Edition unobfuscated), so every version from 26.1 onwards is
mojmap-only. The migration off Yarn was done on 1.21.11 — nothing to redo.

In most cases a version bump requires editing **two files** (`gradle.properties` and
`fabric.mod.json`) and running a build. Mixins are kept minimal and use Fabric API events where
possible, which makes them more resilient across versions.

## Quick upgrade (most cases)

### 1. Look up new version numbers

Fabric meta is the source of truth (`https://meta.fabricmc.net`, `https://maven.fabricmc.net`):

```bash
# Is the target version out and stable?
curl -s https://meta.fabricmc.net/v2/versions/game | head -40
# Latest stable loader
curl -s https://meta.fabricmc.net/v2/versions/loader | head -8
# Fabric API builds for that Minecraft version
curl -s https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml \
  | grep -oE '<version>[^<]*\+26\.2</version>' | tail -5
```

There is **no mappings version to look up** — `loom.officialMojangMappings()` follows
`minecraft_version` automatically.

### 2. Update `gradle.properties`

Edit only these three lines (plus `mod_version` for the release):

```properties
minecraft_version=26.2
loader_version=0.19.5
fabric_version=0.159.0+26.2
```

### 3. Update `fabric.mod.json` dependency range

In `src/client/resources/fabric.mod.json`:

```json
"depends": {
    "fabricloader": ">=0.19.5",
    "minecraft": "~26.2",
    ...
}
```

### 4. Build

```bash
./gradlew build
```

If it compiles and the tests pass, the jar is in `build/libs/`. **This is not enough** — see
verification below.

## When the build breaks

Compilation failures mean a Mojang name changed. To look up the new name, inspect the remapped
Minecraft jar directly rather than guessing:

```bash
JAR=$(find ~/.gradle/caches/fabric-loom/minecraftMaven -name 'minecraft-clientonly-*.jar' | head -1)
javap -p -cp "$JAR" net.minecraft.client.gui.GuiGraphics | grep -i deferred
```

[mappings.dev](https://mappings.dev/) and [Linkie](https://linkie.shedaniel.dev/mappings) are the
web equivalents.

## Verifying the mixins — the part a green build does NOT cover

The five mixins in `mixin/` target Minecraft internals by **name strings**. A wrong target compiles
fine and fails at runtime, when the mixin is applied. Check each target exists before shipping:

| Mixin | Target class | Members it depends on |
|---|---|---|
| `BossBarHudMixin` | `BossHealthOverlay` | `extractRenderState(GuiGraphicsExtractor)`, field `events` |
| `ClientPlayNetworkHandlerMixin` | `ClientPacketListener` | `handleTabListCustomisation`, `handleOpenScreen`, `handleContainerContent`, `handleContainerSetSlot` |
| `HandledScreenAccessor` | `AbstractContainerScreen` | fields `leftPos`, `topPos` |
| `InGameHudMixin` | `Hud` | `setOverlayMessage(Component, boolean)` |
| `ScreenRenderMixin` | `Screen` | `extractRenderStateWithTooltipAndSubtitles`, **and** the `@At` descriptor `Lnet/minecraft/client/gui/GuiGraphicsExtractor;extractDeferredElements(IIF)V` |

`ScreenRenderMixin`'s `@At(target = ...)` is a raw descriptor string — the compiler never checks
it. Verify it by hand with `javap` every single time.

### What 26.2 changed, as a worked example

26.2 moved the GUI to a retained render-state pipeline, and every one of these was invisible to
the compiler until the right class was inspected:

| Was (1.21.11) | Is (26.2) |
|---|---|
| `GuiGraphics` | `GuiGraphicsExtractor` |
| `GuiGraphics.drawString` / `drawCenteredString` | `text` / `centeredText` |
| `Screen.render` / `renderBackground` | `extractRenderState` / `extractBackground` |
| `Minecraft.setScreen` | `setScreenAndShow` |
| `Minecraft.screen` (field) | moved to `Gui`: `mc.gui.screen()` |
| `Options.hideGui` | moved to `Hud`: `mc.gui.hud.isHidden()` |
| `Player.displayClientMessage(c, false)` | `sendSystemMessage(c)` |
| `ClickType` | `ContainerInput` |
| `MultiPlayerGameMode.handleInventoryMouseClick` | `handleContainerInput` |
| `Identifier.of` | `Identifier.fromNamespaceAndPath` |
| Fabric `HudRenderCallback` | Fabric `hud.HudElementRegistry.addLast(Identifier, HudElement)` |
| Fabric `KeyBindingHelper.registerKeyBinding` | `KeyMappingHelper.registerKeyMapping` |

Then launch the client (`./gradlew runClient`) and confirm no mixin apply errors in the log. That
is the only real proof.

## Highest-risk area: `network/`

`BackgroundGuiRefresh` + `ServerGuiInterceptor` read server containers blind: they send a chat
command (`/boss`, `/pets`, `/pass quests`), cancel the `OpenScreen` packet client-side, harvest
the content packets for that container id, then send a single `ServerboundContainerClosePacket`.
No click is ever sent. They depend on server-side behaviour that no compiler and no mapping check
can validate, and on the packet mixins in `ClientPlayNetworkHandlerMixin` running on the client
thread only. Design and invariants: `docs/architecture-background-refresh.md`.

Test this **first** on the live server after any upgrade: the three HUDs must fill on join
within a few seconds, with no screen flashing and no `Hard timeout` / `No screen opened` line
in `latest.log`, and the player's own GUIs (compass, chests, `/ah`) must stay usable meanwhile.

## Philosophy

1. **Single source of truth for versions** — `gradle.properties` holds the version numbers,
   `build.gradle` holds the mappings choice.
2. **Use Fabric API events over mixins** — Fabric API abstracts most version differences. Mixins
   are reserved for places where no event exists (bossbar text intercept, GUI silent refresh).
3. **Minimal mixin surface** — six small mixins, each doing one thing. Less to fix when MC changes.

## Migrating mappings again (reference)

Only needed if the mappings choice ever changes again. Loom does the bulk of the work, including
mixin `method =` strings, `@Shadow` field names, `@Accessor` names and `@At` descriptors:

```bash
./gradlew migrateClientMappings --mappings "net.minecraft:mappings:<mc-version>" \
  --overrideInputsIHaveABackup
```

It misses **fully-qualified names written inline** (`net.minecraft.text.Text.translatable(...)`)
and occasionally drops an import without renaming its usages. Grep for leftovers afterwards.

## After a successful upgrade

1. Test in-game on the MinePiece server: boss timer detection, parchment reading, HUD edit, and
   the background GUI refresh above all.
2. Commit `gradle.properties`, `fabric.mod.json`, and any mixin fixes.
3. Tag the release: `git tag vX.Y.Z && git push --tags`, then `gh release create` with the jar
   (as a **pre-release** if players are still on the previous Minecraft version — the in-mod
   update notifier reads `releases/latest`).
