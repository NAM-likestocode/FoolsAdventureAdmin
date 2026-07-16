# FoolsAdventure — Design Document

> Living design document. Confirmed decisions describe the current direction; draft ideas
> may be changed or removed. Unresolved items are collected in **Section 7**.

---

## 1. Game Vision

### Tone and aesthetic

- **Vibe:** RPG, adventure, knights, and magic — deliberately not modern or sci-fi.
- **World feel:** vast and varied, with an emphasis on genuinely unique content instead of a
  small set of structures or mobs repeatedly reskinned.

### Server context

- Designed for a **multiplayer server** with roughly 30 total players.
- Expected peak concurrency is around 10 players online at once.
- Group content (party bosses, portal contention, trading) is designed around this scale.
- **3 admins** manage content creation (boss placement, quests, secret dungeons, rare ores).
  Content only expands when admins actively add it — there is no procedural content
  generation beyond the initial world.

### Core design pillars

1. **Explore a world worth getting lost in.** Custom structures, dungeons, secrets, varied
   creatures, and altered terrain should make discovery a major part of progression.
2. **Choose a distinct role.** Roles change movement, combat, and utility rather than only
   changing stats.
3. **Earn power through adventure.** Bosses and dungeon clears drive progression; vanilla mob
   grinders should not.
4. **Specialize without becoming locked out.** Players may use off-role gear and abilities,
   but perform noticeably better within their chosen role.
5. **Support both parties and long-term mastery.** Groups should be able to tackle top content
   before a dedicated max-level player can solo it.

---

## 2. World and Technical Foundation

### Base mods

- Create (builds, trains, and recipe automation — no Aeronautics)
- Epic Fight (combat overhaul — replaces vanilla combat with directional attacks, rolling,
  combos, and animation-driven melee). Epic Fight itself will be modified alongside
  FoolsAdventure to role-gate certain combat actions (e.g. specific combo chains or dodge
  types available only to certain roles). Exact mapping depends on a deeper audit of what
  Epic Fight exposes and is deferred until that review.
- One or more Animals & Mobs mods as the foundation for creature variety

### World generation — confirmed

- **Foundation:** Larion World Generation.
- **Adjustment:** reduce ocean frequency so the map is more land-dense and explorable on foot
  or by train.
- **Ore reduction:** vanilla ore generation rates reduced by roughly **35%** to slow
  material progression and increase the value of quest-reward materials and trading.
- **Admin-placed rare ores:** the highest-tier custom ores do not spawn naturally. Admins
  scatter them manually via the admin map overlay, controlling exactly how much top-end
  material enters the economy.
- **Difficulty zoning:** the world should visually communicate danger level through biome
  appearance. Ideally the world generator is adjusted so that higher-difficulty biomes are
  placed further from spawn, with the beginner-friendly zone concentrated around the spawn
  area.
- **Xaeros World Map integration:** the map is altered to show difficulty regions. Dungeon
  locations, portal markers, and other points of interest appear on the map **only after
  the player has visited them** — nothing is revealed in advance.
- Layer custom structures and dungeons onto the resulting world.

### Travel — confirmed direction

- **No fast travel, waypoints, or teleportation systems.** Travel time is an intentional
  part of the game. Reaching distant content requires commitment and planning.
- **Elytras are removed.** Flight trivializes distance and exploration.
- Available travel methods: walking, horses, boats, minecarts, and **Create trains**.
- Create trains offer the most efficient long-distance travel but require infrastructure
  investment — building rail lines and stations is a meaningful project for players and
  clans.
- The Mage's short-range teleport is a combat and utility ability, not a travel shortcut.

### Vanilla system changes — confirmed

| System | Status |
|--------|--------|
| Elytras | **Removed** |
| Vanilla XP | **Exists** (used for vanilla mechanics that need it, not for role leveling) |
| Enchanting | **Removed** (replaced by the affix and crafting systems) |
| Villager trading | **Removed** (replaced by custom NPC shops) |
| The End (dimension + Ender Dragon) | **Removed entirely** |
| The Wither | **Exists** |
| Brewing/potions | Exists (may be adjusted) |
| The Nether | Exists (may be adjusted) |

### Underground — confirmed direction

- Underground areas contain **stronger and more dangerous mobs** than the surface at the
  same distance from spawn. Going underground early is a serious risk.
- This is deliberate: it pushes early-game players toward surface quests and overworld
  content rather than mining their way to strong gear. Players will likely survive on iron
  or equivalent armor for a meaningful stretch before they can safely mine deeper resources.

---

## 3. Player Progression

### Leveling model

- **Level cap:** 150.
- Progression uses a custom system rather than vanilla XP.
- Common mobs provide a small amount of useful early XP, with sharply diminishing value at
  higher levels.
- Boss kills and dungeon clears award the main, front-loaded chunks of XP.
- The curve becomes especially steep after level 100. Reaching 150 should be a genuine
  long-term achievement.

| Level | Approx. playtime | Power benchmark |
|------:|-----------------:|-----------------|
| 20 | 12 hours | Weak; early game |
| 60 | 40 hours | Roughly "endgame-lite" |
| 100 | 90 hours | Able to defeat top bosses with a group |
| 150 | 150 hours | Full endgame; able to solo any content |

These estimates describe an average player; skilled players may progress faster.

### Death penalty — confirmed

- Dying costs **2 full levels** plus all progress toward the next level.
- The penalty applies identically in PvE and PvP.
- **The starter village is a PvP-free zone.** No player combat is allowed within the
  village boundaries.
- **PvP death cooldown:** after a PvP death, the killed player is protected from further
  level loss for approximately 20 minutes. This prevents grief-killing from compounding the
  penalty.
- Outside the starter village, PvP is fully open at all levels with no restrictions.
- At the progression curve's steeper end this is a severe punishment, reinforcing the value of
  caution, group play, and preparation before high-level content.

**Item loss on death — confirmed:**

- **Equipment above a certain rarity grade** is automatically kept on death.
- **Materials, consumables, and low-rarity items** are dropped and lost as in vanilla.
- **10% loss chance for high-rarity gear:** even items above the safe threshold have a
  **10% chance** of being lost on death unless the item has been **soulbound** (see below).
  This keeps a meaningful risk even for well-geared players without making every death
  devastating.
- **Rarity cutoff: Heroic and above** is kept on death. Common, Uncommon, and Rare items
  are dropped and lost.

**Soulbinding — confirmed:**

- A craftable upgrade applied to an item in the same style as a netherite upgrade (smithing
  table or equivalent station, requires specific materials).
- Once applied, the item gains a **soulbound** attribute and can **never be lost on death**.
- The upgrade is **permanent** — it does not wear off or need reapplication.
- Soulbinding is a **late-game luxury** — the materials are expensive and obtained
  primarily through crafting, with some components from dungeon drops or quests.
  Players will need to prioritize which items to protect first. Exact recipe TBD.

### Level rewards

Levels and milestones can grant more than one kind of reward:

- role-specific ability unlocks and upgrades;
- stat increases such as health or damage;
- Mage spell points; and
- access to the full strength of higher-level gear.

### Gear system — confirmed

All equipment is **custom**. Vanilla Minecraft gear tiers (iron, diamond, netherite) are
replaced by a purpose-built loot system.

**Rarity tiers (8 levels):**

| Tier | Position |
|------|----------|
| Common | lowest |
| Uncommon | |
| Rare | |
| Heroic | |
| Arcane | |
| Epic | |
| Legendary | |
| Mythic | highest |

All eight tiers are confirmed.

**Item stat rules:**

- **Crafted items** have **fixed stats** — every copy is identical.
- **Dropped items** have **randomized attributes** by default, producing natural variation
  between copies of the same item.
- **Boss-specific and hand-placed drops** may override randomization with designer-set stats
  when a precise reward is desired.

**Affix system — confirmed:**

- Dropped items can roll both **simple affixes** (flat stat boosts like +health, +damage,
  +speed) and **interesting affixes** (proc effects like chance to inflict burning, lifesteal
  on hit, cooldown reduction, etc.).
- Interesting affixes are significantly rarer than simple ones.
- The **maximum number of affixes** an item can roll depends on its rarity:

| Rarity | Max affixes |
|--------|------------:|
| Common | 0 |
| Uncommon | 1 |
| Rare | 1 |
| Heroic | 2 |
| Arcane | 2 |
| Epic | 3 |
| Legendary | 3 |
| Mythic | 4 |

**Additional gear features:**

- **Extra equipment slots** beyond vanilla: **1 ring, 1 amulet, 1 cape, and 1 belt**. These
  are natural fits for set bonuses, role-specific utility, and unique boss drops.
- **Role-specific items:** some gear is designed for a particular role. A bow may only be
  truly effective for a Ranger; other roles can still pick it up and trade it, but gain
  little from equipping it themselves.
- **Set bonuses:** wearing a matched set of items grants additional effects beyond the
  individual pieces.
- **Boss-specific drops:** certain bosses drop unique items that cannot be obtained elsewhere.
- **One-time items:** approximately **15 per role** (~75 total across 5 roles). These do
  not all need to exist at launch — they can be added over time as content expands. When a
  player claims a one-time item, a **server-wide announcement** is displayed to all
  players.
- **Player trading:** items are freely tradeable. A Knight who finds a Ranger bow can trade
  it for something useful to them, encouraging cross-role cooperation.

### Gear level requirements — confirmed

Gear uses a **soft level requirement**, not a hard equip lock.

- An under-leveled player may equip any item, but the item is capped at a weak baseline until
  its requirement is met. For example, a legendary chestplate may perform like leather armor.
- The penalty is flat regardless of how far below the requirement the player is.
- Some items may also impose an active drawback, such as reduced movement speed, swing speed,
  or accuracy.
- This mirrors role affinity: equipment remains usable, but must be earned before it becomes
  effective.
- Tooltips and UI should clearly explain the unmet requirement and suppressed stats.

---

## 4. Roles and Specialization

### Shared role rules

- Players choose a role in an Origins-style GUI before first spawning.
- **Restarting is possible:** a player may abandon their current role and start over at
  level 0 with a new role. On restart, the player loses:
  - All levels (reset to 0).
  - All quest progress.
  - Clan membership.
  - Everything in their inventory.
  - This is a full wipe — not a free respec.
- Roles provide a stat baseline, weapon or ability affinities, and a unique movement and/or
  ability kit.
- Specialization is soft: off-role weapons and abilities remain usable but are less effective.
- Medium- and high-tier magic is exclusive to the Mage.
- Other roles may learn limited low-tier utility magic, such as a small self-heal, but cannot
  develop a second full magic kit.

### Roster — confirmed

All five roles below are confirmed for launch. Additional roles may be added later.
Druid/Beastmaster, Bard, and Engineer/Tinkerer have been removed from the roster for now.

**Knight** — sword-and-shield specialist with high HP and reliable all-round performance.
Limited movement options (1-2 abilities). Weak magic and below-average bow use.

**Mage** — low starting HP. Deliberately weak in the early game with limited spell access,
becoming increasingly powerful as spells unlock through leveling and spell-point investment.
This late-game payoff is the trade-off for a rough start. Begins with or unlocks a
short-range teleport that improves with level, plus magic shields for self and eventually
teammates. Full spell system in **Section 5**.

**Assassin** — high damage, low HP. Movement abilities are the defining feature, with a
large kit of ~5 options (climbing, rolling, dodging, invisibility, double jump) that
unlock progressively with level. By comparison, a Knight may only have 1-2 movement
options. The breadth of the Assassin's mobility kit is what makes the role feel distinct
— survival depends on outmaneuvering rather than absorbing damage.

**Ranger** — bow and trap specialist with high mobility, decent HP, weak melee, and weak
magic. Shares the Assassin's agility theme but plays at range.

**Berserker** — high-risk melee damage dealer with low defense. Weak magic and ranged combat.
**Rage** (hotkey activation): grants invincibility for 20 seconds and a 1.3x damage
multiplier, but ends by dropping the Berserker to 1 heart. High-reward, high-commitment
ability — using it carelessly is a death sentence.

---

## 5. Mage Magic System

Mage subtypes are specializations within the Mage role, not standalone roles. This preserves
the rule that medium- and high-tier magic belongs to the Mage while still allowing distinct
builds.

### Spellbook system — confirmed

- Mages carry a **book of spells** as their primary weapon/tool.
- Spells are selected from the book and shown in the GUI. The Mage then activates the
  selected spell by aiming at the target location or enemy.
- **Spell types** include projectiles, area-of-effect casts on a targeted zone, and other
  variants. Mages are not close-range fighters — they excel at AoE damage and high-output
  ranged spells.
- **High-ranking offensive spells** (the most powerful projectile and AoE damage spells)
  are limited to the **Battlemage** subtype as signature or near-signature abilities.

### Subtypes — confirmed roster

- **Elementalist/Battlemage:** the primary damage-caster. Direct fire, ice, and lightning
  damage spells. **Signature:** exclusive access to the highest-tier offensive spells
  (major AoE and burst damage). Other subtypes can learn lower-tier damage spells through
  the core pool, but the top end is Battlemage-only.
- **Cleric:** healing over time and party buffs. **Signature: Revive** — no other subtype
  can learn it.
- **Necromancer/Warlock:** life drain, and the ability to raise and command undead minions.
  Possibly spends HP as a resource instead of mana. Fragile with a high skill ceiling.
  **Signature:** summoning undead companions — only a Necromancer can raise the dead.
- **Chronomancer:** time-manipulation specialist. Can freeze enemies in place, and
  **reverse themselves** — rewinding their own position and health to a point a few
  seconds prior (similar to Tracer's recall in Overwatch). Builds on the base Mage
  teleport kit with stronger and more frequent blinks. **Signature:** the self-reversal
  ability.

### Spell points and home schools

- Mages earn spell points separately from the general upgrades shared by all roles.
- Spell points learn spells and increase their ranks.
- The pool is limited, so no Mage can maximize every spell.
- A subtype acts as a **home school**, not a hard lock. Home-school spells cost fewer points
  and can be developed further before diminishing returns.
- Other core spells remain learnable at a higher opportunity cost. For example, a Cleric can
  build meaningful damage, but must invest more than an Elementalist would.

### Spell categories

1. **Core spells** form the shared pool. Any Mage subtype may learn them, while the relevant
   home school uses them more efficiently.
2. **Signature spells** are hard-locked to one subtype. They are reserved for abilities that
   define that subtype's fantasy.

**Signature rule:** if a spell would let one subtype fully replace another — such as a
Battlemage reviving allies or a Cleric raising an undead army — it should be signature-locked.
Broad damage, sustain, or utility effects should usually remain core spells.

**Confirmed signatures:**

| Subtype | Signature |
|---------|-----------|
| Battlemage | Highest-tier offensive spells (major AoE and burst) |
| Cleric | Revive |
| Necromancer | Undead summoning |
| Chronomancer | Self-reversal (position and health rewind) |

---

## 6. Adventure Content and Game Phases

### Dungeon system — confirmed direction

Dungeons are the primary progression content. They come in two fundamental forms:

**Overworld dungeons — small encounters**

- Physical structures that exist directly in the world, generated at world creation.
- Relatively rare: roughly **5,000+ blocks** between each small overworld dungeon to keep
  them worth seeking out.
- One-time clear: once completed, the dungeon stays cleared.
- These reward exploration and are part of what makes the world feel hand-crafted.

**Overworld dungeons — major boss encounters**

- Large, high-stakes dungeons and boss arenas that are **manually placed** by an admin rather
  than procedurally generated.
- The admin tooling must support designating a region or structure as a major encounter and
  controlling the space allocated to each boss (limiting how much world area a single boss
  occupies).
- Full admin tooling design is documented in **[AdminTooling.md](AdminTooling.md)**.

**Portal dungeons**

- Accessed through portals in the overworld that transport the player to a separate
  dimension containing the dungeon.
- Portal dungeons are **repeatable**: after a clear (or full party wipe), the portal closes
  on a **global cooldown** — no one can re-enter until the cooldown expires.
- **Entry mechanic:** when the first player enters a portal, it remains open for **one
  minute**. Everyone within a **10-block radius** of the portal is teleported in together.
  After one minute the portal seals — no further entry until the dungeon resolves.
- **Multiple groups may enter** during the one-minute window. However, only the group that
  deals the most damage to the boss (or lands the killing blow) receives the loot.
- **No difficulty scaling.** Dungeons do not adjust to party size. Players must discover for
  themselves how hard a dungeon is and how many people they need to bring. Mapping out which
  portals are farmable and which require a full group is an intentional part of the
  metagame.
- This is the main renewable XP and loot source at all stages of the game.
- **Portal contention is intentional.** A strong clan can control a portal by clearing it
  regularly. Other players must either negotiate access, time their runs around the
  controlling group, sneak in during off-hours, or fight their way in — entering during
  the one-minute window and ambushing the group inside. This creates organic PvP and
  political tension around valuable resources.

**Portal dungeon tiers — confirmed:**

| Tier | Difficulty | Approximate group needed | Global cooldown |
|-----:|------------|--------------------------|----------------:|
| 1 | Easy; soloable or small group | 1-2 players | 20 minutes |
| 2 | Moderate; requires a group | 3-4 players | 35 minutes |
| 3 | Hard; experienced group of level 50+ | 4-5 players | 2 hours |
| 4 | Very hard; group of 5+ level 100s | 5+ players | 1 day |
| 5 | Extreme; multiple max-level or 10+ level 100s | 10+ players | 3 days |

Group sizes are approximate — the dungeon does not communicate its requirements. Players
must learn through experience and word of mouth.

**Loot on repeated clears — confirmed:**

- **First clear** of any portal dungeon awards bonus loot.
- **Tiers 1-4:** loot diminishes on subsequent clears compared to the first-clear bonus.
- **Tier 5:** loot does **not** diminish on repeat clears — the difficulty is its own
  gate. However, the first clear awards an additional **special high-tier drop** that does
  not repeat.

**Secret dungeons — confirmed direction**

- **Admin-placed** like major boss encounters, hidden inside custom builds and locations.
- NPCs can be configured by admins to hint at or direct players toward secret dungeons,
  but there is no automatic discovery mechanic — finding them depends on exploration,
  NPC clues, and word of mouth.
- Award disproportionately strong loot and XP compared to normal content.
- Can be either overworld or portal-based.

### Quest system — confirmed direction

Quests serve multiple purposes and are a core progression system alongside dungeons.

**Quest sources:**

- **Village NPCs** offer quests through dialogue.
- **Quest boards** in settlements list available tasks.

**Quest purposes:**

- **Leveling:** straightforward quests that reward meaningful XP as an alternative or
  supplement to dungeon grinding.
- **Materials and crafting:** quests that reward special metals, materials, or crafting
  components not available through normal drops.
- **Boss-gate quests:** quest chains that serve as prerequisites for major boss encounters.
  The boss only spawns once the required quests are completed and materials gathered. This
  ties the biggest fights to narrative buildup rather than just walking into an arena.
  - **Medium bosses:** roughly **5 quests** in the chain leading up to the fight.
  - **Endgame bosses:** up to **20 quests** in the chain, potentially including medium boss
    fights as intermediate steps along the way.
  - All quest chains are designed and written by admins — they are hand-crafted narrative
    content, not procedurally generated.

**Admin management:**

- Quests must be **configurable in-game by an admin** — creating, editing, enabling, and
  disabling quests without requiring a server restart or code changes. This includes
  setting up boss-gate quest chains and their required materials.
- Full admin tooling design is documented in **[AdminTooling.md](AdminTooling.md)**.

### Crafting — confirmed direction

- Custom crafting stations and combination methods exist beyond vanilla crafting.
- Crafted items always have **fixed stats** (see Section 3).
- Some recipes require **quest-reward materials** that cannot be obtained through drops alone,
  tying crafting progression into the quest system.

**Materials — confirmed direction:**

- Vanilla ores (iron, gold, diamond, etc.) remain relevant in the early and mid game.
- A large set of **custom materials** is introduced for higher-tier crafting. The most
  valuable custom ores do not spawn naturally and are admin-placed (see Section 2).
- The progression is intentional: players start with vanilla materials, then transition
  toward custom materials obtained through quests, dungeons, and trading as they advance.

**Crafting stations — confirmed direction:**

- **Role-themed stations** exist (e.g. a forge for heavy armor, an arcane table for magical
  items).
- No environmental or biome requirements for now — stations can be placed and used anywhere.
- Specific station types and their recipes are TBD.

### Party and clan systems — confirmed

Two separate social systems serve different purposes:

**Party system:**

- Temporary groups formed for dungeon runs and combat.
- **No maximum party size.**
- **XP split by contribution** — contribution is measured as a combination of **damage
  dealt, damage reflected, and healing done**. This ensures support roles like Cleric
  receive a fair share of XP. Damage from summoned entities (e.g. Necromancer undead)
  credits the summoner.
- Shared HP visibility for party members.

**Clan system:**

- Persistent larger groups (e.g. 10 players) that represent a long-term team.
- Members may be offline; the clan persists across sessions.
- Separate from the party system — a clan of 10 might have 4 online who form a party together.
- **Base claim:** clans can claim a region on the minimap. This is **visual only** — the
  land itself is not mechanically protected, but the claim shows on the map as clan
  territory.
- **Leaderboard:** clans are ranked, providing a competitive element.
- **Clan bank:** a shared storage system using **gold coins** as currency. Members deposit
  and withdraw gold through an in-game interface.
- **Clan roles and permissions:** an in-game interface allows the clan leader to assign roles
  (e.g. leader, officer, member, recruit) with configurable permissions. Bank access,
  invite rights, and task assignments can be restricted by role — for example, recruits
  may be unable to withdraw from the bank, while officers handle diplomacy and resource
  allocation.

### Intended game phases

**Early game (roughly levels 1-20):**

- Player spawns in a **starter village** — a safe hub with NPCs, quest boards, and basic
  services.
- A defined **beginner zone** surrounds spawn. Surface mobs here are beatable at low level
  but still require caution. The zone is visually distinct and marked on the world map.
- Quests from the village and quest boards are the primary early progression path.
- Underground is already dangerous — strong mobs discourage mining early, keeping players
  on the surface doing quests and exploring.
- Players will likely survive on iron-tier armor for a while. Reduced ore rates mean
  better materials come through quest rewards and dungeon loot rather than mining.
- Tier 1 portal dungeons become accessible as the player gains a few levels.

**Midgame (roughly levels 20-60):**

- Players push beyond the beginner zone into higher-difficulty biomes.
- Tier 2-3 portal dungeons and overworld encounters become the main content.
- Role specialization deepens; Mages choose a subtype; gear sets start to matter.
- Group play becomes increasingly important for harder content.

**Late game (roughly levels 60-100):**

- Tier 4 portal dungeons and major admin-placed boss encounters.
- Advanced builds, high-tier gear sets, and party composition matter significantly.
- Quest chains leading to boss-gate encounters provide narrative-driven progression.

**Mastery (levels 100-150):**

- Tier 5 portal dungeons requiring large coordinated groups.
- Secret dungeons and the rarest loot.
- The steepest progression curve — reaching 150 is a genuine long-term achievement.
- Eventually enough power to solo content that previously required a full party.

### Economy — confirmed direction

- **Gold coins** are the primary currency. They drop from mobs, dungeons, and quests, and
  are used in player-to-player trading, NPC shops, and the clan bank.
- **Player trading** uses gold coins alongside direct item-for-item barter.
- The coin economy ties into clan banks, giving clans a shared financial resource to manage
  collectively through their role-based permission system.

**NPC shops — confirmed:**

- NPCs sell **gear, materials, and consumables** (potions, food, etc.) for gold.
- Players can also **sell items back** to NPC shops for gold.
- This creates a baseline floor for item value and gives gold a consistent use beyond
  player-to-player trading.

**Economy tuning:**

- All gold values (mob drops, dungeon rewards, quest payouts, shop prices, sell-back ratios)
  are **configurable via JSON** and will be tuned in-game during testing. No values are
  hardcoded.

---

## 7. Open Decisions

### Roles and combat

- How do Epic Fight's combat mechanics distribute across roles?
- What are the stat baselines (HP, damage, affinity numbers) for each role?

### Gear

- What specific interesting affixes exist (full list of proc effects)?

### Crafting

- What specific custom crafting stations exist and what does each produce?
- What combination or item-upgrading methods exist beyond station crafting?
- What materials are required for the soulbind upgrade?

### Economy and loot

- All gold values are JSON-configurable and deferred to in-game testing.
- How does loot distribution work alongside the contribution-based XP split — does loot
  follow the same rule, or is it separate?

---

## 8. Implementation Progress

### Done

- [x] **World generation** — Larion forked, ported to MC 26.2, oceans reduced, all terrain
  and biome adjustments applied.

### To do

- [ ] Custom leveling system (XP from bosses/dungeons, diminishing mob XP, level cap 150)
- [ ] Death penalty (lose 2 levels + progress, item loss by rarity, 10% high-rarity roll)
- [ ] Soulbinding crafting upgrade
- [ ] Role selection GUI (Origins-style, 5 roles)
- [ ] Knight abilities and stat baseline
- [ ] Assassin movement ability kit (climb, roll, dodge, invis, double jump — level-gated)
- [ ] Ranger abilities (bow affinity, traps, mobility)
- [ ] Berserker rage mechanic (20s invincibility, 1.3x damage, drop to 1 heart)
- [ ] Mage spellbook system (spell selection GUI, aim-to-cast)
- [ ] Mage subtypes (Battlemage, Cleric, Necromancer, Chronomancer)
- [ ] Spell point system and home-school efficiency
- [ ] Signature spells per subtype
- [ ] Soft role specialization (off-role penalties)
- [ ] Low-tier magic access for non-Mage roles
- [ ] Epic Fight mod integration and role-gated combat actions
- [ ] Custom gear system (8 rarity tiers, replace vanilla tiers)
- [ ] Affix system (simple + interesting affixes, rarity-based max count)
- [ ] Extra equipment slots (ring, amulet, cape, belt)
- [ ] Set bonus system
- [ ] Soft gear level requirements (stat suppression + active penalties)
- [ ] Gear tooltips showing unmet requirements
- [ ] Role-specific item affinity system
- [ ] One-time items with server-wide announcements
- [ ] Boss-specific unique drops
- [ ] Gold coin currency (drops, shops, trading)
- [ ] NPC shop system (buy and sell, JSON-configurable prices)
- [ ] Player-to-player trading interface
- [ ] Portal dungeon system (dimension teleport, 1-min entry window, 10-block radius)
- [ ] Portal dungeon tiers 1-5 (global cooldowns, loot diminishing)
- [ ] First-clear bonus loot and tier 5 special drops
- [ ] Loot distribution (damage contribution-based)
- [ ] Overworld dungeon generation (5,000+ block spacing, one-time clear)
- [ ] Admin tooling — admin mode and map overlay
- [ ] Admin tooling — boss placement with movement zone painting
- [ ] Admin tooling — area protection brush
- [ ] Admin tooling — NPC placement, dialogue editor, patrol paths
- [ ] Admin tooling — quest editor and quest chain flowchart
- [ ] Admin tooling — dungeon management and portal configuration
- [ ] Admin tooling — rare ore scattering via map
- [ ] Admin commands (`/admin`, `/boss`, `/npc`, `/protect`, `/quest`, `/dungeon`)
- [ ] Quest system (NPC quests, quest boards, leveling/material/boss-gate purposes)
- [ ] Boss-gate quest chains (5 quests for medium, up to 20 for endgame)
- [ ] Party system (no max size, contribution-based XP, shared HP visibility)
- [ ] Clan system (persistent groups, base claim on minimap, leaderboard)
- [ ] Clan bank (gold deposits/withdrawals, role-based permissions)
- [ ] Clan roles and permission GUI
- [ ] PvP-free starter village zone
- [ ] PvP death cooldown (20 min protection from level loss)
- [ ] Portal contention mechanics (ambush, multi-group entry, damage-based loot)
- [ ] Role restart (full wipe — levels, quests, inventory, clan)
- [ ] Crafting stations (role-themed, no biome requirements)
- [ ] Custom materials and quest-reward-only materials
- [ ] Difficulty zoning (biome appearance by distance from spawn)
- [ ] Underground mob difficulty scaling
- [ ] Ore rate reduction (~35%)
- [ ] Xaeros World Map integration (visit-to-reveal, difficulty regions)
- [ ] Elytra removal
- [ ] Vanilla enchanting removal
- [ ] Villager trading removal (replaced by NPC shops)
- [ ] The End removal
- [ ] Beginner zone design and starter village content
- [ ] Config file structure (JSON backing store for all admin-placed content)
