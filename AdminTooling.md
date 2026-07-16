# FoolsAdmin — Admin Tooling Design

> Detailed design for the in-game admin tools used to place content, configure quests,
> manage NPCs, and protect world areas. Referenced from the main design document.

---

## 1. Design Principles

- **Everything is in-game.** Admins should never need to hand-edit config files or restart
  the server to create, move, or modify content. Config files exist as the backing store
  and are written to automatically — they can be version-controlled or backed up, but the
  GUI is the primary interface.
- **Visual and spatial.** Placing a boss, painting a protection zone, or drawing an NPC path
  should feel like using a map editor inside Minecraft, not typing coordinates into a
  command.
- **Live and non-destructive.** Changes take effect immediately but can be reverted. Players
  may remain online while admins work.
- **Commands for quick overrides.** A command layer exists for testing, scripting, and fast
  operations, but the GUI handles all complex configuration.

---

## 2. Admin Mode

Entering admin mode (e.g. `/admin`) enables:

- A persistent **admin toolbar** with tool selectors for each system described below.
- The ability to open the **map overlay** (see Section 3).
- Visual indicators for all placed content (boss zones, protection regions, NPC paths,
  dungeon markers) that are invisible to normal players.
- Admin mode does not grant creative mode by default — the admin remains in the world as
  a player but with access to the tooling layer on top.

---

## 3. Map Overlay

A top-down map view that can be opened at any time while in admin mode. The map is the
central hub for spatial configuration.

### Display

- Renders a scrollable, zoomable 2D overhead view of the world.
- Layers can be toggled on and off:
  - **Boss zones** — colored regions showing where each boss can move.
  - **Protection zones** — shaded areas where blocks are indestructible.
  - **NPC positions and paths** — icons for stationary NPCs, drawn lines for patrol routes.
  - **Dungeon markers** — icons for overworld dungeons (small and major) and portal locations.
  - **Quest-linked locations** — markers for quest objectives, quest boards, and quest NPCs.
- The admin's current position is always shown.

### Interaction

- **Click on any marker** to open its configuration panel (boss settings, NPC dialogue, etc.)
  without leaving the map view.
- **Right-click** to place new content at a map position.
- The map and the 3D world are linked — jumping to a map position teleports the admin there
  for fine-tuning in 3D, and the map re-centers when the admin moves in the world.

---

## 4. Boss Placement and Zone Configuration

### Placing a boss

1. Admin enters the world location where the boss should spawn (either by walking there or
   clicking a position on the map overlay).
2. Selects **Place Boss** from the admin toolbar.
3. A configuration panel opens:
   - **Boss type** — select from a registry of defined boss entities.
   - **Spawn point** — the exact block position (defaults to where the admin is standing).
   - **Level / difficulty** — the boss's combat stats and intended player level.
   - **Loot table** — assign or create a loot table for the boss.
   - **Respawn behavior** — one-time kill, respawn after cooldown, or quest-gated spawn.
   - **Quest gate** — optionally link to a quest chain that must be completed before the boss
     appears.

### Movement zone

- After placing a boss, the admin **paints the movement zone** on the map overlay.
- This works like a brush tool: the admin paints connected tiles on the map to define the
  area the boss is allowed to roam within.
- The zone boundary is visualized in the 3D world as a subtle particle border (visible only
  in admin mode).
- The boss AI is hard-constrained to this zone — it will not path, be knocked, or teleport
  outside it.
- Zones can be resized or repainted at any time.

### Boss arena features

- **Arena trigger region** — an optional outer boundary that, when a player enters, starts
  the encounter (locks exits, begins boss behavior, etc.).
- **Reset behavior** — what happens when all players leave or die (boss resets to spawn
  point, heals, cooldown begins).

---

## 5. Area Protection (Indestructible Zones)

### Painting protection

- Admin selects the **Protection Brush** from the toolbar.
- On the map overlay, the admin paints regions that should be indestructible, similar to
  painting in an image editor.
- Brush size is adjustable (single block to large area).
- The height range of the protection can be set:
  - **Full column** — bedrock to build limit (default for structures).
  - **Custom range** — specific Y-level bounds for partial protection (e.g. protect a bridge
    but not the ground below it).

### Protection rules

- Protected blocks cannot be broken, moved, or exploded by any non-admin player or entity.
- Placing blocks inside a protected zone by non-admins can be allowed or denied per zone
  (useful if a structure should be explorable but not modifiable).
- Protection is invisible to normal players — no visible barrier, particles, or UI element.
  The block simply does not break.

### Management

- Protection zones are listed in a panel and can be named (e.g. "Castle Ruins East",
  "Boss Arena — Dragon").
- Zones can be toggled on/off, deleted, or repainted.

---

## 6. NPC System

### Placing an NPC

1. Admin walks to the desired location or selects a map position.
2. Selects **Place NPC** from the toolbar.
3. Configuration panel:
   - **Name and appearance** — display name, skin, and optional custom model.
   - **Behavior type:**
     - **Stationary** — stands in place, faces the nearest player.
     - **Patrol** — follows a painted path (see below).
     - **Scheduled** — appears and disappears on a timed schedule.
   - **Lifespan** — permanent, or present for a set duration (e.g. "available for 7 real-time
     days after a quest is triggered"). Useful for event NPCs or story beats that are
     temporary.
   - **Interaction type** — dialogue, quest giver, merchant, or ambient (no interaction,
     just scenery/atmosphere).

### Dialogue editor

- Opens when configuring an NPC's dialogue or quest offering.
- **Node-based dialogue tree:**
  - Each node contains the NPC's spoken text (displayed as chat or a dialogue UI to the
    player).
  - Nodes can branch based on player responses (multiple-choice replies).
  - Nodes can check conditions:
    - Player's role, level, quest progress, inventory contents, clan membership.
  - Nodes can trigger actions:
    - Grant a quest, complete a quest stage, give an item, open a shop, teleport the player,
      spawn an entity, play a sound or particle effect.
- **Preview mode** — the admin can walk through the dialogue as if they were a player to
  test it before going live.

### Patrol paths

- Admin selects **Paint Path** and draws a route on the map overlay or by placing waypoints
  in the 3D world.
- The NPC walks this path on a loop, pausing at each waypoint for a configurable duration.
- Path speed is adjustable (stroll, walk, run).
- The NPC remains interactable while patrolling — a player approaching them pauses their
  movement until the interaction ends.
- Paths can be edited by dragging waypoints on the map.

### Story NPCs

- NPCs can be linked to **story sequences**: a chain of dialogue, movement, and world events
  that play out in order.
- Example: an NPC walks to a location, delivers a speech, then a boss spawns. The NPC
  disappears after the event concludes.
- Story sequences are built in the dialogue editor using a timeline of nodes that include
  movement commands and world triggers alongside dialogue.

---

## 7. Quest Configuration

### Quest editor

- Opened from the admin toolbar or by clicking a quest marker on the map.
- **Quest properties:**
  - Name, description, and flavor text.
  - Level requirement (minimum player level to accept).
  - Repeatable or one-time.
  - Reward list: XP, items, materials, currency, reputation.
- **Objective types:**
  - Kill a specific mob or boss (with count).
  - Collect items or materials (with count).
  - Visit a location (enter a defined region).
  - Talk to an NPC.
  - Clear a dungeon.
  - Deliver an item to an NPC.
  - Survive for a duration in a zone.
  - Custom trigger (fired by admin command or other mod integration).

### Quest chains

- Quests can be linked into ordered chains where completing one unlocks the next.
- A chain can be visualized as a flowchart in the editor showing prerequisites and branches.
- **Boss-gate chains:** the final quest in a chain triggers a boss spawn. The editor clearly
  marks this relationship and allows the admin to set which boss, where, and what materials
  or quest completions are prerequisites.

### Quest assignment

- Quests are assigned to **NPCs** (the NPC offers them through dialogue) or to **quest
  boards** (the player browses available quests at a board).
- An NPC or board can offer multiple quests. The editor shows which quests are assigned where.
- Quest availability can be conditional (level, role, prior quest completion, time of day).

---

## 8. Dungeon Management

### Overworld dungeon markers

- Small overworld dungeons generated at world creation are automatically tracked.
- Admins can mark additional locations as dungeons or remove the dungeon status from a
  generated structure.
- Each dungeon marker tracks: cleared/uncleared state, loot table, mob spawner
  configuration.

### Portal dungeon configuration

- Portal locations are placed like bosses — admin walks to a location and assigns a portal.
- Portal properties:
  - Linked dimension / dungeon layout.
  - Tier (1-5, determines cooldown and expected difficulty).
  - Current cooldown state (can be manually reset by admin).
  - Loot table for the dungeon and its boss(es).

### Dungeon testing

- Admin can enter any portal dungeon regardless of cooldown state for testing purposes.
- A `/resetdungeon <name>` command instantly resets a dungeon's cleared state and cooldown.

---

## 9. Command Reference

Commands exist as shortcuts and for scripting. All commands require admin permission.

| Command | Purpose |
|---------|---------|
| `/admin` | Toggle admin mode on/off |
| `/admin map` | Open the map overlay |
| `/boss place <type>` | Start boss placement at current position |
| `/boss remove <name>` | Remove a placed boss |
| `/boss list` | List all placed bosses |
| `/npc place <type>` | Start NPC placement at current position |
| `/npc remove <name>` | Remove a placed NPC |
| `/npc list` | List all placed NPCs |
| `/protect add <name>` | Start painting a new protection zone |
| `/protect remove <name>` | Remove a protection zone |
| `/protect list` | List all protection zones |
| `/quest create <name>` | Open quest editor for a new quest |
| `/quest enable/disable <name>` | Toggle a quest's availability |
| `/quest grant <player> <quest>` | Force-grant a quest to a player (testing) |
| `/quest complete <player> <quest>` | Force-complete a quest for a player (testing) |
| `/dungeon reset <name>` | Reset a dungeon's cooldown and cleared state |
| `/dungeon setcooldown <name> <time>` | Override a dungeon's cooldown duration |

---

## 10. Data Storage

- All admin-placed content is stored in **JSON config files** organized by type:
  - `config/bosses/` — one file per boss with position, zone, loot, and quest links.
  - `config/npcs/` — one file per NPC with position, dialogue tree, path, and schedule.
  - `config/protection/` — one file per zone with region data.
  - `config/quests/` — one file per quest or chain with objectives, rewards, and assignments.
  - `config/dungeons/` — one file per dungeon with portal link, tier, and loot table.
- Files are written automatically when the admin makes changes through the GUI.
- Files can be backed up, version-controlled, or copied between servers.
- On server start, all config files are loaded. Manual edits to files are picked up on
  restart or via a `/admin reload` command.
