package game;

import entities.DefenderType;
import entities.EnemyType;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The classmates brought into one level. Like a MapleStory party, each of them
 * gets EXP for every kill. It also hands out Credits and drops, and keeps the
 * totals for the result screen.
 */
public class Party {
    private static final double DROP_CHANCE = 0.01;
    private static final int OVERLEVEL_GAP = 8;

    private final Profile profile;
    private final List<DefenderType> members;
    private final Random rng;
    private final List<Gear> gearFound = new ArrayList<>();
    private final List<Hero> levelUps = new ArrayList<>();
    private final List<Quest> questsDone = new ArrayList<>();
    private int creditsEarned;
    private int expEarned;

    /**
     * Creates a party.
     *
     * @param profile the player's profile
     * @param members the classmates brought into the level
     * @param rng random source for drops
     */
    public Party(Profile profile, List<DefenderType> members, Random rng) {
        this.profile = profile;
        this.members = new ArrayList<>(members);
        if (!this.members.contains(profile.getMain())) {
            this.members.add(profile.getMain());
        }
        this.rng = rng;
    }

    /**
     * Gets a classmate's sheet.
     *
     * @param type the classmate
     * @return the sheet
     */
    public Hero hero(DefenderType type) {
        return profile.hero(type);
    }

    /**
     * Pays out for a defeated enemy. Classmates far above the enemy's level
     * earn a quarter of the EXP, so replaying the first map is not the best way to train.
     *
     * @param type the enemy
     * @param enemyLevel the enemy's level
     * @return the classmates who levelled up, usually none
     */
    public List<Hero> award(EnemyType type, int enemyLevel) {
        double spirit = profile.has(KeyItem.TEAM_SPIRIT) ? 1.1 : 1.0;
        int exp = (int) Math.round(type.getReward() * (1 + 0.1 * enemyLevel) * spirit);
        questsDone.addAll(profile.getQuests().recordKill(type));
        List<Hero> leveled = new ArrayList<>();
        for (DefenderType t : members) {
            Hero h = profile.hero(t);
            int share = h.getLevel() > enemyLevel + OVERLEVEL_GAP ? Math.max(1, exp / 4) : exp;
            if (h.addExp(share) > 0) {
                leveled.add(h);
                levelUps.add(h);
            }
        }
        expEarned += exp;
        return leveled;
    }

    /**
     * Gets the quests finished in this level.
     *
     * @return the quests
     */
    public List<Quest> getQuestsDone() {
        return questsDone;
    }

    /**
     * Tells whether the player holds a key item.
     *
     * @param item the item
     * @return true if owned
     */
    public boolean has(KeyItem item) {
        return profile.has(item);
    }

    /**
     * Gets the Credits an enemy leaves on the floor.
     *
     * @param type the enemy
     * @param enemyLevel the enemy's level
     * @return Credits
     */
    public int creditsFor(EnemyType type, int enemyLevel) {
        return (int) Math.round(type.getReward() * (1 + 0.1 * enemyLevel));
    }

    /**
     * Banks a picked-up drop.
     *
     * @param credits Credits gained
     * @param gear the item gained, or null
     */
    public void collect(int credits, Gear gear) {
        creditsEarned += credits;
        profile.addCredits(credits);
        if (gear != null) {
            profile.addGear(gear);
            gearFound.add(gear);
        }
    }

    /**
     * Takes back part of the Credits earned this level, for a knockout.
     *
     * @return the Credits taken, which land on the floor
     */
    public int spillCredits() {
        int amount = Math.min(creditsEarned / 4, profile.getCredits());
        creditsEarned -= amount;
        profile.addCredits(-amount);
        return amount;
    }

    /**
     * Rolls for a gear drop. Bosses always drop an item. The item is not
     * banked until it is picked up.
     *
     * @param type the enemy
     * @return the item found, or null
     */
    public Gear rollDrop(EnemyType type) {
        boolean boss = type.getKind() == EnemyType.Kind.BOSS;
        if (!boss && rng.nextDouble() >= DROP_CHANCE) {
            return null;
        }
        int best = Gear.bestTierAt(profile.getMaxUnlocked());
        List<Gear> options = new ArrayList<>();
        for (Gear g : Gear.values()) {
            if (g.getTier() <= best && (!boss || g.getTier() == best)) {
                options.add(g);
            }
        }
        return options.get(rng.nextInt(options.size()));
    }

    /**
     * Gets the sheet of the classmate the player steers.
     *
     * @return the main character's sheet
     */
    public Hero mainHero() {
        return profile.hero(profile.getMain());
    }

    /**
     * Gets the party members.
     *
     * @return classmates in the level
     */
    public List<DefenderType> getMembers() {
        return members;
    }

    /**
     * Gets the items found so far.
     *
     * @return items in the order found
     */
    public List<Gear> getGearFound() {
        return gearFound;
    }

    /**
     * Gets every level-up so far.
     *
     * @return one entry per level-up event
     */
    public List<Hero> getLevelUps() {
        return levelUps;
    }

    /**
     * Gets the Credits earned so far.
     *
     * @return Credits
     */
    public int getCreditsEarned() {
        return creditsEarned;
    }

    /**
     * Gets the EXP each member earned so far, before any overlevel penalty.
     *
     * @return EXP
     */
    public int getExpEarned() {
        return expEarned;
    }
}
