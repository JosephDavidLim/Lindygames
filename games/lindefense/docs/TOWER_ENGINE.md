# Tower engine guide (for the per-tower mechanic agents)

The shared engine implements the global tower rules (spec 3.0), the data table for all 20 towers, the
behaviour framework and the reusable simulation primitives. The special per-tower mechanics are the
`// TODO(B1|B2|B3)` stubs in `tower/behavior/*Behavior.java`. **Only edit your own group's files**:

| Agent | Towers | Data (numbers) | Behaviours | Tests |
|---|---|---|---|---|
| B1 | Student, Student+AI, Brad, Gemini, Claude, GPT, DeepSeek, Copilot | `tower/data/HeroTowers`, `PrimaryTowers`, `MilitaryTowers` | `tower/behavior/<Name>Behavior` | `test/HeroPrimaryMilitaryTests` |
| B2 | DALL-E, Sora, Suno, Grok, Perplexity | `tower/data/MagicTowers`, `SupportTowers` | same | `test/MagicSupportTests` |
| B3 | Qwen, Llama, Mistral, Cohere, GLM, Kimi, MiniMax | `tower/data/TitanTowers` | same | `test/TitanTests` |

Shared code (`Combat`, `Tower`, `Enemy`, `effects/*`, ...) should not need edits. If you find a bug in it, report it.
Build with `./build.sh` (must stay warning-free) and run `./test.sh`.

## 1. Rules implemented once, in shared code

* **Units**: spec pixels = world units 1:1 (1200x900 world). Enemy 1.0x speed = `Enemy.BASE_SPEED` = 60 px/s. DeepSeek's "entire map"
  range is `TowerStats.UNLIMITED_RANGE` (2000 px, larger than the map diagonal).
* **Damage types** (`DamageType`, applied in `Combat.applyHit(HitContext)`): SHARP cannot hurt Lead-type (unless the enemy is ARMOR
  CRACKED or `HitSpec.ignoresResistance`); BLUNT and KINETIC are identical (x2 vs Ceramic-type); SONIC is x2 vs Ceramic-type and knocks
  Lead-type back 20 px (non-boss, through the knockback budget); EXPLOSIVE splashes and cannot damage Camo unless the source has detection
  (`HitContext.detection`, default `source.canSeeCamo()`; a revealed enemy can be hit); MAGIC/ENERGY have no rules; GRAVITY slows via the
  hit's `slowFraction`. `DamageType.TRUE` is internal (map hazards, tests): ignores everything.
* **Lead-type / Ceramic-type** (`Enemy.isLeadType()`, `isCeramicType()`): *Lead* = Heap Sort and Lockheed Martin (whole body) and Radix Sort in
  its **armoured phases**, i.e. `radixPhase() >= 1` (HP at or below 6 of its 8 layers). *Ceramic* = the **outer layer** of Merge Sort, Quick
  Sort and TimSort, i.e. the body that releases children when destroyed (Merge Run / Quick Partition / Insertion Sort are not Ceramic).
  *MOAB-class* = any of the 5 bosses (`Enemy.isMoabClass()`). Boss armour fractions, BFB weak-hit halving and Palantir phases are unchanged.
* **Statuses** (`Enemy.applyStatus`, kinds in `StatusEffect.Kind`): SLOW strongest wins and never stacks, bosses get 1/3 of any slow (so a
  spec line "bosses SLOWED 50%" delivers 16.7%); STUN/FREEZE ignore bosses unless the caller passes `bossOverride = true`
  (`HitSpec.bossStunDuration`); Palantir phase 1 ignores slow/stun/knockback (`isControlImmune()`); MARKED (percent) and MARKED_FLAT (+N
  damage) keep the strongest of each kind; DoT burn is one instance that refreshes (`Enemy.applyBurn(dps, duration, tower)` credits kills);
  ARMOR_CRACK removes Lead immunity and makes Fortified enemies take x2 damage (the "HP bonus ignored" rule); FREEZE additionally makes the
  enemy take x2 damage. Flat marks add after armour and percent marks, and never apply to a blocked hit.
* **Knockback** moves the enemy back along its own route only (never off the path; clamps at distance 0). Per-hit knockback goes through
  `Enemy.applyKnockback`, which keeps the anti-soft-lock **budget**: at most 40% (bosses 25%) of the enemy's unslowed movement per second can
  be undone, so stacked pushers cannot hold an enemy in place. Scheduled events and abilities (Sora Scene Rewind 120 px, Stampede, Tempest)
  should use `HitSpec.uncappedKnockback` / `Enemy.applyKnockbackUncapped`. Bosses are immune unless `HitSpec.bossKnockback` is set.
* **Buff stacking** (`Tower` buff book, `BuffStat`): `addAuraBuff(stat, sourceType, value)` (recomputed ~10x/s, never call it for timed
  effects) and `addTimedBuff(stat, sourceType, value, seconds)`. A tower's total per stat = sum over source tower TYPES of the highest value
  that type contributes; caps: RATE +100%, DAMAGE_FLAT +5, PIERCE (flat + percent) +10. `Tower.effectiveRate/Range/Pierce()` and
  `flatDamageBuff()` apply them. Own-ability multipliers that are not "buffs from another tower" (Caffeine Rush x2, Overclock x3) use
  `Tower.addSelfBuff(key, rateMult, projectileSpeedMult, seconds)` and are not capped. `Tower.addBuff` is reserved for map events.
* **Targeting**: First, Last, Strongest, Weakest, Closest and `DENSEST_CLUSTER` (only towers whose behaviour returns true from
  `allowsDensestCluster`, i.e. Grok U2+; `Tower.setTargeting` ignores it otherwise, `Tower.cycleTargeting` skips it). The single definition
  of "densest" is `combat.Clusters`: an enemy's density = number of OTHER alive on-track enemies within 60 px; densest = highest density,
  ties to First. Use `Clusters.densest(...)`/`densestPoint(...)` for any "aim at the densest cluster" effect (Copilot drone, Brad barrage).
* **Camo detection**: `Tower.canSeeCamo()` = `stats.seesCamo` (built-in or gained by upgrade in the data table) OR an aura grant (Perplexity's
  `auraDetection`) OR a map-wide grant while a Perplexity with `revealAllCamo` exists (`TowerManager.recomputeAuras`). Companions/drones
  have their own `detection` flag.
* **Heroes**: one of each; `TowerManager.placementProblem` returns "Only one X allowed", `Shop.unavailableReason(game, type)` gives the
  position-independent reason for greying out shop cards.
* **Abilities** (`AbilityDef`, `AbilityManager`): cooldowns tick in game time, start at 50%, Surveillance locks them. Abilities that need a
  location have `needsTarget`; the sim API is `Game.activateAbility(Tower, Vec point)` (the no-point overload refuses them). Your behaviour's
  `activateAbility(game, tower, target)` returns false to refuse (no cooldown spent). Snap points with `Effects.nearestTrackPoint` /
  `bendsInRange`.
* **Round income**: `TowerBehavior.onRoundEnd` (default pays `stats.incomePerWave`). `Game.getRoundPopValue()` is the cash from pops this wave.

## 2. The data table (`tower/data/*.java`)

Each tower is one `TowerBuilder` chain: `.attack(kind, damageType, range, interval, damage, pierce)`, `.base(s -> ...)`, exactly three
`.upgrade(cost, specMechanicName, description, s -> ...)` and `.ability(AbilityBuilder.of(name, cooldown, desc).p("duration", 10)...)`
(`.target()` for player-chosen locations). Tier labels (`.tiers(base, u1, u2, u3)`) double as upgrade names; displayed name is the family name
and the nickname is `TowerDef.subtitle`. Upgrade lambdas are cumulative edits of `TowerStats`.

* Typed stats: `kind, hit (HitSpec), range, rate/interval(), pierce, projectileSpeed, splashRadius, chainHops/chainRange, bounces, clones,
  projectilesPerShot, targets, seesCamo, ignoresLos, aura* (auraRange/auraRate/auraDamage/auraPierce/auraRangeBonus/auraDetection/
  auraSurveillanceImmunity), field* (enemy slow/mark field), incomePerWave`.
* Mechanic parameters are named: `s.set("burnDps", 1)` in the data, `t.getStats().p("burnDps")` in behaviour code (`p(key)` throws on a
  missing key, `p(key, default)` is for "not unlocked yet", `has(key)`, `pi(key)`). Ability parameters: `t.getAbility().p("duration")`.
* Situational damage lives in the `HitSpec`: `multVsBoss, flatVsBoss, multVsFortified, flatVsFortified, flatVsLead, multVsLead, flatVsCeramic,
  multVsCeramic, multVsSortingAndLists, multVsHighHp (base HP >= 5), knockbackVsFast, knockbackVsLead, armorBreak(Duration), markBonus,
  markFlat, markDuration, dotDps/dotDuration, slowFraction/Duration, stripsFortified, stripsCamoRegrow`.
* Unspecified numbers in the spec (kept in the data so they are easy to retune): default projectile speed 450 px/s, GLM slow duration 1.5 s,
  Llama range 170 and charge speed 260 px/s, MiniMax bounce range 100 px, Gemini ricochet range for the chain upgrade 80 px.
* Add a new tunable by adding a `set(...)` in the right upgrade row and reading it in the behaviour; add a check to your test class.

## 3. Behaviour hooks (`tower/behavior/TowerBehavior`)

One stateless singleton per tower (registered in `Behaviors`). Hooks (all optional): `onPlaced, onUpgraded, onRemoved, update(dt)` (every sim
step), `attack` (default = data-driven attack; override for `AttackKind.CUSTOM`), `modifyAttack(AttackContext)` (edit damage/pierce/shots/hit
spec before firing, or `cancel`), `onFire`, `beforeHit(HitContext)` / `onHit(HitContext)` (edit `ctx.extra`, `ctx.editHit()`, `ctx.finalMult`;
`ctx.dealt`, `ctx.immune`, `ctx.projectile` afterwards), `onKill(enemy)`, `onRoundEnd`, `activateAbility(game, tower, target)`,
`auraContribution(game, src, dst)` (default = data-driven aura; call `super` and add faction-wide effects, see `CopilotBehavior`),
`allowsDensestCluster`, `priorityComparator`. `stubAbility(game, tower)` is the placeholder to replace.

Per-tower memory (`Tower`): `counter/addCounter/setCounter` (pop counts, cycle indices), `periodic(key, interval, dt)` (true once per
interval), `registerStreak(enemyId)` (returns consecutive hits already landed on that target; resets on a new target; used by DeepSeek and
Kimi in `modifyAttack`), `state()` free-form map (drone handles, ...), `getKills()` (pops credited to the tower).

Generic attacks: PROJECTILE (homing, pierce, `splashRadius` -> blast hits up to `pierce` enemies if pierce > 1 else all, `bounces` with
`bounceSpeedGain`, `clones`), INSTANT (beam through up to `pierce` enemies on the line, then `chainHops` ricochets; `projectilesPerShot` beams),
CHAIN, PULSE, CUSTOM (behaviour does everything; examples: `LlamaBehavior`, `PerplexityBehavior`). Fire your own projectiles with
`Combat.fire(Projectile.Shot.of(game, tower, start, target))` and edit the shot first; `Combat.executeAttack(AttackContext, origin, detection,
buffed)` fires a tower-style attack from any origin.

## 4. Primitives (`effects/*`, owned by `game.getEffects()`; all removed automatically when their owner is sold)

* **`TrackZone(center, radius, owner)`** - area on the track: `duration` or `permanent`, `tickInterval` + `tickHit` (HitSpec), `slow`, `speedCap`
  (0.25 = enemies move at 25% speed), `armorCrack` (+`crackBosses`), `affectsBosses`, `bossesOnly`, `detection`, `kind` label.
  `game.getEffects().addZone(z)`. Used by paint zones, Sora loops, tornado, vortex, Outpainting, Tempest.
* **`TrackWalker(route, startDist, endDist, speed, owner, hit)`** - summon walking a route forward (`end > start`) or backward; `radius`,
  `pierce` (distinct enemies), `perEnemyCooldown` (0 = hit each enemy once), `bossHit` (alternative spec vs bosses), `lifetime`, `detection`.
  Used by DALL-E creatures, Llama charges, the Stampede herd.
* **`Drone`** - `Drone.orbit(owner, stats, radius, angle)`, `Drone.patrol(owner, stats, route, from, to, speed)`, `Drone.fixed(...)`; stats from
  `Drone.statsFor(kind, range, interval, damage, type, pierce, speed)` (edit `.hit` for marks), or `mirrorOwner = true` (MiniMax clones),
  `lifetime`, `detection`, `targeting`, `kind` label. Fires through the normal attack code, credited to the owner, no owner flat-damage buff.
  Look drones up with `Effects.dronesOf(owner, kind)`.
* **`Barrage`** - `new Barrage(source, hitSpec, count, splash)` with `aim` (POINT / DENSEST / BOSS_ELSE_DENSEST), `scatter`, `stagger`;
  `game.getEffects().fire(b)` returns the aim point (null if nothing to aim at). Shells are area hits (so `multVsBoss`, EXPLOSIVE-vs-camo apply).
* **`NodeHazard`** - `NodeHazard.line(owner, a, b, hit, cooldown)` / `NodeHazard.network(owner, nodes, hit, cooldown, slow)`: links damage
  crossing enemies at most once per `crossCooldown` per enemy; `region` polygon slow. `addHazard`.
* **`Effects.schedule(delay, runnable)`**, `nearestTrackPoint(Vec)`, `bendsInRange(Vec, range)`.
* **Enemy-level operations**: `Enemy.startReverse(duration, contactDps, tower)` (EnemyManager applies contact damage, credited to the tower);
  `Enemy.rewind(seconds)`; `Enemy.hold(duration, atDistance, pullSpeed)` (vortex); `EnemyManager.destroyWithoutChildren(enemy, tower)` (reward
  paid once, kill credited, stored bucket contents released); `Enemy.stripCamoRegrow()`; `Enemy.stripFortified()`;
  `Boss.disablePhaseShield(seconds)` (Palantir: armour 0 and no control immunity; no effect on other bosses).

## 5. Visuals

Emit `VisualEvent`s through `game.getCombat().emit(...)` (throttled): `beam, chain, hit, explosion, text, death, split, teleport, ability,
banner, phase` already render. Zones, walkers, drones and hazards are readable from `game.getEffects().getZones()/getWalkers()/getDrones()/
getHazards()` for a future renderer (UI integration is a later phase); give them a `kind` label and `rgb`.

## 6. Tests

Each group class has `runAll()` calling `run("name", Class::method)` and `check(cond, message)` from `TestKit` (shared counters, builders:
`newGame`, `towerNearTrack`, `forcePlace`, `spawnAt`, `killOutright`, `runSeconds`). Conventions: freeze enemies
(`g.setGlobalEnemySpeedMultiplier(0)`) so positions are exact; keep the owner of an effect away from the scene (a tower near the enemies
attacks them too); use `spawnScaled(...)` with a big HP scale when you need damage numbers not capped by remaining HP; call
`g.getCombat().attack(tower)` plus `updateProjectiles` to test a single shot without advancing game time. Every `TODO` you implement needs a
test next to the existing `// TODO(Bx) tests:` markers. `stressMaxedRoster` in `HeadlessTests` runs all 20 maxed towers against late waves
and must stay exception-free.
