package entities;

import game.Board;
import game.Hero;

/**
 * A classmate placed on the grid. Subclasses supply the behavior in
 * {@link Entity#update(game.Board)}.
 */
public abstract class Defender extends Entity {
    /** Which defender this is. */
    protected final DefenderType type;
    /** Lane the defender stands in. */
    protected final int row;
    /** Column the defender stands in. */
    protected final int col;
    private static final int BITE_ROLL_TICKS = 18;

    /** This classmate's character sheet, or null for plain base stats. */
    protected Hero hero;
    private int actionAge = 999;
    private int biteTicks;
    private boolean dodging;
    private int disabledTicks;

    /**
     * Creates a defender centered in its cell.
     *
     * @param type the defender type
     * @param row lane index
     * @param col column index
     */
    protected Defender(DefenderType type, int row, int col) {
        super(col + 0.5, type.getHealth());
        this.type = type;
        this.row = row;
        this.col = col;
    }

    /**
     * Records that the defender just did its thing (threw, paid out, healed).
     * Called by subclasses.
     */
    protected void actionDone() {
        actionAge = 0;
    }

    /**
     * Counts one tick since the last action. The board calls this before each update.
     */
    public void ageAction() {
        if (actionAge < 999) {
            actionAge++;
        }
    }

    /**
     * Gives this defender its character sheet and scales its health to match.
     * One-hit specials (1 HP) stay at 1.
     *
     * @param sheet the sheet, or null for base stats
     */
    public void assignHero(Hero sheet) {
        hero = sheet;
        syncHealthToHero();
    }

    /**
     * Stops this defender acting for a while (Detention, or a shush).
     *
     * @param ticks how long
     */
    public void disable(int ticks) {
        disabledTicks = Math.max(disabledTicks, ticks);
    }

    /**
     * Tells whether this defender is unable to act.
     *
     * @return true while disabled
     */
    public boolean isDisabled() {
        return disabledTicks > 0;
    }

    /**
     * Counts a disable down by one tick.
     *
     * @return true if the defender may act this tick
     */
    public boolean tickDisable() {
        if (disabledTicks > 0) {
            disabledTicks--;
            return false;
        }
        return true;
    }

    /**
     * Applies a level-up: maximum health rises and health refills.
     */
    public void onLevelUp() {
        syncHealthToHero();
        health = maxHealth;
    }

    private void syncHealthToHero() {
        if (hero == null || type.getHealth() <= 1) {
            return;
        }
        double fraction = health / maxHealth;
        maxHealth = type.getHealth() * hero.healthMultiplier();
        health = maxHealth * fraction;
    }

    /**
     * Runs once after placement, when the sheet is known. Defenders with a
     * starting timer set it here so that DEX and terrain count.
     *
     * @param board the board
     */
    public void onPlaced(Board board) {
    }

    /**
     * Gets this defender's power after STR and level.
     *
     * @return the type's power times the sheet's multiplier
     */
    protected double power() {
        return type.getPower() * powerMultiplier();
    }

    /**
     * Gets a value that scales at half the normal rate, for things that
     * would break the game at full rate: Focus Point payouts and stun length.
     *
     * @param base the unscaled value
     * @return the scaled value
     */
    protected double softPower(double base) {
        return base * (1 + (powerMultiplier() - 1) / 2);
    }

    private double powerMultiplier() {
        return hero == null ? 1 : hero.powerMultiplier();
    }

    /**
     * Gets the ticks between actions after DEX and the tile underfoot.
     *
     * @param board the board
     * @return ticks, at least 1
     */
    protected int actionTicks(Board board) {
        double speed = board.terrainAt(row, col).getDefenderSpeed();
        if (hero != null) {
            speed *= hero.speedMultiplier();
        }
        return Math.max(1, (int) Math.round(Board.seconds(type.getIntervalSec()) / speed));
    }

    /**
     * Gets the chance that this defender's hit is critical.
     *
     * @return chance from 0 to 0.5
     */
    protected double critChance() {
        return hero == null ? 0 : hero.critChance();
    }

    /**
     * Takes one tick of biting. Every chomp cycle has a chance, from LUK, to
     * miss completely.
     *
     * @param board the board
     * @param amount damage this tick
     */
    public void receiveBite(Board board, double amount) {
        if (biteTicks % BITE_ROLL_TICKS == 0) {
            dodging = hero != null && board.roll(hero.dodgeChance());
            if (dodging) {
                board.addPopup(new Popup(Popup.Kind.MISS, row, col + 0.5, 0, null));
            }
        }
        biteTicks++;
        if (!dodging) {
            takeDamage(amount);
        }
    }

    /**
     * Gets the character sheet.
     *
     * @return the sheet, or null
     */
    public Hero getHero() {
        return hero;
    }

    /**
     * Gets how many ticks ago the defender last acted, for animation.
     *
     * @return ticks since the last action, 999 if it never has
     */
    public int getActionAge() {
        return actionAge;
    }

    /**
     * Tells whether enemies must stop and bite this defender. Most defenders
     * block; a bomb counting down does not, so enemies walk through it and
     * get caught in the blast.
     *
     * @return true if enemies stop at this defender
     */
    public boolean isBlocking() {
        return true;
    }

    /**
     * Called every tick an enemy is biting this defender. Does nothing by
     * default; thorn defenders override it.
     *
     * @param attacker the enemy that is biting
     */
    public void onBitten(Enemy attacker) {
    }

    /**
     * Gets the defender type.
     *
     * @return the type
     */
    public DefenderType getType() {
        return type;
    }

    /**
     * Gets the lane.
     *
     * @return row index
     */
    public int getRow() {
        return row;
    }

    /**
     * Gets the column.
     *
     * @return column index
     */
    public int getCol() {
        return col;
    }
}
