package game;

import entities.Avatar;
import entities.Bomb;
import entities.Coffee;
import entities.Defender;
import entities.DefenderType;
import entities.Enemy;
import entities.LaneClear;
import entities.Trigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Developer tool: plays levels with a human-like bot and no graphics, to check
 * the game logic and measure difficulty.
 *
 * <p>Usage: no arguments plays full campaigns over many seeds and prints a
 * difficulty table; one argument (a level number) plays that level once on a
 * new profile and prints progress.
 */
public class HeadlessDemo {
    private static final int MAX_TICKS = Board.seconds(60 * 12);
    private static final int SEEDS = 30;
    private static final int MAX_TRIES = 3;

    /** What happened in one simulated game. */
    private static final class Result {
        boolean won;
        int ticks;
        int lost;
        int highlighters;
        int peak;
    }

    /**
     * Plays a whole campaign per seed with a profile that carries over, and
     * prints a difficulty table. With one argument, plays that level once on a
     * new profile and prints progress.
     *
     * @param args optional level number
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            int n = Integer.parseInt(args[0]);
            Profile p = new Profile();
            p.levelCleared(n - 1);
            Result r = play(n, 1, p, true);
            System.out.printf("%s in %ds, lost %d defenders, %d highlighters used%n",
                    r.won ? "WON" : "LOST", r.ticks / 30, r.lost, r.highlighters);
            return;
        }
        int count = Levels.count();
        int[] firstTry = new int[count + 1];
        int[] tries = new int[count + 1];
        int[] reached = new int[count + 1];
        double[] time = new double[count + 1];
        double[] peak = new double[count + 1];
        double[] heroLevel = new double[count + 1];
        double[] credits = new double[count + 1];
        for (int seed = 1; seed <= SEEDS; seed++) {
            Profile p = new Profile();
            for (int n = 1; n <= count; n++) {
                reached[n]++;
                train(p, n);
                heroLevel[n] += averageLevel(p, n);
                credits[n] += p.getCredits();
                boolean won = false;
                for (int attempt = 1; attempt <= MAX_TRIES && !won; attempt++) {
                    Result r = play(n, seed * 100 + attempt, p, false);
                    won = r.won;
                    tries[n]++;
                    time[n] += r.ticks / 30.0;
                    peak[n] += r.peak;
                    if (attempt == 1 && won) {
                        firstTry[n]++;
                    }
                    train(p, n);
                }
                if (!won) {
                    break;
                }
                p.levelCleared(n);
            }
        }
        System.out.printf("%-3s %-17s %-10s %4s %6s %6s %5s %5s %6s %6s%n", "Lv", "Name", "Map", "mob", "first%",
                "tries", "time", "peak", "herLv", "credit");
        for (int n = 1; n <= count; n++) {
            Level l = Levels.get(n);
            int runs = Math.max(1, reached[n]);
            int plays = Math.max(1, tries[n]);
            System.out.printf("%-3d %-17s %-10.10s %4d %5d%% %6.2f %4.0fs %5.1f %6.1f %6.0f   (%d/%d runs reached)%n",
                    n, l.getName(), l.getMap().getDisplayName(), l.getEnemyLevel(), firstTry[n] * 100 / runs,
                    (double) tries[n] / runs, time[n] / plays, peak[n] / plays, heroLevel[n] / runs,
                    credits[n] / runs, reached[n], SEEDS);
        }
    }

    /** Spends points and buys the best gear it can, as a sensible player would between levels. */
    private static void train(Profile p, int levelNumber) {
        if (Boolean.getBoolean("notrain")) {
            return;
        }
        List<DefenderType> squad = Bot.pickLoadout(Levels.unlockedBefore(levelNumber),
                Levels.slotsAfterClearing(levelNumber - 1));
        for (DefenderType t : squad) {
            Hero h = p.hero(t);
            h.autoAssign();
            if (h.canAdvance()) {
                h.advance(Job.optionsFor(t)[0]);
            }
            for (Gear.Slot slot : Gear.Slot.values()) {
                Gear best = null;
                for (Gear g : Gear.values()) {
                    boolean better = g.getSlot() == slot
                            && (h.getEquipped(slot) == null || g.getTier() > h.getEquipped(slot).getTier());
                    if (better && (p.countOf(g) > 0 || (p.getCredits() >= g.getPrice()
                            && g.getTier() <= Gear.bestTierAt(p.getMaxUnlocked())))) {
                        best = g;
                    }
                }
                if (best != null && (p.countOf(best) > 0 || p.buy(best))) {
                    p.equip(h, best);
                }
            }
        }
    }

    private static double averageLevel(Profile p, int levelNumber) {
        List<DefenderType> squad = Bot.pickLoadout(Levels.unlockedBefore(levelNumber),
                Levels.slotsAfterClearing(levelNumber - 1));
        double sum = 0;
        for (DefenderType t : squad) {
            sum += p.hero(t).getLevel();
        }
        return sum / squad.size();
    }

    private static Result play(int levelNumber, int seed, Profile profile, boolean verbose) {
        Level level = Levels.get(levelNumber);
        int slots = Levels.slotsAfterClearing(levelNumber - 1);
        List<DefenderType> loadout = Bot.pickLoadout(Levels.unlockedBefore(levelNumber), slots);
        Random rng = new Random(seed);
        Party party = new Party(profile, loadout, rng);
        Board board = new Board(level, rng, party);
        Shop shop = new Shop(loadout, slots, party);
        Bot bot = new Bot(board, shop);
        Result r = new Result();
        Defender[][] previous = new Defender[Board.ROWS][Board.COLS];

        while (!board.isWon() && !board.isLost() && board.getTick() < MAX_TICKS) {
            bot.step();
            bot.driveAvatar();
            shop.update();
            board.update();
            board.drainPopups();
            board.drainLevelUps();
            for (int row = 0; row < Board.ROWS; row++) {
                for (int col = 0; col < Board.COLS; col++) {
                    Defender prev = previous[row][col];
                    Defender now = board.defenderAt(row, col);
                    if (prev != null && now != prev && prev.isDead() && !(prev instanceof Bomb)
                            && !(prev instanceof Trigger) && !(prev instanceof LaneClear)) {
                        r.lost++;
                    }
                    previous[row][col] = now;
                }
            }
            r.peak = Math.max(r.peak, board.getEnemies().size());
            if (verbose && board.getTick() % Board.seconds(10) == 0) {
                System.out.printf("t=%3ds FP=%4d enemies=%2d lost=%d%n", board.getTick() / 30,
                        board.getFocusPoints(), board.getEnemies().size(), r.lost);
            }
        }
        r.won = board.isWon();
        r.ticks = board.getTick();
        for (int row = 0; row < Board.ROWS; row++) {
            if (board.isHighlighterUsed(row)) {
                r.highlighters++;
            }
        }
        return r;
    }

    /**
     * A bot that plays like a reasonable human: about one click every 0.6
     * seconds, collects coffees after noticing them, builds generators first,
     * and answers threats lane by lane.
     */
    private static final class Bot {
        private static final int ACTION_GAP = 18;
        private static final int NOTICE_TICKS = 40;

        private final Board board;
        private final Shop shop;
        private final Map<Coffee, Integer> seen = new HashMap<>();
        private int cooldown;

        Bot(Board board, Shop shop) {
            this.board = board;
            this.shop = shop;
        }

        static List<DefenderType> pickLoadout(List<DefenderType> owned, int slots) {
            List<DefenderType> out = new ArrayList<>();
            DefenderType[][] roles = {
                {DefenderType.AARAV},
                {DefenderType.HANBO, DefenderType.ABHINAV, DefenderType.LUIS},
                {DefenderType.AUSTIN, DefenderType.MATT},
                {DefenderType.NIK, DefenderType.TANISH},
                {DefenderType.TUHIN, DefenderType.IMRAAN, DefenderType.KAI},
                {DefenderType.SAM},
                {DefenderType.JUSTIN, DefenderType.ARNAV, DefenderType.CAMILO, DefenderType.JAMES, DefenderType.JOSEPH}
            };
            for (DefenderType[] group : roles) {
                for (DefenderType t : group) {
                    if (owned.contains(t) && !out.contains(t)) {
                        out.add(t);
                        break;
                    }
                }
            }
            for (DefenderType t : owned) {
                if (out.size() < slots && !out.contains(t)) {
                    out.add(t);
                }
            }
            return out;
        }

        void step() {
            if (cooldown > 0) {
                cooldown--;
                return;
            }
            if (collectCoffee() || build()) {
                cooldown = ACTION_GAP;
            }
        }

        /** Runs to the most advanced enemy and swings, or fetches drops when things are calm. */
        void driveAvatar() {
            Avatar a = board.getAvatar();
            Enemy target = null;
            for (Enemy e : board.getEnemies()) {
                if (e.getX() < Board.COLS && (target == null || e.getX() < target.getX())) {
                    target = e;
                }
            }
            double goalX = a.getX();
            int goalRow = a.getRow();
            if (target != null) {
                goalX = target.getX() - 0.9;
                goalRow = target.getRow();
            } else if (!board.getDrops().isEmpty()) {
                goalX = board.getDrops().get(0).getX();
                goalRow = board.getDrops().get(0).getRow();
            }
            boolean jump = false;
            int vertical = 0;
            if (a.isClimbing()) {
                vertical = -1;
            } else if (goalRow > a.getRow() && a.getHeight() == 0) {
                vertical = 1;
                jump = true;
            } else if (goalRow < a.getRow()) {
                goalX = Avatar.ladderX(a.getRow());
                if (Math.abs(goalX - a.getX()) <= 0.3) {
                    vertical = -1;
                }
            }
            int move = Math.abs(goalX - a.getX()) < 0.15 ? 0 : goalX > a.getX() ? 1 : -1;
            a.command(move, vertical, target != null && goalRow == a.getRow(), jump);
        }

        private boolean collectCoffee() {
            seen.keySet().retainAll(board.getCoffees());
            for (Coffee c : board.getCoffees()) {
                int first = seen.computeIfAbsent(c, k -> board.getTick());
                if (board.getTick() - first >= NOTICE_TICKS) {
                    board.collectCoffee(c);
                    return true;
                }
            }
            return false;
        }

        private boolean build() {
            int[] lanes = board.getLevel().getLanes();
            int gens = count(DefenderType.Kind.GENERATOR, -1);
            boolean threat = false;
            for (int row : lanes) {
                if (nearest(row) < 6.5) {
                    threat = true;
                }
            }
            if (!threat && gens < Math.min(4, lanes.length + 1) && placeGenerator()) {
                return true;
            }
            // lanes sorted by how close the nearest enemy is
            List<Integer> order = new ArrayList<>();
            for (int row : lanes) {
                order.add(row);
            }
            order.sort((a, b) -> Double.compare(nearest(a), nearest(b)));

            for (int row : order) {
                double near = nearest(row);
                if (near < 9 && count(DefenderType.Kind.SHOOTER, row) == 0 && placeShooter(row)) {
                    return true;
                }
            }
            if (bomb()) {
                return true;
            }
            for (int row : order) {
                int foes = countEnemies(row, 6);
                if (foes >= 4 && placeSpecial(DefenderType.ARNAV, row)) {
                    return true;
                }
            }
            for (int row : order) {
                double near = nearest(row);
                if (near < 7 && count(DefenderType.Kind.WALL, row) == 0 && placeWall(row)) {
                    return true;
                }
            }
            for (int row : order) {
                if (count(DefenderType.Kind.SHOOTER, row) == 0 && placeShooter(row)) {
                    return true;
                }
            }
            for (DefenderType.Kind ranged : new DefenderType.Kind[] {DefenderType.Kind.SNIPER}) {
                if (count(ranged, -1) < 2 && board.getFocusPoints() > 200) {
                    for (int row : order) {
                        if (board.defenderAt(row, 0) == null && tryPlace(ranged, row, 0)) {
                            return true;
                        }
                    }
                }
            }
            for (int row : order) {
                if (nearest(row) < 7 && count(DefenderType.Kind.POINT_BLANK, row) == 0) {
                    for (int col : new int[] {4, 3}) {
                        if (board.defenderAt(row, col) == null && tryPlace(DefenderType.Kind.POINT_BLANK, row, col)) {
                            return true;
                        }
                    }
                }
            }
            if (gens < 6 && board.getFocusPoints() > 150 && placeGenerator()) {
                return true;
            }
            for (int row : order) {
                if (count(DefenderType.Kind.SHOOTER, row) < 3 && board.getFocusPoints() > 220
                        && placeShooter(row)) {
                    return true;
                }
            }
            for (int row : order) {
                if (board.getFocusPoints() > 120 && placeMineOrTrap(row)) {
                    return true;
                }
            }
            return false;
        }

        private boolean placeGenerator() {
            int[] lanes = board.getLevel().getLanes();
            for (int col = 0; col <= 1; col++) {
                for (int row : lanes) {
                    if (board.defenderAt(row, col) == null && tryPlace(DefenderType.Kind.GENERATOR, row, col)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private boolean placeShooter(int row) {
            for (int col : new int[] {2, 3, 1, 4}) {
                if (board.defenderAt(row, col) == null && tryPlace(DefenderType.Kind.SHOOTER, row, col)) {
                    return true;
                }
            }
            return false;
        }

        private boolean placeWall(int row) {
            for (int col : new int[] {5, 4, 6}) {
                if (board.defenderAt(row, col) == null && tryPlace(DefenderType.Kind.WALL, row, col)) {
                    return true;
                }
            }
            return false;
        }

        private boolean placeMineOrTrap(int row) {
            for (int col : new int[] {7, 6}) {
                if (board.defenderAt(row, col) == null
                        && (tryPlace(DefenderType.Kind.MINE, row, col)
                        || tryPlace(DefenderType.Kind.TRAP, row, col))) {
                    return true;
                }
            }
            return false;
        }

        private boolean placeSpecial(DefenderType type, int row) {
            int idx = indexOf(type);
            if (idx < 0) {
                return false;
            }
            for (int col = 0; col < Board.COLS; col++) {
                if (board.defenderAt(row, col) == null && use(idx, row, col)) {
                    return true;
                }
            }
            return false;
        }

        private boolean bomb() {
            int idx = indexOf(DefenderType.SAM);
            if (idx < 0) {
                return false;
            }
            for (int row : board.getLevel().getLanes()) {
                for (int col = 2; col < Board.COLS - 1; col++) {
                    if (board.defenderAt(row, col) == null && clustered(row, col) >= 3 && use(idx, row, col)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private int clustered(int row, int col) {
            int n = 0;
            for (Enemy e : board.getEnemies()) {
                if (Math.abs(e.getRow() - row) <= 1 && Math.abs((int) Math.floor(e.getX()) - col) <= 1) {
                    n++;
                }
            }
            return n;
        }

        private boolean tryPlace(DefenderType.Kind kind, int row, int col) {
            List<Card> cards = shop.getCards();
            int best = -1;
            for (int i = 0; i < cards.size(); i++) {
                DefenderType t = cards.get(i).getType();
                if (t.role() == kind && shop.isUsable(i, board.getFocusPoints())
                        && (best < 0 || t.getCost() > cards.get(best).getType().getCost())) {
                    best = i;
                }
            }
            return best >= 0 && use(best, row, col);
        }

        private boolean use(int idx, int row, int col) {
            if (!shop.isUsable(idx, board.getFocusPoints())) {
                return false;
            }
            shop.select(idx);
            boolean ok = shop.tryPlace(board, row, col);
            if (!ok) {
                shop.select(-1);
            }
            return ok;
        }

        private int indexOf(DefenderType type) {
            for (int i = 0; i < shop.getCards().size(); i++) {
                if (shop.getCards().get(i).getType() == type) {
                    return i;
                }
            }
            return -1;
        }

        private int count(DefenderType.Kind kind, int row) {
            int n = 0;
            for (int r = 0; r < Board.ROWS; r++) {
                if (row >= 0 && r != row) {
                    continue;
                }
                for (int c = 0; c < Board.COLS; c++) {
                    Defender d = board.defenderAt(r, c);
                    if (d != null && d.getType().role() == kind) {
                        n++;
                    }
                }
            }
            return n;
        }

        private double nearest(int row) {
            double best = 99;
            for (Enemy e : board.getEnemies()) {
                if (e.getRow() == row && e.getX() < best) {
                    best = e.getX();
                }
            }
            return best;
        }

        private int countEnemies(int row, double maxX) {
            int n = 0;
            for (Enemy e : board.getEnemies()) {
                if (e.getRow() == row && e.getX() < maxX) {
                    n++;
                }
            }
            return n;
        }
    }
}
