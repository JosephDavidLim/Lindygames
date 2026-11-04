# Lindefense - AI Tower Defense

![Lindefense title card](docs/titlecard.png)

Lindefense is a complete, playable tower-defense game written in Java with the Stanford ACM Graphics
library (`GCanvas`, `GCompound`, `GOval`, `GRect`, `GPolygon`, `GLine`, `GLabel`). You defend a track
with 20 real AI models and companies (3 heroes + 17 towers) against 100 waves of computer-science
enemies - data structures, sorting algorithms and five corporate MOAB-class bosses - ending with the
BAD-class final boss **Palantir** on Wave 100.

The window opens large (about 92% of your screen) and is freely resizable: the whole game scales to
fit whatever size you drag it to.

All tuning numbers (tower stats, upgrades, abilities, enemy stats, boss phases, every wave) are in
data classes and documented in [DESIGN.md](DESIGN.md), which is generated from those classes.

## 1. Setup

### Quick start (macOS / Linux)

Copy-paste this whole block into a terminal:

```bash
git clone https://github.com/hanb0i/Lindefense.git
cd Lindefense
./setup.sh      # finds or installs a compatible JDK, checks lib/acm.jar, builds, runs the tests
./run.sh        # launch the game
```

`setup.sh` is safe to re-run. It:

1. Looks for a JDK **11-25** (`$JAVA_HOME`, then `~/.jdks/*`, then `/usr/libexec/java_home` on macOS,
   then `javac` on your `PATH`). If none is found, it downloads **Eclipse Temurin JDK 21** into
   `~/.jdks/`. It needs no `sudo` and no Homebrew, and it doesn't change your system Java.
2. Downloads `lib/acm.jar` if it is missing (the jar is already committed, so normally nothing is downloaded).
3. Builds into `build/` and runs the headless test suite. The last line should read `Passed: N  Failed: 0`.

> **Why not the newest Java?** `acm.jar`'s `GraphicsProgram` extends `javax.swing.JApplet`, and the
> Applet API was removed in **JDK 26**. On JDK 26+ the build fails with
> `cannot access JApplet ... class file for javax.swing.JApplet not found`. Keep your newer JDK
> installed if you like: `build.sh` / `run.sh` / `test.sh` pick a compatible JDK automatically
> (see `scripts/java-env.sh`).

### Manual JDK install (if you'd rather not use `setup.sh`)

```bash
# macOS with Homebrew
brew install --cask temurin@21

# Ubuntu / Debian
sudo apt-get update && sudo apt-get install -y openjdk-21-jdk

# Fedora
sudo dnf install -y java-21-openjdk-devel

# Any OS with SDKMAN (https://sdkman.io)
sdk install java 21-tem
```

Then confirm the JDK and point the scripts at it if it isn't the default:

```bash
javac -version                                   # expect javac 11 - 25
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS only; on Linux use the JDK's install dir
```

### Windows (PowerShell)

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
# open a NEW terminal so PATH / JAVA_HOME refresh, then:
git clone https://github.com/hanb0i/Lindefense.git
cd Lindefense
.\test.bat
.\run.bat
```

If you also have JDK 26+ installed, set `JAVA_HOME` to the JDK 21 folder first (the `.bat` scripts
put `%JAVA_HOME%\bin` at the front of `PATH`):

```powershell
$env:JAVA_HOME = (Get-ChildItem "C:\Program Files\Eclipse Adoptium\jdk-21*" | Select-Object -First 1).FullName
```

### The ACM graphics library

`lib/acm.jar` (the Stanford/ACM Java Task Force graphics library) is included in the repository.
If it is ever missing, `setup.sh` re-downloads it. To do it by hand:

```bash
mkdir -p lib
curl -L -o lib/acm.jar https://cs.stanford.edu/people/eroberts/jtf/acm.jar
unzip -l lib/acm.jar | grep GraphicsProgram      # should list acm/program/GraphicsProgram.class
```

(Any CS106A course copy of `acm.jar` also works.) There are no other dependencies or build tools.

### Troubleshooting

| Symptom | Fix |
|---|---|
| `cannot access JApplet` / `class file for javax.swing.JApplet not found` | You are on JDK 26+. Run `./setup.sh`, or set `JAVA_HOME` to a JDK 11-25 |
| `No compatible JDK found` | Run `./setup.sh`, or install JDK 21 using one of the commands above |
| `lib/acm.jar is missing` | Run `./setup.sh`, or use the `curl` command above |
| `permission denied: ./run.sh` | `chmod +x *.sh` |
| Window doesn't open over SSH / in CI | Expected: the game needs a display. `./test.sh` runs headless |

## 2. Build and run

macOS / Linux:

```
./build.sh      # compiles src/ into build/ with -Xlint
./run.sh        # builds if needed, then launches the game
./test.sh       # builds and runs the headless verification suite
```

Windows (Command Prompt / PowerShell):

```
build.bat
run.bat
test.bat
```

Manual equivalents. If `java -version` shows JDK 26 or newer, first run
`export JAVA_HOME="$(scripts/java-env.sh)" && export PATH="$JAVA_HOME/bin:$PATH"` in that terminal:

```
javac -d build -cp lib/acm.jar @<(find src -name '*.java')      # or list the files
java  -cp build:lib/acm.jar lindefense.Main                      # use ; instead of : on Windows
java  -cp build:lib/acm.jar lindefense.test.HeadlessTests        # headless checks
java  -cp build:lib/acm.jar lindefense.tools.DesignDocGenerator  # regenerate DESIGN.md
java  -cp build:lib/acm.jar lindefense.tools.AutoPlayer [maxWave] [mapIndex]   # balance probe (bot plays the maps)
#   optional: -Dbot.maxTowers=N -Dbot.random=true to handicap the bot (tower cap / random placement)
```

## 3. Controls

| Input | Action |
|---|---|
| Shop card, then left-click on the map | Buy and place a tower (ghost shows range; green = valid, red = invalid with the reason) |
| `B` / **ALL** button | Toggle the shop between one faction tab and a grid of all 20 towers grouped by faction (unaffordable towers are dimmed with a red price; hovering shows the shortfall). Clicking a faction tab leaves ALL mode |
| Left-click a placed tower | Select it: stats, upgrade button, targeting, ability, sell |
| Left-click an enemy | Inspect it (HP, speed, mechanic, modifiers) |
| Right-click / `Esc` | Cancel placement or deselect |
| `Space` / **Start Wave** button | Start the next wave (never starts automatically unless **Auto** is on) |
| `1` `2` `3` / buttons | Game speed 1x / 2x / 3x |
| `P` / `||` button | Pause (resume with `P` or the play button) |
| `U` | Upgrade the selected tower |
| `S` | Sell the selected tower (70% refund) |
| `A` | Fire the selected tower's ability |
| `T` | Cycle targeting priority (First / Last / Strongest / Weakest / Closest) |
| **Auto** button | Auto-start the next wave 8 s after the current one is cleared |
| **Menu** button | Back to the main menu |

## 4. Gameplay

- You start with **$650** and **100 lives**. Enemies that reach an exit remove lives based on their
  strength (1 for a Primitive Variable, up to 100 for a ZOMG or Palantir). At 0 lives you lose.
- Destroying enemies pays cash; every wave pays a bonus ($100 + wave) and Perplexity towers add
  research income. Rewards taper in the late game (see DESIGN.md) so economy decisions matter.
- Every tower has **four upgrades** (five states) named after its real model lineup - upgrading
  Claude buys "Claude Sonnet 5.5", then "Claude Fable 5.1", "Claude Opus 5.5" and finally the
  expensive superclass "Claude Mythos", which changes how the tower plays. (This deviates from the
  original spec's "exactly 3 upgrades" rule at the user's request.) Each tower also has a unique
  **active ability** with a cooldown (gold ring = ready, bar = cooling down), configurable
  **targeting**, and sells for 70% of everything spent on it.
- **Modifiers**: *Camo* enemies cannot be targeted without detection (Perplexity, Bellarmine
  Student + AI, Brad Lindemann T3, or their auras). *Regrow* enemies heal 1 HP every 2 s.
  *Fortified* enemies have double HP (Qwen strips it).
- **Bosses** appear on waves 40 (MOAB), 60 (BFB), 80 (ZOMG), 90 (DDT) and 100 (BAD) with large
  health bars, armour, and children released on destruction.
- **Palantir** (Wave 100) has three phases (armour 60% / 35% / 10%, faster each phase, Lockheed
  escorts in phase 3) and **Predictive Surveillance**: every 30/20/14 s it analyses your
  highest-investment towers for 3 s (red reticles and analysis lines), then disrupts them for 8 s
  (-50% speed, -30% range, abilities locked). Counter it with Perplexity (Encrypted Index), Copilot
  (Workspace Firewall), Bellarmine Student + AI (AGI Thesis) or Brad (Supreme Commander) auras,
  Brad's Command Override, or by spreading your investment. Destroying Palantir wins the campaign.

## 5. Towers

| Faction | Towers | Role |
|---|---|---|
| Heroes | Bellarmine Student, Bellarmine Student + AI, Brad Lindemann | Physical damage; autonomous targeting + detection; commander support and heavy artillery (most expensive unit by far) |
| Primary | Gemini, Claude, GPT | Chain lightning vs groups; piercing + slowing crowd control; adaptive all-rounder |
| Military | DeepSeek, Copilot | Railgun vs complex enemies/bosses; attack-speed aura (double for Military) and Surveillance firewall |
| Magic | DALL-E, Sora, Suno | Splash + Sprite summons; time-slowing pulses; sonic pulses vs dense groups |
| Support | Grok, Perplexity | Disruption of enemy mechanics; camo detection, marks and income |
| Experimental Titans | Qwen, Llama, Mistral, Cohere, GLM, Kimi, MiniMax | Layer/Fortified stripping; kinetic knockback; wind vs fast enemies; armour-breaking single target; gravity crowd control; scaling damage vs non-Fortified; bouncing/cloning orbs |

Full stats, upgrade paths and abilities: [DESIGN.md section 2](DESIGN.md#2-towers).

## 6. Enemies

| Enemy | HP | Speed | Mechanic |
|---|---|---|---|
| Primitive Variable | 1 | 1.0x | Basic |
| Static Array | 2 | 1.4x | Releases 1 Primitive Variable |
| Dynamic Array | 3 | 1.8x | Releases 1 Static Array |
| Singly Linked List | 4 | 3.2x | Very fast |
| Doubly Linked List | 5 | 3.5x | Accelerates when damaged |
| Bubble Sort | 3 | 0.9x | Swaps position with the enemy ahead |
| Selection Sort | 4 | 1.0x | Jumps to the front of its group |
| Insertion Sort | 4 | 1.5x | Leapfrogs the enemy ahead |
| Shell Sort | 5 | 2.0x | Gap jumps (140 / 70 / 35 / 18 px) |
| Merge Sort | 6 | 1.1x | Splits into two Merge Runs (2 HP) |
| Quick Sort | 7 | 1.8x | Splits into two Quick Partitions (3 HP) |
| Heap Sort | 8 | 1.2x | Physical-immune; shields enemies behind it |
| Counting Sort | 1 + 4 nodes | 1.3x | Releases 4 Primitive Variables |
| Radix Sort | 8 | 1.5x | Armour changes every two layers |
| Bucket Sort | 6 | 1.4x | Stores nearby weaker enemies, releases them on death |
| TimSort | 10 | 1.6x | Fast; splits into Merge Sort + Insertion Sort |

Bosses: Bellarmine College Prep (MOAB), SW Resource Consultant (BFB), 3Com (ZOMG), Lockheed Martin
(DDT: camo, fast, Physical-immune), Palantir (BAD). Details and precise rules:
[DESIGN.md sections 3-4](DESIGN.md#3-enemies).

## 7. Maps

| Map | Difficulty | Unique mechanics |
|---|---|---|
| The Silicon Circuit | Beginner | Two lanes; Capacitor Pads charge Support/Hero towers for cash and speed; Trace Short-Circuits zap one lane and boost towers along it |
| The Legacy Monolith | Intermediate | Serpentine server rack; chassis block line of sight; Thermal Overload Venting every 10 waves on a random floor |
| The Hallucination Canyon | Advanced | Two crossing routes; Attention Drift teleports 25% of enemies at the hub; Noise Storms halve range unless a Logic Specialist (DeepSeek/Copilot) is near |
| The Singularity Core | Expert | Four portals spiralling into a vortex; Gravitational Pull speeds enemies up near the core; Event Horizon Collapse on wave 50 resets cooldowns and scatters enemies |

The map select screen lays out every map registered in `MapCatalog` (4 per row), so additional maps appear automatically; `DESIGN.md` section 5 is generated from the same registry.

## 8. Project layout

```
src/lindefense/
  Main.java                 entry point
  sim/      Game (state, economy, fixed-step loop), Vec, Rng
  map/      GameMap, Path (waypoints), Obstacle, the four maps, MapCatalog
  enemy/    EnemyType (data table), Enemy, Boss (phases + Predictive Surveillance), EnemyManager, modifiers/status
  tower/    TowerType, TowerCatalog (data table), TowerDef/TowerStats/UpgradeDef/AbilityDef, Tower,
            TowerManager, AbilityManager, Shop, Targeting
  combat/   Combat (targeting, attacks, damage formula), Projectile, HitSpec, DamageType, VisualEvent
  wave/     WaveData (all 100 waves), WaveDef, SpawnGroup, WaveManager
  ui/       LindefenseProgram (ACM GraphicsProgram), ScaledLayer (resize scaling), GameScreen, WorldView,
            HudPanel, SidePanel, TowerSprite, MenuScreen, MapSelectScreen, UIButton, Ui, DemoDriver
  ui/sprites/  PLACEHOLDER enemy sprites (EnemySprite, SpriteFactory, Animated); see section 10
  test/     HeadlessTests - verification suite run without a window
  tools/    DesignDocGenerator (DESIGN.md), AutoPlayer (balance bot)
lib/acm.jar
setup.sh                      one-shot setup: JDK check/install, acm.jar check, build + tests
scripts/java-env.sh           picks a JDK 11-25 for the shell scripts (JDK 26+ removed JApplet)
build.sh / run.sh / test.sh   (and .bat equivalents)
```

The simulation (`sim`, `map`, `enemy`, `tower`, `combat`, `wave`) has no dependency on ACM or AWT
and runs headless; the `ui` package renders it.

## 9. Testing

`./test.sh` runs `lindefense.test.HeadlessTests`, which exercises the real game logic: purchase /
upgrade / sell and the 70% refund, exactly four upgrades (five states) for all 20 towers, rewards without
duplicate-kill exploits, lives on leaks, every targeting priority, camo vs detection, Fortified and
Regrow, every splitter's children, boss children, all 100 wave compositions (including Palantir on
Wave 100), Palantir's phases and Predictive Surveillance, ability cooldowns, traversal of every
route on all four maps, map mechanics, win and loss conditions, and a late-wave stress run.

`JAVA_OPTS="-Dlindefense.demo=<dir>" ./run.sh` runs a scripted UI walkthrough
(menu -> map select -> game -> window resizes -> Palantir) that writes PNG snapshots and verifies
click-to-place after resizing.

## 10. Replacing the placeholder sprites

The towers, enemies and their stats are final, so `TowerType`, `TowerCatalog` and `EnemyType` don't
change. Every tower and enemy already has its look defined in the design spec (the "Lindefense" master
prompt: §14A *Unique Enemy Character and Model Design* and the *Tower Visual Design* list). Those
definitions are copied into the tables below. The game currently draws stand-ins instead of those
designs, and the job is to replace them. All of the work is in `src/lindefense/ui/`; the simulation
packages (`sim`, `map`, `enemy`, `tower`, `combat`, `wave`) stay untouched.

### What is a placeholder today

| File | Draws today | Should draw |
|---|---|---|
| `src/lindefense/ui/sprites/SpriteFactory.java` | Every enemy and boss as the same orange circle (turns red as HP drops, fades when camo) | A character per enemy type ([enemy tables](#enemy-sprites)) |
| `SpriteFactory.createDeathEffect` / `createSplitEffect` | One expanding ring | A pop effect, and splitting enemies visibly coming apart into their children |
| `src/lindefense/ui/SidePanel.java` → `placeholderIcon(...)` | Shop icons as plain two-colour ovals | `TowerIcons.create(type, 0, size)`, a new class (it doesn't exist yet) drawing each tower at icon size |
| `src/lindefense/ui/TowerSprite.java` → `silhouette(...)` | One body shape per faction (all three Primary towers are the same circle) | Each tower's own look ([tower table](#tower-sprites)) |

`TowerSprite` is otherwise finished. Keep its tier hardware (rings, side barrels, longer turret,
superclass glow and crown), tier pips, hero star, ability ring, cooldown bar and Surveillance warning,
and replace only the body and turret art. `EnemySprite.java` and `Animated.java` are also marked
placeholder, but they are the interfaces `WorldView` calls, so keep their method signatures.

### Workflow

```bash
./setup.sh                      # once per machine: JDK + acm.jar + build + tests
./build.sh && ./run.sh          # rebuild after every edit, then play

# scripted walkthrough: writes PNG snapshots to build/snapshots, then exits on its own
./build.sh && JAVA_OPTS="-Dlindefense.demo=build/snapshots" ./run.sh
open build/snapshots            # macOS (Linux: xdg-open build/snapshots)

./test.sh                       # must still end with "Failed: 0"
```

The snapshots that show sprites:

- `03_game_default.png`: GPT, Claude, Perplexity, Brad Lindemann and Suno at tier 0, plus the 28 px shop icons on the Primary tab.
- `03b_shop_all.png`: all 20 shop icons at 18 px.
- `06_palantir.png`: tier-3 towers, Palantir, and a Bellarmine College Prep. This is the only snapshot with enemies in it.

No snapshot shows the ordinary enemies, so check those in a real game (`./run.sh`, pick a map, `Space`
starts a wave, `3` sets 3x speed). Wave 1 is Primitive Variables, and the sorting algorithms start at
wave 20. The wave table in `DESIGN.md` lists what each wave spawns.

`./build.sh` deletes `build/` and `build/` is git-ignored, so old snapshots are cleared on every
rebuild and never get committed. Don't run two builds in the same folder at once: the second one
deletes the first one's `build/` mid-run.

### Rules (from the spec)

- **Original art only.** Draw with ACM shapes (`GOval`, `GRect`, `GPolygon`, `GLine`, `GArc`, `GLabel`)
  grouped in a `GCompound`. Don't copy Bloons TD sprites or use real company logos. PNG images through
  `GImage` are allowed if every enemy type gets its own image and the files are committed to the repo
  (relative paths, nothing downloaded at runtime).
- **Build once, then mutate.** Create every shape in the constructor. `sync()` and `step()` only move,
  recolour, show or hide existing shapes. Late waves have about 95 enemies on screen at once, so
  creating objects per frame causes stutter.
- **Only `GPolygon` can rotate** in this `acm.jar` (`GCompound`, `GImage`, `GOval` and `GRect` can't).
  Anything that turns to face a target or the direction of travel has to be a `GPolygon`, like the
  existing turret.
- **Match the hitbox.** Draw centred on (0, 0) and size the body from `e.getRadius()` (radius column
  below) so what players see is what towers hit. Bosses must be clearly larger and more detailed than
  ordinary enemies.
- **Read-only.** Sprite code reads `Enemy`/`Tower` state and never changes it.
- **One art style.** Use the same outline weight, light direction and proportions for every enemy.
- **No duplicates.** `WorldView` already draws HP bars (enemies with 4+ max HP), boss health bars,
  range circles and projectiles.
- **Readable when small.** Shop icons are 18 px (ALL grid) and 28 px (faction tabs). The smallest
  enemy, Primitive Variable, has a 9 px radius.

### The interface `WorldView` and `SidePanel` call

```java
// When an enemy first appears (once), then every frame:
EnemySprite s = SpriteFactory.create(enemy);   // build the whole character here
s.sync(enemy, timeSeconds);                    // setLocation(enemy.getPosition()), update state visuals
s.step(dt);                                    // advance animation (return value ignored for enemies)

// When an enemy dies (not when it leaks):
Animated fx = enemy.getType().hasChildren()
        ? SpriteFactory.createSplitEffect(enemy)
        : SpriteFactory.createDeathEffect(enemy);
// fx must also be a GObject (e.g. extend GCompound) or it is never shown.
// step(dt) runs every frame until it returns false, then fx is removed.

// Shop cards (replaces placeholderIcon in SidePanel):
GObject icon = TowerIcons.create(type, 0, size); // draw inside a size x size box, top-left at (0, 0)
icon.setLocation(x, y);
```

A simple structure is one `EnemySprite` subclass per enemy type in `src/lindefense/ui/sprites/`, with
`SpriteFactory.create` switching on `e.getType()`.

State a sprite can read (methods on `Enemy` unless noted):

| To show | Read |
|---|---|
| Position and heading | `getPosition()`, `getRoute().directionAt(getDistance())` |
| Damage state | `getHpFraction()`, `getHp()`, `getMaxHp()`, `getLastHitTime()` |
| Modifiers | `isCamo()`, `isRevealed()`, `isRegrow()`, `isFortified()` (Qwen can strip Fortified mid-wave) |
| Status effects | `isStunned()`, `getSlowFraction()`, `isDisrupted()`, `getMarkedBonus()` |
| Mechanics | `getAccelBonus()` (Doubly Linked List), `currentShellGap()` (Shell Sort), `radixPhase()` 0-3 (Radix Sort), `getStoredEnemies()` (Bucket Sort) |
| Boss state (cast to `Boss`) | `getPhase()`, `getPhaseCount()`, `getPhaseName()`, `isPalantir()`, `isTelegraphing()` |

### Enemy sprites

| `EnemyType` | Radius | Spec design | Mechanic the art should hint at |
|---|---|---|---|
| `PRIMITIVE_VARIABLE` | 9 | A small, simple data orb | - |
| `STATIC_ARRAY` | 12 | An orderly segmented container | Releases 1 Primitive Variable |
| `DYNAMIC_ARRAY` | 14 | An expandable, growing container | Releases 1 Static Array |
| `SINGLY_LINKED_LIST` | 13 | Connected nodes with one-directional links | Very fast |
| `DOUBLY_LINKED_LIST` | 14 | Connected nodes with two-directional links | Speeds up when damaged |
| `BUBBLE_SORT` | 15 | A bubbly character that visibly swaps positions | Swaps with the enemy ahead |
| `SELECTION_SORT` | 15 | A selection indicator or targeting motif | Jumps to the front of its group |
| `INSERTION_SORT` | 14 | A character that slides between other enemies | Leapfrogs the enemy ahead |
| `SHELL_SORT` | 15 | Segmented shells or gap-jumping effects | Gap jumps of 140 / 70 / 35 / 18 px |
| `MERGE_SORT` | 17 | A character that visibly divides into two | Splits into 2 Merge Runs |
| `MERGE_RUN` | 11 | *Not in the spec (split child):* half of a Merge Sort | - |
| `QUICK_SORT` | 17 | A partition-themed character that splits into two groups | Splits into 2 Quick Partitions |
| `QUICK_PARTITION` | 12 | *Not in the spec (split child):* one side of a Quick Sort | - |
| `HEAP_SORT` | 19 | A tree- or hierarchy-shaped armoured character | Physical-immune; shields enemies behind it |
| `COUNTING_SORT` | 16 | Four visible counters or nodes | Releases 4 Primitive Variables |
| `RADIX_SORT` | 18 | Changing digit-based armour | Armour type changes every 2 layers |
| `BUCKET_SORT` | 19 | A bucket shape that visibly stores other enemies | Holds up to 6 weaker enemies |
| `TIMSORT` | 19 | A hybrid of insertion-sort and merge-sort visuals | Splits into a Merge Sort and an Insertion Sort |

Bosses (the spec requires each to be much larger and more detailed than ordinary enemies):

| `EnemyType` | Class | Radius | Spec design |
|---|---|---|---|
| `BELLARMINE_COLLEGE_PREP` | MOAB | 34 | Heavy MOAB-style armoured design |
| `SW_RESOURCE_CONSULTANT` | BFB | 42 | Large, reinforced BFB-style design |
| `THREE_COM` | ZOMG | 52 | Massive ZOMG-style design |
| `LOCKHEED_MARTIN` | DDT | 30 | Fast, stealth-oriented DDT-style design (always camo) |
| `PALANTIR` | BAD | 64 | Enormous, intimidating corporate airship with a surveillance and predictive-intelligence look. It has three phases, telegraphs Predictive Surveillance, and needs a dramatic entrance and defeat |

Modifiers and damage apply to every enemy, bosses included:

- **Camo:** semi-transparent or cloaked.
- **Regrow:** a regenerating effect, such as glowing growth or segments restoring.
- **Fortified:** visible armour plating, metal reinforcement or a protective shell.
- An enemy with several modifiers shows all of them at once, without losing its own identity.
- Losing HP or layers visibly changes the enemy, and splitting enemies visibly separate into their child characters.

### Tower sprites

Colours are already set in `TowerType` (`primaryRgb` fill / `secondaryRgb` accent). Looks are from the
spec's *Tower Visual Design* list:

| `TowerType` | Fill / accent | Spec look |
|---|---|---|
| `BELLARMINE_STUDENT` | `0xd8d8d8` / `0x1b3a6b` | *Not in the list:* student hero who throws chemistry textbooks and pencils |
| `BELLARMINE_STUDENT_AI` | `0x9fd3ff` / `0x1b3a6b` | *Not in the list:* student with an autonomous AI assistant, firing laser-etched notes |
| `BRAD_LINDEMANN` | `0xffd700` / `0x111111` | Gold-and-black plasma fortress (listed as "Lindeman") |
| `GEMINI` | `0x4f7cff` / `0x9b5cff` | Blue-purple dual barrel |
| `CLAUDE` | `0xe88a4a` / `0xf3d9c4` | Warm orange boomerang launcher |
| `GPT` | `0x10a37f` / `0xffffff` | Green and white energy turret |
| `DEEPSEEK` | `0x1f3b8f` / `0x4d7cff` | Dark blue precision cannon |
| `COPILOT` | `0x2f7fe0` / `0xb8dcff` | Blue automated machine gun |
| `DALLE` | `0xffb347` / `0x5bc0eb` | *Not in the list:* "Sketch Summoner" that paints splash zones and summons illustrated creatures |
| `SORA` | `0x8fd3ff` / `0xffffff` | Light blue projector cannon |
| `SUNO` | `0xff5fa2` / `0xffd1e6` | Pink soundwave speaker |
| `GROK` | `0x151515` / `0xff7a00` | Black and neon orange rapid-fire tower |
| `PERPLEXITY` | `0x20b2aa` / `0xe0fffa` | Turquoise radar turret |
| `QWEN` | `0x7b3fe4` / `0xd9c6ff` | Violet compact dart turret |
| `LLAMA` | `0xf2c94c` / `0x8a5a2b` | Yellow multi-barrel tower |
| `MISTRAL` | `0x2bb3a3` / `0xd8fff6` | Teal wind launcher |
| `COHERE` | `0xff7f6e` / `0xffe3de` | Coral support beacon |
| `GLM` | `0x19e6ff` / `0x0a2a3a` | Electric cyan coil |
| `KIMI` | `0x3d3fa8` / `0xc6c7ff` | Indigo sniper tower |
| `MINIMAX` | `0xe03131` / `0xffd6d6` | Red explosive turret |
| `DALLE_SPRITE` | `0xffe08a` / `0x5bc0eb` | DALL·E's summoned "painted familiar" (not sold in the shop) |

The three heroes and DALL·E are missing from the spec's visual list, so their rows above come from
their tower write-ups in the spec. A few looks in the list also predate the final mechanics. For
example, Claude is drawn as a "boomerang launcher" but fires piercing shots in-game, so agree with the
team on which one the art follows.

Upgrades: the spec requires every upgrade to visibly transform the tower, with a larger or more
elaborate barrel, brighter accents, extra rings or antennas, small tier indicators and better firing
effects. `TowerSprite.rebuild()` runs once per tier change, so add per-tier detail there and only
update existing shapes in `sync()`. Shop icons (`TowerIcons`) show tier 0.
