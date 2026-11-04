package view;

import acm.graphics.GCanvas;
import acm.graphics.GCompound;
import acm.graphics.GFillable;
import acm.graphics.GLabel;
import acm.graphics.GObject;
import acm.graphics.GOval;
import acm.graphics.GRect;
import acm.graphics.GRoundRect;

import entities.Avatar;
import entities.Bomb;
import entities.Coffee;
import entities.Defender;
import entities.Drop;
import entities.Effect;
import entities.DefenderType;
import entities.Enemy;
import entities.LaneClear;
import entities.Popup;
import entities.Projectile;
import entities.Trigger;
import game.Board;
import game.Card;
import game.GameMap;
import game.Gear;
import game.Hero;
import game.Level;
import game.Levels;
import game.Party;
import game.Terrain;
import game.Shop;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * The map being played: draws the {@link Board}, the shop bar and the status
 * bar. The mouse places classmates, and the keyboard steers the avatar.
 */
public class PlayScreen extends Screen {
    private static final int CARD_X = 96;
    private static final int CARD_H = 78;
    private static final int CARDS_END = 562;
    private static final int END_DELAY_TICKS = 60;
    private static final Color CLOSED = new Color(24, 22, 34, 190);
    private static final int MAX_FLOATERS = 40;
    private static final int FLOATER_LIFE = 28;
    private static final int INTRO_TICKS = 150;
    private static final int LEVEL_UP_TICKS = 75;
    private static final int PARTICLES = 26;
    private static final double PARTY_X = 206;

    private final int levelNumber;
    private final List<DefenderType> loadout;
    private Board board;
    private Shop shop;
    private Party party;
    private StatBar bossBar;
    private GRoundRect bossFlash;
    private Enemy announcedBoss;
    private int shakeTicks;
    private int questsShown;
    private AvatarView avatarView;
    private final Map<Drop, GObject> dropViews = new HashMap<>();
    private final List<Floater> floaters = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final Random fxRandom = new Random();
    private boolean heldLeft;
    private boolean heldRight;
    private boolean heldAttack;
    private boolean heldUp;
    private boolean heldDown;
    private boolean jumpRequest;
    private double mouseX = -1;
    private double mouseY = -1;
    private GRect hoverTile;
    private GObject ghost;
    private int ghostCard = -1;
    private Tooltip tooltip;
    private GCompound introBox;
    private GLabel levelUpLabel;
    private GRoundRect levelUpBack;
    private int levelUpTimer;
    private StatBar hpBar;
    private StatBar expBar;
    private GLabel mainLevel;
    private GLabel creditsLabel;
    private final List<GLabel> partyLevels = new ArrayList<>();
    private final List<GRect> partyExp = new ArrayList<>();
    private List<DefenderType> partyMembers;
    private int slots = Levels.BASE_SLOTS;
    private double cardStep = 78;
    private int cardW = 72;

    private final Map<Defender, DefenderView> defenderViews = new HashMap<>();
    private final Map<Enemy, EnemyView> enemyViews = new HashMap<>();
    private final Map<Projectile, ProjectileView> projectileViews = new HashMap<>();
    private final List<Dying> dying = new ArrayList<>();
    private final Map<Coffee, GObject> coffeeViews = new HashMap<>();
    private final Map<Effect, GObject> effectViews = new HashMap<>();
    private final List<Poof> poofs = new ArrayList<>();
    private int lastShotSound = -99;
    private int lastPopSound = -99;
    private Button soundButton;
    private Button musicButton;

    private final List<GRoundRect> cardSelect = new ArrayList<>();
    private final List<GRoundRect> cardCooldown = new ArrayList<>();
    private final List<GRoundRect> cardDim = new ArrayList<>();
    private final GRect[] highlighters = new GRect[Board.ROWS];
    private GLabel fpLabel;
    private GLabel banner;
    private GRect progressFill;
    private final List<GObject> pauseObjects = new ArrayList<>();
    private boolean paused;
    private int endTimer;

    /**
     * Creates the play screen for a level.
     *
     * @param canvas the canvas to draw on
     * @param nav the navigator
     * @param levelNumber the level to play
     * @param loadout the defenders for the shop bar, at most six
     */
    public PlayScreen(GCanvas canvas, Navigator nav, int levelNumber, List<DefenderType> loadout) {
        super(canvas, nav);
        this.levelNumber = levelNumber;
        this.loadout = loadout;
    }

    @Override
    public void show() {
        Level level = Levels.get(levelNumber);
        Random rng = new Random();
        party = new Party(nav.getProfile(), loadout, rng);
        partyMembers = party.getMembers();
        board = new Board(level, rng, party);
        slots = nav.getSlots();
        shop = new Shop(loadout, slots, party);
        cardStep = Math.min(78.0, (double) (CARDS_END - CARD_X) / slots);
        cardW = (int) (cardStep - 6);
        drawLawn(level);
        drawTopBar(level);
        drawStatusBar();
        addParticles(level.getMap());
        avatarView = new AvatarView(board.getAvatar());
        canvas.add(avatarView);
        tooltip = new Tooltip();
        canvas.add(tooltip);
        drawIntro(level);
        levelUpBack = Ui.round(0, Layout.TOP + 30, 10, 44, 16, new Color(6, 6, 20, 225));
        levelUpBack.setVisible(false);
        canvas.add(levelUpBack);
        levelUpLabel = Ui.centered("", "Serif-bold-26", Ui.GOLD, Layout.W / 2.0, Layout.TOP + 62);
        levelUpLabel.setVisible(false);
        canvas.add(levelUpLabel);
        banner = Ui.centered("", "Serif-bold-34", Ui.GOLD, Layout.W / 2.0, Layout.TOP + 210);
        banner.setVisible(false);
        canvas.add(banner);
        Music.play(level.getMap().getMusic());
        Music.preload("hugewave");
        refresh();
    }

    private void drawLawn(Level level) {
        canvas.add(Assets.mapScene(level.getMap(), Layout.W, Layout.BAR_Y - Layout.TOP), 0, Layout.TOP);
        for (int r = 0; r < Board.ROWS; r++) {
            if (!level.hasLane(r)) {
                canvas.add(Ui.rect(0, Layout.TOP + r * Layout.CELL, Layout.W, Layout.CELL, CLOSED));
                continue;
            }
            for (int c = 0; c < Board.COLS; c++) {
                Terrain t = level.terrainAt(r, c);
                if (t != Terrain.FLOOR) {
                    canvas.add(Assets.terrain(t), c * Layout.CELL, Layout.TOP + r * Layout.CELL);
                }
            }
            if (Avatar.ladderWorks(level, r)) {
                canvas.add(Assets.ladder(), Avatar.ladderX(r) * Layout.CELL - 20, Layout.TOP + r * Layout.CELL - 22);
            }
            highlighters[r] = Ui.rect(1, Layout.TOP + r * Layout.CELL + 14, 12, Layout.CELL - 28,
                    new Color(255, 80, 170));
            canvas.add(highlighters[r]);
        }
        hoverTile = new GRect(Layout.CELL, Layout.CELL);
        hoverTile.setFilled(true);
        hoverTile.setVisible(false);
        canvas.add(hoverTile);
    }

    private void drawStatusBar() {
        canvas.add(Ui.rect(0, Layout.BAR_Y, Layout.W, Layout.BAR_H, Ui.FRAME));
        canvas.add(Ui.rect(0, Layout.BAR_Y, Layout.W, 3, Ui.FRAME_TOP));
        Avatar a = board.getAvatar();
        DefenderType mainType = a.getSheet().getType();
        canvas.add(Assets.defender(mainType, 50), 3, Layout.BAR_Y + 8);
        mainLevel = Ui.text("", "SansSerif-bold-12", Ui.GOLD, 56, Layout.BAR_Y + 18);
        canvas.add(mainLevel);
        hpBar = new StatBar(56, Layout.BAR_Y + 23, 144, 15, Ui.HP_RED);
        expBar = new StatBar(56, Layout.BAR_Y + 42, 144, 15, new Color(190, 110, 255));
        canvas.add(hpBar);
        canvas.add(expBar);
        double memberW = Math.min(50, (Layout.W - PARTY_X - 96) / Math.max(1, partyMembers.size()));
        for (int i = 0; i < partyMembers.size(); i++) {
            DefenderType t = partyMembers.get(i);
            double x = PARTY_X + i * memberW;
            canvas.add(Assets.defender(t, 34), x + (memberW - 34) / 2, Layout.BAR_Y + 4);
            GLabel lv = Ui.centered("", "SansSerif-bold-10", Ui.CHALK, x + memberW / 2, Layout.BAR_Y + 49);
            canvas.add(lv);
            partyLevels.add(lv);
            canvas.add(Ui.rect(x + 3, Layout.BAR_Y + 53, memberW - 6, 5, new Color(6, 6, 18)));
            GRect fill = Ui.rect(x + 3, Layout.BAR_Y + 53, 1, 5, Ui.EXP_YELLOW);
            canvas.add(fill);
            partyExp.add(fill);
        }
        GOval coin = new GOval(Layout.W - 88, Layout.BAR_Y + 12, 16, 16);
        coin.setFilled(true);
        coin.setFillColor(Ui.GOLD);
        coin.setColor(Ui.INK);
        canvas.add(coin);
        creditsLabel = Ui.text("", "SansSerif-bold-14", Ui.GOLD, Layout.W - 68, Layout.BAR_Y + 25);
        canvas.add(creditsLabel);
        canvas.add(Ui.text("Credits", "SansSerif-plain-10", Ui.CHALK, Layout.W - 88, Layout.BAR_Y + 44));
    }

    private void refreshBoss() {
        Enemy boss = board.findBoss();
        if (boss != null) {
            if (boss != announcedBoss) {
                announcedBoss = boss;
                shakeTicks = 18;
                Sfx.play("boom");
                bossFlash = Ui.round(0, Layout.TOP, Layout.W, Layout.BAR_Y - Layout.TOP, 1, new Color(255, 60, 40, 110));
                canvas.add(bossFlash);
                showAnnouncement(boss.getType().getDisplayName() + " appears!");
            }
            if (bossBar == null) {
                bossBar = new StatBar(130, Layout.TOP + 6, 460, 20, Ui.HP_RED);
                canvas.add(bossBar);
            }
            bossBar.setVisible(true);
            bossBar.show(boss.getHealth() / boss.getMaxHealth(), boss.getType().getDisplayName() + "  Lv." + boss.getEnemyLevel()
                    + "   " + (int) Math.ceil(boss.getHealth()) + " / " + (int) boss.getMaxHealth());
            bossBar.sendToFront();
        } else if (bossBar != null) {
            bossBar.setVisible(false);
        }
        if (shakeTicks > 0) {
            shakeTicks--;
            double amount = shakeTicks * 0.45;
            canvas.setLocation((int) (fxRandom.nextDouble() * amount - amount / 2),
                    (int) (fxRandom.nextDouble() * amount - amount / 2));
            if (bossFlash != null && shakeTicks % 3 == 0) {
                bossFlash.setFillColor(new Color(255, 60, 40, Math.max(0, shakeTicks * 6)));
                bossFlash.setColor(bossFlash.getFillColor());
            }
            if (shakeTicks == 0) {
                canvas.setLocation(0, 0);
                if (bossFlash != null) {
                    canvas.remove(bossFlash);
                    bossFlash = null;
                }
            }
        }
    }

    private void announceQuests() {
        List<game.Quest> done = party.getQuestsDone();
        while (questsShown < done.size()) {
            showAnnouncement("Request done: " + done.get(questsShown).getTitle() + "!");
            Sfx.play("levelup");
            questsShown++;
        }
    }

    private void showAnnouncement(String text) {
        Ui.recenter(levelUpLabel, text, Layout.W / 2.0);
        levelUpBack.setBounds(levelUpLabel.getX() - 16, Layout.TOP + 30, levelUpLabel.getWidth() + 32, 44);
        levelUpTimer = LEVEL_UP_TICKS;
    }

    @Override
    public void hide() {
        canvas.setLocation(0, 0);
        super.hide();
    }

    private void refreshStatusBar() {
        Avatar a = board.getAvatar();
        Hero sheet = a.getSheet();
        mainLevel.setLabel("Lv." + sheet.getLevel() + " " + sheet.getTitle());
        hpBar.show(a.getHealth() / a.getMaxHealth(), "HP " + (int) Math.ceil(a.getHealth()) + "/" + (int) a.getMaxHealth());
        expBar.show(sheet.getExpFraction(), "EXP " + Math.round(sheet.getExpFraction() * 100) + "%");
        double memberW = Math.min(50, (Layout.W - PARTY_X - 96) / Math.max(1, partyMembers.size()));
        for (int i = 0; i < partyMembers.size(); i++) {
            Hero h = party.hero(partyMembers.get(i));
            Ui.recenter(partyLevels.get(i), "Lv." + h.getLevel(), PARTY_X + i * memberW + memberW / 2);
            partyExp.get(i).setSize(Math.max(1, (memberW - 6) * h.getExpFraction()), 5);
        }
        creditsLabel.setLabel(String.valueOf(nav.getProfile().getCredits()));
    }

    private void drawIntro(Level level) {
        introBox = new GCompound();
        introBox.add(Ui.round(0, 0, 470, 84, 16, new Color(6, 6, 20, 225)));
        introBox.add(Ui.centered(level.getMap().getDisplayName() + ": " + level.getName(), "Serif-bold-24", Ui.GOLD, 235, 30));
        introBox.add(Ui.centered(level.getMap().getRule(),
                "SansSerif-plain-13", Ui.CHALK, 235, 52));
        boolean hasLadders = false;
        for (int r = 0; r < Board.ROWS; r++) {
            hasLadders |= Avatar.ladderWorks(level, r);
        }
        String controls = hasLadders ? "Arrows run and climb ladders. Z jumps, Down+Z drops. Space attacks."
                : "Arrows run. Z jumps. Space attacks.";
        introBox.add(Ui.centered(controls + " Enemies Lv." + level.getEnemyLevel() + ".",
                "SansSerif-bold-12", Ui.CHALK, 235, 72));
        canvas.add(introBox, (Layout.W - 470) / 2.0, Layout.TOP + 12);
    }

    private void addParticles(GameMap map) {
        for (int i = 0; i < PARTICLES; i++) {
            Particle p = new Particle(map, fxRandom);
            particles.add(p);
            canvas.add(p.shape);
        }
    }

    private void drawTopBar(Level level) {
        canvas.add(Ui.rect(0, 0, Layout.W, Layout.TOP, Ui.WOOD_DARK));
        canvas.add(Ui.rect(0, Layout.TOP - 2, Layout.W, 2, Ui.GOLD));

        canvas.add(Ui.round(8, 8, 80, 74, 12, Ui.PAPER));
        canvas.add(Assets.coffee(34), 31, 12);
        fpLabel = Ui.centered("0", "SansSerif-bold-22", Ui.INK, 48, 72);
        canvas.add(fpLabel);

        List<Card> cards = shop.getCards();
        for (int i = 0; i < slots; i++) {
            int x = (int) Math.round(CARD_X + i * cardStep);
            if (i >= cards.size()) {
                canvas.add(Ui.round(x, 6, cardW, CARD_H, 10, Ui.WOOD_DARK));
                cardSelect.add(null);
                cardCooldown.add(null);
                cardDim.add(null);
                continue;
            }
            DefenderType t = cards.get(i).getType();
            GRoundRect sel = Ui.round(x - 3, 3, cardW + 6, CARD_H + 6, 13, Ui.GOLD);
            sel.setVisible(false);
            canvas.add(sel);
            cardSelect.add(sel);
            canvas.add(Ui.round(x, 6, cardW, CARD_H, 10, Ui.PAPER));
            int spr = Math.min(50, cardW - 14);
            canvas.add(Assets.defender(t, spr), x + (cardW - spr) / 2.0, 6);
            canvas.add(Ui.centered(t.getDisplayName(), "SansSerif-bold-" + (cardW >= 68 ? 10 : cardW >= 58 ? 9 : 8),
                    Ui.INK, x + cardW / 2.0, 62));
            canvas.add(Ui.centered(String.valueOf(t.getCost()), "SansSerif-bold-" + (cardW >= 62 ? 13 : 12), Ui.MONEY,
                    x + cardW / 2.0, 77));
            GRoundRect dim = Ui.round(x, 6, cardW, CARD_H, 10, new Color(90, 90, 90, 130));
            canvas.add(dim);
            cardDim.add(dim);
            GRoundRect cd = Ui.round(x, 6, cardW, 0.1, 10, new Color(0, 0, 0, 140));
            canvas.add(cd);
            cardCooldown.add(cd);
        }

        canvas.add(Ui.text("Lv " + level.getNumber() + ": " + level.getName(), "SansSerif-bold-12",
                Ui.CHALK, 574, 54));
        canvas.add(Ui.round(574, 62, 136, 12, 6, new Color(60, 40, 28)));
        progressFill = Ui.rect(576, 64, 1, 8, new Color(120, 210, 110));
        canvas.add(progressFill);
        canvas.add(Ui.text("Enemies spawned", "SansSerif-plain-10", Ui.CHALK, 574, 86));
        musicButton = addButton("music", "Music", 568, 8, 52, 30, Ui.PAPER, Ui.INK, 10);
        soundButton = addButton("sound", "SFX", 624, 8, 38, 30, Ui.PAPER, Ui.INK, 10);
        addButton("pause", "II", 666, 8, 44, 30, Ui.PAPER, Ui.INK, 16);
        refreshToggles();
    }

    @Override
    public void update() {
        if (paused) {
            return;
        }
        shop.update();
        int move = (heldRight ? 1 : 0) - (heldLeft ? 1 : 0);
        int vertical = (heldDown ? 1 : 0) - (heldUp ? 1 : 0);
        board.getAvatar().command(move, vertical, heldAttack, jumpRequest);
        jumpRequest = false;
        board.update();
        refresh();
        if (board.isWon() || board.isLost()) {
            endTimer++;
            if (endTimer == 1) {
                showBanner(board.isWon() ? "LEVEL CLEARED!" : "THEY BROKE THROUGH!");
                Sfx.play(board.isWon() ? "win" : "lose");
                Music.stop();
            }
            if (endTimer >= END_DELAY_TICKS) {
                if (board.isWon()) {
                    nav.levelCleared(levelNumber);
                }
                nav.showResult(levelNumber, board.isWon(), loadout, party);
            }
        }
    }

    private void refreshToggles() {
        Color off = Ui.DISABLED;
        soundButton.setFill(Sfx.isMuted() ? off : Ui.PAPER);
        musicButton.setFill(Music.isMuted() ? off : Ui.PAPER);
    }

    private void showBanner(String text) {
        Ui.recenter(banner, text, Layout.W / 2.0);
        banner.setVisible(true);
        banner.sendToFront();
    }

    private void refresh() {
        Ui.recenter(fpLabel, String.valueOf(board.getFocusPoints()), 48);
        refreshCards();
        syncDefenders();
        syncEnemies();
        syncProjectiles();
        syncCoffees();
        syncEffects();
        syncDrops();
        syncPopups();
        syncLevelUps();
        refreshStatusBar();
        refreshBoss();
        announceQuests();
        refreshHover();
        for (Particle p : particles) {
            p.update();
        }
        avatarView.update(board.getTick());
        avatarView.sendToFront();
        if (avatarView.justSwung()) {
            Sfx.play("swing");
        }
        introBox.setVisible(board.getTick() < INTRO_TICKS);
        if (introBox.isVisible()) {
            introBox.sendToFront();
        }
        updatePoofs();
        updateFloaters();
        tooltip.sendToFront();
        for (int r = 0; r < Board.ROWS; r++) {
            if (highlighters[r] != null && board.isHighlighterUsed(r)) {
                highlighters[r].setFillColor(Ui.DISABLED);
                highlighters[r].setColor(Ui.DISABLED);
            }
        }
        progressFill.setSize(Math.max(1, 132 * board.getWaves().getProgress()), 8);

        int hugeTick = board.getWaves().getHugeWaveTick();
        int t = board.getTick();
        if (endTimer == 0) {
            if (t >= hugeTick - 90) {
                Music.play("hugewave");
            }
            if (t >= hugeTick - 90 && t < hugeTick + 60) {
                showBanner("A HUGE JUSTIN IS APPROACHING!");
            } else {
                banner.setVisible(false);
            }
        } else {
            banner.sendToFront();
        }
    }

    private void refreshCards() {
        List<Card> cards = shop.getCards();
        for (int i = 0; i < cards.size(); i++) {
            cardSelect.get(i).setVisible(shop.getSelected() == i);
            Card c = cards.get(i);
            cardCooldown.get(i).setSize(cardW, Math.max(0.1, CARD_H * c.getRechargeFraction()));
            cardDim.get(i).setVisible(c.isReady() && !shop.isUsable(i, board.getFocusPoints()));
        }
    }

    private void syncDefenders() {
        for (int r = 0; r < Board.ROWS; r++) {
            for (int c = 0; c < Board.COLS; c++) {
                Defender d = board.defenderAt(r, c);
                if (d != null && !defenderViews.containsKey(d)) {
                    DefenderView v = new DefenderView(d);
                    defenderViews.put(d, v);
                    canvas.add(v);
                }
            }
        }
        Iterator<Map.Entry<Defender, DefenderView>> it = defenderViews.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Defender, DefenderView> e = it.next();
            Defender d = e.getKey();
            if (board.defenderAt(d.getRow(), d.getCol()) != d) {
                boolean spent = d instanceof Bomb || d instanceof Trigger || d instanceof LaneClear;
                if (d.isDead() && !spent) {
                    dying.add(new Dying(e.getValue()));
                } else {
                    canvas.remove(e.getValue());
                }
                it.remove();
            } else {
                e.getValue().update(board.getTick());
            }
        }
    }

    private void syncEnemies() {
        for (Enemy e : board.getEnemies()) {
            if (!enemyViews.containsKey(e)) {
                EnemyView v = new EnemyView(e);
                enemyViews.put(e, v);
                canvas.add(v);
            }
        }
        Iterator<Map.Entry<Enemy, EnemyView>> it = enemyViews.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Enemy, EnemyView> e = it.next();
            if (!board.getEnemies().contains(e.getKey())) {
                EnemyView v = e.getValue();
                if (e.getKey().isDead()) {
                    dying.add(new Dying(v));
                    if (board.getTick() - lastPopSound >= 3) {
                        Sfx.play("pop");
                        lastPopSound = board.getTick();
                    }
                } else {
                    canvas.remove(v);
                }
                it.remove();
            } else {
                e.getValue().update();
            }
        }
    }

    private void syncProjectiles() {
        for (Projectile p : board.getProjectiles()) {
            if (!projectileViews.containsKey(p)) {
                ProjectileView v = new ProjectileView(p);
                projectileViews.put(p, v);
                canvas.add(v);
                if (board.getTick() - lastShotSound >= 5) {
                    Sfx.play("shoot");
                    lastShotSound = board.getTick();
                }
            }
        }
        Iterator<Map.Entry<Projectile, ProjectileView>> it = projectileViews.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Projectile, ProjectileView> e = it.next();
            if (!board.getProjectiles().contains(e.getKey())) {
                ProjectileView v = e.getValue();
                if (e.getKey().getX() <= Board.COLS) {
                    addBurst(v.impactX() + 6, v.impactY(), v.sparkColor(), 22, 7);
                }
                canvas.remove(v);
                it.remove();
            } else {
                e.getValue().update();
            }
        }
    }

    private void syncCoffees() {
        for (Coffee c : board.getCoffees()) {
            if (!coffeeViews.containsKey(c)) {
                GObject v = Assets.coffee(40);
                coffeeViews.put(c, v);
                canvas.add(v);
            }
        }
        Iterator<Map.Entry<Coffee, GObject>> it = coffeeViews.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Coffee, GObject> e = it.next();
            if (!board.getCoffees().contains(e.getKey())) {
                canvas.remove(e.getValue());
                it.remove();
            } else {
                Coffee c = e.getKey();
                double bob = Math.sin((board.getTick() + c.getX() * 13) * 0.12) * 3;
                e.getValue().setLocation(c.getX() * Layout.CELL - 20,
                        Layout.TOP + c.getY() * Layout.CELL - 20 + bob);
            }
        }
    }

    @Override
    protected void onButton(String id) {
        if ("pause".equals(id)) {
            setPaused(!paused);
        } else if ("sound".equals(id)) {
            Sfx.setMuted(!Sfx.isMuted());
            refreshToggles();
        } else if ("music".equals(id)) {
            Music.setMuted(!Music.isMuted());
            refreshToggles();
        } else if ("resume".equals(id)) {
            setPaused(false);
        } else if ("quit".equals(id)) {
            nav.saveProfile();
            nav.showLevelSelect();
        }
    }

    private void setPaused(boolean on) {
        paused = on;
        heldLeft = false;
        heldRight = false;
        heldAttack = false;
        heldUp = false;
        heldDown = false;
        if (on) {
            GRect shade = Ui.rect(0, 0, Layout.W, Layout.H, new Color(0, 0, 0, 150));
            canvas.add(shade);
            GLabel title = Ui.centered("PAUSED", "Serif-bold-48", Ui.CHALK, Layout.W / 2.0, 210);
            canvas.add(title);
            pauseObjects.add(shade);
            pauseObjects.add(title);
            for (Button b : new Button[] {
                addButton("resume", "Resume", Layout.W / 2.0 - 90, 236, 180, 44, Ui.GOLD, Ui.INK, 20),
                addButton("quit", "Quit level", Layout.W / 2.0 - 90, 292, 180, 44, Ui.PAPER, Ui.INK, 20)}) {
                pauseObjects.add(b.getShape());
            }
        } else {
            for (GObject o : pauseObjects) {
                canvas.remove(o);
            }
            pauseObjects.clear();
            buttons.removeIf(b -> "resume".equals(b.getId()) || "quit".equals(b.getId()));
        }
    }

    @Override
    protected void onClick(double x, double y) {
        if (paused || endTimer > 0) {
            return;
        }
        if (y >= Layout.BAR_Y) {
            return;
        }
        if (y < Layout.TOP) {
            int i = (int) Math.floor((x - CARD_X) / cardStep);
            double inCard = (x - CARD_X) - i * cardStep;
            if (i >= 0 && i < shop.getCards().size() && inCard <= cardW) {
                shop.select(i);
                Sfx.play("select");
            } else {
                shop.select(-1);
            }
            return;
        }
        for (Coffee c : new ArrayList<>(board.getCoffees())) {
            double dx = x - c.getX() * Layout.CELL;
            double dy = y - (Layout.TOP + c.getY() * Layout.CELL);
            if (Math.abs(dx) <= 30 && Math.abs(dy) <= 30) {
                board.collectCoffee(c);
                Sfx.play("coffee");
                return;
            }
        }
        int col = (int) (x / Layout.CELL);
        int row = (int) ((y - Layout.TOP) / Layout.CELL);
        if (shop.tryPlace(board, row, col)) {
            Sfx.play("place");
        }
    }

    @Override
    public void keyDown(int code) {
        if (paused || endTimer > 0) {
            return;
        }
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
            heldLeft = true;
        } else if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
            heldRight = true;
        } else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
            heldUp = true;
        } else if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
            heldDown = true;
        } else if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_X) {
            heldAttack = true;
        } else if (code == KeyEvent.VK_Z || code == KeyEvent.VK_ALT) {
            jumpRequest = true;
        }
    }

    @Override
    public void keyUp(int code) {
        if (code == KeyEvent.VK_LEFT || code == KeyEvent.VK_A) {
            heldLeft = false;
        } else if (code == KeyEvent.VK_RIGHT || code == KeyEvent.VK_D) {
            heldRight = false;
        } else if (code == KeyEvent.VK_UP || code == KeyEvent.VK_W) {
            heldUp = false;
        } else if (code == KeyEvent.VK_DOWN || code == KeyEvent.VK_S) {
            heldDown = false;
        } else if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_X) {
            heldAttack = false;
        }
    }

    @Override
    protected void onHover(double x, double y) {
        mouseX = x;
        mouseY = y;
    }

    private void refreshHover() {
        refreshGhost();
        tooltip.hide();
        if (paused || mouseY < 0) {
            return;
        }
        if (mouseY < Layout.TOP) {
            int i = (int) Math.floor((mouseX - CARD_X) / cardStep);
            if (i >= 0 && i < shop.getCards().size() && mouseX - CARD_X - i * cardStep <= cardW) {
                DefenderType t = shop.getCards().get(i).getType();
                Hero h = party.hero(t);
                tooltip.show(t.getDisplayName(), mouseX, mouseY, "Lv." + h.getLevel() + " " + h.getTitle(),
                        t.describe(), t.getCost() + " FP");
            }
            return;
        }
        if (mouseY >= Layout.BAR_Y) {
            return;
        }
        for (Enemy e : board.getEnemies()) {
            double ex = e.getX() * Layout.CELL;
            double ey = Layout.TOP + (e.getRow() + 0.5) * Layout.CELL;
            if (Math.hypot(mouseX - ex, mouseY - ey) < 34) {
                tooltip.show(e.getType().getDisplayName(), mouseX, mouseY, "Lv." + e.getEnemyLevel(),
                        "HP " + (int) Math.ceil(e.getHealth()) + "/" + (int) e.getMaxHealth());
                return;
            }
        }
    }

    private void refreshGhost() {
        int selected = shop.getSelected();
        if (selected != ghostCard) {
            if (ghost != null) {
                canvas.remove(ghost);
                ghost = null;
            }
            ghostCard = selected;
            if (selected >= 0) {
                ghost = Assets.defender(shop.getCards().get(selected).getType(), 66);
                canvas.add(ghost);
            }
        }
        int col = (int) Math.floor(mouseX / Layout.CELL);
        int row = (int) Math.floor((mouseY - Layout.TOP) / Layout.CELL);
        boolean onMap = selected >= 0 && !paused && col >= 0 && col < Board.COLS && row >= 0 && row < Board.ROWS
                && board.getLevel().hasLane(row);
        hoverTile.setVisible(onMap);
        if (ghost != null) {
            ghost.setVisible(onMap);
        }
        if (!onMap) {
            return;
        }
        boolean ok = board.terrainAt(row, col).isBuildable() && board.defenderAt(row, col) == null;
        Color c = ok ? new Color(90, 230, 120, 80) : new Color(240, 70, 60, 90);
        hoverTile.setFillColor(c);
        hoverTile.setColor(ok ? new Color(90, 230, 120) : new Color(240, 70, 60));
        hoverTile.setLocation(col * Layout.CELL, Layout.TOP + row * Layout.CELL);
        ghost.setLocation(col * Layout.CELL + 7, Layout.TOP + row * Layout.CELL + 8);
        ghost.sendToFront();
    }

    private void syncDrops() {
        for (Drop d : board.getDrops()) {
            if (!dropViews.containsKey(d)) {
                GObject v;
                if (d.getGear() != null) {
                    GCompound box = new GCompound();
                    Color c = Assets.rarityColor(d.getGear().getTier());
                    box.add(Ui.round(-4, -4, 38, 38, 19, new Color(c.getRed(), c.getGreen(), c.getBlue(), 110)));
                    box.add(Assets.gear(d.getGear(), 30));
                    v = box;
                } else {
                    GOval coin = new GOval(14, 14);
                    coin.setFilled(true);
                    coin.setFillColor(Ui.GOLD);
                    coin.setColor(new Color(160, 110, 20));
                    v = coin;
                }
                dropViews.put(d, v);
                canvas.add(v);
            }
        }
        Iterator<Map.Entry<Drop, GObject>> it = dropViews.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Drop, GObject> e = it.next();
            Drop d = e.getKey();
            if (!board.getDrops().contains(d)) {
                canvas.remove(e.getValue());
                it.remove();
                continue;
            }
            int age = d.getAge();
            double floorY = Layout.TOP + d.getRow() * Layout.CELL + Layout.CELL - 40;
            double hop = age < 14 ? -Math.sin(Math.PI * age / 14.0) * 34 : Math.sin(age * 0.12) * 2;
            double px = d.getX() * Layout.CELL;
            if (e.getValue() instanceof GOval) {
                double w = Math.max(3, 14 * Math.abs(Math.cos(age * 0.18)));
                ((GOval) e.getValue()).setBounds(px - w / 2, floorY + hop + 12, w, 14);
            } else {
                e.getValue().setLocation(px - 15, floorY + hop);
            }
        }
    }

    private void syncPopups() {
        for (Popup p : board.drainPopups()) {
            if (floaters.size() >= MAX_FLOATERS && p.getKind() == Popup.Kind.DAMAGE) {
                continue;
            }
            String text;
            String font = "SansSerif-bold-16";
            Color color;
            switch (p.getKind()) {
                case CRIT:
                    text = p.getAmount() + "!";
                    font = "SansSerif-bold-24";
                    color = new Color(255, 60, 70);
                    break;
                case MISS:
                    text = "MISS";
                    color = new Color(225, 225, 235);
                    break;
                case HURT:
                    text = "-" + p.getAmount();
                    font = "SansSerif-bold-20";
                    color = new Color(205, 90, 235);
                    Sfx.play("hit");
                    break;
                case COIN:
                    text = "+" + p.getAmount();
                    font = "SansSerif-bold-13";
                    color = Ui.GOLD;
                    Sfx.play("coin");
                    break;
                case SHOUT:
                    text = p.getText();
                    font = "Serif-bold-24";
                    color = new Color(255, 245, 120);
                    Sfx.play("zap");
                    break;
                case GEAR:
                    Gear g = p.getGear();
                    text = "Got " + g.getDisplayName() + "!";
                    color = Assets.rarityColor(g.getTier());
                    Sfx.play("coffee");
                    break;
                default:
                    text = String.valueOf(p.getAmount());
                    color = new Color(255, 160, 40);
                    break;
            }
            double px = p.getX() * Layout.CELL + (fxRandom.nextDouble() - 0.5) * 18;
            double py = Layout.TOP + p.getRow() * Layout.CELL + 18;
            Floater f = new Floater(text, font, color, px, py);
            floaters.add(f);
            canvas.add(f.shadow);
            canvas.add(f.label);
        }
    }

    private void updateFloaters() {
        Iterator<Floater> it = floaters.iterator();
        while (it.hasNext()) {
            Floater f = it.next();
            f.age++;
            if (f.age >= FLOATER_LIFE) {
                canvas.remove(f.shadow);
                canvas.remove(f.label);
                it.remove();
                continue;
            }
            f.label.move(0, -1);
            f.shadow.move(0, -1);
            f.label.sendToFront();
            if (f.age > FLOATER_LIFE / 2) {
                int alpha = 255 - 255 * (f.age - FLOATER_LIFE / 2) / (FLOATER_LIFE - FLOATER_LIFE / 2);
                Color c = f.color;
                f.label.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha));
                f.shadow.setColor(new Color(0, 0, 0, alpha / 2));
            }
        }
    }

    private void syncLevelUps() {
        for (Hero h : board.drainLevelUps()) {
            showAnnouncement("LEVEL UP! " + h.getType().getDisplayName() + " is Lv." + h.getLevel());
            Sfx.play("levelup");
        }
        if (levelUpTimer > 0) {
            levelUpTimer--;
            levelUpBack.setVisible(true);
            levelUpBack.sendToFront();
            levelUpLabel.setVisible(true);
            levelUpLabel.sendToFront();
        } else {
            levelUpBack.setVisible(false);
            levelUpLabel.setVisible(false);
        }
    }

    /** A number or word rising from where something happened. */
    private static final class Floater {
        private final GLabel label;
        private final GLabel shadow;
        private final Color color;
        private int age;

        Floater(String text, String font, Color color, double cx, double y) {
            this.color = color;
            this.shadow = Ui.centered(text, font, new Color(0, 0, 0, 130), cx + 1.5, y + 1.5);
            this.label = Ui.centered(text, font, color, cx, y);
        }
    }

    /** A speck of the map's atmosphere, drifting across the scene. */
    private static final class Particle {
        private final GObject shape;
        private final Random rng;
        private final double minY = Layout.TOP;
        private final double maxY = Layout.BAR_Y;
        private final double vx;
        private final double vy;
        private double x;
        private double y;
        private int age;

        Particle(GameMap map, Random rng) {
            this.rng = rng;
            Color c;
            double size = 3 + rng.nextDouble() * 3;
            double driftX = (rng.nextDouble() - 0.5) * 0.5;
            double driftY;
            switch (map) {
                case HALLWAY:
                    c = new Color(250, 250, 240, 200);
                    driftY = 0.5 + rng.nextDouble() * 0.5;
                    driftX += 0.6;
                    break;
                case CAFETERIA:
                    c = new Color(255, 255, 255, 110);
                    driftY = -0.3 - rng.nextDouble() * 0.4;
                    size += 4;
                    break;
                case LIBRARY:
                    c = new Color(255, 220, 140, 130);
                    driftY = -0.1 - rng.nextDouble() * 0.15;
                    break;
                case GYM:
                    Color[] party = {new Color(240, 80, 80), new Color(80, 150, 240), new Color(250, 210, 60),
                        new Color(110, 200, 110)};
                    c = party[rng.nextInt(party.length)];
                    driftY = 0.7 + rng.nextDouble() * 0.6;
                    break;
                case ADMISSIONS:
                    c = new Color(255, 225, 120, 190);
                    driftY = 0.25 + rng.nextDouble() * 0.3;
                    size -= 1;
                    break;
                default:
                    c = new Color(255, 255, 255, 120);
                    driftY = 0.15 + rng.nextDouble() * 0.25;
                    break;
            }
            this.vx = driftX;
            this.vy = driftY;
            this.x = rng.nextDouble() * Layout.W;
            this.y = minY + rng.nextDouble() * (maxY - minY);
            GObject s;
            if (map == GameMap.HALLWAY || map == GameMap.GYM) {
                s = Ui.rect(0, 0, size + 2, size, c);
            } else {
                GOval o = new GOval(size, size);
                o.setFilled(true);
                o.setColor(c);
                s = o;
            }
            this.shape = s;
            shape.setLocation(x, y);
        }

        void update() {
            age++;
            x += vx + Math.sin(age * 0.05) * 0.2;
            y += vy;
            if (y > maxY - 6) {
                y = minY;
                x = rng.nextDouble() * Layout.W;
            } else if (y < minY) {
                y = maxY - 6;
                x = rng.nextDouble() * Layout.W;
            }
            if (x > Layout.W) {
                x = 0;
            } else if (x < 0) {
                x = Layout.W;
            }
            shape.setLocation(x, y);
        }
    }

    private void syncEffects() {
        for (Effect e : board.getEffects()) {
            if (!effectViews.containsKey(e)) {
                GObject v = makeEffect(e);
                effectViews.put(e, v);
                canvas.add(v);
                String sound = soundFor(e.getKind());
                if (sound != null) {
                    Sfx.play(sound);
                }
            }
        }
        Iterator<Map.Entry<Effect, GObject>> it = effectViews.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Effect, GObject> e = it.next();
            if (!board.getEffects().contains(e.getKey())) {
                canvas.remove(e.getValue());
                it.remove();
            } else {
                Color c = effectColor(e.getKey());
                ((GFillable) e.getValue()).setFillColor(c);
                e.getValue().setColor(c);
            }
        }
    }

    private GObject makeEffect(Effect e) {
        double cx = (e.getCol() + 0.5) * Layout.CELL;
        double cy = Layout.TOP + (e.getRow() + 0.5) * Layout.CELL;
        double reach = (e.getRadius() + 0.5) * Layout.CELL;
        GObject v;
        switch (e.getKind()) {
            case LANE:
                v = new GRect(0, Layout.TOP + e.getRow() * Layout.CELL + 8, Layout.W, Layout.CELL - 16);
                break;
            case LEVEL_UP:
                v = new GRect(cx - 34, Layout.TOP + e.getRow() * Layout.CELL - 16, 68, Layout.CELL + 12);
                break;
            case SWIPE:
                v = new GRect((e.getCol() + 1) * Layout.CELL, Layout.TOP + e.getRow() * Layout.CELL + 14,
                        e.getRadius() * Layout.CELL, Layout.CELL - 28);
                break;
            case SNIPE:
                v = new GOval(cx - 22, cy - 22, 44, 44);
                break;
            case BOLT:
                v = new GRect(cx - 6, Layout.TOP, 12, cy - Layout.TOP + 20);
                break;
            default:
                v = new GOval(cx - reach, cy - reach, reach * 2, reach * 2);
                break;
        }
        ((GFillable) v).setFilled(true);
        return v;
    }

    private static String soundFor(Effect.Kind kind) {
        switch (kind) {
            case BLAST:
                return "boom";
            case HEAL:
                return "heal";
            case SWIPE:
            case SNIPE:
                return "hit";
            case LEVEL_UP:
                return null;
            default:
                return "zap";
        }
    }

    private Color effectColor(Effect e) {
        int a = (int) (190 * e.getFraction());
        switch (e.getKind()) {
            case LANE:
                return new Color(255, 225, 70, a);
            case SWIPE:
                return new Color(255, 170, 60, a);
            case SNIPE:
                return new Color(230, 50, 50, a);
            case BOLT:
                return new Color(255, 250, 150, a);
            case LEVEL_UP:
                return new Color(255, 225, 90, (int) (200 * e.getFraction()));
            case HEAL:
                return new Color(90, 220, 120, (int) (150 * e.getFraction()));
            case STUN:
                return new Color(255, 235, 90, a);
            default:
                return new Color(255, 140, 40, a);
        }
    }

    /** A short expanding burst: a puff where something was defeated or a spark where a shot landed. */
    private static final class Poof {
        private final GOval oval;
        private final double cx;
        private final double cy;
        private final Color color;
        private final double maxR;
        private final int maxLife;
        private int life;

        Poof(GOval oval, double cx, double cy, Color color, double maxR, int life) {
            this.oval = oval;
            this.cx = cx;
            this.cy = cy;
            this.color = color;
            this.maxR = maxR;
            this.maxLife = life;
            this.life = life;
        }
    }

    /** A defeated character playing its hop-and-flatten animation before it is removed. */
    private static final class Dying {
        private final SpriteView view;
        private int age;

        Dying(SpriteView view) {
            this.view = view;
        }
    }

    private void addBurst(double cx, double cy, Color c, double maxR, int life) {
        GOval o = new GOval(cx - 4, cy - 4, 8, 8);
        o.setFilled(true);
        o.setFillColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 200));
        o.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 0));
        poofs.add(new Poof(o, cx, cy, c, maxR, life));
        canvas.add(o);
    }

    private void updatePoofs() {
        Iterator<Dying> dit = dying.iterator();
        while (dit.hasNext()) {
            Dying d = dit.next();
            if (!d.view.playDeath(d.age++)) {
                addBurst(d.view.centerX(), d.view.centerY() + 14, Color.WHITE, 40, 10);
                canvas.remove(d.view);
                dit.remove();
            }
        }
        Iterator<Poof> it = poofs.iterator();
        while (it.hasNext()) {
            Poof p = it.next();
            p.life--;
            if (p.life <= 0) {
                canvas.remove(p.oval);
                it.remove();
                continue;
            }
            double f = 1.0 - (double) p.life / p.maxLife;
            double r = 4 + (p.maxR - 4) * f;
            p.oval.setBounds(p.cx - r, p.cy - r, r * 2, r * 2);
            p.oval.setFillColor(new Color(p.color.getRed(), p.color.getGreen(), p.color.getBlue(),
                    (int) (200 * (1.0 - f))));
        }
    }

    /**
     * Gets the board, for tests and debugging.
     *
     * @return the board being played
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Gets the shop, for tests and debugging.
     *
     * @return the shop bar
     */
    public Shop getShop() {
        return shop;
    }
}
