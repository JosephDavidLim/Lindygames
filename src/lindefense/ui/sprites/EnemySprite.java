package lindefense.ui.sprites;

import acm.graphics.GCompound;
import lindefense.enemy.Enemy;

/** PLACEHOLDER (to be replaced by the real sprite package). */
public abstract class EnemySprite extends GCompound implements Animated {
    /** Positions and updates the sprite for the enemy's current state (logical units). */
    public abstract void sync(Enemy e, double timeSeconds);
}
