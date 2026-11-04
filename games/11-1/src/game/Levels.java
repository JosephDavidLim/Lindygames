package game;

import entities.DefenderType;
import entities.EnemyType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The eighteen periods of the climb through the Institute: six floors of three
 * each, and the third on every floor holds its boss. Enemy levels rise with the squad.
 */
public final class Levels {
    private static final int[] ONE_LANE = {2};
    private static final int[] THREE_LANES = {1, 2, 3};
    private static final int[] ALL_LANES = {0, 1, 2, 3, 4};

    private static final List<Level> ALL = new ArrayList<>();

    static {
        EnemyType hw = EnemyType.HOMEWORK_SHEET;
        EnemyType pencil = EnemyType.SHARP_PENCIL;
        EnemyType card = EnemyType.REPORT_CARD;
        EnemyType plane = EnemyType.PAPER_AIRPLANE;
        EnemyType eraser = EnemyType.ERASER;
        EnemyType swarm = EnemyType.SCANTRON_SWARM;
        EnemyType fee = EnemyType.APPLICATION_FEE;
        EnemyType essay = EnemyType.PERSONAL_ESSAY;
        EnemyType reject = EnemyType.REJECTION_LETTER;
        EnemyType midterm = EnemyType.MIDTERM_EXAM;
        EnemyType deadline = EnemyType.THE_DEADLINE;
        EnemyType monitor = EnemyType.HALL_MONITOR;
        EnemyType meat = EnemyType.MYSTERY_MEAT;
        EnemyType notice = EnemyType.OVERDUE_NOTICE;
        EnemyType ball = EnemyType.DODGEBALL;

        add(GameMap.CLASSROOM, 1, "Day 1,204", ONE_LANE, 3, 2, 1, enemies(hw), null, null, DefenderType.AARAV);
        add(GameMap.CLASSROOM, 1, "Homework Pile", THREE_LANES, 4, 4, 1, enemies(hw), null, null,
                DefenderType.IMRAAN);
        add(GameMap.CLASSROOM, 2, "The Substitute", ALL_LANES, 5, 10, 3, enemies(hw, pencil), EnemyType.THE_SUBSTITUTE, null, DefenderType.SAM);

        String[] hall1 = {"....L....", "......L..", "..L......", "......L..", "....L...."};
        String[] hall2 = {"..L...L..", "....L....", "..L...L..", "....L....", "..L...L.."};
        String[] hall3 = {"...L.L...", "...L.L...", ".........", "...L.L...", "...L.L..."};
        add(GameMap.HALLWAY, 3, "Locker Jam", ALL_LANES, 5, 12, 4, enemies(hw, pencil, monitor), null, hall1,
                DefenderType.AUSTIN);
        add(GameMap.HALLWAY, 4, "Report Card Day", ALL_LANES, 5, 15, 5, enemies(pencil, card, monitor), null, hall2,
                DefenderType.KAI);
        add(GameMap.HALLWAY, 5, "The Vice Principal", ALL_LANES, 6, 18, 5, enemies(pencil, card, monitor), EnemyType.VICE_PRINCIPAL, hall3,
                DefenderType.ABHINAV);

        String[] cafe1 = {".....MM..", "...MM....", ".....MM..", "...MM....", ".....MM.."};
        String[] cafe2 = {"....MMMM.", ".........", "....MMMM.", ".........", "....MMMM."};
        String[] cafe3 = {"...MMMMMM", "...M..M..", "...MMMMMM", "...M..M..", "...MMMMMM"};
        add(GameMap.CAFETERIA, 6, "Lunch Rush", ALL_LANES, 6, 20, 6, enemies(pencil, card, plane, meat), null, cafe1,
                DefenderType.MATT);
        add(GameMap.CAFETERIA, 7, "Food Fight", ALL_LANES, 6, 23, 6, enemies(pencil, card, plane, meat), null, cafe2,
                DefenderType.JOSEPH);
        add(GameMap.CAFETERIA, 8, "The Lunch Lady", ALL_LANES, 7, 24, 6, enemies(pencil, plane, eraser, meat), EnemyType.LUNCH_LADY,
                cafe3, DefenderType.NIK);

        String[] lib1 = {"....CCCC.", "....CCCC.", ".........", "....CCCC.", "....CCCC."};
        String[] lib2 = {"...CC.CC.", "...CC.CC.", "...CC.CC.", "...CC.CC.", "...CC.CC."};
        String[] lib3 = {"...CCCCCC", ".........", "...CCCCCC", ".........", "...CCCCCC"};
        add(GameMap.LIBRARY, 9, "Quiet Hours", ALL_LANES, 7, 25, 6, enemies(card, eraser, swarm, notice), null, lib1,
                DefenderType.CAMILO);
        add(GameMap.LIBRARY, 10, "Finals Week", ALL_LANES, 7, 26, 7, enemies(card, eraser, swarm, notice), midterm,
                lib2, DefenderType.JUSTIN);
        add(GameMap.LIBRARY, 11, "The Librarian", ALL_LANES, 7, 27, 7, enemies(card, eraser, swarm, notice), EnemyType.LIBRARIAN,
                lib3, DefenderType.HANBO);

        String[] gym1 = {".HH......", ".........", ".HH......", ".........", ".HH......"};
        String[] gym2 = {"..H...H..", "..H...H..", ".........", "..H...H..", "..H...H.."};
        String[] gym3 = {"H.H.H.H..", ".........", "H.H.H.H..", ".........", "H.H.H.H.."};
        add(GameMap.GYM, 12, "Pep Rally", ALL_LANES, 7, 24, 6, enemies(card, eraser, swarm, ball), null, gym1,
                DefenderType.TUHIN);
        add(GameMap.GYM, 13, "Dodgeball", ALL_LANES, 7, 23, 6, enemies(card, plane, swarm, ball), null, gym2,
                DefenderType.JAMES);
        add(GameMap.GYM, 14, "The Coach", ALL_LANES, 8, 25, 7, enemies(card, plane, ball, fee, essay), EnemyType.COACH, gym3,
                DefenderType.ARNAV);

        String[] adm1 = {"..H..L.MM", "....CC...", "..H..L.MM", "....CC...", "..H..L.MM"};
        String[] adm2 = {".H.L..CC.", "...MM....", ".H.L..CC.", "...MM....", ".H.L..CC."};
        String[] adm3 = {".H..L.MMM", "...CC....", ".H..L.MMM", "...CC....", ".H..L.MMM"};
        add(GameMap.ADMISSIONS, 15, "Application Fees", ALL_LANES, 8, 28, 7,
                enemies(card, plane, eraser, swarm, fee, essay), null, adm1, DefenderType.TANISH);
        add(GameMap.ADMISSIONS, 16, "Essay Season", ALL_LANES, 8, 29, 8,
                enemies(card, plane, eraser, swarm, fee, essay, reject), null, adm2);
        add(GameMap.ADMISSIONS, 17, "The Deadline", ALL_LANES, 9, 25, 6,
                enemies(card, plane, eraser, swarm, fee, essay, reject), deadline, adm3);
    }

    private Levels() {
    }

    private static List<EnemyType> enemies(EnemyType... types) {
        return Arrays.asList(types);
    }

    private static void add(GameMap map, int enemyLevel, String name, int[] lanes, int waves, int base,
                            int growth, List<EnemyType> allowed, EnemyType boss, String[] layout,
                            DefenderType... unlocks) {
        ALL.add(new Level(ALL.size() + 1, name, lanes, waves, base, growth, allowed, boss,
                Arrays.asList(unlocks), map, enemyLevel, layout));
    }

    /** Card slots the player starts with. */
    public static final int BASE_SLOTS = 6;
    /** Most card slots the player can earn. */
    public static final int MAX_SLOTS = 8;

    /**
     * Gets how many cards the player may bring into a level once a given
     * level has been cleared. A 7th slot is earned by clearing level 6 (the
     * Vice Principal) and an 8th by clearing level 12 (the Librarian).
     *
     * @param highestCleared the highest level cleared so far, 0 for none
     * @return number of card slots
     */
    public static int slotsAfterClearing(int highestCleared) {
        return BASE_SLOTS + (highestCleared >= 6 ? 1 : 0) + (highestCleared >= 12 ? 1 : 0);
    }

    /**
     * Gets one level.
     *
     * @param number level number from 1 to {@link #count()}
     * @return the level
     */
    public static Level get(int number) {
        return ALL.get(number - 1);
    }

    /**
     * Gets how many levels exist.
     *
     * @return level count
     */
    public static int count() {
        return ALL.size();
    }

    /**
     * Gets every defender the player owns when starting a level: everything
     * unlocked by the levels before it, plus the first-level starter.
     *
     * @param levelNumber the level about to be played
     * @return unlocked defender types in unlock order
     */
    public static List<DefenderType> unlockedBefore(int levelNumber) {
        List<DefenderType> result = new ArrayList<>();
        result.add(DefenderType.LUIS);
        for (int i = 1; i < levelNumber; i++) {
            for (DefenderType t : get(i).getUnlocks()) {
                if (!result.contains(t)) {
                    result.add(t);
                }
            }
        }
        return result;
    }

    /**
     * Gets the level a classmate starts at when joining: the enemy level of
     * the level that unlocks them.
     *
     * @param type the classmate
     * @return starting level, 1 for the starter
     */
    public static int joinLevel(DefenderType type) {
        for (Level l : ALL) {
            if (l.getUnlocks().contains(type)) {
                return l.getEnemyLevel();
            }
        }
        return 1;
    }

    /**
     * Gets the levels on one map.
     *
     * @param map the map
     * @return its levels in order
     */
    public static List<Level> onMap(GameMap map) {
        List<Level> result = new ArrayList<>();
        for (Level l : ALL) {
            if (l.getMap() == map) {
                result.add(l);
            }
        }
        return result;
    }
}
