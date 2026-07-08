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

## File map

```
com.gambitsmp
├── GambitSMP.java              main plugin class / wiring
├── cards/                      CardType enum, CardRarity, CardManager (item build/read)
├── legendary/                  LegendaryType enum, LegendaryManager
├── player/                     PlayerCardData (per-player state), PlayerDataManager
├── shrine/                     ShrineType, ShrineManager (persistence), ShrineListener (GUIs)
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
