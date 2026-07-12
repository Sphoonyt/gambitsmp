# GambitSMP

A custom Paper plugin (Java 17, targets Paper 1.20.4) implementing your GambitSMP card system:
right-clickable "card" items that grant passive or active abilities, legendary items,
a kill-drop "mystery card" system, three shrine types, and a hardcore
lose-hearts-forever / ban-at-zero-hearts death system.

## Building

```
cd GambitSMP
mvn package
```

This needs internet access to `repo.papermc.io` and Maven Central (it wasn't buildable in this
sandbox because that domain isn't reachable from here — pull the project down and build it
locally or in your normal dev environment). The shaded jar lands in `target/GambitSMP.jar`;
drop it in your server's `plugins/` folder. Requires Paper 1.20.4 (or update the `pom.xml`
version + adjust any changed API calls for a different Minecraft version).

## How it works

### Cards
- Every card is a `PAPER` item with the card's name/description in its lore and a hidden NBT
  tag identifying it. Right-click (not sneaking) with a card item to **equip** it — this
  consumes the item and adds it to your equipped list (default max **3**, change
  `max-equipped-cards` in `config.yml`).
- Cards are either:
  - **Passive** — always on while equipped (e.g. Reinforced Armor, Hellflame, Feather).
  - **Active** — has a cooldown, and you trigger it with **sneak + right-click while your hand
    is empty**. If you have more than one active card equipped, the first one that's off
    cooldown fires. Active cards: Berserk, Negation, Nightbringer, Warden Beam, Gravity.
- `/gambit cards [player]` lists someone's equipped cards, legendaries and max hearts.
- `/gambit unequip <CARD_NAME>` unequips a card back into your inventory as an item.
- Admin: `/gambit give card <player> <CARD_NAME>` to spawn any card in.

### Legendaries
- Legendary items work the same way (right-click to equip) and grant a bonus passive:
  - **Sunken Anchor** → King of the Sea (+30% damage while in water)
  - **Rosen Hellfire** → Immortal Flames (longer/harder-to-put-out fire — currently a flag
    other card logic can check via `PlayerCardData#hasImmortalFlames()`; extend `CombatListener`
    /a `EntityCombustEvent` listener if you want it to also extend the wearer's own burn time)
  - **Vampiric Claws** → Hard Syphon (+50% to any healing from your cards, e.g. Vampiric)
  - **Fang** → Venomous Snake (your hits poison; hook up rotten-flesh-as-golden-carrot in a
    `PlayerItemConsumeEvent`/`FoodLevelChangeEvent` listener the same way — stubbed out as a
    clearly marked spot to extend)
  - **Fang + Vampiric Claws combined in an anvil** → **Vampiric Fang**, a standalone Netherite
    Sword-based weapon that lifesteals half a heart per hit, no card slot required.

### Kill drops
- When a player kills another player, a **Mystery Card** drops at the death location. It has a
  card already secretly rolled onto it. Right-clicking it opens a small menu: **Use now**
  (equip immediately, or get the physical card if your slots are full) or **Keep in inventory**
  (get the physical, identified card item to equip whenever you want).

### Shrines
- Shrines are single trigger blocks placed by an admin: stand next to/look at the block and run
  `/gambit shrine create <glory|abysmal|opportunity>`. Right-clicking that block opens the
  shrine's menu. (This plugin only tracks the *interaction block* — building an actual temple
  structure around it is on you / worldedit / a schematic.)
  - **Shrine of Glory** — pick one of your **equipped** cards to sacrifice permanently for
    `+1 max heart` (configurable). That card can never be equipped by you again.
  - **Shrine of Abysmal** — pay current health (`shrines.abysmal-heart-cost`, default 1 heart)
    for a random new card item.
  - **Shrine of Opportunity** — pick one of your equipped cards and reroll it into a new random
    equipped card (never the same one, never one you already have, never a banned one).

### Hardcore hearts / elimination
- If you die **without any cards equipped**, you permanently lose 2 max hearts
  (`hearts-lost-on-death-without-cards`).
- Losing hearts is permanent (it edits your max-health attribute, which persists in your
  player data). If your max hearts ever reach **0**, you are banned from the server
  (`HealthUtil.loseHearts`).
- `/gambit reset <player>` is an admin escape hatch that restores someone to full hearts and
  clears their banned-card list — useful for a fresh season/testing.

## Design decisions worth knowing about

Your spec left some mechanical details open to interpretation — here's what I chose, all easy
to tweak in `CardType.java` / the listener files / `config.yml`:

- **Activation model**: cards with a stated cooldown (Nightbringer, Warden Beam, Gravity,
  and my additions Berserk + Negation) are "active" abilities triggered by sneak+right-click
  with an empty hand. Everything else is passive and fires automatically off combat/movement
  events. On-hit passive cards without a stated number (Vampiric, Frozen Anchor, Grounding
  Bolt, Metal Thief, Heavy Swing, Greed's Touch) got sensible chances/internal cooldowns in
  `config.yml` under `chances:` so they aren't spammy — tune freely.
- **Piggy's Weight** triggers on ≥10-block-equivalent fall damage (5.0 raw fall damage ≈ 10 blocks).
- **Combo** tracks hits per-victim, resets never (persists for the session) — extend with a decay
  timer if you want it to reset after a gap.
- **Blood Trail** shows particles to the *card owner* only, on any player below 50% health.
- Card pool for Mystery Card drops and Shrine of Abysmal/Opportunity is a flat random pick
  across all 27 cards (no rarity weighting yet) — easy to weight by `CardRarity` if you want
  legendaries/rares to be scarcer.

### Custom textures
- Every card, legendary, and the Mystery Card drop carries a `CustomModelData`
  value (see `CardType.customModelData()` / `LegendaryType.customModelData()`),
  so a resource pack can give each one a distinct icon without any NBT hacks.
- A ready-to-use resource pack skeleton with placeholder (solid color,
  rarity-tinted) textures already wired up ships alongside this project - see
  `GambitSMP-resourcepack/README.md`. Load it, and everything already looks
  different from vanilla items; swap the placeholder PNGs for real art whenever
  you're ready, no plugin changes required.

### Feedback (sounds & particles)
- Every card, legendary, and the Mystery Card has its own tailored sound/particle
  combo when it actually does something - not one generic "ding" reused everywhere.
  A few examples of the reasoning: Berserk gets a roar + rising flames (aggressive),
  Negation gets a totem shimmer (defensive/protective), Phantom's vanish gets an
  Enderman-teleport warp + smoke poof, Frozen Anchor's freeze gets a glass-forming
  crystallize sound. Full breakdown is in the inline comments in `ActiveCardService`,
  `CombatListener`, `MovementListener`, and `PeriodicTaskManager`.
- Active-card feedback plays at the **world** level (audible/visible to nearby
  players), not just to the caster - on a PvP SMP, "did they just pop an ability"
  matters to everyone around, not only the person who pressed the button.
- High-frequency passives (Bloodthirsty's per-hit stacking, Vampiric Fang's
  per-hit lifesteal, Reinforced Armor's durability save) intentionally get quiet
  particle-only feedback with no sound, since anything louder would get grating
  within seconds of real combat. Rarer procs (Vampiric's chance, Greed's Touch,
  Metal Thief) get a full sound+particle combo since they don't repeat as often.
- Continuously-active passives that ramp up over time (Speed Demon, Lightning
  Speed) only play a sound/particle at the moment they cross into a faster tier,
  not on every tick - the tier-up is the actual notable event.
- While going through this pass, I found Rosen Hellfire's "fire lasts longer"
  effect had never actually been wired to anything - it's now hooked up via
  `EntityCombustEvent` (extends burn duration by 75%) with its own feedback.

### CardDimension
- Brand new players spawn in a custom dimension called **CardDimension** on their
  very first join (returning players who die/respawn use normal vanilla bed/anchor
  rules, unaffected). Anyone can get there any time with **`/gambit spawn`**, which
  teleports you to right in front of the portal.
- At CardDimension's spawn sits a **large obsidian nether portal** (5 wide x 4 tall
  interior by default, configurable) that starts **unlit** - it's just an obsidian
  frame until activated. A **lodestone** sits a few blocks in front of it. Right-click
  the lodestone while holding a **Special Matter** item (`/gambit give specialmatter
  <player>` to obtain one, admin-only) to consume it and light the portal - after
  that, walking through sends you to the main overworld's spawn point. There's no
  return portal on the overworld side by design; `/gambit spawn` is the way back.
- The portal's obsidian frame and interior portal blocks are **unbreakable** (can't
  be broken by hand or by explosions), but nothing stops you building on top of or
  around the frame - only the tracked frame/interior blocks themselves are protected.
- **Terrain** uses vanilla's **Amplified** world type - tall, exaggerated, dramatic
  terrain shape is generated entirely by the game itself (not hand-rolled), which is
  both simpler and far more reliable than a fully custom generator. The plugin only
  adds two things on top of that: ores (coal/iron/gold/redstone/lapis/copper, no
  diamond, plus scattered **Ancient Debris** as the netherite source, since vanilla
  doesn't have a "netherite ore" block) and sparse spruce trees, both placed by a
  small custom populator. Vanilla's own ore/tree generation doesn't run here at all,
  since the biome CardDimension uses (see below) is defined with no vegetation/ore
  features of its own - so there's no double-generation to worry about.
- **Purple grass, leaves, and water** all use *real* vanilla blocks, tinted purple
  through two different mechanisms (details + the exact tradeoffs in
  `GambitSMP-resourcepack/README.md`):
  - Grass and water color come from a **datapack** (bundled in this plugin,
    auto-installed into the default world's `datapacks/` folder on first run) that
    overrides the unused `the_void` biome's colors. **This requires one full server
    restart** after first install before the purple color shows up - biome/registry
    data only loads at boot, not on `/reload`. Everything else about CardDimension
    (world, portal, ores, trees, teleporting) works immediately regardless.
  - Spruce leaves specifically **ignore biome color entirely** in vanilla (a
    hardcoded client quirk shared with birch leaves), so that trick doesn't work for
    trees - the companion resource pack instead ships a pre-math'd texture override
    for `spruce_leaves.png` that lands on purple *after* accounting for that fixed
    tint. This one's a global override (affects spruce leaves everywhere on the
    server, not just CardDimension) - there's no per-dimension texture mechanism to
    scope it further within the public API.
- **Known quirk**: because Amplified terrain can be extremely tall/jagged, the
  portal's exact spawn spot is wherever the highest block happens to be at (0, 0) -
  occasionally that could be a cliff edge or an odd perch. If it looks bad, delete
  `carddimension.yml` from the plugin's data folder *and* delete the CardDimension
  world folder, then restart to regenerate both from scratch at a fresh spot.
- **Honesty note on risk**: this is the single most version-sensitive part of the
  whole plugin - custom `ChunkGenerator`/`BiomeProvider` APIs and biome datapack
  schemas are exactly the kind of thing that shifts between Minecraft versions, and
  I wasn't able to compile-test any of it here. If world creation throws errors on
  startup, the biome JSON schema and the `ChunkGenerator` method signatures in
  `CardDimensionGenerator`/`CardDimensionManager` are the first places to check.

## File map

```
com.gambitsmp
├── GambitSMP.java              main plugin class / wiring
├── cards/                      CardType enum, CardRarity, CardManager (item build/read)
├── legendary/                  LegendaryType enum, LegendaryManager
├── player/                     PlayerCardData (per-player state), PlayerDataManager
├── shrine/                     ShrineType, ShrineManager (persistence), ShrineListener (GUIs)
├── gui/                        CardGuiListener (admin card browser + player equip GUI)
├── dimension/
│   ├── CardDimensionGenerator  custom terrain (rolling hills, purple-tinted biome)
│   ├── CardDimensionOrePopulator ore veins minus diamond, plus Ancient Debris
│   ├── CardDimensionTreePopulator sparse spruce trees
│   ├── CardDimensionManager    world/portal creation, datapack extraction, persistence
│   ├── SpecialMatterManager    builds/identifies the portal-activation item
│   ├── PortalProtectionListener unbreakable frame/interior blocks
│   ├── SpecialMatterListener   lodestone + Special Matter activation ritual
│   ├── PortalTravelListener    redirects the activated portal to overworld spawn
│   └── FirstJoinListener       sends brand-new players to CardDimension
├── listeners/
│   ├── CardEquipListener       right-click to equip cards/legendaries
│   ├── ActiveCardListener      sneak+right-click ability triggers
│   ├── CombatListener          all on-hit passive card + legendary effects
│   ├── MovementListener        fall damage, armor durability, sneak, webs, XP cards
│   ├── DeathListener           heart loss on death w/o cards, mystery card drop
│   ├── MysteryDropListener     use-now/keep GUI for mystery card drops
│   ├── FusionListener          anvil combine Fang + Vampiric Claws → Vampiric Fang
│   └── GambitCommand           /gambit admin & utility command
└── util/
    ├── HealthUtil              heart/health math, permanent loss, ban-at-zero
    └── PeriodicTaskManager     all "every second/30s/60s" passive card effects
```
