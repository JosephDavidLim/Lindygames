package view;

import acm.graphics.GCanvas;
import acm.graphics.GCompound;

import entities.DefenderType;
import game.GameMap;
import game.Gear;
import game.Hero;
import game.Job;
import game.Levels;
import game.Profile;
import game.Stat;

import java.awt.Color;
import java.util.List;

/**
 * The character window: pick a classmate, spend stat points, swap gear and
 * choose who the player steers.
 */
public class SquadScreen extends Screen {
    private static final double TILE_W = 58;
    private static final double TILE_H = 68;
    private static final double GRID_X = 18;
    private static final double GRID_Y = 44;
    private static final int PER_ROW = 6;
    private static final double SLOT_Y = 288;
    private static final double SLOT_W = 174;
    private static final double SLOT_H = 72;

    private final int returnLevel;
    private final Profile profile;
    private final List<DefenderType> roster;
    private DefenderType selected;
    private Tooltip tooltip;

    /**
     * Creates the window.
     *
     * @param canvas the canvas to draw on
     * @param nav the navigator
     * @param returnLevel level whose loadout to return to, or 0 for the world map
     */
    public SquadScreen(GCanvas canvas, Navigator nav, int returnLevel) {
        super(canvas, nav);
        this.returnLevel = returnLevel;
        this.profile = nav.getProfile();
        this.roster = Levels.unlockedBefore(profile.getMaxUnlocked());
        this.selected = roster.contains(profile.getMain()) ? profile.getMain() : roster.get(0);
    }

    @Override
    public void show() {
        Music.play("title");
        redraw();
    }

    private void redraw() {
        canvas.removeAll();
        buttons.clear();
        fillMapBackground(GameMap.CLASSROOM);
        drawRoster();
        drawGear();
        drawStats();
        addButton("back", "Back", 20, Layout.H - 44, 100, 34, Ui.PAPER, Ui.INK, 15);
        addButton("store", "Black Market", 130, Layout.H - 44, 110, 34, Ui.GOLD, Ui.INK, 12);
        Button all = addButton("autoall", "Auto-assign all", 248, Layout.H - 44, 130, 34, Ui.GOLD, Ui.INK, 12);
        boolean waiting = profile.hasUnspentAp();
        all.setEnabled(waiting, waiting ? Ui.GOLD : Ui.DISABLED);
        tooltip = new Tooltip();
        canvas.add(tooltip);
    }

    private void drawRoster() {
        canvas.add(Ui.window("Squad", 8, 8, 372, 268));
        for (int i = 0; i < roster.size(); i++) {
            DefenderType t = roster.get(i);
            Hero h = profile.hero(t);
            double x = GRID_X + (i % PER_ROW) * (TILE_W - 0.5);
            double y = GRID_Y + (i / PER_ROW) * (TILE_H + 8);
            canvas.add(Ui.round(x, y, TILE_W - 4, TILE_H, 8, t == selected ? Ui.SELECTED : Ui.PAPER));
            canvas.add(Assets.defender(t, 46), x + 4, y + 2);
            canvas.add(Ui.centered("Lv." + h.getLevel(), "SansSerif-bold-11", Ui.INK, x + (TILE_W - 4) / 2, y + 62));
            if (h.getAp() > 0 || h.canAdvance()) {
                canvas.add(Ui.round(x + TILE_W - 20, y + 2, 15, 15, 8, Ui.HP_RED));
                canvas.add(Ui.centered("!", "SansSerif-bold-12", Color.WHITE, x + TILE_W - 12.5, y + 14));
            }
            if (t == profile.getMain()) {
                canvas.add(Ui.round(x + 2, y + 2, 26, 13, 6, Ui.GOLD));
                canvas.add(Ui.centered("YOU", "SansSerif-bold-9", Color.WHITE, x + 15, y + 12));
            }
        }
    }

    private void drawGear() {
        Hero h = profile.hero(selected);
        canvas.add(Ui.window("Equipment: click a slot to swap", 8, 280, 372, 216));
        Gear.Slot[] slots = Gear.Slot.values();
        for (int i = 0; i < slots.length; i++) {
            double x = 16 + (i % 2) * (SLOT_W + 4);
            double y = SLOT_Y + 26 + (i / 2) * (SLOT_H + 8);
            Gear worn = h.getEquipped(slots[i]);
            addButton("slot" + i, "", x, y, SLOT_W, SLOT_H, Ui.PAPER, Ui.INK, 12);
            canvas.add(Ui.text(slots[i].getLabel(), "SansSerif-bold-11", Ui.MUTED, x + 62, y + 16));
            if (worn != null) {
                canvas.add(Assets.gear(worn, 50), x + 7, y + 11);
                canvas.add(Ui.text(worn.getDisplayName(), "SansSerif-bold-11", Assets.rarityColor(worn.getTier()),
                        x + 62, y + 34));
                canvas.add(Ui.text(worn.describe(), "SansSerif-plain-12", Ui.INK, x + 62, y + 52));
            } else {
                canvas.add(Ui.text("Empty", "SansSerif-plain-12", Ui.MUTED, x + 62, y + 34));
            }
        }
        int setTier = h.setTier();
        String setLine = setTier < 0 ? "Wear 4 items of one tier for a set bonus."
                : Gear.setName(setTier) + ": +" + Math.round(h.setBonus() * 100) + "% power!";
        canvas.add(Ui.text(setLine, "SansSerif-bold-12", setTier < 0 ? Ui.MUTED : Ui.GOOD, 18, 484));
    }

    private void drawStats() {
        Hero h = profile.hero(selected);
        double wx = 388;
        canvas.add(Ui.window("Character", wx, 8, 324, 538));
        canvas.add(Assets.defender(selected, 92), wx + 12, 44);
        canvas.add(Ui.text(selected.getDisplayName(), "Serif-bold-20", Ui.INK, wx + 112, 66));
        canvas.add(Ui.text("Lv." + h.getLevel() + " " + h.getTitle(), "SansSerif-bold-14", Ui.GOLD, wx + 112, 86));
        canvas.add(Ui.text(selected.describe(), "SansSerif-plain-12", Ui.INK, wx + 112, 104));
        String expText = h.getLevel() >= Hero.MAX_LEVEL ? "MAX" : h.getExp() + " / " + Hero.expToNext(h.getLevel());
        canvas.add(Ui.bar(wx + 112, 112, 196, 16, h.getExpFraction(), new Color(190, 110, 255), "EXP " + expText));
        if (selected == profile.getMain()) {
            canvas.add(Ui.text("You play as this classmate.", "SansSerif-bold-11", Ui.GOLD, wx + 112, 146));
        } else {
            addButton("main", "Play as this classmate", wx + 112, 132, 196, 24, Ui.PAPER, Ui.INK, 12);
        }

        canvas.add(Ui.text(selected.construct(), "SansSerif-bold-11", Ui.PINK, wx + 16, 164));
        canvas.add(Ui.text("Your attack: " + selected.heroMove(), "SansSerif-plain-11", Ui.MONEY, wx + 16, 178));
        canvas.add(Ui.text("Stat points to spend: " + h.getAp(), "SansSerif-bold-14",
                h.getAp() > 0 ? Ui.PINK : Ui.INK, wx + 16, 194));
        Stat[] stats = Stat.values();
        for (int i = 0; i < stats.length; i++) {
            double y = 200 + i * 43;
            Stat s = stats[i];
            int gearBonus = h.total(s) - h.getSpent(s);
            canvas.add(Ui.round(wx + 14, y, 296, 40, 8, Ui.PAPER));
            canvas.add(Ui.text(s.getLabel(), "SansSerif-bold-15", Ui.INK, wx + 24, y + 26));
            canvas.add(Ui.text(h.getSpent(s) + (gearBonus > 0 ? " (+" + gearBonus + ")" : ""), "SansSerif-bold-14",
                    Ui.GOLD, wx + 66, y + 26));
            canvas.add(Ui.text(s.getEffect(), "SansSerif-plain-11", Ui.INK, wx + 126, y + 26));
            Button plus = addButton("stat" + i, "+", wx + 272, y + 6, 30, 28, Ui.GOLD, Ui.INK, 18);
            plus.setEnabled(h.getAp() > 0, h.getAp() > 0 ? Ui.GOLD : Ui.DISABLED);
        }
        Button auto = addButton("auto", "Auto-assign", wx + 14, 374, 144, 30, Ui.GOLD, Ui.INK, 13);
        auto.setEnabled(h.getAp() > 0, h.getAp() > 0 ? Ui.GOLD : Ui.DISABLED);
        int cost = h.getLevel() * Hero.RESET_COST_PER_LEVEL;
        Button reset = addButton("reset", "Reset (" + cost + " Credits)", wx + 166, 374, 144, 30, Ui.PAPER, Ui.INK, 12);
        reset.setEnabled(profile.getCredits() >= cost, profile.getCredits() >= cost ? Ui.PAPER : Ui.DISABLED);

        drawJob(h, wx);
        GCompound totals = new GCompound();
        String[] lines = {
            "Power  +" + percent(h.powerMultiplier()) + "%     Action speed  +" + percent(h.speedMultiplier()) + "%",
            "Card recharge  +" + percent(h.rechargeMultiplier()) + "%     Health  +" + percent(h.healthMultiplier()) + "%",
            "Crit chance  " + Math.round(h.critChance() * 100) + "%     Dodge chance  " + Math.round(h.dodgeChance() * 100) + "%",
            "Credits: " + profile.getCredits()
        };
        for (int i = 0; i < lines.length; i++) {
            totals.add(Ui.text(lines[i], "SansSerif-bold-12", i == 3 ? Ui.MONEY : Ui.INK, 0, i * 18));
        }
        canvas.add(totals, wx + 18, 420);
    }

    private void drawJob(Hero h, double wx) {
        if (h.getJob() != null) {
            canvas.add(Ui.text("Construct: " + h.getJob().getDisplayName(), "SansSerif-bold-14", Ui.GOLD, wx + 18, 510));
            canvas.add(Ui.text(h.getJob().getDescription(), "SansSerif-plain-12", Ui.INK, wx + 18, 528));
        } else if (h.canAdvance()) {
            canvas.add(Ui.text("Choose a second construct! Hover for details.", "SansSerif-bold-12", Ui.PINK, wx + 18, 498));
            Job[] options = Job.optionsFor(selected);
            for (int i = 0; i < options.length; i++) {
                addButton("job" + options[i].ordinal(), options[i].getDisplayName(), wx + 14 + i * 152, 504, 146, 32,
                        Ui.GOLD, Ui.INK, 14);
            }
        } else {
            canvas.add(Ui.text("Second construct at Lv." + Job.ADVANCE_LEVEL + ":", "SansSerif-bold-12", Ui.INK, wx + 18, 510));
            Job[] options = Job.optionsFor(selected);
            canvas.add(Ui.text(options[0].getDisplayName() + " or " + options[1].getDisplayName(), "SansSerif-plain-12", Ui.INK,
                    wx + 18, 528));
        }
    }

    private static long percent(double multiplier) {
        return Math.round((multiplier - 1) * 100);
    }

    @Override
    protected void onHover(double x, double y) {
        if (tooltip == null) {
            return;
        }
        tooltip.hide();
        Gear.Slot[] slots = Gear.Slot.values();
        for (Button b : buttons) {
            if (b.getId().startsWith("job") && b.contains(x, y)) {
                Job j = Job.values()[Integer.parseInt(b.getId().substring(3))];
                tooltip.show(j.getDisplayName(), x, y, j.getDescription());
                tooltip.sendToFront();
                return;
            }
            if (b.getId().startsWith("slot") && b.contains(x, y)) {
                Gear.Slot slot = slots[Integer.parseInt(b.getId().substring(4))];
                int held = 0;
                for (Gear g : Gear.values()) {
                    if (g.getSlot() == slot) {
                        held += profile.countOf(g);
                    }
                }
                tooltip.show(slot.getLabel() + " slot", x, y, "Boosts " + slot.getStat().getLabel(), held + " spare in your bag");
                tooltip.sendToFront();
                return;
            }
        }
    }

    @Override
    protected void onClick(double x, double y) {
        for (int i = 0; i < roster.size(); i++) {
            double tx = GRID_X + (i % PER_ROW) * (TILE_W - 0.5);
            double ty = GRID_Y + (i / PER_ROW) * (TILE_H + 8);
            if (x >= tx && x <= tx + TILE_W - 4 && y >= ty && y <= ty + TILE_H) {
                selected = roster.get(i);
                Sfx.play("select");
                redraw();
                return;
            }
        }
    }

    @Override
    protected void onButton(String id) {
        Hero h = profile.hero(selected);
        if ("back".equals(id)) {
            nav.saveProfile();
            if (returnLevel > 0) {
                nav.showLoadout(returnLevel);
            } else {
                nav.showLevelSelect();
            }
            return;
        } else if ("store".equals(id)) {
            nav.saveProfile();
            nav.showStore(returnLevel);
            return;
        } else if ("main".equals(id)) {
            profile.setMain(selected);
        } else if (id.startsWith("job")) {
            h.advance(Job.values()[Integer.parseInt(id.substring(3))]);
            Sfx.play("levelup");
        } else if ("autoall".equals(id)) {
            profile.autoAssignAll();
        } else if ("auto".equals(id)) {
            h.autoAssign();
        } else if ("reset".equals(id)) {
            profile.resetStats(h);
        } else if (id.startsWith("stat")) {
            h.spend(Stat.values()[Integer.parseInt(id.substring(4))]);
        } else if (id.startsWith("slot")) {
            profile.cycleSlot(h, Gear.Slot.values()[Integer.parseInt(id.substring(4))]);
        }
        Sfx.play("select");
        nav.saveProfile();
        redraw();
    }
}
