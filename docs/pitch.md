# Topic pitch: 11/1

**Game type:** Cyberpunk lane defense with RPG progression, in Java with ACM graphics.

**Idea:** Curve City, 2187. Grades have come alive, and every student at the
Meridian Institute is fitted with a constructed ability. Our class squad climbs
the Institute floor by floor, placing classmates on a 5x9 grid against walking
homework, report cards and college applications, while the player runs and
fights as one of them. Between fights you explore corridors and piece together
a fixed story about a system nobody can leave.

**Required data structures:**
- 2-D arrays: `Defender[][] grid` for the board, `Terrain[][]` for each level's layout
- `ArrayList<Enemy>`, `ArrayList<Projectile>`, `ArrayList<Coffee>` for live objects
- `java.util.*`: `Random` for point-budget wave spawns, `Iterator` for safe removal

**Levels:** 6 floors of 3 levels each, with a boss ending every floor, new
classmates unlocked along the way, a pre-level loadout, and levels, stats and gear.

**Team:** 2-3 students: engine/board, defenders/UI, enemies/waves/art.

**Content note:** Everyone shown as a character has agreed to be included.
Keep all jokes friendly.
