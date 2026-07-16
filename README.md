# FoolsAdmin

NeoForge admin tooling mod for the FoolsAdventure project.

In-game tools for placing and editing world content — bosses, NPCs, dialogue, and a live map overlay — without hand-editing config files.

## Features (current)

- **`/admin` command** — opens the admin screen (permission level: gamemasters)
- **World map overlay** — scroll/zoom map with live tile streaming from the server
- **Bosses** — spawn points, paint/erase movement zones, brush tools
- **NPCs** — spawn points, patrol waypoints, dwell times, stationary mode
- **Dialogue / Quests tab** — multi-line dialogue with delays, assign to NPCs
- **Live sync** — content snapshots and mutations over the network; changes persist in world saved data

Design notes: see `AdminTooling.md`. Broader game design: see `FoolsAdventurePlan.md`.

## Requirements

- Minecraft **26.2**
- NeoForge **26.2.0.17-beta**
- Java **25**

## Build & run

```bash
./gradlew build
./gradlew runClient
./gradlew runServer
```

If IDE libraries look wrong:

```bash
./gradlew --refresh-dependencies
./gradlew clean
```

## Mod identity

| | |
|---|---|
| Mod ID | `foolsadmin` |
| Display name | `FoolsAdmin` |
| Package | `com.fool.admin` |
| Main class | `com.fool.admin.FoolsAdmin` |

## License

All Rights Reserved (see `TEMPLATE_LICENSE.txt` / `mod_license` in `gradle.properties`).
