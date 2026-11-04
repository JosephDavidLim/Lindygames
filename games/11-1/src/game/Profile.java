package game;

import entities.DefenderType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the player keeps between levels: unlocked levels, Credits, the
 * inventory and each classmate's {@link Hero} sheet. It saves to a small text
 * file and still reads the older save that held only a level number.
 */
public class Profile {
    private static final int FIRST_ATTEMPT = 7;

    private final Map<DefenderType, Hero> heroes = new EnumMap<>(DefenderType.class);
    private final Map<Gear, Integer> inventory = new EnumMap<>(Gear.class);
    private int maxUnlocked = 1;
    private int credits;
    private DefenderType main = DefenderType.LUIS;
    private final QuestLog quests = new QuestLog();
    private int attempt = FIRST_ATTEMPT;
    private boolean introSeen;

    /**
     * Gets a classmate's sheet, creating it on first use. New classmates join
     * at the enemy level of the map that unlocks them, so late arrivals are useful.
     *
     * @param type the classmate
     * @return the sheet
     */
    public Hero hero(DefenderType type) {
        return heroes.computeIfAbsent(type, t -> new Hero(t, Levels.joinLevel(t)));
    }

    /**
     * Buys one item.
     *
     * @param gear the item
     * @return true if the player could afford it and the store sells it yet
     */
    public boolean buy(Gear gear) {
        if (credits < gear.getPrice() || gear.getTier() > Gear.bestTierAt(maxUnlocked)) {
            return false;
        }
        credits -= gear.getPrice();
        addGear(gear);
        return true;
    }

    /**
     * Sells one spare copy of an item for a third of its price.
     *
     * @param gear the item
     * @return true if a spare copy was sold
     */
    public boolean sell(Gear gear) {
        if (countOf(gear) <= 0) {
            return false;
        }
        inventory.merge(gear, -1, Integer::sum);
        credits += gear.getPrice() / 3;
        return true;
    }

    /**
     * Spends every unlocked classmate's free points using each one's suggested split.
     */
    public void autoAssignAll() {
        for (DefenderType t : Levels.unlockedBefore(maxUnlocked)) {
            hero(t).autoAssign();
        }
    }

    /**
     * Adds an item to the inventory.
     *
     * @param gear the item
     */
    public void addGear(Gear gear) {
        inventory.merge(gear, 1, Integer::sum);
    }

    /**
     * Gets how many unequipped copies of an item the player holds.
     *
     * @param gear the item
     * @return the count
     */
    public int countOf(Gear gear) {
        return inventory.getOrDefault(gear, 0);
    }

    /**
     * Equips an item from the inventory, returning whatever was worn to it.
     *
     * @param hero who wears it
     * @param gear the item, which must be in the inventory
     * @return true if it was equipped
     */
    public boolean equip(Hero hero, Gear gear) {
        if (countOf(gear) <= 0) {
            return false;
        }
        unequip(hero, gear.getSlot());
        inventory.merge(gear, -1, Integer::sum);
        hero.setEquipped(gear.getSlot(), gear);
        return true;
    }

    /**
     * Moves the item in a slot back to the inventory.
     *
     * @param hero who wears it
     * @param slot the slot
     */
    public void unequip(Hero hero, Gear.Slot slot) {
        Gear worn = hero.getEquipped(slot);
        if (worn != null) {
            addGear(worn);
            hero.setEquipped(slot, null);
        }
    }

    /**
     * Swaps a slot to the next item the player holds for it, then to empty.
     *
     * @param hero who wears it
     * @param slot the slot
     */
    public void cycleSlot(Hero hero, Gear.Slot slot) {
        List<Gear> options = new ArrayList<>();
        for (Gear g : Gear.values()) {
            if (g.getSlot() == slot) {
                options.add(g);
            }
        }
        Gear worn = hero.getEquipped(slot);
        int start = worn == null ? 0 : options.indexOf(worn) + 1;
        for (int i = start; i < options.size(); i++) {
            if (countOf(options.get(i)) > 0) {
                equip(hero, options.get(i));
                return;
            }
        }
        unequip(hero, slot);
    }

    /**
     * Takes back a classmate's stat points for a fee.
     *
     * @param hero the classmate
     * @return true if the player could afford it
     */
    public boolean resetStats(Hero hero) {
        int cost = hero.getLevel() * Hero.RESET_COST_PER_LEVEL;
        if (credits < cost) {
            return false;
        }
        credits -= cost;
        hero.resetStats();
        return true;
    }

    /**
     * Tells whether any unlocked classmate has stat points to spend.
     *
     * @return true if points are waiting
     */
    public boolean hasUnspentAp() {
        for (DefenderType t : Levels.unlockedBefore(maxUnlocked)) {
            if (hero(t).getAp() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Tells whether anything is waiting in the Squad window: points to spend or a job to choose.
     *
     * @return true if the player should visit it
     */
    public boolean needsSquadVisit() {
        for (DefenderType t : Levels.unlockedBefore(maxUnlocked)) {
            if (hero(t).getAp() > 0 || hero(t).canAdvance()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes the profile to a file.
     *
     * @param file the save file
     */
    public void save(File file) {
        List<String> lines = new ArrayList<>();
        lines.add("v2");
        lines.add("unlocked " + maxUnlocked);
        lines.add("credits " + credits);
        lines.add("main " + main.name());
        if (introSeen) {
            lines.add("intro");
        }
        lines.add("attempt " + attempt);
        for (Quest q : Quest.values()) {
            if (quests.stateOf(q) != QuestLog.State.NEW) {
                lines.add("quest " + q.name() + " " + quests.stateOf(q).name() + " " + quests.progressOf(q));
            }
        }
        for (Hero h : heroes.values()) {
            StringBuilder s = new StringBuilder("hero " + h.getType().name() + " " + h.getLevel()
                    + " " + h.getExp() + " " + h.getAp());
            for (Stat stat : Stat.values()) {
                s.append(' ').append(h.getSpent(stat));
            }
            for (Gear.Slot slot : Gear.Slot.values()) {
                Gear g = h.getEquipped(slot);
                s.append(' ').append(g == null ? "-" : g.name());
            }
            s.append(' ').append(h.getJob() == null ? "-" : h.getJob().name());
            lines.add(s.toString());
        }
        for (Map.Entry<Gear, Integer> e : inventory.entrySet()) {
            if (e.getValue() > 0) {
                lines.add("gear " + e.getKey().name() + " " + e.getValue());
            }
        }
        try {
            Files.write(file.toPath(), lines);
        } catch (IOException e) {
            System.err.println("Could not save progress: " + e.getMessage());
        }
    }

    /**
     * Reads a profile. A missing or damaged file gives a new or partial
     * profile instead of an error.
     *
     * @param file the save file
     * @return the profile
     */
    public static Profile load(File file) {
        Profile p = new Profile();
        if (!file.exists()) {
            return p;
        }
        List<String> lines;
        try {
            lines = Files.readAllLines(file.toPath());
        } catch (IOException e) {
            return p;
        }
        for (String line : lines) {
            try {
                p.readLine(line.trim().split("\\s+"));
            } catch (IllegalArgumentException | IndexOutOfBoundsException e) {
                System.err.println("Skipped a damaged save line: " + line);
            }
        }
        return p;
    }

    private void readLine(String[] w) {
        if (w.length == 1 && w[0].matches("\\d+")) {
            setMaxUnlocked(Integer.parseInt(w[0]));
        } else if ("unlocked".equals(w[0])) {
            setMaxUnlocked(Integer.parseInt(w[1]));
        } else if ("credits".equals(w[0])) {
            credits = Math.max(0, Integer.parseInt(w[1]));
        } else if ("main".equals(w[0])) {
            main = DefenderType.valueOf(w[1]);
        } else if ("intro".equals(w[0])) {
            introSeen = true;
        } else if ("attempt".equals(w[0])) {
            attempt = Math.max(FIRST_ATTEMPT, Integer.parseInt(w[1]));
        } else if ("quest".equals(w[0])) {
            quests.restore(Quest.valueOf(w[1]), QuestLog.State.valueOf(w[2]), Integer.parseInt(w[3]));
        } else if ("gear".equals(w[0])) {
            inventory.put(Gear.valueOf(w[1]), Math.max(0, Integer.parseInt(w[2])));
        } else if ("hero".equals(w[0])) {
            readHero(w);
        }
    }

    private void readHero(String[] w) {
        Stat[] all = Stat.values();
        Gear.Slot[] slots = Gear.Slot.values();
        DefenderType type = DefenderType.valueOf(w[1]);
        Hero h = new Hero(type, Integer.parseInt(w[2]));
        int[] spent = new int[all.length];
        for (int i = 0; i < all.length; i++) {
            spent[i] = Integer.parseInt(w[5 + i]);
        }
        h.restore(Integer.parseInt(w[3]), Integer.parseInt(w[4]), spent);
        for (int i = 0; i < slots.length; i++) {
            String name = w[5 + all.length + i];
            if (!"-".equals(name)) {
                h.setEquipped(slots[i], Gear.valueOf(name));
            }
        }
        int jobAt = 5 + all.length + slots.length;
        if (w.length > jobAt && !"-".equals(w[jobAt])) {
            h.restoreJob(Job.valueOf(w[jobAt]));
        }
        heroes.put(type, h);
    }

    private void setMaxUnlocked(int n) {
        maxUnlocked = Math.max(1, Math.min(Levels.count(), n));
    }

    /**
     * Records a cleared level.
     *
     * @param level the level cleared
     * @return true if this was its first clear
     */
    public boolean levelCleared(int level) {
        boolean first = level >= maxUnlocked;
        if (first && level < Levels.count()) {
            maxUnlocked = level + 1;
        }
        return first;
    }

    /**
     * Gets the quest log.
     *
     * @return the log
     */
    public QuestLog getQuests() {
        return quests;
    }

    /**
     * Collects a finished quest's reward.
     *
     * @param q the quest
     * @return true if a reward was waiting
     */
    public boolean claim(Quest q) {
        if (!quests.markClaimed(q)) {
            return false;
        }
        credits += q.getCredits();
        if (q.getGear() != null) {
            addGear(q.getGear());
        }
        return true;
    }

    /**
     * Gets the quests the player can see now: unlocked by progress, given by
     * a classmate they have met, and not yet finished with.
     *
     * @return quests in order
     */
    public List<Quest> visibleQuests() {
        List<DefenderType> met = Levels.unlockedBefore(maxUnlocked);
        List<Quest> result = new ArrayList<>();
        for (Quest q : Quest.values()) {
            if (q.getMinLevel() <= maxUnlocked && met.contains(q.getNpc())
                    && quests.stateOf(q) != QuestLog.State.CLAIMED) {
                result.add(q);
            }
        }
        return result;
    }

    /**
     * Tells whether a quest's reward is waiting.
     *
     * @return true if any quest is ready to collect
     */
    public boolean hasQuestReady() {
        for (Quest q : Quest.values()) {
            if (quests.stateOf(q) == QuestLog.State.READY) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets which time around this is. The story knows, even if the student doesn't.
     *
     * @return the attempt number
     */
    public int getAttempt() {
        return attempt;
    }

    /**
     * Begins the next attempt, after the ending.
     */
    public void nextAttempt() {
        attempt++;
    }

    /**
     * Tells whether the opening story has been shown.
     *
     * @return true after the first time
     */
    public boolean isIntroSeen() {
        return introSeen;
    }

    /**
     * Records that the opening story was shown.
     */
    public void markIntroSeen() {
        introSeen = true;
    }

    /**
     * Tells whether the player holds a key item.
     *
     * @param item the item
     * @return true once its boss is beaten
     */
    public boolean has(KeyItem item) {
        return item.isOwned(maxUnlocked);
    }

    /**
     * Gets the classmate the player steers.
     *
     * @return the main character
     */
    public DefenderType getMain() {
        return main;
    }

    /**
     * Chooses the classmate the player steers.
     *
     * @param type the new main character
     */
    public void setMain(DefenderType type) {
        main = type;
    }

    /**
     * Adds Credits.
     *
     * @param amount Credits earned
     */
    public void addCredits(int amount) {
        credits += amount;
    }

    /**
     * Gets the player's Credits.
     *
     * @return Credits
     */
    public int getCredits() {
        return credits;
    }

    /**
     * Gets the highest level the player may start.
     *
     * @return level number
     */
    public int getMaxUnlocked() {
        return maxUnlocked;
    }
}
