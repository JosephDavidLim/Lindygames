# 11/1 (formerly Squad vs. Semester)

Working design document. Numbers are starting values to tune while playtesting.

## 1. Core loop
1. Focus Points (FP) are earned from generators and from falling coffees.
2. Spend FP to place a defender in an empty grid cell. Each defender has a cost and a recharge.
3. School enemies walk right to left along 5 lanes and stop to bite the first defender in their lane.
4. Defenders attack down their own lane. Clear the wave before anything gets past column 0.
5. Win a level by clearing every scheduled wave. Lose if an enemy breaks through the last defense.

## 2. Board and units
- Grid: 5 rows x 9 columns, cell 80x80 px, `Defender[][] board = new Defender[5][9]`.
- Window: 720 wide, 490 tall (90 px shop bar on top, 400 px lawn).
- One defender per cell. Enemies are not in the grid: they have a pixel x and a row.
- An enemy's column = `(int) (x / CELL)`. This is how it finds the defender it is eating.
- Time: a fixed tick at about 30 per second (`pause(33)`). Stats below use seconds; code converts to ticks.

## 3. Economy
| Item | Value |
|---|---|
| Starting FP | 100 |
| Falling coffee | +25 FP, appears every 10 s at a random spot, click to collect, vanishes after 8 s |
| Sage Aarav / Tanimbus | generators (see roster) |
| Refund | none (keeps decisions meaningful) |

Each shop card has a cost and a recharge timer. The card is grayed out until you can afford it and it has recharged.

## 4. Defender roster (17)
The original stat tables below are kept for history. Section 25 has the current roster and enemy behaviors.

Core set (levels 1-3):

| Codename | Role | Cost | HP | Recharge | Behavior |
|---|---|---|---|---|---|
| Sage Aarav | Generator | 50 | 60 | 5 s | +25 FP every 8 s |
| Lobber Luis | Shooter | 100 | 60 | 5 s | 20 dmg shot every 1.5 s down the lane |
| Iron Imraan | Tank | 75 | 400 | 12 s | Blocks, no attack |
| Boom Sam | Area blast | 150 | 1 | 25 s | After a 1.5 s fuse, 150 dmg to a 3x3 area, then disappears |
| Chillstin | Slower | 125 | 60 | 6 s | 15 dmg shot that slows the target by half for 3 s |
| Nik-Scope | Sniper | 175 | 50 | 8 s | 80 dmg shot every 3 s |

Unlocked later:

| Codename | Role | Cost | HP | Behavior |
|---|---|---|---|---|
| Kaiju Kai | Cheap wall | 25 | 150 | Blocks |
| Abhi-Ace | Basic shooter | 90 | 60 | 15 dmg every 1.2 s |
| Zippy Matt | Fast shooter | 125 | 50 | 10 dmg every 0.7 s |
| Justin Time | Double shooter | 175 | 60 | Two 20 dmg shots every 1.5 s |
| Spear Hanbo | Piercing shooter | 175 | 60 | 25 dmg to every enemy in the lane every 2 s |
| Tanimbus | Generator | 125 | 80 | +50 FP every 15 s |
| Sunny Joe | Support | 100 | 70 | Heals all 8 neighbors 10 HP every 3 s |
| Thorn Tuhin | Thorn tank | 100 | 600 | Blocks, deals 10 dmg back to each enemy that bites it |
| Cami-Mine | Mine | 25 | 1 | Arms after 8 s, then explodes for 300 dmg on contact |
| Trapper Che | Trap | 75 | 1 | Hidden, stuns the first enemy to step on it for 3 s, then disappears |
| Blitz Arnav | Lane clear | 200 | 1 | Instant: 1000 dmg to every enemy in this row |

## 5. Enemy roster (grades and requirements)
See section 25 for current enemy behaviors.

The enemies are everything a student fights through a school year. Each type is data in an `EnemyType` enum, built on a few behavior classes (`Walker`, `ReportCard`, `Flyer`, `Drainer`, `Boss`).

**World 1: The Semester**

| Enemy | HP | Speed (cells/s) | Bite dmg/s | Behavior |
|---|---|---|---|---|
| Homework Sheet | 100 | 0.12 | 10 | Basic walker |
| Sharp Pencil | 60 | 0.25 | 8 | Fast and fragile |
| Report Card | 250 | 0.12 | 10 | Shows a letter grade that drops A to F as it takes damage. At F it panics and moves 1.5x faster |
| Paper Airplane | 120 | 0.2 | 10 | Flies over the first defender it meets (once), then walks |
| Eraser | 150 | 0.10 | 30 | Heavy biter, wipes out defenders fast |
| **Midterm Exam** (mini-boss) | 600 | 0.08 | 25 | Appears on level 5. Calls in 2 Sharp Pencils every 15 s |

**World 2: College Apps**

| Enemy | HP | Speed (cells/s) | Bite dmg/s | Behavior |
|---|---|---|---|---|
| Application Fee | 120 | 0.12 | 10 | Steals 25 FP every 10 s while alive. Kill it fast |
| Personal Essay | 400 | 0.10 | 15 | Long word count, soaks up damage |
| Rejection Letter | 250 | 0.18 | 60 | Fast and brutal, crushes whatever it reaches |
| Scantron Swarm | 60 | 0.20 | 8 | Always spawns in groups of 3 |
| **The Deadline** (final boss) | 3000 | 0.06 | 40 | Spawns 2 Sharp Pencils every 10 s, crushes what it walks into |

Counters to teach: Paper Airplane vs Iron Imraan (the jump skips it), Report Card vs Nik-Scope (big hits keep it from panicking), swarms vs Boom Sam and Spear Hanbo, Eraser and Rejection Letter vs Chillstin plus Thorn Tuhin, Application Fee vs a quick shooter on its lane.

## 6. How levels work (modeled on Plants vs. Zombies)
- **Structure:** 2 worlds of 5 levels. The first levels use 1 lane, then 3, then all 5. Each win unlocks 1-3 new defenders.
- **Loadout:** Before each level you pick up to 6 defenders from the unlocked ones.
- **Waves:** Each level has several waves. The final wave of each level is a "huge wave" announced with a banner.
- **Point-budget spawning:** each enemy type has a point value (Homework Sheet 1, Sharp Pencil 1, Report Card 3, Paper Airplane 2, Eraser 3, Application Fee 3, Rejection Letter 4, Essay 4, Scantron Swarm 3, bosses fixed). `WaveManager` gives every wave a budget, spends it on random enemies from the level's allowed list using `Random`, and spreads them over the lanes. The budget grows each wave, and the huge wave gets 2-3 times the normal budget.
- **Last-line defense:** each lane has a one-use Highlighter that sweeps the lane when an enemy reaches column 0. The level is lost only when a second enemy gets through that same lane.
- **Win:** every wave cleared and no enemies left. **Lose:** an enemy crosses the left edge after the Highlighter is used.
- **Variety:** every 5th level is a boss level.

| Lvl | Setting | Lanes | Waves | Unlocked by clearing it | Enemies added |
|---|---|---|---|---|---|
| 1 | First day, syllabus week | 1 | 3 | Sage Aarav | Homework Sheet |
| 2 | Homework pile | 3 | 4 | Iron Imraan | |
| 3 | Pop quiz | 5 | 5 | Boom Sam | Sharp Pencil |
| 4 | Report card day | 5 | 5 | Chillstin | Report Card |
| 5 | **Midterm (boss)** | 5 | 6 | Kaiju Kai, Abhi-Ace | Midterm Exam |
| 6 | SAT season | 5 | 6 | Zippy Matt, Sunny Joe | Paper Airplane |
| 7 | Common App | 5 | 7 | Nik-Scope, Cami-Mine | Eraser, Scantron Swarm |
| 8 | Application fees | 5 | 7 | Justin Time, Spear Hanbo, Thorn Tuhin | Application Fee |
| 9 | Essay season | 5 | 8 | Trapper Che, Blitz Arnav, Tanimbus | Personal Essay, Rejection Letter |
| 10 | **The Deadline (final boss)** | 5 | 9 | (the finale) | The Deadline |

Lobber Luis is the starter, available from level 1. Every defender is obtainable.

## 7. Screens and UI
- **Menu:** title, Play, Level Select, How to Play.
- **Level select:** 10 buttons, locked ones grayed out.
- **Loadout:** pick up to 6 unlocked defenders.
- **Playing:** shop bar (cards with cost and recharge shading), FP counter, wave progress bar, pause button.
- **Win / Lose:** summary, next level, retry.
- **Controls:** click a card, then click a cell. Click falling coffees. Press P to pause, D for debug overlay (grid coords, hitboxes).

## 8. Architecture
Packages: `game` (flow and board), `entities` (things on the board), `util` (helpers).

Key classes:
- `GameApp extends GraphicsProgram`: window, main loop, mouse events, screen switching.
- `GameState` (enum): MENU, SELECT, LOADOUT, PLAYING, PAUSED, WON, LOST.
- `Board`: owns `Defender[][] grid`, `ArrayList<Enemy> enemies`, `ArrayList<Projectile> projectiles`, `ArrayList<Coffee> coffees`. Methods: `place(row, col, type)`, `update()`, `enemyInLane(row, fromX)`.
- `Entity` (abstract): `x` (in cell units), `health`, `abstract void update(Board b)`, `takeDamage(int)`, `isDead()`.
- `Defender` (abstract) extends `Entity`: `cost`, `row`, `col`, `cooldownTicks`. Subclasses: `Generator`, `Shooter`, `Wall`, `Bomb`, `Support`, `Trap`. Each codename is data (a `DefenderType` enum holding name, cost, hp, sprite file, factory) on top of a few behavior classes. This keeps 17 defenders from becoming 17 hand-written classes.
- `Enemy` (abstract) extends `Entity`: `speed`, `biteDamage`, `slowTicks`. Behavior subclasses: `Walker` (plain stats), `ReportCard`, `Flyer` (Paper Airplane), `Drainer` (Application Fee), `Boss`. Each enemy is data in an `EnemyType` enum (name, HP, speed, bite, points, sprite).
- `Projectile`: row, x, damage, effect (slow, pierce).
- `Coffee`: falling collectible.
- `Shop` / `Card`: cost, recharge, selected state, `ArrayList<Card>`.
- `Level` / `WaveManager`: builds each wave from a point budget (see section 6) into an `ArrayList<SpawnEvent>`; `Random` for the point-budget enemy picks and coffee positions.
- `SpriteFactory`: loads a PNG as a `GImage` scaled to a cell, or builds a fallback `GCompound`.

Mermaid UML: `docs/uml/class-diagram.md`.

## 9. Game loop (in `run()`)
```
while (running) {
    long start = now();
    handleQueuedClicks();      // mouse thread only enqueues
    if (state == PLAYING) board.update();   // one tick
    redrawChangedSprites();
    pause(max(0, 33 - (now() - start)));
}
```
`Board.update()` order: spawn from WaveManager, update defenders (generate, fire), update projectiles (move, hit-test), update enemies (move or bite), remove dead things, check win/lose.

- **Hit test:** projectile and enemy are in the same row and `projectile.x >= enemy.x`. No general 2-D collision needed.
- **Bite:** enemy looks up `grid[row][colOf(x)]`; if a defender is there, stop and damage it each tick.
- **Removal:** iterate with an `Iterator` (or backwards by index) and remove dead entities, never inside a for-each.
- **Threading:** ACM mouse handlers run on the AWT thread while `run()` loops on another. Handlers only push click events into a list, and the loop reads them. This avoids ConcurrentModificationException.

## 10. ACM notes
- `GraphicsProgram`, `addMouseListeners()`, `mouseClicked(MouseEvent)`.
- Sprites: `GImage`, `setSize`; health bars and cards: `GRect`; labels: `GLabel`.
- Move sprites with `setLocation`, not by recreating them each tick.
- Group a unit's parts with `GCompound`. Use `remove(obj)` when an entity dies.
- Optional: `acm.util.SoundClip` for sound effects.
- Photos and PNG paths are relative to the working directory, so run from the project root.

## 11. Rubric mapping
| Requirement | Where it's met |
|---|---|
| ACM graphics | Everything on screen |
| Multi-d array | `Defender[][] grid` |
| ArrayList / `java.util` | enemies, projectiles, coffees, cards, spawn events; `Random`, `Iterator`, `Collections.sort` |
| Javadoc, no extra `*`s | every public class and method uses a plain `/** ... */` |
| UML | `docs/uml/class-diagram.md` (export to PNG for submission) |
| README with instructions | `README.md` |
| Code-review presentation | show the inheritance tree, `Board.update()`, and the grid |
| Demo video (2-4 min) | script in `docs/videos/` |
| Music video | each of us appears; parody song |
| Class time | log every session in `docs/progress-log.md` |

## 12. Milestones (each ends with something runnable)
1. **Grid and click:** window, grid, clicking a cell highlights it. (Started: `GameApp`.)
2. **One defender and one enemy:** place Lobber Luis, a Homework Sheet walks, shots hit, enemy dies.
3. **Eating and dying:** enemies bite defenders, defenders die, loss at the left edge.
4. **Economy and shop:** FP, falling coffees, cards with recharge, Sage Aarav.
5. **Full core set:** Iron Imraan, Boom Sam, Chillstin, Nik-Scope. All 6 work.
6. **Waves and levels:** `WaveManager`, level 1-5, win screen, mini-boss.
7. **Roster and bosses:** remaining defenders, Paper Airplane, Application Fee, The Deadline, levels 6-10.
8. **Screens:** menu, level select, loadout, pause.
9. **Polish:** sound, animation (swap two frames), hit flashes, balance pass.
10. **Deliverables:** Javadoc pass, UML PNG, README check, slides, both videos.

Milestones 1-5 are the safe submission. Anything after is a bonus, so cut from the end if time runs short.

## 13. Testing and balance
- Debug key to give 9999 FP and skip waves.
- A `--level N` style flag in `main` to jump to any level.
- Playtest checklist per level: winnable with the intended defenders, not winnable by spamming one type.
- Watch for: enemies stacking at the same x, a defender dying mid-shot, clicks on locked cards.

## 14. Stretch goals
Save progress to a file (`java.io`), high-score list sorted with `Collections.sort`, endless mode, a second map, difficulty setting.

## 15. Open decisions
- Final names and roles, once classmates have seen them.
- Whether to pick 6 defenders per level or let the player use all.
- Art for the enemies (code-drawn paper shapes vs pixel art).

## 16. Implementation status (backend)
Backend and ACM view are done. Logic is tested headlessly; screens were checked by rendering them offscreen.
- **Logic and drawing are separate.** `entities` and `game.Board` never touch ACM. A separate view class will read `Board` and draw it with `GImage`/`GRect`. This is why entities no longer hold a `GObject sprite`.
- **Positions are in cell units** (x = 0.0 to 9.0). The view multiplies by `CELL` (80 px).
- **Done:** `Entity`, `Defender` (`Generator`, `Shooter`, `Wall`, `Bomb`), `DefenderType` (13 of 17), `Enemy` (`Walker`, `ReportCard`, `Flyer`, `Drainer`, `Boss`), `EnemyType` (all 11), `Projectile`, `Coffee`, `Board`, `Level`, `Levels` (all 10), `WaveManager`, `SpawnEvent`, `Shop`, `Card`, `GameState`.
- **Not yet:** Sunny Joe (heal), Cami-Mine (mine), Trapper Che (trap), Blitz Arnav (lane clear); the ACM view; menus.
- **Dev tool:** `java -cp out:lib/acm.jar game.HeadlessDemo [level]` auto-plays levels. A naive auto-player wins 8 of 10; levels 2 and 10 lose, which is the balance baseline to tune against.
- **View done** (`src/view`): `Screen` base class with `MenuScreen`, `LevelSelectScreen`, `LoadoutScreen`, `PlayScreen`, `ResultScreen`; `DefenderView` and `EnemyView` follow entities each tick; `Assets` loads and caches sprites with a placeholder fallback; `GameApp` runs the loop and saves progress to `progress.txt`.
- **Art:** chibi avatars and enemies are drawn by `tools/make_sprites.py` (no photos are used by the game).

## 17. Character names
Each constant in `DefenderType` is a classmate's first name; the in-game title is the creative name. Section 25 has the current roster.

## 18. Balance
Measured with `game.HeadlessDemo` (30 seeds per level, a human-like bot that clicks about once every 0.6 s):

| Level | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|---|---|---|---|---|---|---|---|---|---|---|
| Bot win rate | 100% | 100% | 100% | 100% | 100% | 93% | 100% | 93% | 76% | 63% |

Levels 1-7 ramp up, and 8-10 are hard. The bot times abilities perfectly, so expect real play to be harder. Wave budgets (`waves, base, growth` in `Levels.java`) are the main knobs; Rejection Letter was softened to 45 bite/s and 0.16 speed. Humans can beat the bot by using specials better, so treat these as a floor. Re-run `java -cp out:lib/acm.jar game.HeadlessDemo` after any change.

## 19. Sound and animation
Sound effects are synthesized by `tools/make_sounds.py` into `res/sfx/*.wav` and played by `view.Sfx` (silent if audio is unavailable; toggle with the Snd button). Enemies hop while walking, lunge while biting, flash when hit, wobble with a halo when stunned and puff away when defeated. Bombs, mines, heals and lane clears draw fading effects.

## 20. Music
`view.Music` loops one WAV at a time from `res/music/` and fades between tracks (700 ms). Title screens use School Bell Adventure, levels 1-5 Pencil Tap Battle, levels 6-10 Deadline Chaos, and the final wave of any level switches to Huge Wave. The music stops when a level ends so the win and lose stingers are clear. MP3 sources live in `Music/`; `tools/convert_music.sh` makes the 32 kHz mono WAVs (about 22 MB total).

## 21. Ability verification
`game` logic was checked against each defender's stats: generators pay on schedule (25 FP at 8 s, 50 FP at 15 s); every shooter's damage over 20 s matches its listed DPS within about 10%; piercing, slowing, lane-only firing and "never shoot backwards" hold; walls block and thorns kill biters; Joseph heals diagonal neighbors; mines, traps and lane clear behave as listed. The bomb deals 150 to the 3x3 around it after exactly 45 ticks (1.5 s), misses cells 2 away, does not hurt friendly defenders, and cannot be eaten while its fuse burns (enemies walk through it and are caught in the blast). That last rule was a bug found and fixed during verification: a 1 HP bomb used to be bitten to death before it could explode.

## 22. Card slots
Like the original game, the player starts with 6 card slots and earns more: a 7th after clearing level 6 (the Midterm boss) and an 8th after clearing level 12 (Finals Week). `Levels.slotsAfterClearing()` holds the rule, `Shop` takes the slot count, `PlayScreen` shrinks the cards to fit 6, 7 or 8 in the shop bar, `LoadoutScreen` limits the choice, and `ResultScreen` announces "NEW CARD SLOT!" the first time one is earned. The balance table above was measured with the bot using the slots it would have at each level; the extra slots made levels 9 and 10 much easier, so their wave budgets were raised. See section 24 for the current numbers.

## 23. Animation (code motion on the existing sprites)
No new art was drawn. `Assets` makes a set of squashed and stretched copies of each character's picture (`Pose`: NEUTRAL, TALL, WIDE, SQUASH, FLAT) on a shared canvas, and `SpriteView` swaps between them and nudges the body, so characters can lean, lunge and flatten while their health bars stay put.

- **Throwing (shooters):** in the last 5 ticks before a shot the shooter leans back and stretches tall (`Shooter.isWindingUp`), then lunges forward and squashes as the shot leaves, then recovers (`Defender.getActionAge` counts ticks since the last action).
- **Cheering (generators, Sunny Joe):** a hop up and a squash when they pay out or heal.
- **Hurt:** one short flinch away from the attacker per bite (4 ticks, at most every 18), not constant shaking.
- **Bomb:** shakes harder and alternates stretch and squash as the fuse runs out.
- **Enemies:** a squash-and-stretch waddle while walking; an 18-tick bite cycle (lean back, chomp forward, squash) while biting; knockback and a white flash when hit; a dizzy lean when stunned.
- **Defeat (enemies and defenders):** hop up, drop, flatten into a pancake, then a puff. Bombs, mines, traps and lane clears just vanish into their effect.
- **Ammo (`ProjectileView`):** a crumpled paper ball (Lobber Luis), eraser chunk (Abhi-Ace), pencil (Zippy Matt), snowball (Chillstin) and spear (Spear Hanbo). Round shots lob in a small arc; sharp ones fly flat. Each has a fading trail and makes a colored spark where it lands.

## 24. Player abilities (removed)
Red Pen, Fire Drill, Cram Session and Snow Day were removed in favor of the RPG in section 26. Older sections that mention them, or the old 10-level campaign, are history; sections 25 to 27 are current.

## 25. Current roster: every character has its own behavior
| Classmate | In-game name | Cost | HP | Behavior |
|---|---|---|---|---|
| Aarav | Sage Aarav | 50 | 60 | Generator: +25 FP every 8 s |
| Tanish | Tanimbus | 150 | 80 | **Two abilities.** Every 15 s: +50 FP, and lightning hits the 3 most advanced enemies for 40 and stuns them 1 s |
| Joseph | Sunny Joe | 100 | 70 | Support: heals the 8 neighboring cells 10 HP every 3 s |
| Luis | Lobber Luis | 100 | 60 | Basic shooter (starter): 20 damage every 1.5 s |
| Abhinav | Abhi-Ace | 125 | 60 | Ricochet: 15 damage every 1.2 s; each hit also strikes the nearest enemy within 2 cells in an adjacent lane |
| Matt | Zippy Matt | 175 | 50 | Fan: 10 damage every 1 s in his lane and both neighbors |
| Justin | Justin Time | 150 | 80 | Close blast: 60 damage to every enemy within 2 cells ahead, every 2 s |
| Hanbo | Spear Hanbo | 175 | 60 | Piercing: 25 damage to everything in the lane every 2 s |
| Austin | Chillstin | 125 | 60 | Slow: 15 damage every 1.5 s, slows for 3 s |
| Nik | Nik-Scope | 175 | 50 | Sniper: 60 damage every 3.5 s to the toughest enemy on screen, any lane (10 s card recharge) |
| Imraan | Iron Imraan | 75 | 400 | **Two abilities.** Tank, and repairs 8 HP/s after 2 s without being bitten |
| Kai | Kaiju Kai | 50 | 150 | **Two abilities.** Cheap wall, and stomps everything in his cell and the next for 10 every 1.5 s |
| Tuhin | Thorn Tuhin | 100 | 600 | Thorn tank: hurts whatever bites him |
| Sam | Boom Sam | 150 | 1 | Bomb: 150 damage in a 3x3 after 1.5 s |
| Camilo | Cami-Mine | 25 | 1 | Mine: arms in 8 s, 300 damage to its cell when stepped on |
| James | Trapper Che | 75 | 1 | Trap: arms in 2 s, stuns the first enemy to step on it for 3 s |
| Arnav | Blitz Arnav | 200 | 1 | Lane clear: 1000 damage to every enemy in the lane, once |

Enemies with their own behavior: Report Card (panics at F), Paper Airplane (jumps the first defender), Application Fee (steals FP), Eraser (heavy biter, also immune to slow), Personal Essay (damage sponge, also stops once at half health to revise and heal 150), Midterm Exam and The Deadline (bosses that call in pencils), Scantron Swarm (arrives in threes). Homework Sheet, Sharp Pencil and Rejection Letter are plain walkers that differ in stats: basic, fast, and fast with a heavy bite.

Rule for adding a second ability: it goes on top of the character's job and never replaces it. Only the shooters that were doing the same thing (Abhi-Ace, Zippy Matt, Justin Time, Nik-Scope) were given new jobs.

To keep the screen readable, the final "huge wave" now multiplies its budget by 1.6 instead of 2.5, and Homework Sheets and Sharp Pencils are no longer bought for the late levels.

## 26. RPG layer (MapleStory-inspired)
- **Avatar (`entities.Avatar`):** the player steers one classmate by keyboard. It runs, jumps (clearing enemies), climbs the ladders drawn on each map, drops through platforms, attacks up to 3 enemies in reach, and picks up coffee and drops by walking over them. Touching an enemy deals one second of its bite, then gives 1 s of safety. A knockout benches it for 5 s and never loses the level.
- **Sheets (`game.Hero`):** level, EXP, stat points and gear for each classmate. EXP to next level is `20 + 5 * level^2`, capped at Lv.30. New classmates join at the enemy level of the level that unlocks them, so late arrivals are useful.
- **Stats:** 2 points per level. STR +2% power, DEX +1.5% action speed, INT +2% card recharge, LUK +1% crit (double damage) and +1% dodge, with crit and dodge capped at 50%. Each level also gives +1% power and +3% health. FP payouts and stun length scale at half rate so the economy holds.
- **Rewards (`game.Party`):** all party members get EXP per kill, a quarter if more than 8 levels above the enemy. Credits and items land on the floor as `entities.Drop`. Coins vanish after 12 s, and whatever is left is collected at level end. The Highlighter sweep pays nothing.
- **Enemy levels:** each level sets one. Each level above 1 adds 34% health, 14% bite and 3% speed.
- **Saving (`game.Profile`):** a text file. It still reads the older one-number save, and a damaged line is skipped rather than crashing.

## 27. Maps and balance
`game.GameMap` holds six maps of three levels each. Each level's layout is a `Terrain[5][9]` built from strings in `Levels.java`.

Balance is measured by `HeadlessDemo`, which plays whole campaigns with progress carrying over, spending points and buying gear between levels, with up to 3 tries per level. First-try wins over 30 campaigns, by room: 100, 100, 100, 100, 96, 96, 96, 100, 100, 100, 100, 100, 93, 96, 100, 70, 82, 60. 28 of 30 runs finish. With `-Dnotrain=true` (never spending points or buying gear) the bot stalls around rooms 5 to 6, so training matters. The bot never tires, so expect real play to be harder. The final wave's budget multiplier is 1.35, which keeps late levels under about 50 enemies at peak.

## 28. Hero attacks
All attacks start from 18 damage every 0.5 s, then change by job (`Avatar.swing`, described by `DefenderType.heroMove`).

| Job | Classmates | Attack |
|---|---|---|
| Generator | Sage Aarav | Close swing, +3 FP per swing that hits |
| Storm | Tanimbus | Close swing, brief stun |
| Support | Sunny Joe | Close swing, heals nearby classmates and himself |
| Shooter | Lobber Luis, Chillstin, Spear Hanbo | Long throw at one enemy; Chillstin's slows; Hanbo's pierces everything |
| Ricochet | Abhi-Ace | Medium throw, bounces to an adjacent lane |
| Fan | Zippy Matt | Medium throw, half damage in both neighboring lanes |
| Sniper | Nik-Scope | Double damage at any range in the lane, every 1.2 s |
| Close blast | Justin Time | Short reach, 1.4x damage, hits everything |
| Walls | Iron Imraan, Thorn Tuhin, Kaiju Kai | 50% more hero health; Kai stomps both directions |
| Bomb | Boom Sam | Double damage in a 3x3, half as often |
| Mine | Cami-Mine | Short, 1.6x damage on one enemy |
| Trap | Trapper Che | 1.2x damage, 1 s stun, every 1.5 s |
| Lane clear | Blitz Arnav | 2.5x damage on the whole lane, every 2 s |

Stunned enemies do not hurt the hero. A knockout spills a quarter of the Credits earned that level, which can be collected again. Spare gear sells for a third of its price. Ladders are drawn by the game, and only where both lanes they join are open.

## 29. 11/1 (current name, theme and story)
Everything before this section that describes a cheerful school, doors or events is history. The game is now cyberpunk: rain, neon, and an institution you can't leave. The mood reference was a wet, empty school hallway lit in blue and magenta. All art is original and comes from `tools/make_neon.py`.

**World.** Curve City, 2187. A grading system, the Ledger, was given bodies so that it could collect, so homework walks and report cards hunt. The Meridian Institute fits every student with a construct, a surgical ability (`DefenderType.construct`). Focus Points are mined from students' attention.

**Structure.** The story is fixed, with no branches (`Story.java`). There are six floors and three periods per floor, ending in a boss. Before every period the player walks a corridor (`ExploreScreen`) and finds people, terminals, lockers and notes. Looking is optional, but the order of what can be found never changes.

**Plot.**
1. Homeroom: day 1,204 of the semester. A message scratched on the desk, in your handwriting, says DON'T FORGET.
2. Locker Corridor: thousands of lockers are stamped GRADUATED. Your transcript says you've been re-enrolled, and that this is attempt 7.
3. Nutrition Hall: your locker holds six notebooks in your handwriting. The Lunch Lady remembers you, and can't stop herself from fighting you.
4. The Archive: past squads have your friends' names. Memory returns a little each time, on purpose, because hope keeps students working.
5. Compliance Gym: the Coach graduated once, and came back. Outside is a bigger gym.
6. Admissions Tower: you break the Deadline and the door opens. For one second you're out. Then you're assigned to a small room on the first floor, as the new Substitute. A student in the back row wakes up and looks at the clock. The attempt counter on the title screen goes up.

If the player's own hero is the person standing at a stop, the text plays as a recording in their own voice, which suits the story.

**Bosses (`Boss`).** The Substitute (summons homework), The Vice Principal (detention disables the closest classmate in three lanes for 3 s), The Lunch Lady (hits three classmates anywhere), The Librarian (silences three lanes for 2 s), The Coach (speeds up every enemy, and hits the closest classmate), The Deadline (summons pencils, then attacks twice as often and speeds everyone up below half health). The Midterm Exam remains as a mini-boss in Finals Week. Entrances shake the screen, and a big HP bar shows during the fight.

**Other systems.** Key items (`KeyItem`), second constructs at Lv.10 (`Job`), twelve requests (`Quest`, `QuestLog`, `QuestScreen`) and set bonuses of +5, +10, +15 or +25% power for four items of one tier. New regular enemies are Hall Monitor, Mystery Meat, Overdue Notice and Dodgeball.

Difficulty comes mostly from speed and bite rather than health, because extra health only made fights longer. The bot does not use requests, so a person who does gets extra rewards it doesn't.

**Music.** The soundtrack in `Music/` predates this theme and should be replaced with something slower and sadder.
