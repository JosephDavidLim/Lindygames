# 11/1

A cyberpunk lane-defense RPG in Java using ACM graphics, mixing Plants vs.
Zombies with MapleStory. Curve City, 2187: grades have come alive, and every
student at the Meridian Institute is fitted with a constructed ability. You climb
the Institute with your squad, learning what it really is. You can't get out.

## Requirements
- Java 17 or newer
- `acm.jar` (ACM Java Task Force library) placed in `lib/`

## Build and run
From the project root:

```
javac -cp lib/acm.jar -d out $(find src -name "*.java")
java -cp out:lib/acm.jar game.GameApp
```

Run from the project root so images in `res/` are found.

## Animation
Characters are animated in code by squashing, stretching and shifting their pictures (lean-back wind-up and lunging throw, hops, bite cycles, flatten-on-defeat) and each thrower has its own ammo. See `docs/design.md` section 23.

## Test the game logic without graphics
```
java -cp out:lib/acm.jar game.HeadlessDemo        # plays full campaigns, prints a difficulty table
java -Dnotrain=true -cp out:lib/acm.jar game.HeadlessDemo   # same, never spending points or buying gear
java -cp out:lib/acm.jar game.HeadlessDemo 5      # one level, verbose
```

## Controls
**Mouse (the squad)**
- Click a card in the shop bar, then a tile to place that classmate. A ghost shows where they will stand, green if the spot is free and red if not.
- Click falling coffees for Focus Points (FP), or walk over them. Hover over cards, enemies and menu buttons for details.

**Keyboard (your hero)**
- Left and right arrows (or A, D): run.
- Z (or Alt): jump, which clears enemies. Touching one hurts you.
- Up and down arrows (or W, S) at a ladder: climb, even from mid-jump. Down + Z: drop through the platform. Ladders only appear where both lanes are open, so level 1 has none.
- Space (or X): attack. Each classmate attacks differently, and the Squad window says how.
- Walk over coins and items to pick them up. Anything left is collected when the level ends.

**Between levels**
- Choose your classmates: 6 slots at the start, a 7th after The Vice Principal, an 8th after The Librarian.
- Squad window: spend stat points, equip gear, and choose who you play as.
- Black Market: spend Credits on gear, or sell spares for a third of their price.
- Auto-assign all: spends every classmate's points for you, from the Squad window or the results screen.

Clear every wave to win. The yellow Highlighter on each lane sweeps it once; a second breach in that lane loses the level. If your hero is knocked out, they return after 5 s and spill a quarter of the level's Credits where they fell, so run back and grab them. A knockout alone never loses the level. You keep EXP and loot even after a loss. Progress is saved to `progress.txt`.

## The RPG
- **EXP:** every classmate in the level earns EXP per kill, and level-ups refill health.
- **Second construct:** at Lv.10 each classmate is fitted with one of two upgrades, like Railthrower or Mortar Arm.
- **Requests:** classmates ask for favors. Accept, finish, then collect.
- **Set bonuses:** four items of one tier add 5 to 25% power.
- **Stats:** 2 points per level. STR is power, DEX is action speed, INT is card recharge, LUK is crit and dodge.
- **Ranks:** Freshman, then Sophomore at Lv.10, Junior at Lv.20, Senior at Lv.30.
- **Gear:** weapon, hat, shoes and charm, in Common, Rare, Epic and Legendary. Enemies drop them, bosses always do, and the Black Market sells them.

## The story
The story is fixed and has no branches. Before each fight you walk a corridor (`ExploreScreen`) and find people, terminals, lockers and notes (`Story.java`). Walk with the arrows, press Up to look, and take the door at the far end to fight.

Each floor ends with a boss who keeps a permanent key item: The Substitute (Hall Pass, +50 starting FP), The Vice Principal (Master Locker Key, 7th card slot), The Lunch Lady (Free Coffee Refills, +10 FP per coffee), The Librarian (Forbidden Study Guide, 8th card slot), The Coach (Team Spirit, +10% EXP) and The Deadline. The title screen counts your attempts.

## Maps
Six floors with three periods each: Homeroom (plain), Locker Corridor (sealed lockers block building), Nutrition Hall (coolant speeds enemies, more coffee), The Archive (damping fields slow enemies, less coffee), Compliance Gym (overclock pads make classmates act 25% faster), and Admissions Tower (all of it).

Drag the window to any size: the game scales to fit and keeps its shape, with black bars if needed.

## Project layout
```
src/game/       GameApp (window and loop), Board, WaveManager, Level(s), GameMap, Terrain, Shop, Card, Hero, Stat, Gear, Job, KeyItem, Quest, QuestLog, Party, Profile, Story
src/entities/   Entity, Defender, Enemy, Avatar, Projectile, Coffee, Drop, Popup and their subclasses
src/view/       Screens (menu, story, explore, map, loadout, play, result, squad, market, requests) and drawing helpers
res/sprites/    chibi defenders and enemy art (PNG); neon/ holds the night-lit copies the game uses
res/maps/       battle maps, corridors and special tiles (PNG)
res/gear/       item icons (PNG)
res/ui/         title screen (PNG)
res/sfx/        sound effects (WAV)
res/music/      soundtrack (WAV, converted from Music/)
Music/          the original soundtrack MP3s
tools/          make_sprites.py (characters), make_neon.py (maps, corridors, tiles, title, night-lit sprites), make_maps.py (gear icons), make_sounds.py (sound effects), convert_music.sh (MP3 to WAV)
docs/           design.md, UML diagram, topic pitch, video notes
```
The game logic (`game`, `entities`) never touches ACM; only `view` and `GameApp` draw.

## Regenerating the art
All characters are drawn from scratch by a script, then shrunk for smooth edges.
```
python3 -m pip install pillow
python3 tools/make_sprites.py            # all characters
python3 tools/make_sprites.py luis       # just one
```
Edit the `CHARACTERS` table at the top of `tools/make_sprites.py` to change anyone's hair, skin, outfit or smile. You can also draw your own PNGs (128x128, transparent) and drop them into `res/sprites/defenders/` using the lowercase enum name, for example `aarav.png`.

## Music
Four tracks, looped and crossfaded by `view.Music`:
- School Bell Adventure: title, level select, loadout and results
- Pencil Tap Battle: levels 1 to 5
- Deadline Chaos: levels 6 to 10
- Huge Wave: takes over when the final "huge wave" banner appears

Java's built-in audio cannot read MP3, so the game plays WAV copies in `res/music/` (already included). If you replace a track in `Music/`, run `tools/convert_music.sh` (macOS) to rebuild them.

## Defenders
Sage Aarav, Tanimbus, Sunny Joe, Lobber Luis, Boom Sam, Zippy Matt, Nik-Scope, Iron Imraan, Chillstin, Abhi-Ace, Spear Hanbo, Thorn Tuhin, Trapper Che, Blitz Arnav, Cami-Mine, Justin Time, Kaiju Kai. All 17 have working abilities; see `docs/design.md` for the roster and the 10 level unlocks.

## Data structures
- `Defender[][]` 2-D array for the board
- `ArrayList<Enemy>`, `ArrayList<Projectile>` for live objects
- `java.util.Random`, `Iterator` for spawning and safe removal

## Credits
Characters drawn by the team's script. Likenesses are cartoon avatars used with permission.
Soundtrack: AI-generated by the team (edit this line with the tool you used, for example Suno, and your plan's license terms).
